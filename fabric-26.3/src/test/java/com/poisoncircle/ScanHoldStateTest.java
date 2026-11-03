package com.poisoncircle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScanHoldStateTest {
    @Test void completesOnlyAfterFiveSecondsAndOnlyOncePerHold() {
        ScanHoldState hold = new ScanHoldState(10);
        for (int tick = 10; tick < 110; tick++) {
            if (tick % 4 == 0) hold.heartbeat(tick);
            assertFalse(hold.tick(tick, true));
        }
        hold.heartbeat(110);
        assertTrue(hold.tick(110, true));
        hold.heartbeat(111);
        assertFalse(hold.tick(111, true));
    }

    @Test void continuousHoldNeverRepeatsButFreshHoldCanScanAgain() {
        ScanHoldState hold = new ScanHoldState(0);
        int completions = 0;
        for (int tick = 0; tick <= 400; tick++) {
            hold.heartbeat(tick);
            if (hold.tick(tick, true)) completions++;
        }
        assertEquals(1, completions);
        ScanHoldState nextHold = new ScanHoldState(401);
        for (int tick = 401; tick < 501; tick++) {
            nextHold.heartbeat(tick);
            assertFalse(nextHold.tick(tick, true));
        }
        nextHold.heartbeat(501);
        assertTrue(nextHold.tick(501, true));
    }

    @Test void hundredsOfPacketsCannotAdvanceServerTime() {
        ScanHoldState hold = new ScanHoldState(5);
        for (int i = 0; i < 1000; i++) {
            hold.heartbeat(5);
            assertFalse(hold.tick(5, true));
        }
        assertEquals(0, hold.progressTicks(5));
    }

    @Test void releaseOrInvalidTargetRestartsFullDuration() {
        ScanHoldState hold = new ScanHoldState(0);
        for (int tick = 0; tick < 80; tick++) {
            hold.heartbeat(tick);
            assertFalse(hold.tick(tick, true));
        }
        assertFalse(hold.tick(80, false));
        for (int tick = 81; tick < 181; tick++) {
            hold.heartbeat(tick);
            assertFalse(hold.tick(tick, true));
        }
        hold.heartbeat(181);
        assertTrue(hold.tick(181, true));
    }

    @Test void missingHeartbeatOrValidationGapCannotCompleteOldHold() {
        ScanHoldState hold = new ScanHoldState(0);
        assertFalse(hold.expired(8));
        assertTrue(hold.expired(9));
        assertFalse(hold.tick(9, true));
        hold.heartbeat(1000);
        assertFalse(hold.tick(1000, true));
        hold.heartbeat(1002);
        assertFalse(hold.tick(1002, true));
        assertEquals(0, hold.progressTicks(1002));
    }
}
