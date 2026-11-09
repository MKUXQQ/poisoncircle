package com.poisoncircle;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AirborneRulesTest {
    @Test void duoGroupingOddKeepsLast() {
        var ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var g = AirborneRules.groups(ids, "duo");
        assertEquals(List.of(2, 1), g.stream().map(List::size).toList());
    }
    @Test void duosCoverOneTwoAndThreePlayersWithoutLoss() {
        var ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        for (int n = 1; n <= 3; n++) {
            var squads = AirborneRules.groups(ids.subList(0, n), "duo");
            assertEquals(ids.subList(0, n), squads.stream().flatMap(List::stream).toList());
            assertTrue(squads.stream().allMatch(s -> s.size() >= 1 && s.size() <= 2));
            assertEquals((n + 1) / 2, squads.size());
        }
    }
    @Test void flightAlwaysDescendsForEveryControlAndPitch() {
        for (float forward : new float[]{-1, 0, 1}) for (float strafe : new float[]{-1, 0, 1})
            for (float pitch : new float[]{-90, -45, 0, 45, 90}) {
                var v = AirborneRules.physics(forward, strafe, 137, pitch, .8);
                assertTrue(v.y() < 0, "Flight must always descend");
                assertTrue(Double.isFinite(v.x()) && Double.isFinite(v.y()) && Double.isFinite(v.z()));
            }
    }
    @Test void pitchClampsAndWIsFasterThanS() {
        assertEquals(AirborneRules.physics(1, 0, 0, 90, 1), AirborneRules.physics(1, 0, 0, 180, 1));
        assertEquals(AirborneRules.physics(1, 0, 0, -90, 1), AirborneRules.physics(1, 0, 0, -180, 1));
        var w = AirborneRules.physics(1, 0, 0, 0, 1);
        var s = AirborneRules.physics(-1, 0, 0, 0, 1);
        var neutral = AirborneRules.physics(0, 0, 0, 0, 1);
        assertTrue(s.z() > 0, "S brakes without reversing glide");
        assertTrue(neutral.z() > s.z());
        assertTrue(w.z() > neutral.z());
    }
    @Test void pointAssignmentWraps() { assertEquals(1, AirborneRules.cyclicPoint(4, 3)); assertEquals(-1, AirborneRules.cyclicPoint(0, 0)); }
    @Test void routeIncludesTwentyExit() { assertEquals(60, AirborneRules.routeDistance(40, 20)); }
    @Test void countdownAndConfigAreBounded() { assertEquals(100, AirborneRules.COUNTDOWN_TICKS); assertTrue(AirborneRules.validConfig(80, .8)); assertTrue(AirborneRules.validConfig(10, .8)); assertFalse(AirborneRules.validConfig(0, .8)); }
    @Test void physicsPitchChangesVerticalVelocity() { var flat = AirborneRules.physics(1, 0, 0, 0, 1); var dive = AirborneRules.physics(1, 0, 0, 45, 1); assertTrue(dive.y() < flat.y()); }
}
