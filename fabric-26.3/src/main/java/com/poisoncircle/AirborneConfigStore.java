package com.poisoncircle;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

final class AirborneConfigStore {
    private static final long MAX_BYTES = 1_048_576;
    private AirborneConfigStore() {}

    static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("poisoncircle").resolve("airborne.json");
    }

    static JsonObject load(Path path) throws IOException {
        if (!Files.exists(path)) return new JsonObject();
        if (Files.size(path) > MAX_BYTES) throw new IOException("Airborne configuration exceeds 1 MiB");
        try {
            var root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) throw new IOException("Airborne configuration must be a JSON object");
            return root.getAsJsonObject();
        } catch (com.google.gson.JsonParseException | IllegalStateException exception) {
            throw new IOException("Invalid airborne configuration: " + path, exception);
        }
    }

    static void save(Path path, JsonObject data) throws IOException {
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new IOException("Airborne configuration exceeds 1 MiB");
        Path parent = path.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "airborne-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
