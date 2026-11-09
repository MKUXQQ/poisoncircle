package com.poisoncircle;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

class AirborneRedesignTest {
    private static String main(String name) throws Exception {return Files.readString(Path.of("src/main/java/com/poisoncircle/"+name+".java"));}
    @Test void rideableEntityTypeUsesInstancePersistenceOptOut() throws Exception {
        String game=main("AirborneGame"),plane=main("AirbornePlaneEntity");
        assertFalse(game.contains(".noSave()"),"26.3 Entity.startRiding rejects non-serializable EntityTypes even when forced");
        assertTrue(plane.contains("boolean shouldBeSaved() { return false; }"));
        assertTrue(plane.contains("boolean saveAsPassenger(ValueOutput output) { return false; }"));
        assertTrue(game.indexOf("p.startRiding(plane,true,true)")<game.indexOf("st.phase=Phase.ABOARD"));
    }
    @Test void glideUsesMomentumAndPitchTradesSpeedForLift() {
        var previous=new AirborneRules.Vec(0,-.3,1.2);
        var flat=AirborneRules.glide(previous,0,0,0,0);
        var climb=AirborneRules.glide(previous,0,0,0,-45);
        var dive=AirborneRules.glide(previous,0,0,0,60);
        assertTrue(climb.y()>flat.y());
        assertTrue(dive.y()<flat.y());
        assertTrue(flat.z()>.9,"Neutral flight keeps inherited aircraft speed");
        assertTrue(AirborneRules.glide(previous,-1,0,0,0).z()<flat.z());
        assertTrue(AirborneRules.glide(previous,1,0,0,0).z()>flat.z());
    }
    @Test void glideIsFiniteAndBoundedForEveryLegalControl() {
        for(int pitch=-90;pitch<=90;pitch+=15)for(int yaw=-180;yaw<=180;yaw+=30)for(int forward=-1;forward<=1;forward++){
            var v=new AirborneRules.Vec(.4,-.3,.8);
            for(int tick=0;tick<300;tick++){
                v=AirborneRules.glide(v,forward,.5F,yaw,pitch);
                assertTrue(Double.isFinite(v.x())&&Double.isFinite(v.y())&&Double.isFinite(v.z()));
                assertTrue(Math.sqrt(v.x()*v.x()+v.y()*v.y()+v.z()*v.z())<=3.000001);
            }
        }
    }
    @Test void defaultAltitudeAndAircraftDescentAreNotGroundTeleports() throws Exception {
        assertTrue(AirborneRules.validConfig(200,.8));assertTrue(AirborneRules.validConfig(512,.8));assertTrue(AirborneRules.validConfig(513,.8));
        assertTrue(main("AirborneGame").contains("height=200"));
        String plane=main("AirbornePlaneEntity");assertTrue(plane.contains("cruiseAltitude-getY()"));assertTrue(plane.contains("Math.max(-.4"));
        assertFalse(main("AirborneGame").contains("altitude<level.getMaxY()"),"Cruise above build height is legal; build limits apply to ground points only");
    }
    @Test void hudContainsOnlyPreparationAndBoardingText() throws Exception {
        String client=Files.readString(Path.of("src/client/java/com/poisoncircle/AirborneFlightClient.java"));
        String hud=client.substring(client.indexOf("private static void renderHud("),client.indexOf("private static JsonObject object("));
        assertTrue(hud.contains("空降准备【"));assertTrue(hud.contains("即将登机"));assertFalse(hud.contains("目标："));assertFalse(hud.contains("W / S"));
    }
}
