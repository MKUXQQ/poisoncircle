package com.poisoncircle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Renders the wall at the entity's world position, never relative to the player's camera. */
public final class PoisonCircleVisualRenderer extends EntityRenderer<PoisonCircleVisualEntity> {
    public PoisonCircleVisualRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }
    @Override public ResourceLocation getTextureLocation(PoisonCircleVisualEntity entity) { return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS; }
    @Override public void render(PoisonCircleVisualEntity entity, float yaw, float partialTick, PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int packedLight) {
        if (entity.width() >= 0) return;
        if (entity.width() <= 0) return;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation(entity.angle() - (float) (Math.PI / 2)));
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        drawWall(pose, entity.width(), entity.level().getMaxBuildHeight() - entity.level().getMinBuildHeight(), entity.level().getGameTime());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        pose.popPose();
    }
    private static void drawWall(PoseStack pose, double width, double height, long gameTime) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        int vertical = 48; double time = gameTime * 0.12;
        for (int y = 0; y < vertical; y++) {
            double y0 = height * y / vertical, y1 = height * (y + 1) / vertical;
            vertex(buffer, pose, -width / 2, y0, 0, alpha(0, y0, time));
            vertex(buffer, pose, width / 2, y0, 0, alpha(0, y0, time));
            vertex(buffer, pose, width / 2, y1, 0, alpha(0, y1, time));
            vertex(buffer, pose, -width / 2, y1, 0, alpha(0, y1, time));
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
    private static int alpha(double angle, double y, double time) { return (int) (12 + (Math.sin(y * 0.19 - time + angle * 3.0) * 0.5 + 0.5) * 45); }
    private static void vertex(BufferBuilder buffer, PoseStack pose, double x, double y, double z, int alpha) { buffer.addVertex(pose.last().pose(), (float) x, (float) y, (float) z).setColor(255, 45, 28, alpha); }
}
