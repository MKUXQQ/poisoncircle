package com.poisoncircle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WallGeometryTest {
    @Test
    void clipsInteriorExteriorAndCenteredViewsWithoutLosingVisibleArcs() {
        for (double radius : new double[]{1, 25, 100, 1000, 10000}) {
            for (double distance : new double[]{0, radius * 0.5, radius, radius + 100, radius + 500}) {
                double bearing = 2.8; // Also exercises an arc crossing the -pi/pi seam.
                double x = Math.cos(bearing) * distance, z = Math.sin(bearing) * distance;
                WallGeometry.Arc arc = WallGeometry.visibleArc(radius, x, z, 256);
                for (int i = 0; i < 4096; i++) {
                    double angle = 2 * Math.PI * i / 4096;
                    if (Math.hypot(radius * Math.cos(angle) - x, radius * Math.sin(angle) - z) < 256) {
                        assertTrue(arc.segments() > 0);
                        double offset = (angle - arc.start()) % (2 * Math.PI);
                        if (offset < 0) offset += 2 * Math.PI;
                        assertTrue(offset <= arc.span() + 1e-9, "A visible point was culled");
                    }
                }
                if (arc.segments() > 0) {
                    double step = arc.span() / arc.segments();
                    assertTrue(radius * step <= 12.000001);
                    assertTrue(radius * (1 - Math.cos(step / 2)) <= 0.250001);
                }
            }
        }
    }

    @Test
    void omitsRemoteWallsAndKeepsTinyAndCenteredCircles() {
        assertEquals(0, WallGeometry.visibleArc(1000, 0, 0, 256).segments());
        assertEquals(0, WallGeometry.visibleArc(1000, 1400, 0, 256).segments());
        assertEquals(0, WallGeometry.visibleArc(0, 0, 0, 256).segments());
        assertEquals(2 * Math.PI, WallGeometry.visibleArc(1, 0, 0, 256).span());
        assertTrue(WallGeometry.visibleArc(0.01, 0, 0, 256).segments() >= 8);
        assertEquals(2 * Math.PI, WallGeometry.visibleArc(100, 0, 0, 256).span());
    }

    @Test
    void boundsNearbyWallBudgetAndKeepsCameraHeightInTheVerticalGrid() {
        WallGeometry.Arc arc = WallGeometry.visibleArc(1000, 1000, 0, 256);
        assertEquals(43, arc.segments());
        int bands = WallGeometry.bandsPerSide(256);
        assertEquals(8, bands);
        assertEquals(688, arc.segments() * 2 * bands);
        assertTrue(bands * WallGeometry.BAND_HEIGHT >= 256);
        assertTrue((bands - 1) * WallGeometry.BAND_HEIGHT < 256);
        // The renderer ranges from -bands to bands, so a vertex row lies at y = 0.
        assertEquals(0, 0 * WallGeometry.BAND_HEIGHT);
    }
}
