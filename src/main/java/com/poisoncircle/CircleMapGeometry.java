package com.poisoncircle;

import java.util.ArrayList;
import java.util.List;

/** Pure geometry used by Xaero overlays; it never exposes a circle centre as a map marker. */
final class CircleMapGeometry {
    private CircleMapGeometry() {}

    static List<Point> perimeter(double centerX, double centerZ, double radius, int pointCount) {
        int count = Math.max(3, pointCount);
        List<Point> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            if (index == count - 1) {
                result.add(result.getFirst());
                continue;
            }
            double angle = Math.PI * 2.0 * index / (count - 1);
            result.add(new Point(centerX + Math.cos(angle) * radius, centerZ + Math.sin(angle) * radius));
        }
        return result;
    }

    record Point(double x, double z) {}
}
