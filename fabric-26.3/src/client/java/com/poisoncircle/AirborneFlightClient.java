package com.poisoncircle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Presentation and real key input only; flight movement and landing remain server-authoritative. */
public final class AirborneFlightClient {
    private static JsonObject state = new JsonObject();
    private static int ticksSinceSend = 5;
    private static AirborneInputPayload previous;
    private static Object level;
    private static boolean installed;
    private static long receivedNanos;

    private AirborneFlightClient() { }

    public static void install() {
        if (installed) return;
        installed = true;
        EntityRendererRegistry.register(AirborneGame.PLANE_TYPE, AirbornePlaneRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(AirborneFlightClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "airborne_flight"),
            (gui, delta) -> renderHud(gui));
    }

    /** Called by the sole AirborneStatePayload receiver in AirborneAdminClient. */
    public static void acceptState(JsonObject snapshot) {
        state = snapshot.deepCopy();
        level = Minecraft.getInstance().level;
        receivedNanos = System.nanoTime();
    }

    private static void clear() {
        state = new JsonObject();
        previous = null;
        ticksSinceSend = 5;
        level = null;
    }

    private static JsonObject localState() {
        return object(state, "self") != null ? object(state, "self") : state;
    }

    public static String phase() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.level != level || !client.player.isAlive()
            || System.nanoTime() - receivedNanos > 2_000_000_000L) return "none";
        String phase = text(localState(), "phase", "none").toLowerCase(Locale.ROOT);
        return switch (phase) {
            case "flight", "flying", "plane" -> "aboard";
            case "dive" -> "diving";
            case "landed", "done", "idle" -> "none";
            default -> phase;
        };
    }

    public static boolean suppressVanillaMovement() {
        String phase = phase();
        return phase.equals("aboard") || phase.equals("departing") || phase.equals("diving") || phase.equals("landing");
    }

    /** Replaces vanilla gravity/drag only while the server owns local airborne motion. */
    public static boolean moveControlledPlayer(net.minecraft.world.entity.LivingEntity entity) {
        Minecraft client = Minecraft.getInstance();
        if (entity != client.player || !suppressVanillaMovement()) return false;
        if (phase().equals("aboard") || phase().equals("departing")) return true;
        if (entity.isPassenger()) return false;
        JsonObject local = localState();
        if (!local.has("vx") || !local.has("vy") || !local.has("vz")) return false;
        double vx = number(local, "vx", 0), vy = number(local, "vy", 0), vz = number(local, "vz", 0);
        net.minecraft.world.phys.Vec3 velocity = new net.minecraft.world.phys.Vec3(vx, vy, vz);
        double age = Math.min(5, Math.max(0, (System.nanoTime() - receivedNanos) / 50_000_000.0));
        if (local.has("x") && local.has("y") && local.has("z")) {
            net.minecraft.world.phys.Vec3 expected = new net.minecraft.world.phys.Vec3(number(local, "x", entity.getX()),
                number(local, "y", entity.getY()), number(local, "z", entity.getZ())).add(velocity.scale(age));
            net.minecraft.world.phys.Vec3 error = expected.subtract(entity.position());
            // A collision-tested bounded correction avoids snapshot teleports through terrain.
            velocity = velocity.add(error.normalize().scale(Math.min(.25, error.length() * .18)));
        }
        entity.setDeltaMovement(velocity);
        entity.move(net.minecraft.world.entity.MoverType.SELF, velocity);
        return true;
    }

    /** Remote animation is used only if the server explicitly publishes its entity phase list. */
    public static String phaseFor(int entityId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.getId() == entityId) return phase();
        if (client.level != level || System.nanoTime() - receivedNanos > 2_000_000_000L) return "none";
        JsonElement poses = state.get("poses");
        if (poses != null && poses.isJsonArray()) for (JsonElement entry : poses.getAsJsonArray()) {
            if (entry.isJsonObject() && number(entry.getAsJsonObject(), "entityId", -1) == entityId)
                return text(entry.getAsJsonObject(), "phase", "none").toLowerCase(Locale.ROOT);
        }
        return "none";
    }

    /** Server timeline progress, extrapolated only over its four-tick snapshot interval. */
    public static float standProgressFor(int entityId) {
        String phase=phaseFor(entityId);
        if(phase.equals("aboard"))return 0;
        if(!phase.equals("departing"))return 1;
        JsonObject pose=null;
        Minecraft client=Minecraft.getInstance();
        if(client.player!=null&&client.player.getId()==entityId)pose=localState();
        else if(state.has("poses")&&state.get("poses").isJsonArray())for(JsonElement entry:state.getAsJsonArray("poses"))
            if(entry.isJsonObject()&&number(entry.getAsJsonObject(),"entityId",-1)==entityId){pose=entry.getAsJsonObject();break;}
        if(pose==null)return 1;
        double age=Math.min(4,Math.max(0,(System.nanoTime()-receivedNanos)/50_000_000.0));
        return (float)Math.max(0,Math.min(1,number(pose,"standProgress",1)+age/AirborneDeparture.STAND_TICKS));
    }

    private static void tick(Minecraft client) {
        renderPointMarkers(client);
        if (client.level == null || client.player == null || client.level != level) {
            clear();
            return;
        }
        if (!suppressVanillaMovement() || !ClientPlayNetworking.canSend(AirborneInputPayload.TYPE)) {
            previous = null;
            ticksSinceSend = 5;
            return;
        }
        boolean gameplay = client.gui.screen() == null && client.player.isAlive() && !client.player.isSpectator();
        float forward = gameplay ? axis(client.options.keyUp.isDown(), client.options.keyDown.isDown()) : 0;
        float strafe = gameplay ? axis(client.options.keyLeft.isDown(), client.options.keyRight.isDown()) : 0;
        boolean exit = false; // Leaving the carrier is decided by the server at the cargo door.
        AirborneInputPayload input = new AirborneInputPayload(forward, strafe, Mth.wrapDegrees(client.player.getYRot()),
            Mth.clamp(client.player.getXRot(), -90, 90), exit);
        if (++ticksSinceSend >= 5 || changed(input, previous)) {
            ClientPlayNetworking.send(input);
            previous = input;
            ticksSinceSend = 0;
        }
    }

    private static void renderPointMarkers(Minecraft client) {
        if (client.level == null || client.player == null || client.level.getGameTime() % 10 != 0) return;
        boolean hasSetter = client.player.getInventory().contains(stack -> stack.is(AirborneGame.SETTER));
        if (!hasSetter) return;
        var markers = AirborneClientRules.markers(AirborneAdminClient.snapshot(), hasSetter,
            client.level.dimension().identifier().toString(), client.player.getX(), client.player.getY(), client.player.getZ());
        // At most eight points, each with a 12-particle ring and a six-particle column every half second.
        for (var marker : markers) {
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6;
                client.level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    marker.x() + Math.cos(angle) * .8, marker.y() + .12, marker.z() + Math.sin(angle) * .8, 0, .005, 0);
            }
            for (int i = 0; i < 6; i++) client.level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                marker.x(), marker.y() + i * .6, marker.z(), 0, .005, 0);
        }
    }

    static float axis(boolean positive, boolean negative) { return (positive ? 1 : 0) - (negative ? 1 : 0); }

    static boolean changed(AirborneInputPayload next, AirborneInputPayload last) {
        return last == null || next.forward() != last.forward() || next.strafe() != last.strafe()
            || next.jump() != last.jump() || Math.abs(Mth.wrapDegrees(next.yaw() - last.yaw())) >= 2
            || Math.abs(next.pitch() - last.pitch()) >= 2;
    }

    private static void renderHud(GuiGraphicsExtractor gui) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null || !phase().equals("countdown")) return;
        int countdown = Math.max(1, (int)Math.ceil(number(state, "countdown", 0)));
        String title = "空降准备【" + countdown + "】", hint = "即将登机";
        int width = Math.max(client.font.width(title), client.font.width(hint)) + 24;
        int left = (gui.guiWidth() - width) / 2, top = Math.max(28, gui.guiHeight() / 7);
        gui.fill(left, top, left + width, top + 43, 0xC9101C29);
        gui.fill(left, top, left + 3, top + 43, 0xFF65D9EF);
        gui.text(client.font, title, left + 12, top + 8, 0xFFFFFFFF, true);
        gui.text(client.font, hint, left + 12, top + 25, 0xFFBBD7E5, true);
    }

    private static JsonObject object(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static String text(JsonObject object, String key, String fallback) {
        try { return object.has(key) ? object.get(key).getAsString() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static double number(JsonObject object, String key, double fallback) {
        try {
            double value = object.has(key) ? object.get(key).getAsDouble() : fallback;
            return Double.isFinite(value) ? value : fallback;
        } catch (RuntimeException ignored) { return fallback; }
    }
}
