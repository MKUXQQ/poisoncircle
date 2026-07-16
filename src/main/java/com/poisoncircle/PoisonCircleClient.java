package com.poisoncircle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = PoisonCircleMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class PoisonCircleClient {
    private static final ResourceLocation FORCEFIELD_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/forcefield.png");
    private static final Map<ResourceLocation, Snapshot> CIRCLES = new HashMap<>();
    private static final Map<ResourceLocation, DetectorRevealPayload> DETECTOR_REVEALS = new HashMap<>();
    private static long shakeStartedNanos = Long.MIN_VALUE;
    private PoisonCircleClient() {}

    public static void apply(CircleSyncPayload payload) {
        if (payload.active()) CIRCLES.put(payload.dimension(), new Snapshot(payload)); else CIRCLES.remove(payload.dimension());
        XaeroCircleCompatibility.install();
    }
    public static CircleSyncPayload currentCircle() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        Snapshot snapshot = CIRCLES.get(minecraft.level.dimension().location());
        return snapshot == null ? null : snapshot.payload();
    }
    public static void applyDetectorReveal(DetectorRevealPayload payload) { DETECTOR_REVEALS.put(payload.dimension(), payload); }
    public static DetectorRevealPayload currentDetectorReveal() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? null : DETECTOR_REVEALS.get(minecraft.level.dimension().location());
    }

    public static void shakeFromPoisonHit() { shakeStartedNanos = System.nanoTime(); }

    @SubscribeEvent
    public static void shakeCamera(ViewportEvent.ComputeCameraAngles event) {
        if (shakeStartedNanos == Long.MIN_VALUE) return;
        long elapsed = (System.nanoTime() - shakeStartedNanos) / 1_000_000L;
        double strength = ScreenShakeMotion.strength(elapsed);
        if (strength == 0.0) { shakeStartedNanos = Long.MIN_VALUE; return; }
        double phase = elapsed * 0.23;
        event.setPitch(event.getPitch() + (float) (Math.sin(phase * 1.7) * strength * 2.2));
        event.setYaw(event.getYaw() + (float) (Math.cos(phase * 2.3) * strength * 2.2));
    }

    @SubscribeEvent
    public static void openDetector(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() || !event.getItemStack().is(PoisonCircleMod.DETECTOR.get())) return;
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new PoisonCircleDetectorScreen()));
    }

    @SubscribeEvent
    public static void renderStatus(RenderGuiEvent.Post event) {
        CircleSyncPayload circle = currentCircle(); if (circle == null || circle.collapsed()) return;
        Minecraft minecraft = Minecraft.getInstance();
        String status = "毒圈 第 " + circle.round() + "/5 圈 | " + (circle.waiting() ? "距开始缩圈: " : "缩圈剩余: ") + circle.remainingSeconds() + "秒";
        GuiGraphics gui = event.getGuiGraphics(); int x = gui.guiWidth() - minecraft.font.width(status) - 12, y = 72;
        gui.fill(x - 6, y - 5, gui.guiWidth() - 7, y + 13, 0xB0150A0A);
        gui.drawString(minecraft.font, status, x, y, 0xFFFF5555, true);
    }

    @SubscribeEvent
    public static void renderCircleBoundary(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        CircleSyncPayload circle = currentCircle(); Minecraft minecraft = Minecraft.getInstance();
        if (circle == null || circle.collapsed() || minecraft.level == null) return;
        drawVanillaBarrierStyle(circle, event.getCamera(), minecraft.gameRenderer.getDepthFar());
    }

    /** Uses the same texture, depth testing and additive blending as the vanilla world border. */
    private static void drawVanillaBarrierStyle(CircleSyncPayload circle, Camera camera, double far) {
        Vec3 cameraPos = camera.getPosition();
        int segments = Math.max(96, Math.min(384, (int) Math.ceil(circle.currentRadius() * Math.PI * 2 / 3.0)));
        var positions = PoisonCircleBarrierGeometry.positions(circle.currentX(), circle.currentZ(), circle.currentRadius(), segments);
        float scroll = (float) (Util.getMillis() % 3000L) / 3000.0F;
        float bottomUv = (float) (-cameraPos.y * 0.5);
        float topUv = bottomUv + (float) far;
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderTexture(0, FORCEFIELD_TEXTURE);
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        RenderSystem.setShaderColor(1.0F, 0.08F, 0.08F, 0.88F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.disableCull();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (int index = 0; index < segments; index++) {
            PoisonCircleBarrierGeometry.Position first = positions.get(index);
            PoisonCircleBarrierGeometry.Position second = positions.get((index + 1) % segments);
            float u0 = scroll + index * 0.5F;
            float u1 = u0 + 0.5F;
            buffer.addVertex((float) (first.x() - cameraPos.x), (float) -far, (float) (first.z() - cameraPos.z)).setUv(u0, scroll + topUv);
            buffer.addVertex((float) (second.x() - cameraPos.x), (float) -far, (float) (second.z() - cameraPos.z)).setUv(u1, scroll + topUv);
            buffer.addVertex((float) (second.x() - cameraPos.x), (float) far, (float) (second.z() - cameraPos.z)).setUv(u1, scroll + bottomUv);
            buffer.addVertex((float) (first.x() - cameraPos.x), (float) far, (float) (first.z() - cameraPos.z)).setUv(u0, scroll + bottomUv);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
    }

    private record Snapshot(CircleSyncPayload payload) { }
}
