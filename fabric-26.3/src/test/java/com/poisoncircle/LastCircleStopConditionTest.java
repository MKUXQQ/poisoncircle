package com.poisoncircle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastCircleStopConditionTest {
    @Test
    void stopsOnlyWhenAtLeastOneEligiblePlayerExistsAndAllAreDead() {
        assertTrue(LastCircleStopCondition.shouldStop(2, 0));
        assertFalse(LastCircleStopCondition.shouldStop(2, 1));
        assertFalse(LastCircleStopCondition.shouldStop(0, 0));
    }
}
