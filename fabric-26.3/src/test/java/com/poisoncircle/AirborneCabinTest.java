package com.poisoncircle;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AirborneCabinTest {
    @Test void passengersBeginOnDistinctPhysicalSeats() {
        for(int seat=0;seat<2;seat++){
            var p=AirborneDeparture.seat(seat);
            assertEquals(seat==0?-1.9:1.9,p.x(),1e-8);
            assertEquals(-4,p.z(),1e-8);
            assertEquals(-1.15,p.y()+.586875,1e-8,"Flat seated thigh bottom contacts cushion top");
        }
    }
    @Test void standingHappensBeforeMovingIntoTheAisle() {
        int start=AirborneDeparture.DOOR_TICKS;
        int standing=start+AirborneDeparture.STAND_TICKS;
        for(int seat=0;seat<2;seat++){
            var a=AirborneDeparture.position(start,seat);
            var b=AirborneDeparture.position(standing,seat);
            assertEquals(a.x(),b.x(),1e-8);
            assertEquals(-2,b.y(),1e-8);assertEquals(-5.2,b.z(),1e-8);
        }
    }
    @Test void bothPassengerLanesStaySeparatedAndReachRearNotSide() {
        for(int t=0;t<=AirborneDeparture.RELEASE_TICKS;t++){
            var a=AirborneDeparture.position(t,0);var b=AirborneDeparture.position(t,1);
            assertTrue(b.x()-a.x()>=1.099999,"No overlapping passenger paths");
            assertEquals(a.y(),b.y(),1e-8);assertEquals(a.z(),b.z(),1e-8);
            assertTrue(Math.abs(a.x())<=1.9&&Math.abs(b.x())<=1.9);
        }
        var end=AirborneDeparture.position(AirborneDeparture.RELEASE_TICKS,0);
        assertEquals(-13,end.z(),1e-8);assertEquals(-2.85,end.y(),1e-8);
        assertEquals(100,AirborneRules.BOARDING_LOCK_TICKS);
    }
    @Test void compactCabinKeepsSeatsNearTheExitAndMatchesHingedRamp() {
        assertEquals(84,AirborneDeparture.RELEASE_TICKS);
        for(int seat=0;seat<2;seat++){
            var sitting=AirborneDeparture.seat(seat);
            assertEquals(5,sitting.z()-(-9),1e-8,"Rear hinge is five blocks behind the seat");
            var end=AirborneDeparture.position(AirborneDeparture.RELEASE_TICKS,seat);
            assertEquals(9,sitting.z()-end.z(),1e-8);
            assertEquals(-2.05+3.2*Math.cos(Math.toRadians(-100))+.05,AirborneDeparture.RAMP_END_Y,1e-8);
            assertEquals(-9+3.2*Math.sin(Math.toRadians(-100)),AirborneDeparture.RAMP_END_Z,1e-8);
        }
    }
    @Test void heightHasNoProductCeilingButRejectsInvalidNumbers() {
        assertTrue(AirborneRules.validConfig(1e100,.8));
        for(double h:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY})assertFalse(AirborneRules.validConfig(h,.8));
        assertFalse(AirborneRules.validConfig(200,.19));assertFalse(AirborneRules.validConfig(200,2.01));
    }
}
