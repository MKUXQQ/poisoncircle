package com.poisoncircle;

/** Distance-clipped wall tessellation; independent of the rendering API. */
final class WallGeometry {
    static final int BAND_HEIGHT = 32;
    private static final double MAX_EDGE = 12.0;
    private static final double MAX_SAGITTA = 0.25;

    private WallGeometry() {}

    record Arc(double start, double span, int segments) {}

    static Arc visibleArc(double radius, double cameraX, double cameraZ, double far) {
        if (!(radius > 0) || !(far > 0)) return new Arc(0, 0, 0);
        double distance = Math.hypot(cameraX, cameraZ);
        // Include the chord's inward deviation, so distance culling cannot open a visible gap.
        double reach = far + MAX_SAGITTA;
        if (Math.abs(distance - radius) > reach) return new Arc(0, 0, 0);
        double halfSpan = Math.PI;
        if (distance > 0 && distance + radius > reach) {
            double cosine = (distance * distance + radius * radius - reach * reach)
                / (2 * distance * radius);
            halfSpan = Math.acos(Math.max(-1, Math.min(1, cosine)));
        }
        double span = 2 * halfSpan;
        double step = Math.min(Math.PI / 4, Math.min(MAX_EDGE / radius,
            2 * Math.acos(Math.max(-1, 1 - MAX_SAGITTA / radius))));
        int segments = Math.max(1, (int) Math.ceil(span / step));
        return new Arc(Math.atan2(cameraZ, cameraX) - halfSpan, span, segments);
    }

    static int bandsPerSide(double far) {
        return Math.max(1, (int) Math.ceil(far / BAND_HEIGHT));
    }
}
