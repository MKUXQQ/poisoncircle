package com.poisoncircle;

import net.minecraft.resources.Identifier;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.common.HudMod;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementReader;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import xaero.lib.client.graphics.XaeroRenderType;
import xaero.lib.client.graphics.util.ImmediateRenderUtil;

import java.util.ArrayList;
import java.util.List;

/** Draws each circle as one adaptively sampled mesh on Xaero's minimap. */
final class XaeroMinimapCircleOverlay {
    private XaeroMinimapCircleOverlay() {}

    static boolean install() {
        if (HudMod.INSTANCE == null || HudMod.INSTANCE.getMinimap() == null
            || HudMod.INSTANCE.getMinimap().getOverMapRendererHandler() == null) return false;
        HudMod.INSTANCE.getMinimap().getOverMapRendererHandler().add(new Renderer());
        return true;
    }

    private record Circle(double x, double z, double radius, int color, Identifier dimension) {}

    private static final class Provider extends MinimapElementRenderProvider<Circle, Void> {
        private List<Circle> circles = List.of();
        private CircleSyncPayload cachedCircle;
        private DetectorRevealPayload cachedReveal;
        private int index;

        @Override public void begin(MinimapElementRenderLocation location, Void context) {
            index = 0;
            CircleSyncPayload circle = PoisonCircleClient.currentCircle();
            if (circle == null || circle.collapsed()) {
                circles = List.of();
                cachedCircle = null;
                cachedReveal = null;
                return;
            }
            DetectorRevealPayload reveal = PoisonCircleClient.currentDetectorReveal();
            if (cachedCircle != null && circle.dimension().equals(cachedCircle.dimension())
                && Double.compare(circle.currentX(), cachedCircle.currentX()) == 0
                && Double.compare(circle.currentZ(), cachedCircle.currentZ()) == 0
                && Double.compare(circle.currentRadius(), cachedCircle.currentRadius()) == 0
                && circle.hasNext() == cachedCircle.hasNext()
                && Double.compare(circle.nextX(), cachedCircle.nextX()) == 0
                && Double.compare(circle.nextZ(), cachedCircle.nextZ()) == 0
                && Double.compare(circle.nextRadius(), cachedCircle.nextRadius()) == 0
                && java.util.Objects.equals(reveal, cachedReveal)) return;
            List<Circle> generated = new ArrayList<>(3);
            generated.add(new Circle(circle.currentX(), circle.currentZ(), circle.currentRadius(), 0xFFFF3030, circle.dimension()));
            if (circle.hasNext()) generated.add(new Circle(circle.nextX(), circle.nextZ(), circle.nextRadius(), 0xFFFFFFFF, circle.dimension()));
            if (reveal != null && circle.dimension().equals(reveal.dimension()) && (!circle.hasNext() || DetectorRevealStyle.shouldDrawCyan(reveal.x(), reveal.z(), reveal.radius(), circle.nextX(), circle.nextZ(), circle.nextRadius())))
                generated.add(new Circle(reveal.x(), reveal.z(), reveal.radius(), 0xFF54D8FF, circle.dimension()));
            circles = generated;
            cachedCircle = circle;
            cachedReveal = reveal;
        }
        @Override public boolean hasNext(MinimapElementRenderLocation location, Void context) { return index < circles.size(); }
        @Override public Circle getNext(MinimapElementRenderLocation location, Void context) { return circles.get(index++); }
        @Override public void end(MinimapElementRenderLocation location, Void context) { }
    }

