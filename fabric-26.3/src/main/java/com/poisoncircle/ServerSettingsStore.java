package com.poisoncircle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ServerSettingsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private final Logger logger;

    ServerSettingsStore(Path path, Logger logger) { this.path = path; this.logger = logger; }

    Map<String, JsonObject> load() {
        if (!Files.exists(path)) return Map.of();
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            Map<String, JsonObject> result = new HashMap<>();
            for (var entry : root.entrySet()) if (entry.getValue().isJsonObject()) result.put(entry.getKey(), entry.getValue().getAsJsonObject());
            return result;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load poisoncircle settings from " + path, e);
            return Map.of();
        }
    }

    void save(Map<String, JsonObject> settings) {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(settings));
            try { Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { logger.log(Level.SEVERE, "Failed to save poisoncircle settings to " + path, e); }
    }

}
