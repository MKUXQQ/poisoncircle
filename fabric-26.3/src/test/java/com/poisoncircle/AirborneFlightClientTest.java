package com.poisoncircle;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirborneFlightClientTest {
    private String client(String name) throws Exception {
        return Files.readString(Path.of("src/client/java/com/poisoncircle/" + name + ".java"));
    }

    @Test void inputIsPhysicalAndNeverSimulatesUiOrChangesCamera() throws Exception {
        String source = client("AirborneFlightClient");
        assertTrue(source.contains("client.gui.screen() == null"));
        assertTrue(source.contains("keyUp.isDown()") && source.contains("keyLeft.isDown()"));
        assertTrue(source.contains("boolean exit = false;"));
        assertFalse(source.contains("keyJump.isDown()") || source.contains("keyShift.isDown()"));
        assertTrue(source.contains("ticksSinceSend >= 5"));
        assertFalse(source.contains("registerGlobalReceiver"));
        assertFalse(source.contains("setCameraType"));
        assertFalse(source.contains("setYRot("));
    }

    @Test void predictionUsesServerVelocityAndCollisionInsteadOfTeleport() throws Exception {
        String source = client("AirborneFlightClient");
        assertTrue(source.contains("entity != client.player"));
        assertTrue(source.contains("entity.isPassenger()"));
        assertTrue(source.contains("!local.has("));
        assertTrue(source.contains("entity.move(net.minecraft.world.entity.MoverType.SELF, velocity)"));
        assertTrue(source.contains("2_000_000_000L"));
        assertFalse(source.contains("teleport" + "To("));
        assertFalse(source.contains("setPos("));
        assertTrue(client("mixin/AirborneTravelMixin").contains("travel(Lnet/minecraft/world/phys/Vec3;)V"));
    }

    @Test void rendererUsesSyncedRotationAndBlockbenchGeometry() throws Exception {
        assertTrue(client("AirbornePlaneRenderer").contains("entity.getYRot(partialTick)"));
        assertTrue(client("AirbornePlaneRenderer").contains("submitModelPart"));
        assertTrue(client("AirbornePlaneModel").contains("AirbornePlaneModelGeometry.createLayer()"));
        String input = client("mixin/AirborneInputMixin");
        assertTrue(input.contains("extends ClientInput"));
        assertFalse(input.contains("@Shadow"));
    }
}