    private static final class Reader extends MinimapElementReader<Circle, Void> {
        @Override public boolean isHidden(Circle circle, Void context) { return false; }
        @Override public double getRenderX(Circle circle, Void context, float partialTicks) { return circle.x; }
        @Override public double getRenderY(Circle circle, Void context, float partialTicks) { return 0; }
        @Override public double getRenderZ(Circle circle, Void context, float partialTicks) { return circle.z; }
        @Override public double getCoordinateScale(Circle circle, Void context, MinimapElementRenderInfo info) { return 1.0; }
        @Override public boolean isInteractable(MinimapElementRenderLocation location, Circle circle) { return false; }
        @Override public int getInteractionBoxLeft(Circle circle, Void context, float partialTicks) { return (int) -Math.ceil(circle.radius); }
        @Override public int getInteractionBoxRight(Circle circle, Void context, float partialTicks) { return (int) Math.ceil(circle.radius); }
        @Override public int getInteractionBoxTop(Circle circle, Void context, float partialTicks) { return (int) -Math.ceil(circle.radius); }
        @Override public int getInteractionBoxBottom(Circle circle, Void context, float partialTicks) { return (int) Math.ceil(circle.radius); }
        @Override public int getRenderBoxLeft(Circle circle, Void context, float partialTicks) { return (int) -Math.ceil(circle.radius); }
        @Override public int getRenderBoxRight(Circle circle, Void context, float partialTicks) { return (int) Math.ceil(circle.radius); }
        @Override public int getRenderBoxTop(Circle circle, Void context, float partialTicks) { return (int) -Math.ceil(circle.radius); }
        @Override public int getRenderBoxBottom(Circle circle, Void context, float partialTicks) { return (int) Math.ceil(circle.radius); }
        @Override public int getLeftSideLength(Circle circle, net.minecraft.client.Minecraft minecraft) { return 0; }
        @Override public String getMenuName(Circle circle) { return ""; }
        @Override public String getFilterName(Circle circle) { return ""; }
        @Override public int getMenuTextFillLeftPadding(Circle circle) { return 0; }
        @Override public int getRightClickTitleBackgroundColor(Circle circle) { return 0; }
        @Override public boolean shouldScaleBoxWithOptionalScale() { return false; }
    }

    private static final class Renderer extends MinimapElementRenderer<Circle, Void> {
        private final org.joml.Matrix4f mapPose = new org.joml.Matrix4f();
        private double terrainScale, halfView, sin, cos;
        private boolean round;
        Renderer() { super(new Reader(), new Provider(), null); }
        @Override public boolean renderElement(Circle circle, boolean hovered, boolean onScreen, double depth, float scale, double x, double z, MinimapElementRenderInfo info, MinimapElementGraphics gui, XaeroBufferProvider buffers) {
            if (info.mapDimension != null && circle.dimension.equals(info.mapDimension.identifier())
                && Double.isFinite(circle.radius) && circle.radius > 0) {
                double dx = circle.x - info.renderPos.x, dz = circle.z - info.renderPos.z;
                double centerX = (sin * dx - cos * dz) * terrainScale;
                double centerY = (cos * dx + sin * dz) * terrainScale;
                BufferBuilder builder = ImmediateRenderUtil.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
                MapCircleGeometry.draw(centerX, centerY, circle.radius * terrainScale, 1, halfView, round,
                    (ax, ay, bx, by, cx, cy, dx2, dy2) -> {
                        builder.addVertex(mapPose, (float) ax, (float) ay, 0).setColor(circle.color);
                        builder.addVertex(mapPose, (float) bx, (float) by, 0).setColor(circle.color);
                        builder.addVertex(mapPose, (float) cx, (float) cy, 0).setColor(circle.color);
                        builder.addVertex(mapPose, (float) dx2, (float) dy2, 0).setColor(circle.color);
                    });
                MeshData mesh = builder.build();
                if (mesh != null) ImmediateRenderUtil.drawImmediateMeshData(mesh,
                    XaeroRenderType.RP_POSITION_COLOR_NO_CULL, ImmediateRenderUtil.NO_TEXTURES);
            }
            return false;
        }

        @Override public void preRender(MinimapElementRenderInfo info, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider provider) {
            var minimap = HudMod.INSTANCE.getMinimap();
            var renderer = minimap.usingFBO() ? minimap.getMinimapFBORenderer() : minimap.getMinimapSafeModeRenderer();
            var session = xaero.hud.minimap.BuiltInHudModules.MINIMAP.getCurrentSession();
            int size = session.getProcessor().getMinimapSize();
            float uiScale = HudMod.INSTANCE.getSettings().getMinimapScale();
            int shape = HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getEffective(
                xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions.SHAPE);
            boolean north = xaero.hud.minimap.config.util.MinimapConfigClientUtils.getEffectiveNorthLocked(size / 2, shape);
            double angle = Math.PI - Math.toRadians(renderer.getRenderAngle(north));
            sin = Math.sin(angle); cos = Math.cos(angle);
            terrainScale = renderer.getZoom() * uiScale / 2;
            halfView = (int) (size * uiScale / 4);
            round = shape == 1;
            // OVER_MINIMAP clamps marker anchors and does not clip. Keep its unclamped map-center pose.
            mapPose.set(minimap.getOverMapRendererHandler().getGuiGraphics().pose().last().pose());
        }
        @Override public void postRender(MinimapElementRenderInfo info, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider provider) { }
        @Override public boolean shouldRender(MinimapElementRenderLocation location) { return location == MinimapElementRenderLocation.OVER_MINIMAP; }
        @Override public int getOrder() { return -100; }
    }

}
