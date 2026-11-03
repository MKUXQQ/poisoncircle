package com.poisoncircle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CirclePreviewTest {
    @Test
    void promotesThePreviouslyAnnouncedSafeZoneWithoutChangingIt() {
        CirclePreview preview = new CirclePreview(42, -18, 160);

        CirclePreview promoted = preview.promote();

        assertEquals(42, promoted.x());
        assertEquals(-18, promoted.z());
        assertEquals(160, promoted.radius());
    }
}
