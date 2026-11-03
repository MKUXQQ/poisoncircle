package com.poisoncircle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatchEliminationTest {
    @Test void creativeStartThenSurvivalOrAdventureJoinsTheRunningMatch() {
        assertFalse(MatchElimination.shouldEnroll(true, false, false, true));
        assertTrue(MatchElimination.shouldEnroll(true, false, false, false));
        assertFalse(MatchElimination.shouldEnroll(false, false, false, false));
    }
    @Test void creativeStartThenSurvivalDeathStopsEvenAfterImmediateRespawn() {
        boolean hadParticipants = MatchElimination.shouldEnroll(true, false, false, true);
        assertFalse(MatchElimination.shouldStop(hadParticipants, false));
        hadParticipants |= MatchElimination.shouldEnroll(true, false, false, false);
        assertFalse(MatchElimination.shouldStop(hadParticipants,
            MatchElimination.blocksStop(false, true, false, true)));
        assertTrue(MatchElimination.shouldStop(hadParticipants,
            MatchElimination.blocksStop(true, true, false, false)));
        assertTrue(MatchElimination.shouldStop(hadParticipants,
            MatchElimination.blocksStop(true, true, false, true)));
        assertTrue(MatchElimination.immune(true, false, false, true, false));
    }
    @Test void lobbyAndWithdrawnPlayersCannotRejoinUntilNextMatch() {
        assertFalse(MatchElimination.shouldEnroll(true, false, true, false));
        assertFalse(MatchElimination.shouldEnroll(true, true, false, false));
        assertTrue(MatchElimination.shouldEnroll(true, false, false, false));
    }
    @Test void eliminatedRespawnAndLobbyWithdrawalStayImmune() {
        assertTrue(MatchElimination.immune(true, false, false, true, false));
        assertTrue(MatchElimination.immune(false, true, false, true, false));
        assertTrue(MatchElimination.immune(false, false, true, true, false));
        assertFalse(MatchElimination.immune(false, false, false, true, false));
    }
    @Test void zeroInitialParticipantsDoesNotStopButLastLobbyWithdrawalDoes() {
        assertFalse(MatchElimination.shouldStop(false, false));
        assertTrue(MatchElimination.shouldStop(true, false));
        assertFalse(MatchElimination.shouldStop(true, true));
    }
    @Test void recordedDeathStaysEliminatedAfterFastRespawn() {
        assertFalse(MatchElimination.blocksStop(true, true, false, true));
    }
    @Test void disconnectedParticipantDoesNotCountAsDead() {
        assertTrue(MatchElimination.blocksStop(false, false, false, false));
    }
    @Test void onlyLivingSurvivalOrAdventureParticipantBlocksStop() {
        assertTrue(MatchElimination.blocksStop(false, true, false, true));
        assertTrue(MatchElimination.blocksStop(false, true, true, true));
        assertFalse(MatchElimination.blocksStop(false, true, false, false));
    }

    @Test void offlineOrCreativePlayersDoNotMasqueradeAsDeaths() {
        assertTrue(MatchElimination.blocksStop(false, false, true, false));
        assertTrue(MatchElimination.blocksStop(false, true, true, true));
    }
}
