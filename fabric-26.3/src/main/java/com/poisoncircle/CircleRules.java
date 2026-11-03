package com.poisoncircle;

/** Shared invariants for command validation, persisted settings and generated circles. */
final class CircleRules {
    static final double FINAL_RADIUS = 0;
    private CircleRules() { }

    static boolean validStart(double initial, double end) {
        return Double.isFinite(initial) && initial >= 1 && end == FINAL_RADIUS;
    }

    static double radiusForStage(double initial, int completed) {
        return completed >= 5 ? FINAL_RADIUS : initial * ((5 - Math.max(0, completed)) / 5.0);
    }

    static boolean canReveal(int targetRound) { return targetRound > 0 && targetRound < 5; }
}
