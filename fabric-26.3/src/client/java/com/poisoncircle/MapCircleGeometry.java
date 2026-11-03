package com.poisoncircle;

/** Clips filled stroke quads before they leave the minimap's local viewport. */
final class MapCircleGeometry {
    private MapCircleGeometry() {}
    interface QuadSink { void quad(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy); }

    static void draw(double centerX, double centerY, double radius, double halfWidth, double halfView,
                     boolean round, QuadSink sink) {
        if (!Double.isFinite(centerX) || !Double.isFinite(centerY) || !Double.isFinite(radius)
            || radius <= 0 || halfWidth <= 0 || halfView <= 0) return;
        int segments = Math.max(32, Math.min(1024, (int) Math.ceil(Math.PI * Math.sqrt(radius / 0.5))));
        double[] input = new double[144], output = new double[144];
        double previousX = centerX + radius, previousY = centerY;
        for (int i = 1; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double nextX = centerX + Math.cos(angle) * radius, nextY = centerY + Math.sin(angle) * radius;
            double dx = nextX - previousX, dy = nextY - previousY, length = Math.hypot(dx, dy);
            double ox = -dy / length * halfWidth, oy = dx / length * halfWidth;
            input[0] = previousX + ox; input[1] = previousY + oy;
            input[2] = previousX - ox; input[3] = previousY - oy;
            input[4] = nextX - ox; input[5] = nextY - oy;
            input[6] = nextX + ox; input[7] = nextY + oy;
            int count = 4;
            int edges = round ? 64 : 4;
            for (int edge = 0; edge < edges && count > 0; edge++) {
                double normalAngle = 2 * Math.PI * edge / edges;
                double nx = Math.cos(normalAngle), ny = Math.sin(normalAngle);
                // Inscribed polygon never draws outside the round map mask.
                double limit = round ? halfView * Math.cos(Math.PI / edges) : halfView;
                int written = 0;
                double ax = input[(count - 1) * 2], ay = input[(count - 1) * 2 + 1];
                double ad = ax * nx + ay * ny - limit;
                for (int p = 0; p < count; p++) {
                    double bx = input[p * 2], by = input[p * 2 + 1], bd = bx * nx + by * ny - limit;
                    if ((ad <= 0) != (bd <= 0)) {
                        double t = ad / (ad - bd);
                        output[written++] = ax + t * (bx - ax); output[written++] = ay + t * (by - ay);
                    }
                    if (bd <= 0) { output[written++] = bx; output[written++] = by; }
                    ax = bx; ay = by; ad = bd;
                }
                count = written / 2;
                double[] swap = input; input = output; output = swap;
            }
            for (int p = 1; p + 1 < count; p++)
                sink.quad(input[0], input[1], input[p * 2], input[p * 2 + 1],
                    input[(p + 1) * 2], input[(p + 1) * 2 + 1], input[(p + 1) * 2], input[(p + 1) * 2 + 1]);
            previousX = nextX; previousY = nextY;
        }
    }
}
