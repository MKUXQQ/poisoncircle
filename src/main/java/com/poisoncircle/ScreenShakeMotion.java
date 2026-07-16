package com.poisoncircle;

/** Short, restrained camera motion for a confirmed poison-circle hit. */
final class ScreenShakeMotion {
    static final long DURATION_MILLIS = 250;
    private ScreenShakeMotion() {}

    static double strength(long elapsedMillis) {
        if (elapsedMillis < 0 || elapsedMillis >= DURATION_MILLIS) return 0.0;
        double fade = 1.0 - (double) elapsedMillis / DURATION_MILLIS;
        return fade * fade;
    }
}
