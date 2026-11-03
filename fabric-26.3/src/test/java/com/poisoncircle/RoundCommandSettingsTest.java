package com.poisoncircle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RoundCommandSettingsTest {
    @Test
    void storesCommandsByTheirShrinkRound() {
        RoundCommandSettings settings = new RoundCommandSettings();
        settings.set(3, "say 第三圈开始");

        assertEquals("say 第三圈开始", settings.get(3));
        assertNull(settings.get(2));
    }
}
