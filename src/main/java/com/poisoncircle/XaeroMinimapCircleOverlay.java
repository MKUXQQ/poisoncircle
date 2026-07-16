package com.poisoncircle;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import xaero.common.HudMod;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementReader;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderer;

import java.util.ArrayList;
import java.util.List;

/** Draws circle perimeter pixels in Xaero's transformed minimap coordinate space. */
final class XaeroMinimapCircleOverlay {
    private XaeroMinimapCircleOverlay() {}

    static void install() {
        HudMod.INSTANCE.getMinimap().getOverMapRendererHandler().add(new Renderer());
    }

    private record Pixel(double x, double z, int color) {}

    private static final class Provider extends MinimapElementRenderProvider<Pixel, Void> {
        private List<Pixel> pixels = List.of();
        private int index;

        @Override public void begin(MinimapElementRenderLocation location, Void context) {
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
        @Override public boolean hasNext(MinimapElementRenderLocation location, Void context) { return index < pixels.size(); }
        @Override public Pixel getNext(MinimapElementRenderLocation location, Void context) { return pixels.get(index++); }
        @Override public void end(MinimapElementRenderLocation location, Void context) { pixels = List.of(); }
    }

    private static final class Reader extends MinimapElementReader<Pixel, Void> {
        @Override public boolean isHidden(Pixel pixel, Void context) { return false; }
        @Override public double getRenderX(Pixel pixel, Void context, float partialTicks) { return pixel.x; }
        @Override public double getRenderY(Pixel pixel, Void context, float partialTicks) { return 0; }
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

    private static final class Renderer extends MinimapElementRenderer<Pixel, Void> {
        Renderer() { super(new Reader(), new Provider(), null); }
        @Override public boolean renderElement(Pixel pixel, boolean hovered, boolean onScreen, double depth, float scale, double x, double z, MinimapElementRenderInfo info, GuiGraphics gui, MultiBufferSource.BufferSource buffers) {
            gui.fill(-2, -2, 3, 3, pixel.color); return false;
        }
        @Override public void preRender(MinimapElementRenderInfo info, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider provider) { }
        @Override public void postRender(MinimapElementRenderInfo info, MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider provider) { }
        @Override public boolean shouldRender(MinimapElementRenderLocation location) { return location == MinimapElementRenderLocation.OVER_MINIMAP; }
        @Override public int getOrder() { return -100; }
    }

    private static void addCircle(List<Pixel> target, double x, double z, double radius, int color) {
        for (CircleMapGeometry.Point point : CircleMapGeometry.perimeter(x, z, radius, 1025)) target.add(new Pixel(point.x(), point.z(), color));
    }
}
