package com.poisoncircle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

final class ControllerFormSettings {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("poisoncircle-controller.properties");
    private static final Properties VALUES = new Properties();

    static {
        if (Files.isRegularFile(FILE)) {
            try (var input = Files.newInputStream(FILE)) { VALUES.load(input); }
            catch (IOException exception) { System.err.println("[PoisonCircle] Could not read controller settings: " + exception); }
        }
    }

    private ControllerFormSettings() { }

    static String get(String key, String fallback) { return VALUES.getProperty(key, fallback); }

    static void save(Map<String, String> values) {
        values.forEach(VALUES::setProperty);
        try {
            Files.createDirectories(FILE.getParent());
            Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            try (var output = Files.newOutputStream(temporary)) { VALUES.store(output, "PoisonCircle controller form values"); }
            try { Files.move(temporary, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(temporary, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException exception) {
            System.err.println("[PoisonCircle] Could not save controller settings: " + exception);
        }
    }
}
