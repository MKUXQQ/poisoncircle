package com.poisoncircle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LobbySettingsTest {
    @Test void cylinderIncludesBoundaryAndIgnoresReferenceHeight() {
        LobbySettings lobby = new LobbySettings(true, "minecraft:overworld", 10, -500, 20, 5);
        assertTrue(lobby.contains("minecraft:overworld", 13, 24));
        assertFalse(lobby.contains("minecraft:overworld", 13, 24.001));
        assertFalse(lobby.contains("minecraft:the_nether", 10, 20));
        assertFalse(lobby.disabled().contains("minecraft:overworld", 10, 20));
    }
    @Test void rejectsInvalidBounds() {
        assertThrows(IllegalArgumentException.class, () -> new LobbySettings(true, "minecraft:overworld", Double.NaN, 0, 0, 5));
        assertThrows(IllegalArgumentException.class, () -> new LobbySettings(true, "minecraft:overworld", 0, 0, 0, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new LobbySettings(true, "minecraft:overworld", 0, 0, 0, 0));
    }
}
