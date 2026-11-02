package com.poisoncircle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectorRevealScopeTest {
    @Test
    void revealIsVisibleOnlyToThePlayerWhoReceivedIt() {
        DetectorRevealScope scope = new DetectorRevealScope("user-a");

        assertTrue(scope.visibleTo("user-a"));
        assertFalse(scope.visibleTo("user-b"));
    }
}
