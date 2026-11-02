package com.poisoncircle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenShakeMotionTest {
    @Test
    void fadesToZeroAfterItsShortDuration() {
        assertTrue(ScreenShakeMotion.strength(0) > ScreenShakeMotion.strength(100));
        assertEquals(0.0, ScreenShakeMotion.strength(260));
    }
}
