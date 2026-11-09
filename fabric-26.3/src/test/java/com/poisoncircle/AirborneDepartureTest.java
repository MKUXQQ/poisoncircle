package com.poisoncircle;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

class AirborneDepartureTest {
    @Test void approachWaitsUntilCruiseAndNeverOvershootsAtAnyLegalSpeed(){
        for(double speed:new double[]{.2,.8,2}){
            double remaining=20;
            assertEquals(0,AirborneDeparture.approachStep(remaining,speed,false));
            for(int tick=0;tick<102;tick++)remaining-=AirborneDeparture.approachStep(remaining,speed,true);
            assertEquals(0,remaining,1e-8);
        }
    }
    @Test void doorOpensBeforeEitherPassengerMoves(){
        for(int seat=0;seat<2;seat++)for(int tick=0;tick<=AirborneDeparture.DOOR_TICKS;tick++){
            var p=AirborneDeparture.position(tick,seat);var start=AirborneDeparture.seat(seat);
            assertEquals(start.x(),p.x(),1e-8);assertEquals(start.y(),p.y(),1e-8);assertEquals(start.z(),p.z(),1e-8);
        }
        assertEquals(0,AirborneDeparture.doorProgress(-10));
        assertEquals(1,AirborneDeparture.doorProgress(30));
        assertEquals(1,AirborneDeparture.doorProgress(1000));
    }
    @Test void bothPassengersFollowRearRampAndReleaseWithoutInputs(){
        for(int seat=0;seat<2;seat++){
            var previous=AirborneDeparture.position(0,seat);
            for(int tick=0;tick<=AirborneDeparture.RELEASE_TICKS;tick++){
                var p=AirborneDeparture.position(tick,seat);
                double step=Math.sqrt(Math.pow(p.x()-previous.x(),2)+Math.pow(p.y()-previous.y(),2)+Math.pow(p.z()-previous.z(),2));
                assertTrue(step<.35,"Continuous animation without instantaneous seat-to-exit teleport");
                if(p.z()<-5.2)assertEquals(seat==0?-.55:.55,p.x(),1e-8,"Stay in central rear aisle");
                if(tick<AirborneDeparture.RELEASE_TICKS)assertFalse(AirborneDeparture.release(tick));
                previous=p;
            }
            assertEquals(seat==0?-.55:.55,previous.x(),1e-8);assertEquals(-13,previous.z(),1e-8);assertEquals(-2.85,previous.y(),1e-8);
            assertTrue(AirborneDeparture.release(AirborneDeparture.RELEASE_TICKS));
        }
    }
    @Test void serverTimelineDoesNotDependOnWalkInputsAndClearsActualMount()throws Exception{
        String game=Files.readString(Path.of("src/main/java/com/poisoncircle/AirborneGame.java"));
        String sequence=game.substring(game.indexOf("private static void animateDeparture("),game.indexOf("private static void move("));
        assertTrue(sequence.contains("plane.hasReachedTarget()||age>=400"));
        assertTrue(sequence.contains("age>=AirborneRules.BOARDING_LOCK_TICKS"));
        assertTrue(sequence.contains("AirborneDeparture.release(elapsed)"));
        assertFalse(sequence.contains("st.input"));assertFalse(sequence.contains("lastInput"));
        assertTrue(sequence.contains("position.x(),position.y(),position.z()"));
        assertTrue(game.contains("p.stopRiding();require(!(p.getVehicle() instanceof AirbornePlaneEntity)"));
        assertTrue(game.contains("st.phase=Phase.DIVING"));
    }
    @Test void zeroInputFlightAlwaysReachesTargetAndReleasesWithinBound(){
        for(double speed:new double[]{.2,.4,.8,1,1.7,2}){
            double remaining=20,altitude=280;int opening=-1,released=-1;
            for(int tick=1;tick<=400+AirborneDeparture.RELEASE_TICKS;tick++){
                double descent=Math.max(-.4,Math.min(0,200-altitude));
                remaining-=AirborneDeparture.approachStep(remaining,speed,tick>=100&&Math.abs(200-altitude)<=.4);
                altitude+=descent;
                if(opening<0&&tick>=100&&((remaining<.1&&Math.abs(altitude-200)<.1)||tick>=400))opening=tick;
                if(opening>=0&&AirborneDeparture.release(tick-opening)){released=tick;break;}
            }
            assertTrue(opening>=100&&opening<=301,"Five-second stay then target arrival, speed="+speed);
            assertEquals(0,remaining,1e-8);assertEquals(200,altitude,1e-8);
            assertEquals(opening+AirborneDeparture.RELEASE_TICKS,released);
        }
    }
    @Test void doorStateAndStandingInterpolationAreWired()throws Exception{
        String entity=Files.readString(Path.of("src/main/java/com/poisoncircle/AirbornePlaneEntity.java"));
        assertTrue(entity.contains("builder.define(DOOR,0F)"));
        String renderer=Files.readString(Path.of("src/client/java/com/poisoncircle/AirbornePlaneRenderer.java"));
        assertTrue(renderer.contains("getChild(\"cargo_door\").xRot"));assertTrue(renderer.contains("Math.toRadians(100)"));
        assertTrue(renderer.contains("state.door=entity.doorProgress(partialTick)"));
        String client=Files.readString(Path.of("src/client/java/com/poisoncircle/AirborneFlightClient.java"));
        assertTrue(client.contains("standProgressFor(int entityId)"));
        String game=Files.readString(Path.of("src/main/java/com/poisoncircle/AirborneGame.java"));
        assertTrue(game.contains("pose.addProperty(\"standProgress\",standProgress(st))"));
        assertTrue(game.contains("self.addProperty(\"standProgress\",st==null?1:standProgress(st))"));
    }
}
