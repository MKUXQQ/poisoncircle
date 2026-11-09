package com.poisoncircle;

/** Packet-driven transitions, independent of frame rate and world game-time changes. */
final class CircleAudioTransitions {
    private boolean observed;
    private boolean waiting;
    private int round;
    private int closingRound = Integer.MIN_VALUE;
    private boolean warned;
    private boolean damaged;
    private long lastDamage;

    boolean update(int nextRound, boolean nextWaiting, boolean active, boolean collapsed) {
        if (!active) {
            observed = false;
            warned = false;
            closingRound = Integer.MIN_VALUE;
            return false;
        }
        if (!observed) {
            observed = true;
            round = nextRound;
            waiting = nextWaiting;
            // An existing moving circle on first observation is not a new start.
            if (!nextWaiting) closingRound = nextRound;
            return false;
        }
        boolean changed = nextRound != round;
        boolean trigger = !nextWaiting && !collapsed && (waiting || changed) && closingRound != nextRound;
        if (changed) warned = false;
        if (trigger) closingRound = nextRound;
        waiting = nextWaiting;
        round = nextRound;
        return trigger;
    }

    boolean warning() {
        if (warned) return false;
        warned = true;
        return true;
    }

    boolean damage(long nowNanos) {
        if (damaged && nowNanos - lastDamage < 1_000_000_000L) return false;
        damaged = true;
        lastDamage = nowNanos;
        return true;
    }
}
