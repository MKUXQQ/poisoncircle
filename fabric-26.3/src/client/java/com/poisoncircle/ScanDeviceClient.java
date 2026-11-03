package com.poisoncircle;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Server-authoritative sessions, with immediate local cancellation on release. */
public final class ScanDeviceClient {
    private record Session(BlockPos position, ScanAnimationState animation, float yaw, float pitch) { }
    private static final Map<Integer, Session> sessions = new HashMap<>();
    private static final ScanAnimationState local = new ScanAnimationState();
    private static BlockPos target;
    private static BlockPos releaseTarget;
    private static int heartbeatTicks;
    private static CameraType originalCamera;
    private static boolean savedLook;
    private static float originalYaw;
    private static float originalPitch;
    private static ClientLevel sessionLevel;
    private static LocalPlayer sessionPlayer;

    private ScanDeviceClient() { }

    private static BlockPos lookedAt(Minecraft client) {
        if (client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
            && client.player.isWithinBlockInteractionRange(hit.getBlockPos(), 0)
            && client.level.getBlockState(hit.getBlockPos()).is(ScanDevice.BLOCK)) {
            return hit.getBlockPos().immutable();
        }
        return null;
    }

    public static boolean isAnimating(int entityId) { return sessions.containsKey(entityId); }

    public static float age(int entityId, float partialTick) {
        Session session = sessions.get(entityId);
        return session == null ? 0 : session.animation().age(partialTick);
    }

    public static float wrist(float partialTick) {
        Minecraft client = Minecraft.getInstance();
        Session session = client.player == null ? null : sessions.get(client.player.getId());
        return session == null ? 0 : session.animation().wrist(partialTick);
    }

    public static boolean shouldLockMovement() {
        Minecraft client = Minecraft.getInstance();
        return originalCamera != null && target != null && client.level == sessionLevel
            && client.player == sessionPlayer && client.player != null && client.player.isAlive()
            && !client.player.isSpectator() && client.gui.screen() == null && client.options.keyUse.isDown();
    }

    private static void restoreCamera(Minecraft client) {
        if (originalCamera != null) client.options.setCameraType(originalCamera);
        originalCamera = null;
        if (savedLook && client.level == sessionLevel && client.player == sessionPlayer && client.player != null) {
            client.player.setYRot(originalYaw);
            client.player.setXRot(originalPitch);
            client.player.setYHeadRot(originalYaw);
            client.player.setYBodyRot(originalYaw);
        }
        savedLook = false;
    }

    private static void stop(Minecraft client, boolean notifyServer) {
        if (target != null) releaseTarget = target;
        if (target != null && notifyServer && ClientPlayNetworking.canSend(ScanHoldPayload.TYPE)) {
            ClientPlayNetworking.send(new ScanHoldPayload(target, false));
        }
        if (sessionPlayer != null) sessions.remove(sessionPlayer.getId());
        target = null;
        heartbeatTicks = 0;
        local.complete();
        local.tick(client.options.keyUse.isDown());
        restoreCamera(client);
    }

    /** Detector reveal may arrive before the session's final update. */
    public static void completed() { stop(Minecraft.getInstance(), false); }

    private static void receive(ScanSessionPayload payload, Minecraft client) {
        if (client.player == null || client.level == null) return;
        boolean own = payload.entityId() == client.player.getId();
        if (!payload.active()) {
            sessions.remove(payload.entityId());
            if (own) stop(client, false);
            return;
        }
        if (own && (target == null || !target.equals(payload.position()) || !client.options.keyUse.isDown()
            || client.gui.screen() != null || !client.player.isAlive() || client.player.isSpectator()
            || client.level != sessionLevel || client.player != sessionPlayer)) {
            ClientPlayNetworking.send(new ScanHoldPayload(payload.position(), false));
            stop(client, false);
            return;
        }
        Session session = sessions.get(payload.entityId());
        if (session == null || !session.position().equals(payload.position())) {
            session = new Session(payload.position().immutable(), new ScanAnimationState(), payload.yaw(), payload.pitch());
            sessions.put(payload.entityId(), session);
        }
        session.animation().sync(true, payload.progressTicks());
        if (own) {
            local.sync(true, payload.progressTicks());
            if (originalCamera == null) originalCamera = client.options.getCameraType();
            lockPose(client, session);
        }
    }

    private static void lockPose(Minecraft client, Session session) {
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        client.player.setYRot(session.yaw());
        client.player.setXRot(session.pitch());
        client.player.setYHeadRot(session.yaw());
        client.player.setYBodyRot(session.yaw());
    }

    private static void tick(Minecraft client) {
        boolean useDown = client.options.keyUse.isDown();
        if (client.level != sessionLevel || client.player != sessionPlayer) {
            stop(client, true);
            sessions.clear();
            sessionLevel = client.level;
            sessionPlayer = client.player;
        }
        if (!useDown && releaseTarget != null) {
            if (ClientPlayNetworking.canSend(ScanHoldPayload.TYPE)) {
                ClientPlayNetworking.send(new ScanHoldPayload(releaseTarget, false));
            }
            releaseTarget = null;
        }
        local.tick(useDown);
        sessions.values().forEach(session -> session.animation().tick(true));
        boolean valid = client.level != null && client.player != null && client.player.isAlive()
            && !client.player.isSpectator() && client.gui.screen() == null;
        if (!valid || !useDown || !ClientPlayNetworking.canSend(ScanHoldPayload.TYPE)) {
            if (target != null || originalCamera != null) stop(client, true);
            return;
        }
        if (target == null) {
            if (local.completed()) return;
            target = lookedAt(client);
            if (target == null) return;
            originalYaw = client.player.getYRot();
            originalPitch = client.player.getXRot();
            savedLook = true;
            heartbeatTicks = 0;
            ClientPlayNetworking.send(new ScanHoldPayload(target, true));
        } else if (++heartbeatTicks >= 4) {
            heartbeatTicks = 0;
            // A third-person crosshair no longer points at the console after the camera switches.
            ClientPlayNetworking.send(new ScanHoldPayload(target, true));
        }
        Session session = sessions.get(client.player.getId());
        if (session != null) lockPose(client, session);
    }

    public static void install() {
        ClientPlayNetworking.registerGlobalReceiver(ScanSessionPayload.TYPE,
            (payload, context) -> receive(payload, context.client()));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "scanner_progress"),
            (gui, delta) -> {
                Minecraft client = Minecraft.getInstance();
                if (!shouldLockMovement()) return;
                Session session = sessions.get(client.player.getId());
                if (session == null) return;
                int progress = session.animation().progressTicks();
                int width = Math.min(180, gui.guiWidth() - 24);
                int left = (gui.guiWidth() - width) / 2;
                int top = Math.max(28, gui.guiHeight() / 4);
                // projectPointToScreen returns projected NDC depth, not a front/behind sign.
                // A fixed HUD remains visible even when the console is offscreen or occluded.
                gui.fill(left - 6, top - 19, left + width + 6, top + 12, 0xDC10252B);
                gui.fill(left, top, left + width, top + 6, 0xFF071014);
                gui.fill(left + 1, top + 1, left + 1 + (width - 2) * progress / 100, top + 5, 0xFF55E0C0);
                String label = "正在扫描安全区 " + progress + "%";
                gui.text(client.font, label, left + (width - client.font.width(label)) / 2, top - 13, 0xFFFFFFFF, true);
            });
        ClientTickEvents.START_CLIENT_TICK.register(ScanDeviceClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            stop(client, false);
            sessions.clear();
            releaseTarget = null;
            sessionLevel = null;
            sessionPlayer = null;
            local.tick(false);
        });
    }
}
