package com.poisoncircle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoisonCircleVisualStateTest {
    @Test
    void visualStateUpdatesItsWorldPositionAndRadiusWithoutCollision() {
        PoisonCircleVisualState state = new PoisonCircleVisualState(0, 0, 0);

        PoisonCircleVisualState updated = state.moveTo(120, -48, 350);

        assertEquals(120, updated.x());
        assertEquals(-48, updated.z());
        assertEquals(350, updated.radius());
        assertFalse(updated.collides());
    }

    @Test
    void segmentLayoutPlacesEveryWallEntityOnTheWorldCircle() {
        var positions = PoisonCircleSegmentLayout.positions(100, -50, 80, 16);

        assertEquals(16, positions.size());
        assertEquals(80, Math.hypot(positions.get(0).x() - 100, positions.get(0).z() + 50), 0.001);
        assertEquals(0, positions.get(0).angle(), 0.001);
    }

    @Test
    void barrierMeshUsesWorldSpacePositionsAroundTheCircle() {
        var vertices = PoisonCircleBarrierGeometry.positions(40, -25, 60, 24);

        assertEquals(24, vertices.size());
        assertEquals(60, Math.hypot(vertices.get(6).x() - 40, vertices.get(6).z() + 25), 0.001);
    }

    @Test
    void terrainQualityRejectsHighOrUnevenOrFluidCandidates() {
        assertTrue(PoisonCircleTerrainQuality.accepts(new int[] {66, 67, 66, 68, 67}, 63, false));
        assertFalse(PoisonCircleTerrainQuality.accepts(new int[] {66, 73, 66, 68, 67}, 63, false));
        assertFalse(PoisonCircleTerrainQuality.accepts(new int[] {101, 102, 100, 101, 102}, 63, false));
        assertFalse(PoisonCircleTerrainQuality.accepts(new int[] {66, 67, 66, 68, 67}, 63, true));
    }
}
