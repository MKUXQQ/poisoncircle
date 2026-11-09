package com.poisoncircle;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CircleAudioTest {
    @Test void shrinkingEdgeOnlyPlaysOnceAndLateJoinDoesNotReplay() {
        CircleAudioTransitions edges = new CircleAudioTransitions();
        assertFalse(edges.update(1, true, true, false));
        assertTrue(edges.update(1, false, true, false));
        for (int tick = 0; tick < 200; tick++) assertFalse(edges.update(1, false, true, false));
        assertFalse(edges.update(1, true, true, false));
        assertFalse(edges.update(1, false, true, false));
        assertTrue(edges.update(2, false, true, false));
        assertFalse(edges.update(2, false, true, false));
        assertFalse(edges.update(3, false, true, true));
        CircleAudioTransitions late = new CircleAudioTransitions();
        assertFalse(late.update(1, false, true, false));
        assertFalse(late.update(1, false, true, false));
        assertFalse(late.update(1, false, false, false));
        assertFalse(late.update(1, true, true, false));
        assertTrue(late.update(1, false, true, false));
    }

    @Test void warningHasOneEdgePerRoundAndDamageUsesMonotonicOneSecondInterval() {
        CircleAudioTransitions edges = new CircleAudioTransitions();
        assertFalse(edges.update(1, true, true, false));
        assertTrue(edges.warning());
        assertFalse(edges.warning());
        assertTrue(edges.update(1, false, true, false));
        assertFalse(edges.warning());
        assertFalse(edges.update(2, true, true, false));
        assertTrue(edges.warning());
        assertFalse(edges.warning());
        long start = -2_000_000_000L;
        assertTrue(edges.damage(start));
        assertFalse(edges.damage(start));
        assertFalse(edges.damage(start + 999_999_999L));
        assertTrue(edges.damage(start + 1_000_000_000L));
        assertFalse(edges.damage(start + 1_100_000_000L));
        assertTrue(new CircleAudioTransitions().damage(start));
    }

    @Test void packetsTriggerCustomCuesAndMissingAssetsStaySilent() throws Exception {
        JsonObject sounds = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/poisoncircle/sounds.json"))).getAsJsonObject();
        for (String name : new String[]{"warning", "closing", "damage"})
            assertEquals("poisoncircle:circle/" + name, sounds.getAsJsonObject("circle_" + name).getAsJsonArray("sounds").get(0).getAsJsonObject().get("name").getAsString());
        String audio = Files.readString(Path.of("src/client/java/com/poisoncircle/CircleAudioClient.java"));
        assertTrue(audio.contains("getResource(asset).isEmpty()"));
        assertTrue(audio.contains("ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE"));
        assertTrue(audio.contains("ClientPlayConnectionEvents.DISCONNECT"));
        assertTrue(audio.contains("transitions.damage(System.nanoTime())"));
        assertFalse(audio.contains("SoundEvents."));
        String client = Files.readString(Path.of("src/client/java/com/poisoncircle/PoisonCircleClient.java"));
        assertTrue(client.contains("CircleAudioClient.apply(payload)"));
        assertTrue(client.contains("CircleAudioClient.damage()"));
        String warning = Files.readString(Path.of("src/client/java/com/poisoncircle/ShrinkWarningClient.java"));
        assertEquals(1, warning.split("CircleAudioClient.warning\\(\\)", -1).length - 1);
    }
}
