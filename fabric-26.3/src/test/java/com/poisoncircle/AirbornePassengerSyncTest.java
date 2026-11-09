package com.poisoncircle;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Regression contracts for the two vanilla operations that otherwise undo airborne positions. */
class AirbornePassengerSyncTest {
    private String source(String file) throws Exception {
        return Files.readString(Path.of("src/main/java/com/poisoncircle/"+file));
    }

    @Test void nativeRiderTickUsesTheSameFeetPositionAsTheServerTimeline() throws Exception {
        String plane=source("AirbornePlaneEntity.java");
        int start=plane.indexOf("@Override protected void positionRider(");
        assertTrue(start>=0,"Override native seat-attachment subtraction on both logical sides");
        String rider=plane.substring(start,plane.indexOf("public Vec3 routeVelocity()",start));
        assertTrue(rider.contains("Vec3 feet = getPassengerRidingPosition(passenger)"));
        assertTrue(rider.contains("move.accept(passenger, feet.x, feet.y, feet.z)"));
        assertFalse(rider.contains("super.positionRider"));
        assertFalse(rider.contains("getVehicleAttachmentPoint"));
        assertFalse(rider.contains("isClientSide"),"Client and server must use the same feet semantics");
    }

    @Test void teleportAckOnlyRedirectsItsTwoPositionWritesAndKeepsNativeValidation() throws Exception {
        String mixin=source("mixin/AirbornePlayerMovementMixin.java");
        assertEquals(2,mixin.split("@Redirect\\(method=\"handleAcceptTeleportPacket\"",-1).length-1);
        assertTrue(mixin.contains("Lnet/minecraft/server/level/ServerPlayer;absSnapTo(DDDFF)V"));
        assertTrue(mixin.contains("Lnet/minecraft/server/network/ServerGamePacketListenerImpl;handlePlayerPositionChange(DDDFFZZ)V"));
        assertFalse(mixin.contains("@Inject(method=\"handleAcceptTeleportPacket\""),"Do not cancel the ID check, pending-position cleanup or dimension acknowledgement");
        assertFalse(mixin.contains("awaitingTeleport"));
        assertFalse(mixin.contains("awaitingPositionFromClient"));
    }

    @Test void acknowledgementRetainsOrdinaryMovementAndResetsFlightMovementBaselines() throws Exception {
        String mixin=source("mixin/AirbornePlayerMovementMixin.java");
        assertTrue(mixin.contains("if(!AirborneGame.controlsMovement(target.getUUID()))target.absSnapTo(x,y,z,yaw,pitch)"));
        assertTrue(mixin.contains("if(AirborneGame.controlsMovement(player.getUUID()))resetPosition()"));
        assertTrue(mixin.contains("else handlePlayerPositionChange(x,y,z,yaw,pitch,onGround,horizontalCollision)"));
    }
}
