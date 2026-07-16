package com.poisoncircle;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircleMapGeometryTest {
    @Test
    void createsAClosedDenseCircleWithoutACenterPoint() {
        List<CircleMapGeometry.Point> points = CircleMapGeometry.perimeter(100.0, -40.0, 25.0, 128);

        assertEquals(128, points.size());
        assertTrue(points.stream().allMatch(point -> Math.hypot(point.x() - 100.0, point.z() + 40.0) > 24.9));
        assertEquals(points.get(0), points.get(points.size() - 1));
    }
}
