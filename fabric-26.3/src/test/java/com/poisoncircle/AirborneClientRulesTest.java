package com.poisoncircle;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirborneClientRulesTest {
    private static JsonObject snapshot() {
        return JsonParser.parseString("""
            {"operator":true,"selectedGroup":"work","groups":[
              {"id":"other","points":[{"dimension":"minecraft:overworld","x":0,"y":64,"z":0}]},
              {"id":"work","points":[
                {"dimension":"minecraft:overworld","x":128,"y":64,"z":0},
                {"dimension":"minecraft:overworld","x":129,"y":64,"z":0},
                {"dimension":"minecraft:the_nether","x":0,"y":64,"z":0},
                {"dimension":"minecraft:overworld","x":"NaN","y":64,"z":0}]}]}
            """).getAsJsonObject();
    }

    @Test void onlyOperatorWithSetterSeesCurrentDimensionNearbyWorkGroup() {
        JsonObject state = snapshot();
        var markers = AirborneClientRules.markers(state, true, "minecraft:overworld", 0, 64, 0);
        assertEquals(1, markers.size());
        assertEquals(128, markers.getFirst().x());
        assertTrue(AirborneClientRules.markers(state, false, "minecraft:overworld", 0, 64, 0).isEmpty());
        state.addProperty("operator", false);
        assertTrue(AirborneClientRules.markers(state, true, "minecraft:overworld", 0, 64, 0).isEmpty());
    }

    @Test void malformedStateFailsClosedAndPointBudgetIsEight() {
        assertTrue(AirborneClientRules.markers(new JsonObject(), true, "minecraft:overworld", 0, 0, 0).isEmpty());
        JsonObject state = snapshot();
        var points = state.getAsJsonArray("groups").get(1).getAsJsonObject().getAsJsonArray("points");
        points.remove(3); points.remove(2); points.remove(1);
        for (int i=0;i<30;i++) points.add(points.get(0).deepCopy());
        assertEquals(8, AirborneClientRules.markers(state, true, "minecraft:overworld", 0, 64, 0).size());
    }

    @Test void emptySelectionDisplaysDraftsAndNamedSelectionDoesNotLeakDrafts() {
        JsonObject state = snapshot();
        var draft = JsonParser.parseString("""
            [{"dimension":"minecraft:overworld","x":3,"y":64,"z":4},
             {"dimension":"minecraft:the_nether","x":0,"y":64,"z":0}]
            """).getAsJsonArray();
        state.add("draftPoints", draft);
        state.addProperty("selectedGroup", "");
        var markers = AirborneClientRules.markers(state, true, "minecraft:overworld", 0, 64, 0);
        assertEquals(1, markers.size());
        assertEquals(3, markers.getFirst().x());
        state.addProperty("selectedGroup", "work");
        assertEquals(128, AirborneClientRules.markers(state, true, "minecraft:overworld", 0, 64, 0).getFirst().x());
        state.addProperty("selectedGroup", "missing");
        assertTrue(AirborneClientRules.markers(state, true, "minecraft:overworld", 0, 64, 0).isEmpty());
    }

    @Test void sharedGroupSelectionUpdatesMarkersAndModeDoesNotResetIt() {
        JsonObject state = snapshot();
        state.addProperty("activeMode", "solo");
        var solo = AirborneClientRules.markers(state, true, "minecraft:overworld", 0, 64, 0);
        state.addProperty("activeMode", "duo");
        assertEquals(solo, AirborneClientRules.markers(state, true, "minecraft:overworld", 0, 64, 0));
        state.addProperty("selectedGroup", "other");
        var selected = AirborneClientRules.markers(state, true, "minecraft:overworld", 0, 64, 0);
        assertEquals(1, selected.size());
        assertEquals(0, selected.getFirst().x());
    }

    @Test void warningLastsExactlyThreeSecondsAndLandingHasSeparateLegPitch() {
        assertFalse(AirborneClientRules.warningVisible(-1));
        assertTrue(AirborneClientRules.warningVisible(0));
        assertTrue(AirborneClientRules.warningVisible(2_999_999_999L));
        assertFalse(AirborneClientRules.warningVisible(3_000_000_000L));
        assertNotEquals(AirborneClientRules.legPitch(false), AirborneClientRules.legPitch(true));
    }
}
