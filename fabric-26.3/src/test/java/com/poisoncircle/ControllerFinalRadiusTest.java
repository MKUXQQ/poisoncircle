package com.poisoncircle;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ControllerFinalRadiusTest {
    @Test
    void controllerIgnoresSavedFinalRadiusAndAlwaysStartsAtZero() throws Exception {
        String source = Files.readString(Path.of("src/client/java/com/poisoncircle/PoisonCircleControlScreen.java"));
        assertTrue(source.contains("values.put(\"final\", \"0\")"));
        assertTrue(source.contains("fields.get(\"final\").setEditable(false)"));
        assertTrue(source.contains("yield \"start \" + decimal(initial) + \" 0\""));
        assertFalse(source.contains("ControllerFormSettings.get(\"final\""));
    }
}
