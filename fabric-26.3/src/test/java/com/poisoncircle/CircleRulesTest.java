package com.poisoncircle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CircleRulesTest {
    @Test void commandAndSettingsOnlyAcceptZeroFinalRadius() {
        assertTrue(CircleRules.validStart(500, 0));
        assertTrue(CircleRules.validStart(1, 0));
        for (double end : new double[] {50, 1, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertFalse(CircleRules.validStart(500, end));
        }
        for (double initial : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertFalse(CircleRules.validStart(initial, 0));
        }
    }

    @Test void generatedFifthCircleIsExactlyZeroEvenForExtremeInitialRadius() {
        for (double initial : new double[] {1, 500, 1234.567, Double.MAX_VALUE}) {
            assertEquals(0, CircleRules.radiusForStage(initial, 5));
            assertEquals(0, CircleRules.radiusForStage(initial, 6));
        }
        assertEquals(500, CircleRules.radiusForStage(500, 0));
        assertEquals(100, CircleRules.radiusForStage(500, 4));
    }

    @Test void fifthAndLaterRoundCannotRevealOrConsumeDetector() {
        assertTrue(CircleRules.canReveal(3));
        assertTrue(CircleRules.canReveal(4));
        assertFalse(CircleRules.canReveal(5));
        assertFalse(CircleRules.canReveal(6));
        assertFalse(CircleRules.canReveal(0));
    }
}
