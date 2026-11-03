package com.poisoncircle;

/** Global collapse ends only after every eligible player has died. */
final class LastCircleStopCondition {
    private LastCircleStopCondition() {}
    static boolean shouldStop(int eligiblePlayers, int livingPlayers) { return eligiblePlayers > 0 && livingPlayers == 0; }
}
