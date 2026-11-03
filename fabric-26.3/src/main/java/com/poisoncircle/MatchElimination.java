package com.poisoncircle;

/** Offline participants remain unconfirmed; a recorded death stays eliminated after respawn. */
final class MatchElimination {
    private MatchElimination() {}
    static boolean shouldEnroll(boolean active, boolean withdrawn, boolean inLobby, boolean exemptMode) {
        return active && !withdrawn && !inLobby && !exemptMode;
    }
    static boolean shouldStop(boolean hadParticipants, boolean anyUnconfirmedParticipant) {
        return hadParticipants && !anyUnconfirmedParticipant;
    }
    static boolean immune(boolean eliminated, boolean withdrawn, boolean inLobby, boolean alive, boolean exemptMode) {
        return eliminated || withdrawn || inLobby || !alive || exemptMode;
    }
    static boolean blocksStop(boolean eliminated, boolean online, boolean exemptMode, boolean alive) {
        return !eliminated && (!online || exemptMode || alive);
    }
}
