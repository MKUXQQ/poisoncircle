package com.poisoncircle;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AirborneSoundsTest {
    @Test void observedDoorOpeningPlaysOnceAndLateTrackingDoesNotReplayIt() {
        AirborneSoundTransitions edges = new AirborneSoundTransitions();
        assertFalse(edges.door(0));
        assertTrue(edges.door(.02F));
        for (int tick = 1; tick <= 100; tick++) assertFalse(edges.door(Math.min(1, tick / 30F)));
        assertFalse(edges.door(0));
        assertTrue(edges.door(.1F));
        AirborneSoundTransitions late = new AirborneSoundTransitions();
        assertFalse(late.door(1));
        assertFalse(late.door(1));
    }

    @Test void localExitOnlyFiresOnBoardingToDivingEdge() {
        AirborneSoundTransitions edges = new AirborneSoundTransitions();
        assertFalse(edges.phase("aboard"));
        assertFalse(edges.phase("departing"));
        assertTrue(edges.phase("diving"));
        for (int tick = 0; tick < 100; tick++) assertFalse(edges.phase("diving"));
        assertFalse(edges.phase("landing"));
        assertFalse(edges.phase("none"));
        assertFalse(edges.phase("diving"));
        assertFalse(edges.phase("aboard"));
        assertTrue(edges.phase("diving"));
        assertFalse(new AirborneSoundTransitions().phase("diving"));
    }

    @Test void soundsUseOnlyCustomFilesAndClientOwnsTheirCleanup() throws Exception {
        Path resources = Path.of("src/main/resources/assets/poisoncircle");
        JsonObject sounds = JsonParser.parseString(Files.readString(resources.resolve("sounds.json"))).getAsJsonObject();
        for (String name : new String[]{"engine", "door", "exit"}) {
            JsonObject file = sounds.getAsJsonObject("airborne_" + name).getAsJsonArray("sounds").get(0).getAsJsonObject();
            assertEquals("poisoncircle:airborne/" + name, file.get("name").getAsString());
        }
        String client = Files.readString(Path.of("src/client/java/com/poisoncircle/AirbornePlaneSounds.java"));
        assertTrue(client.contains("if (aircraft.engine == null)"));
        assertTrue(client.contains("AIRCRAFT.computeIfAbsent(plane"));
        assertTrue(client.contains("ClientPlayConnectionEvents.DISCONNECT.register"));
        assertTrue(client.contains("ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register"));
        assertTrue(client.contains("loader.addListenerOrdering(ResourceReloaderKeys.Client.SOUNDS, id)"));
        assertTrue(client.contains("entry.getValue().stop(client)"));
        assertTrue(client.contains("client.getSoundManager().stop(engine)"));
        assertTrue(client.contains("Minecraft.getInstance().level != plane.level()"));
        assertTrue(client.contains("attenuation = SoundInstance.Attenuation.LINEAR"));
        assertFalse(client.contains("SoundEvents."), "Do not substitute vanilla recordings for supplied aircraft audio");
    }
}
