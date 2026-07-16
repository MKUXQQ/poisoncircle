package com.poisoncircle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Coordinate-free map that shows the active zone and the safely pre-announced following zone. */
public final class PoisonCircleDetectorScreen extends Screen {
    public PoisonCircleDetectorScreen() { super(Component.literal("安全区探测器")); }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui, mouseX, mouseY, partialTick);
        CircleSyncPayload circle = PoisonCircleClient.currentCircle();
        int cx = width / 2, cy = height / 2 + 8, mapRadius = Math.min(150, Math.min(width / 2 - 24, height / 2 - 58));
        gui.fill(cx - mapRadius - 8, cy - mapRadius - 8, cx + mapRadius + 8, cy + mapRadius + 8, 0xE9101825);
        gui.fill(cx - mapRadius, cy - mapRadius, cx + mapRadius, cy + mapRadius, 0xD9951D25);
        if (circle == null || circle.collapsed() || Minecraft.getInstance().player == null) {
            gui.drawCenteredString(font, "当前维度没有可扫描的安全区", cx, cy - 4, 0xFFFF7777);
            gui.drawCenteredString(font, "按 ESC 返回", cx, cy + 16, 0xFFB7C5D6);
            return;
        }
        Player player = Minecraft.getInstance().player;
        double dx = player.getX() - circle.currentX(), dz = player.getZ() - circle.currentZ();
        double playerDistance = Math.hypot(dx, dz);
        DetectorRevealPayload reveal = PoisonCircleClient.currentDetectorReveal();
        boolean showAnnounced = reveal != null;
        double announcedX = showAnnounced ? reveal.x() : circle.currentX();
        double announcedZ = showAnnounced ? reveal.z() : circle.currentZ();
        double announcedRadius = showAnnounced ? reveal.radius() : 0;
        double announcedDistance = showAnnounced ? Math.hypot(announcedX - circle.currentX(), announcedZ - circle.currentZ()) + announcedRadius : 0;
        double scaleWorld = Math.max(circle.currentRadius(), Math.max(playerDistance, announcedDistance)) * 1.12;
        double currentMapRadius = mapRadius * circle.currentRadius() / scaleWorld;
        fillCircle(gui, cx, cy, currentMapRadius, 0xE91C3045);
        drawCircle(gui, cx, cy, currentMapRadius, 0xFFFFFFFF, 2);
        if (showAnnounced) drawCircle(gui, cx + mapRadius * (announcedX - circle.currentX()) / scaleWorld, cy + mapRadius * (announcedZ - circle.currentZ()) / scaleWorld, mapRadius * announcedRadius / scaleWorld, 0xFFFFFFFF, 1);
        int px = (int) Math.round(cx + mapRadius * dx / scaleWorld), pz = (int) Math.round(cy + mapRadius * dz / scaleWorld);
        for (Player teammate : player.level().players()) {
            if (teammate == player || player.getTeam() == null || !player.getTeam().isAlliedTo(teammate.getTeam())) continue;
            int teammateX = (int) Math.round(cx + mapRadius * (teammate.getX() - circle.currentX()) / scaleWorld);
            int teammateZ = (int) Math.round(cy + mapRadius * (teammate.getZ() - circle.currentZ()) / scaleWorld);
            if (Math.abs(teammateX - cx) <= mapRadius && Math.abs(teammateZ - cy) <= mapRadius) gui.fill(teammateX - 2, teammateZ - 2, teammateX + 3, teammateZ + 3, 0xFF55FF55);
        }
        gui.fill(px - 3, pz - 3, px + 4, pz + 4, 0xFFFFD84A);
        gui.drawCenteredString(font, "安全区探测器", cx, 18, 0xFFFF5A4F);
        gui.drawCenteredString(font, "白色：安全区边界  黄色：自己  绿色：队友", cx, 34, 0xFFD4DFE9);
        String timing = circle.waiting() ? "距离缩圈开始 " : "缩圈结束 ";
        gui.drawCenteredString(font, "第 " + circle.round() + "/5 圈 · " + timing + circle.remainingSeconds() + " 秒", cx, height - 44, 0xFFFFFFFF);
        gui.drawCenteredString(font, "距离安全区中心 " + Math.round(playerDistance) + " 格 · " + (playerDistance <= circle.currentRadius() ? "安全区内" : "毒圈外"), cx, height - 28, playerDistance <= circle.currentRadius() ? 0xFF7FFFA0 : 0xFFFF7777);
    }

    private static void drawCircle(GuiGraphics gui, double cx, double cy, double radius, int color, int thickness) {
        int steps = Math.max(96, (int) (radius * 6));
        for (int i = 0; i < steps; i++) {
            double angle = Math.PI * 2 * i / steps;
            int x = (int) Math.round(cx + Math.cos(angle) * radius), y = (int) Math.round(cy + Math.sin(angle) * radius);
            gui.fill(x - thickness / 2, y - thickness / 2, x + thickness / 2 + 1, y + thickness / 2 + 1, color);
        }
    }

    private static void fillCircle(GuiGraphics gui, int cx, int cy, double radius, int color) {
        int roundedRadius = (int) Math.ceil(radius);
        double radiusSquared = radius * radius;
        for (int y = -roundedRadius; y <= roundedRadius; y++) {
            int halfWidth = (int) Math.sqrt(Math.max(0, radiusSquared - y * (double) y));
            gui.hLine(cx - halfWidth, cx + halfWidth, cy + y, color);
        }
    }
}
