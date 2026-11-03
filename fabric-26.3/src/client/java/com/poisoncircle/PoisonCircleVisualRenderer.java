package com.poisoncircle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** Submits the circular, scrolling forcefield through the 26.3 feature renderer. */
public final class PoisonCircleVisualRenderer {
    private static final net.minecraft.client.renderer.rendertype.RenderType WALL =
        RenderTypes.entityTranslucentEmissive(Identifier.fromNamespaceAndPath(
            PoisonCircleMod.MOD_ID, "textures/environment/circle_wall.png"));
    private PoisonCircleVisualRenderer() {}

    public static void submit(LevelRenderContext context) {
        CircleSyncPayload circle = PoisonCircleClient.currentCircle();
        if (circle == null || circle.collapsed() || circle.currentRadius() <= 0) return;
        Vec3 camera = context.levelState().cameraRenderState.pos;
        double far = net.minecraft.client.Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0;
        WallGeometry.Arc arc = WallGeometry.visibleArc(circle.currentRadius(),
            camera.x - circle.currentX(), camera.z - circle.currentZ(), far);
        if (arc.segments() == 0) return;
        double repeats = Math.max(1, Math.rint(Math.PI * 2 * circle.currentRadius() / 16));
        float scroll = (float) (System.nanoTime() / 1_000_000L % 24000L) / 24000;
        PoseStack pose = context.poseStack();
        int bands = WallGeometry.bandsPerSide(far);
        var frustum = context.levelState().cameraRenderState.cullFrustum;
        context.submitNodeCollector().submitCustomGeometry(pose, WALL,
            (matrix, vertices) -> {
                for (int i = 0; i < arc.segments(); i++) {
                    double a = arc.start() + arc.span() * i / arc.segments();
                    double b = arc.start() + arc.span() * (i + 1) / arc.segments();
                    float x0 = (float) (circle.currentX() + Math.cos(a) * circle.currentRadius() - camera.x);
                    float z0 = (float) (circle.currentZ() + Math.sin(a) * circle.currentRadius() - camera.z);
                    float x1 = (float) (circle.currentX() + Math.cos(b) * circle.currentRadius() - camera.x);
                    float z1 = (float) (circle.currentZ() + Math.sin(b) * circle.currentRadius() - camera.z);
                    float u0 = (float) (a / (Math.PI * 2) * repeats);
                    float u1 = (float) (b / (Math.PI * 2) * repeats);
                    for (int band = -bands; band < bands; band++) {
                        float y0 = band * WallGeometry.BAND_HEIGHT, y1 = y0 + WallGeometry.BAND_HEIGHT;
                        if (frustum != null && !frustum.isVisible(new AABB(
                            camera.x + Math.min(x0, x1) - 0.01, camera.y + y0, camera.z + Math.min(z0, z1) - 0.01,
                            camera.x + Math.max(x0, x1) + 0.01, camera.y + y1, camera.z + Math.max(z0, z1) + 0.01))) continue;
                        float v0 = scroll - (float) ((camera.y + y0) / 16);
                        float v1 = scroll - (float) ((camera.y + y1) / 16);
                        vertex(vertices, matrix, x0, y0, z0, u0, v0);
                        vertex(vertices, matrix, x1, y0, z1, u1, v0);
                        vertex(vertices, matrix, x1, y1, z1, u1, v1);
                        vertex(vertices, matrix, x0, y1, z0, u0, v1);
                    }
                }
            });
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float z, float u, float v) {
        vertices.addVertex(pose, x, y, z).setColor(255, 255, 255, 255).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 1, 0);
    }
}
