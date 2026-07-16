package com.poisoncircle;

final class DetectorRevealStyle {
    private static final double EPSILON = 0.001;
    private DetectorRevealStyle() {}
    static boolean shouldDrawCyan(double revealX, double revealZ, double revealRadius, double nextX, double nextZ, double nextRadius) {
        return Math.abs(revealX - nextX) > EPSILON || Math.abs(revealZ - nextZ) > EPSILON || Math.abs(revealRadius - nextRadius) > EPSILON;
    }
}
