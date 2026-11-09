package com.poisoncircle;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

public final class PoisonCircleClient implements ClientModInitializer {
    private static final Map<Identifier, TimedCircle> CIRCLES = new HashMap<>();
    private static final Map<Identifier, DetectorRevealPayload> DETECTOR_REVEALS = new HashMap<>();
    private static long shakeStartedNanos = Long.MIN_VALUE;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(CircleSyncPayload.TYPE, (payload, context) -> apply(payload));
        ClientPlayNetworking.registerGlobalReceiver(DetectorRevealPayload.TYPE, (payload, context) -> applyDetectorReveal(payload));
        ClientPlayNetworking.registerGlobalReceiver(PoisonHitPayload.TYPE, (payload, context) -> shakeFromPoisonHit());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CIRCLES.clear();
            DETECTOR_REVEALS.clear();
            shakeStartedNanos = Long.MIN_VALUE;
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() && player.getItemInHand(hand).is(PoisonCircleMod.CONTROLLER)) {
                Minecraft client = Minecraft.getInstance();
                client.execute(() -> client.gui.setScreen(new PoisonCircleControlScreen()));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "circle_status"),
            (gui, delta) -> renderStatus(gui));
        LevelRenderEvents.COLLECT_SUBMITS.register(PoisonCircleVisualRenderer::submit);
        XaeroCircleCompatibility.install();
        ScanDeviceClient.install();
        LobbyClient.install();
        AirborneAdminClient.install();
        AirborneFlightClient.install();
        AirbornePlaneSounds.install();
        CircleAudioClient.install();
        ShrinkWarningClient.install();
    }

    public static void apply(CircleSyncPayload payload) {
        CircleAudioClient.apply(payload);
        if (payload.active()) {
            CIRCLES.put(payload.dimension(), new TimedCircle(payload, System.nanoTime()));
        } else {
            CIRCLES.remove(payload.dimension());
            DETECTOR_REVEALS.remove(payload.dimension());
        }
    }

    public static CircleSyncPayload currentCircle() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return null;
        TimedCircle timed = CIRCLES.get(client.level.dimension().identifier());
        if (timed == null) return null;
        CircleSyncPayload payload = timed.payload();
        if (payload.waiting() || !payload.hasNext() || client.isPaused()) return payload;
        int elapsed = (int) Math.min(payload.remainingTicks(), Math.max(0,
            (System.nanoTime() - timed.receivedNanos()) / 50_000_000L));
        CirclePrediction.State state = new CirclePrediction.State(payload.currentX(), payload.currentZ(), payload.currentRadius(),
            payload.nextX(), payload.nextZ(), payload.nextRadius(), payload.remainingTicks());
        CirclePrediction.State predicted = CirclePrediction.advance(state, elapsed);
        int remaining = predicted.remainingTicks();
        return new CircleSyncPayload(payload.dimension(), predicted.x(), predicted.z(), predicted.radius(),
            payload.nextX(), payload.nextZ(), payload.nextRadius(), payload.round(), (remaining + 19) / 20,
            remaining, payload.waiting(), payload.active(), payload.collapsed(), payload.hasNext());
    }

    public static boolean hasActiveCircle() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return false;
        TimedCircle timed = CIRCLES.get(client.level.dimension().identifier());
        CircleSyncPayload circle = timed == null ? null : timed.payload();
        return circle != null && circle.active() && !circle.collapsed();
    }

    private record TimedCircle(CircleSyncPayload payload, long receivedNanos) {}

    public static void applyDetectorReveal(DetectorRevealPayload payload) {
        DETECTOR_REVEALS.put(payload.dimension(), payload);
        ScanDeviceClient.completed();
    }

    public static DetectorRevealPayload currentDetectorReveal() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return null;
        Identifier dimension = client.level.dimension().identifier();
        DetectorRevealPayload reveal = DETECTOR_REVEALS.get(dimension);
        TimedCircle timed = CIRCLES.get(dimension);
        CircleSyncPayload circle = timed == null ? null : timed.payload();
        if (reveal != null && circle != null && DetectorRevealStyle.isExpired(reveal.targetRound(), circle.round())) {
            DETECTOR_REVEALS.remove(dimension);
            return null;
        }
        return reveal;
    }

    public static void shakeFromPoisonHit() {
        CircleAudioClient.damage();
        shakeStartedNanos = System.nanoTime();
    }

    public static float shakeOffset(boolean yaw) {
        if (shakeStartedNanos == Long.MIN_VALUE) return 0;
        long elapsed = (System.nanoTime() - shakeStartedNanos) / 1_000_000L;
        double strength = ScreenShakeMotion.strength(elapsed);
        if (strength == 0) {
            shakeStartedNanos = Long.MIN_VALUE;
            return 0;
        }
        double phase = elapsed * 0.23;
        return (float) ((yaw ? Math.cos(phase * 2.3) : Math.sin(phase * 1.7)) * strength * 2.2);
    }

    private static void renderStatus(GuiGraphicsExtractor gui) {
        if (shakeStartedNanos != Long.MIN_VALUE) {
            long elapsed = (System.nanoTime() - shakeStartedNanos) / 1_000_000L;
            int alpha = (int) (ScreenShakeMotion.strength(elapsed) * 72);
            if (alpha > 0) gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), alpha << 24 | 0xCC1010);
        }
        CircleSyncPayload circle = currentCircle();
        if (circle == null || circle.collapsed()) return;
        Minecraft client = Minecraft.getInstance();
        String status = "毒圈 第 " + circle.round() + "/5 圈 | "
            + (circle.waiting() ? "距开始缩圈: " : "缩圈剩余: ") + circle.remainingSeconds() + "秒";
        int x = gui.guiWidth() - client.font.width(status) - 12;
        gui.fill(x - 6, 67, gui.guiWidth() - 7, 85, 0xB0150A0A);
        gui.text(client.font, status, x, 72, 0xFFFF5555, true);
    }
}
