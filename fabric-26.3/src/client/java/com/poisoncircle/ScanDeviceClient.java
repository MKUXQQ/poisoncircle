package com.poisoncircle;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Reads held input each tick rather than treating repeated use packets as a hold. */
public final class ScanDeviceClient {
    private static BlockPos target;
    private static int heartbeatTicks;
    private static int progressTicks;
    private static final ScanAnimationState animation = new ScanAnimationState();
    private static final int REQUIRED_TICKS = 100;
    private static boolean movementLocked;

    private ScanDeviceClient() { }

    private static BlockPos lookedAt(Minecraft client) {
        if (client.player != null && client.level != null && client.gui.screen() == null
            && client.options.keyUse.isDown() && client.player.isAlive() && !client.player.isSpectator()
            && client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
            && client.player.isWithinBlockInteractionRange(hit.getBlockPos(), 0)
            && client.level.getBlockState(hit.getBlockPos()).is(ScanDevice.BLOCK)) {
            return hit.getBlockPos();
        }
        return null;
    }

    public static boolean isAnimating(int entityId) {
        Minecraft client = Minecraft.getInstance();
        return animation.active() && client.player != null && client.player.getId() == entityId;
    }

    public static float wrist(float partialTick) { return animation.wrist(partialTick); }

    public static boolean shouldLockMovement() { return movementLocked; }

    /** Called only by the server's successful detector reveal receiver. */
    public static void completed() { animation.complete(); }

    public static void install() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "scanner_progress"),
            (gui, delta) -> {
                if (target == null || progressTicks <= 0 || animation.completed()) return;
                Minecraft client = Minecraft.getInstance();
                if (client.level == null || client.player == null) return;
                Vec3 projected = client.gameRenderer.projectPointToScreen(Vec3.atCenterOf(target).add(0, 1.25, 0));
                if (projected.z <= 0 || Math.abs(projected.x) > 1 || Math.abs(projected.y) > 1) return;
                int width = 120;
                int left = (int) Math.round((projected.x + 1) * 0.5 * gui.guiWidth() - width * 0.5);
                int top = (int) Math.round((1 - projected.y) * 0.5 * gui.guiHeight());
                top = Math.max(12, Math.min(gui.guiHeight() - 16, top));
                gui.fill(left - 2, top - 2, left + width + 2, top + 8, 0xC0202528);
                gui.fill(left, top, left + width, top + 4, 0xFF111719);
                gui.fill(left + 1, top + 1, left + 1 + (width - 2) * progressTicks / REQUIRED_TICKS, top + 3, 0xFF55E0C0);
                String label = "正在扫描安全区 " + (progressTicks * 100 / REQUIRED_TICKS) + "%";
                gui.text(client.font, label, left + (width - client.font.width(label)) / 2, top - 12, 0xFFFFFFFF, true);
            });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BlockPos lookedAt = lookedAt(client);
            boolean canSend = ClientPlayNetworking.canSend(ScanHoldPayload.TYPE);
            boolean useDown = client.options.keyUse.isDown();
            boolean validHold = lookedAt != null && useDown;
            boolean scanning = validHold && canSend && !animation.completed();
            movementLocked = animation.shouldLockMovement(lookedAt != null, useDown);
            animation.tick(scanning, useDown);
            if (scanning) progressTicks = Math.min(REQUIRED_TICKS, progressTicks + 1);
            else progressTicks = 0;

            if (target != null && !target.equals(lookedAt)) {
                if (canSend) ClientPlayNetworking.send(new ScanHoldPayload(target, false));
                target = null;
                heartbeatTicks = 0;
            }
            if (lookedAt != null && canSend && !animation.completed()) {
                if (target == null || ++heartbeatTicks >= 4) {
                    target = lookedAt.immutable();
                    heartbeatTicks = 0;
                    ClientPlayNetworking.send(new ScanHoldPayload(target, true));
                }
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            target = null;
            heartbeatTicks = 0;
            progressTicks = 0;
            movementLocked = false;
            animation.tick(false, false);
        });
    }
}
