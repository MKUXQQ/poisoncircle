package com.poisoncircle;

import java.nio.file.Files;
import java.nio.file.Path;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirborneAvatarPoseTest {
    private String mixin(String name) throws Exception {
        return Files.readString(Path.of("src/client/java/com/poisoncircle/mixin/" + name + ".java"));
    }

    @Test void controlledPhasesAreExplicitAndNormalPlayersKeepVanilla() throws Exception {
        for (String phase : new String[]{"aboard", "departing", "diving", "landing"})
            assertTrue(AirborneAvatarPose.controlled(phase));
        for (String phase : new String[]{"none", "", "flight", "swimming", "gliding"})
            assertFalse(AirborneAvatarPose.controlled(phase));
        assertFalse(AirborneAvatarPose.controlled(null));
        String cape = mixin("AirborneCapeModelMixin");
        assertTrue(cape.contains("if (!AirborneAvatarPose.controlled(AirborneFlightClient.phaseFor(state.id))) return;"));
        assertTrue(cape.contains("cape.setRotation(0, (float) Math.PI, 0)"));
        assertFalse(cape.contains("visible =") || cape.contains("cancel()"));
    }

    @Test void sittingTransitionsToUprightBeforeWalking() throws Exception {
        assertEquals(-Math.PI / 2, AirborneAvatarPose.seatedLegPitch(0), 1e-6);
        assertEquals(-Math.PI / 4, AirborneAvatarPose.seatedLegPitch(.5F), 1e-6);
        assertEquals(0, AirborneAvatarPose.seatedLegPitch(1), 1e-6);
        assertEquals(0, AirborneAvatarPose.seatedLegPitch(2), 1e-6);
        String model = mixin("AirbornePlayerModelMixin");
        assertTrue(model.contains("AirborneFlightClient.standProgressFor(state.id)"));
        assertTrue(model.contains("standing >= 1 ?"));
        String renderer = mixin("AirborneAvatarRendererMixin");
        assertTrue(renderer.contains("state.isPassenger = aboard"));
        assertTrue(renderer.contains("if (aboard) state.walkAnimationSpeed = 0"));
        assertTrue(renderer.contains("(aboard || phase.equals(\"departing\")) && entity.getVehicle() instanceof AirbornePlaneEntity"));
        assertTrue(renderer.contains("state.bodyRot = plane.getYRot(partialTick) + 180"));
        assertTrue(renderer.contains("Mth.wrapDegrees(headYaw - state.bodyRot)"));
    }

    @Test void seatedLegUndersideTouchesPublishedSeatTop() {
        double feetY = -1.736875;
        assertEquals(-1.15, feetY + AirborneAvatarPose.seatedContactHeight(), 1e-7);
        assertEquals(.586875, AirborneAvatarPose.seatedContactHeight(), 1e-7);
    }

    @Test void bakedCapeStaysBehindTorsoThroughGlideAndLandingMatrices() {
        // Actual 26.3 cape cube [-5,0,-1]..[5,16,0], child pivot(0,0,2), baked yaw PI.
        Matrix4f capeLocal = new Matrix4f().translate(0, 0, 2 / 16F).rotateY((float) Math.PI);
        for (float pitch : new float[]{0, -.7F, (float) -Math.PI / 2, -2.4F}) {
            Matrix4f torso = new Matrix4f().rotateY(.8F).rotateX(pitch).scale(-.9375F, -.9375F, .9375F)
                .translate(0, -1.501F, 0);
            Matrix4f capeWorld = new Matrix4f(torso).mul(capeLocal);
            Vector3f bottomWorld = capeWorld.transformPosition(new Vector3f(0, 1, -.5F / 16));
            Vector3f bottomInTorso = new Matrix4f(torso).invert().transformPosition(bottomWorld);
            assertEquals(1, bottomInTorso.y, 1e-5, "Cape bottom remains below shoulders in torso coordinates");
            assertEquals(2.5F / 16, bottomInTorso.z, 1e-5, "Cape remains behind torso");
        }
    }
}
