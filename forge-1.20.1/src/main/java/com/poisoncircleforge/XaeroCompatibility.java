package com.poisoncircleforge;

final class XaeroCompatibility {
    private static boolean installed;
    static void install() { if (installed) return; installed=true; try { XaeroMinimapCircleOverlay.install(); XaeroWorldMapCircleOverlay.install(); } catch (LinkageError | RuntimeException ignored) { } }
}
