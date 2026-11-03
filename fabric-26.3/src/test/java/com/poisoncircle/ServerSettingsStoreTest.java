package com.poisoncircle;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class ServerSettingsStoreTest {
    @TempDir Path directory;

    @Test void roundTripsCommandsAsDataWithoutExecutingThem() {
        ServerSettingsStore store = new ServerSettingsStore(directory.resolve("world.json"), Logger.getAnonymousLogger());
        JsonObject global = new JsonObject();
        global.addProperty("baseDamage", 7);
        global.addProperty("command", "say stored only");
        store.save(Map.of("global", global));
        assertEquals(global, store.load().get("global"));
    }

    @Test void persistsLobbyDimensionHeightRadiusAndDisabledFlag() {
        ServerSettingsStore store = new ServerSettingsStore(directory.resolve("lobby.json"), Logger.getAnonymousLogger());
        LobbySettings lobby = new LobbySettings(false, "minecraft:the_nether", 12.5, 80, -19.5, 64);
        store.save(Map.of("lobby", lobby.toJson()));
        assertEquals(lobby, LobbySettings.fromJson(store.load().get("lobby")));
    }

    @Test void malformedJsonFallsBackToEmptySettings() throws Exception {
        Path path = directory.resolve("broken.json");
        Files.writeString(path, "{broken");
        assertTrue(new ServerSettingsStore(path, Logger.getAnonymousLogger()).load().isEmpty());
    }
}
