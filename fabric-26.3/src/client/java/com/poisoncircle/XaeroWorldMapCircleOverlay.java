package com.poisoncircle;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import xaero.lib.client.graphics.XaeroRenderType;
import xaero.lib.client.graphics.util.ImmediateRenderUtil;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.map.WorldMap;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

/** Draws circle perimeters only on the matching dimension of Xaero's world map. */
final class XaeroWorldMapCircleOverlay {
    private XaeroWorldMapCircleOverlay() {}

    static boolean install() {
        if (WorldMap.mapElementRenderHandler == null) return false;
        WorldMap.mapElementRenderHandler.add(new Renderer());
        return true;
    }

    private record Circle(double x, double z, double radius, int color, Identifier dimension) {}

    private static final class Provider extends ElementRenderProvider<Circle, Void> {
        private List<Circle> circles = List.of();
        private CircleSyncPayload cachedCircle;
        private DetectorRevealPayload cachedReveal;
        private int index;

        @Override public void begin(ElementRenderLocation location, Void context) {
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
            if (reveal != null && circle.dimension().equals(reveal.dimension())
                && (!circle.hasNext() || DetectorRevealStyle.shouldDrawCyan(reveal.x(), reveal.z(), reveal.radius(), circle.nextX(), circle.nextZ(), circle.nextRadius())))
                generated.add(new Circle(reveal.x(), reveal.z(), reveal.radius(), 0xFF54D8FF, circle.dimension()));
            circles = generated;
            cachedCircle = circle;
            cachedReveal = reveal;
        }
        @Override public boolean hasNext(ElementRenderLocation location, Void context) { return index < circles.size(); }
        @Override public Circle getNext(ElementRenderLocation location, Void context) { return circles.get(index++); }
        @Override public void end(ElementRenderLocation location, Void context) { }
    }

    private static final class Reader extends ElementReader<Circle, Void, Renderer> {
        @Override public boolean isHidden(Circle circle, Void context) { return false; }
        @Override public double getRenderX(Circle circle, Void context, float partialTicks) { return circle.x; }
        @Override public double getRenderZ(Circle circle, Void context, float partialTicks) { return circle.z; }
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
        @Override public boolean isInteractable(ElementRenderLocation location, Circle circle) { return false; }
    }

    private static final class Renderer extends ElementRenderer<Circle, Void, Renderer> {
        Renderer() { super(null, new Provider(), new Reader()); }
        @Override public void preRender(ElementRenderInfo info, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider provider, boolean shadow) { }
        @Override public void postRender(ElementRenderInfo info, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider provider, boolean shadow) { }
        @Override public void renderElementShadow(Circle circle, boolean hovered, float scale, double x, double z, ElementRenderInfo info, MapElementGraphics graphics, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider provider) { }
        @Override public boolean renderElement(Circle circle, boolean hovered, double depth, float scale, double x, double z, ElementRenderInfo info, MapElementGraphics graphics, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider provider) {
            if (info.mapDimension != null && circle.dimension.equals(info.mapDimension.identifier())
                && Double.isFinite(circle.radius) && circle.radius > 0
                && Double.isFinite(info.scale) && info.scale > 0) {
                // The outer pose is scaled by 1/info.scale; callback scale sizes waypoint icons.
                double radius = circle.radius * info.scale;
                int segments = Math.max(12, Math.min(256, (int) Math.ceil(radius * 0.75)));
                BufferBuilder builder = ImmediateRenderUtil.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
                double previousX = 0, previousZ = 0;
                for (int i = 0; i <= segments; i++) {
                    double angle = 2.0 * Math.PI * i / segments;
                    double pointX = Math.cos(angle) * radius;
                    double pointZ = Math.sin(angle) * radius;
                    if (i > 0) addSegment(builder, graphics, previousX + x, previousZ + z, pointX + x, pointZ + z, circle.color);
                    previousX = pointX;
                    previousZ = pointZ;
                }
                MeshData mesh = builder.build();
                RenderPipeline pipeline = XaeroRenderType.RP_POSITION_COLOR_NO_CULL;
                ImmediateRenderUtil.drawImmediateMeshData(mesh, pipeline, ImmediateRenderUtil.NO_TEXTURES);
            }
            return false;
        }

        private static void addSegment(BufferBuilder builder, MapElementGraphics graphics, double x1, double y1, double x2, double y2, int color) {
            double dx = x2 - x1, dy = y2 - y1;
            double length = Math.hypot(dx, dy);
            double halfWidth = 1.0;
            double ox = -dy / length * halfWidth, oy = dx / length * halfWidth;
            addVertex(builder, graphics, x1 + ox, y1 + oy, color);
            addVertex(builder, graphics, x1 - ox, y1 - oy, color);
            addVertex(builder, graphics, x2 - ox, y2 - oy, color);
            addVertex(builder, graphics, x2 + ox, y2 + oy, color);
        }

        private static void addVertex(BufferBuilder builder, MapElementGraphics graphics, double x, double y, int color) {
            builder.addVertex(graphics.pose().last().pose(), (float) x, (float) y, 0.0f).setColor(color);
        }
        @Override public boolean shouldRender(ElementRenderLocation location, boolean shadow) { return location == ElementRenderLocation.WORLD_MAP && !shadow; }
        @Override public boolean shouldBeDimScaled() { return false; }
        @Override public int getOrder() { return -100; }
    }

}
