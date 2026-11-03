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

    @Test
    void expiresRevealAsSoonAsItsTargetRoundBecomesCurrent() {
        assertFalse(DetectorRevealStyle.isExpired(3, 2));
        assertTrue(DetectorRevealStyle.isExpired(3, 3));
        assertTrue(DetectorRevealStyle.isExpired(3, 4));
    }
}
