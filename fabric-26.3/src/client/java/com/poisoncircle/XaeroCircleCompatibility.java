package com.poisoncircle;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.LoggerFactory;

/** Keep optional Xaero classes out of the normal client class-loading path. */
final class XaeroCircleCompatibility {
    private XaeroCircleCompatibility() {}

    static void install() {
        boolean minimap = FabricLoader.getInstance().isModLoaded("xaerominimap")
            || FabricLoader.getInstance().isModLoaded("xaerobetterpvp");
        boolean worldMap = FabricLoader.getInstance().isModLoaded("xaeroworldmap");
        if (!minimap && !worldMap) return;
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private boolean minimapPending = minimap;
            private boolean worldMapPending = worldMap;

            @Override public void onEndTick(net.minecraft.client.Minecraft client) {
                if (minimapPending) {
                    try { minimapPending = !XaeroMinimapCircleOverlay.install(); }
                    catch (LinkageError | RuntimeException error) {
                        minimapPending = false;
                        LoggerFactory.getLogger("poisoncircle").error("Unable to register Xaero minimap circles", error);
                    }
                }
                if (worldMapPending) {
                    try { worldMapPending = !XaeroWorldMapCircleOverlay.install(); }
                    catch (LinkageError | RuntimeException error) {
                        worldMapPending = false;
                        LoggerFactory.getLogger("poisoncircle").error("Unable to register Xaero world map circles", error);
                    }
                }
            }
        });
    }
}
