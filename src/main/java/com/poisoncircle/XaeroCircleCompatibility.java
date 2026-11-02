package com.poisoncircle;

/** Optional Xaero bridge.  The two renderer classes are loaded independently so either Xaero mod can be absent. */
final class XaeroCircleCompatibility {
    private static boolean minimapInstalled;
    private static boolean worldMapInstalled;

    private XaeroCircleCompatibility() {}

    static void install() {
        if (!minimapInstalled) {
            try { XaeroMinimapCircleOverlay.install(); minimapInstalled = true; } catch (LinkageError | RuntimeException ignored) { }
        }
        if (!worldMapInstalled) {
            try { XaeroWorldMapCircleOverlay.install(); worldMapInstalled = true; } catch (LinkageError | RuntimeException ignored) { }
        }
    }
}
