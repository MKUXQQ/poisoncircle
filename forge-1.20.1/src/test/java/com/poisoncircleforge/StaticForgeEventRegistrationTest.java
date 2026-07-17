package com.poisoncircleforge;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticForgeEventRegistrationTest {
    @Test
    void staticCommandHandlerIsRegisteredAsAClass() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/poisoncircleforge/PoisonCircleForge.java"));
        assertTrue(source.contains("MinecraftForge.EVENT_BUS.register(PoisonCircleForge.class)"),
                "Forge static @SubscribeEvent handlers must be registered with their Class, not an instance");
    }
}
