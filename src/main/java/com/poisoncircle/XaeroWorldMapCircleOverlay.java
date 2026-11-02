package com.poisoncircle;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import xaero.map.WorldMap;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

import java.util.ArrayList;
import java.util.List;

/** Draws only the current and next circle perimeters on Xaero's world map. */
final class XaeroWorldMapCircleOverlay {
    private XaeroWorldMapCircleOverlay() {}

    static void install() {
        WorldMap.mapElementRenderHandler.add(new Renderer());
    }

    private record Pixel(double x, double z, int color) {}

    private static final class Provider extends ElementRenderProvider<Pixel, Void> {
        private List<Pixel> pixels = List.of();
        private int index;

        @Override public void begin(ElementRenderLocation location, Void context) {
            index = 0;
            CircleSyncPayload circle = PoisonCircleClient.currentCircle();
            if (circle == null || circle.collapsed()) { pixels = List.of(); return; }
            List<Pixel> generated = new ArrayList<>();
            addCircle(generated, circle.currentX(), circle.currentZ(), circle.currentRadius(), 0xFFFF3030);
            if (circle.hasNext()) addCircle(generated, circle.nextX(), circle.nextZ(), circle.nextRadius(), 0xFFFFFFFF);
            DetectorRevealPayload reveal = PoisonCircleClient.currentDetectorReveal();
            if (reveal != null && (!circle.hasNext() || DetectorRevealStyle.shouldDrawCyan(reveal.x(), reveal.z(), reveal.radius(), circle.nextX(), circle.nextZ(), circle.nextRadius())))
                addCircle(generated, reveal.x(), reveal.z(), reveal.radius(), 0xFF54D8FF);
            pixels = generated;
        }
        @Override public boolean hasNext(ElementRenderLocation location, Void context) { return index < pixels.size(); }
        @Override public Pixel getNext(ElementRenderLocation location, Void context) { return pixels.get(index++); }
        @Override public void end(ElementRenderLocation location, Void context) { pixels = List.of(); }
    }

    private static final class Reader extends ElementReader<Pixel, Void, Renderer> {
        @Override public boolean isHidden(Pixel pixel, Void context) { return false; }
        @Override public double getRenderX(Pixel pixel, Void context, float partialTicks) { return pixel.x; }
        @Override public double getRenderZ(Pixel pixel, Void context, float partialTicks) { return pixel.z; }
        @Override public int getInteractionBoxLeft(Pixel pixel, Void context, float partialTicks) { return -1; }
        @Override public int getInteractionBoxRight(Pixel pixel, Void context, float partialTicks) { return 1; }
        @Override public int getInteractionBoxTop(Pixel pixel, Void context, float partialTicks) { return -1; }
        @Override public int getInteractionBoxBottom(Pixel pixel, Void context, float partialTicks) { return 1; }
        @Override public int getRenderBoxLeft(Pixel pixel, Void context, float partialTicks) { return -2; }
        @Override public int getRenderBoxRight(Pixel pixel, Void context, float partialTicks) { return 2; }
        @Override public int getRenderBoxTop(Pixel pixel, Void context, float partialTicks) { return -2; }
        @Override public int getRenderBoxBottom(Pixel pixel, Void context, float partialTicks) { return 2; }
        @Override public int getLeftSideLength(Pixel pixel, net.minecraft.client.Minecraft minecraft) { return 0; }
        @Override public String getMenuName(Pixel pixel) { return ""; }
        @Override public String getFilterName(Pixel pixel) { return ""; }
        @Override public int getMenuTextFillLeftPadding(Pixel pixel) { return 0; }
        @Override public int getRightClickTitleBackgroundColor(Pixel pixel) { return 0; }
        @Override public boolean shouldScaleBoxWithOptionalScale() { return false; }
    }

    private static final class Renderer extends ElementRenderer<Pixel, Void, Renderer> {
        Renderer() { super(null, new Provider(), new Reader()); }
        @Override public void preRender(ElementRenderInfo info, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider provider, boolean shadow) { }
        @Override public void postRender(ElementRenderInfo info, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider provider, boolean shadow) { }
        @Override public void renderElementShadow(Pixel pixel, boolean hovered, float scale, double x, double z, ElementRenderInfo info, GuiGraphics gui, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider provider) { }
        @Override public boolean renderElement(Pixel pixel, boolean hovered, double depth, float scale, double x, double z, ElementRenderInfo info, GuiGraphics gui, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider provider) {
            gui.fill(-2, -2, 3, 3, pixel.color); return false;
        }
        @Override public boolean shouldRender(ElementRenderLocation location, boolean shadow) { return location == ElementRenderLocation.WORLD_MAP; }
        @Override public int getOrder() { return -100; }
    }

    private static void addCircle(List<Pixel> target, double x, double z, double radius, int color) {
        for (CircleMapGeometry.Point point : CircleMapGeometry.perimeter(x, z, radius, 1025)) target.add(new Pixel(point.x(), point.z(), color));
    }
}
