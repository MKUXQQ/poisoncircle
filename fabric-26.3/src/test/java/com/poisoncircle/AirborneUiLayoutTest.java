package com.poisoncircle;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

final class AirborneUiLayoutTest {
    @Test void countdownAndActiveMatchLockMutations() {
        assertTrue(AirborneUiRules.locked("countdown"));
        assertTrue(AirborneUiRules.locked("active"));
        assertTrue(AirborneUiRules.locked("running"));
        assertFalse(AirborneUiRules.locked("idle"));
        assertFalse(AirborneUiRules.locked(null));
    }

    @Test void flightHeightAcceptsEveryFinitePositiveValueAndSpeedKeepsItsLimits() {
        assertTrue(AirborneUiRules.validFlight(Double.MIN_VALUE, .2));
        assertTrue(AirborneUiRules.validFlight(Double.MAX_VALUE, 2));
        assertTrue(AirborneUiRules.validFlight(200, .8));
        assertTrue(AirborneUiRules.validFlight(23, .8));
        assertTrue(AirborneUiRules.validFlight(513, .8));
        assertFalse(AirborneUiRules.validFlight(0, .8));
        assertFalse(AirborneUiRules.validFlight(-1, .8));
        assertFalse(AirborneUiRules.validFlight(80, .19));
        assertFalse(AirborneUiRules.validFlight(80, 2.01));
        assertFalse(AirborneUiRules.validFlight(Double.NaN, .8));
        assertFalse(AirborneUiRules.validFlight(Double.POSITIVE_INFINITY, .8));
        assertFalse(AirborneUiRules.validFlight(80, Double.POSITIVE_INFINITY));
    }

    @Test void bothModesReadTheSameSelectionAndCompleteGroupListWithoutMutatingSnapshot() {
        JsonObject state=state();String before=state.toString();
        assertEquals("duo",AirborneUiRules.activeMode(state));
        assertEquals("work-group",AirborneUiRules.selectedGroup(state,"solo"));
        assertEquals("work-group",AirborneUiRules.selectedGroup(state,"duo"));
        assertEquals("solo-a",AirborneUiRules.groups(state,"solo").get(0).getAsJsonObject().get("id").getAsString());
        assertEquals(2,AirborneUiRules.groups(state,"solo").size());
        assertEquals(2,AirborneUiRules.groups(state,"duo").size());
        assertEquals(2,AirborneUiRules.groups(state,null).size());
        assertEquals(before,state.toString());
        state.addProperty("activeMode","solo");
        assertEquals("work-group",AirborneUiRules.selectedGroup(state,"solo"));
        assertEquals("多人",AirborneUiRules.modeLabel("duo"));
    }

    @Test void anEmptySelectionSupportsCurrentModeDraftAndEmptyGroupLists() {
        JsonObject state=state();
        state.addProperty("selectedGroup","");
        assertEquals("",AirborneUiRules.selectedGroup(state,"solo"));
        assertEquals("",AirborneUiRules.selectedGroup(state,"duo"));
        assertEquals("draft-c",AirborneUiRules.points(state,"").get(0).getAsJsonObject().get("id").getAsString());
        assertEquals("point-a",AirborneUiRules.points(state,"solo-a").get(0).getAsJsonObject().get("id").getAsString());
        assertTrue(AirborneUiRules.points(state,"missing").isEmpty());
        assertTrue(AirborneUiRules.groups(new JsonObject(),"solo").isEmpty());
        assertEquals("",AirborneUiRules.selectedGroup(new JsonObject(),"duo"));
    }

    @Test void pointRequestsContainOnlyGroupAndPointForServerAuthority() {
        JsonObject request=AirborneUiRules.pointRequest("solo-a","point-a");
        assertEquals(2,request.size());
        assertEquals("solo-a",request.get("group").getAsString());
        assertEquals("point-a",request.get("id").getAsString());
        assertEquals("",AirborneUiRules.pointRequest("","draft-c").get("group").getAsString());
    }

    private static JsonObject state() {
        return JsonParser.parseString("""
            {"activeMode":"duo","selectedGroup":"work-group","selectedGroups":{"solo":"solo-a","duo":"duo-b"},
             "draftPoints":[{"id":"draft-c"}],"groups":[
                {"id":"solo-a","name":"1","mode":"solo","points":[{"id":"point-a"}]},
                {"id":"duo-b","name":"2","mode":"duo","points":[]}]}
            """).getAsJsonObject();
    }
}
