package com.poisoncircle;

import java.util.ArrayList;
import java.util.List;

/** World-space anchor positions used by the original-border-style poison wall. */
final class PoisonCircleBarrierGeometry {
    private PoisonCircleBarrierGeometry() { }

    static List<Position> positions(double centerX, double centerZ, double radius, int count) {
        List<Position> positions = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            double angle = Math.PI * 2 * index / count;
            positions.add(new Position(centerX + Math.cos(angle) * radius, centerZ + Math.sin(angle) * radius));
        }
        return positions;
    }

    record Position(double x, double z) { }
}
