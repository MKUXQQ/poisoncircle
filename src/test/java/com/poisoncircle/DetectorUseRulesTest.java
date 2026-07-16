package com.poisoncircle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectorUseRulesTest {
    @Test
    void consumesTheDetectorExceptForCreativePlayers() {
        assertTrue(DetectorUseRules.shouldConsume(false));
        assertFalse(DetectorUseRules.shouldConsume(true));
    }
}
