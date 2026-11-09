package com.poisoncircle;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

class AirborneIntegrationTest {
    private String main(String path) throws Exception { return Files.readString(Path.of("src/main/"+path)); }
    @Test void serverMovementIsCancelledOnlyAfterThreadCheck() throws Exception {
        String source=main("java/com/poisoncircle/mixin/AirbornePlayerMovementMixin.java");
        assertTrue(source.contains("ensureRunningOnSameThread"));
        assertTrue(source.contains("shift=At.Shift.AFTER"));
        assertTrue(source.contains("handleMovePlayer"));
        assertTrue(source.contains("handleMoveVehicle"));
        assertTrue(main("resources/fabric.mod.json").contains("poisoncircle.mixins.json"));
    }
    @Test void twoSeatsDoNotInheritVanillaSinglePassengerRestriction() throws Exception {
        String source=main("java/com/poisoncircle/AirbornePlaneEntity.java");
        assertTrue(source.contains("getPassengers().size() < 2"));
        assertFalse(source.contains("super.canAddPassenger"));
        assertTrue(source.contains("getPassengerRidingPosition"));
        assertTrue(source.contains("LinearInterpolationHandler.create(this, 3)"));
    }
    @Test void flightAndConfigurationRemainBoundedAndRecoverable() throws Exception {
        String source=main("java/com/poisoncircle/AirborneGame.java");
        assertFalse(source.contains(".noSave()"));
        assertTrue(main("java/com/poisoncircle/AirbornePlaneEntity.java").contains("shouldBeSaved() { return false; }"));
        assertTrue(source.contains("flightTicks>1200"));
        assertTrue(source.contains("applyConfig(before)"));
        assertTrue(source.contains("AirborneConfigStore.save"));
        assertTrue(source.contains("LAST_ADMIN"));
        assertTrue(source.contains("d.length()<=128"));
    }
    @Test void soloDoesNotCreateTeamsAndCompactAircraftUsesMatchingScale() throws Exception {
        String source=main("java/com/poisoncircle/AirborneGame.java");
        assertTrue(source.contains("PlayerTeam team=null;String name=null;if(activeMode.equals(\"duo\"))"));
        assertTrue(source.contains("if(team!=null)s.getScoreboard().addPlayerToTeam"));
        assertTrue(source.contains("playScanSound"));
        String renderer=Files.readString(Path.of("src/client/java/com/poisoncircle/AirbornePlaneRenderer.java"));
        assertTrue(renderer.contains("pose.scale(16F, 16F, 16F)"));
        assertTrue(renderer.contains("inflate(26, 10, 26)"));
    }
    @Test void neutralFlightCannotHoverIndefinitely() {
        assertTrue(AirborneRules.physics(0,0,0,0,.8).y()<=-.18);
    }
}
