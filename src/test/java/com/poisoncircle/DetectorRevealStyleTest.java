package com.poisoncircle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectorRevealStyleTest {
    @Test
    void turnsOffCyanWhenTheRevealedCircleBecomesThePublicNextCircle() {
        assertFalse(DetectorRevealStyle.shouldDrawCyan(10, 20, 30, 10, 20, 30));
        assertTrue(DetectorRevealStyle.shouldDrawCyan(10, 20, 30, 10, 20, 29));
    }
}
