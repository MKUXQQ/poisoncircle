package com.poisoncircle;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

/** Client entry point for the airborne administration items. */
public final class AirborneAdminClient {
    private static volatile JsonObject snapshot = new JsonObject();
    private static boolean attackHandled;
    private static long lastActionTick=-10;

    private AirborneAdminClient() { }

    public static void install() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            snapshot = new JsonObject();
            attackHandled=false;
            lastActionTick=-10;
            AirborneControlScreen.clearSession();
            AirbornePointsScreen.clearSession();
        });
        ClientPlayNetworking.registerGlobalReceiver(AirborneStatePayload.TYPE, (payload, context) -> {
            JsonObject state;
            try {
                if (payload.json().length() > 32_768) return;
                var parsed = JsonParser.parseString(payload.json());
                if (!parsed.isJsonObject()) return;
                state = parsed.getAsJsonObject();
            } catch (RuntimeException exception) {
                return;
            }
            snapshot = state;
            AirborneControlScreen.snapshotChanged();
            AirbornePointsScreen.snapshotChanged();
            AirborneFlightClient.acceptState(state);
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!level.isClientSide()) return InteractionResult.PASS;
            var stack = player.getItemInHand(hand);
            Minecraft client = Minecraft.getInstance();
            if (stack.is(AirborneGame.MANAGER)) {
                client.execute(() -> client.gui.setScreen(new AirborneControlScreen()));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!level.isClientSide() || hand!=InteractionHand.MAIN_HAND || !player.getMainHandItem().is(AirborneGame.SETTER)) return InteractionResult.PASS;
            setterAttack();
            return InteractionResult.FAIL; // Consume the attack before vanilla can damage the block.
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!level.isClientSide() || hand!=InteractionHand.MAIN_HAND || !player.getMainHandItem().is(AirborneGame.SETTER)) return InteractionResult.PASS;
            setterAttack();
            return InteractionResult.FAIL;
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!client.options.keyAttack.isDown()) attackHandled=false;
            if (client.gui.screen()!=null || client.player==null || !client.options.keyAttack.isDown()) return;
            if (client.player.getMainHandItem().is(AirborneGame.SETTER) && (client.hitResult==null || client.hitResult.getType()==HitResult.Type.MISS)) setterAttack();
        });
    }

    private static void setterAttack() {
        Minecraft client=Minecraft.getInstance();
        if (attackHandled || client.player==null || client.gui.screen()!=null) return;
        attackHandled=true;
        if (client.player.isShiftKeyDown()) {
            send("open_points",new JsonObject());
            client.execute(()->client.gui.setScreen(new AirbornePointsScreen()));
        } else if (client.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit) {
            var pos=hit.getBlockPos();JsonObject d=new JsonObject();
            d.addProperty("x",pos.getX());d.addProperty("y",pos.getY());d.addProperty("z",pos.getZ());
            send("delete_near_point",d);
        }
    }

    public static JsonObject snapshot() { return snapshot.deepCopy(); }

    static void send(String action, JsonObject data) {
        Minecraft client=Minecraft.getInstance();
        long tick=client.level==null?0:client.level.getGameTime();
        if (action.equals("sync") && tick-lastActionTick<4) return;
        if (!action.equals("sync")) lastActionTick=tick;
        String json = data.toString();
        if (json.length() > 4096) { AirborneControlScreen.report("请求数据过长"); return; }
        if (!ClientPlayNetworking.canSend(AirborneAdminPayload.TYPE)) { AirborneControlScreen.report("服务端尚未启用空降管理网络"); return; }
        ClientPlayNetworking.send(new AirborneAdminPayload(action, json));
    }
}
