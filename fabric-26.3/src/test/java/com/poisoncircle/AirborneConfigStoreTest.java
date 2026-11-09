package com.poisoncircle;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AirborneConfigStoreTest {
    @TempDir Path directory;

    @Test void roundTripUnicodeAndReplaceAtomically() throws Exception {
        Path path = directory.resolve("world/poisoncircle/airborne.json");
        assertTrue(AirborneConfigStore.load(path).isEmpty());
        JsonObject state = new JsonObject();
        state.addProperty("name", "山顶双人组");
        AirborneConfigStore.save(path, state);
        assertEquals(state, AirborneConfigStore.load(path));
        state.addProperty("name", "海边单人组");
        AirborneConfigStore.save(path, state);
        assertEquals(state, AirborneConfigStore.load(path));
        try (var entries = Files.list(path.getParent())) { assertEquals(1, entries.count()); }
    }

    @Test void invalidJsonDoesNotSilentlyBecomeEmptyConfiguration() throws Exception {
        Path path = directory.resolve("airborne.json");
        Files.writeString(path, "[1,2]");
        assertThrows(IOException.class, () -> AirborneConfigStore.load(path));
        assertEquals("[1,2]", Files.readString(path));
        Files.writeString(path, "{broken");
        assertThrows(IOException.class, () -> AirborneConfigStore.load(path));
    }

    @Test void oversizedSavePreservesPreviousFile() throws Exception {
        Path path = directory.resolve("airborne.json");
        JsonObject small = new JsonObject(); small.addProperty("name", "keep");
        AirborneConfigStore.save(path, small);
        JsonObject large = new JsonObject(); large.addProperty("name", "x".repeat(1_048_576));
        assertThrows(IOException.class, () -> AirborneConfigStore.save(path, large));
        assertEquals(small, AirborneConfigStore.load(path));
    }
}
