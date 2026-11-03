package com.poisoncircle;

/** A heartbeat keeps a hold alive; only elapsed validated server ticks complete it. */
final class ScanHoldState {
    static final int REQUIRED_TICKS = 100;
    static final int HEARTBEAT_TIMEOUT_TICKS = 8;
    private long lastHeartbeat;
    private long startedAt = Long.MIN_VALUE;
    private long lastValidated = Long.MIN_VALUE;
    private boolean completed;

    ScanHoldState(long tick) { heartbeat(tick); }
    void heartbeat(long tick) { lastHeartbeat = tick; }
    boolean expired(long tick) { return tick - lastHeartbeat > HEARTBEAT_TIMEOUT_TICKS; }

    boolean tick(long tick, boolean valid) {
        if (!valid || expired(tick)) {
            startedAt = Long.MIN_VALUE;
            lastValidated = Long.MIN_VALUE;
            completed = false;
            return false;
        }
        if (lastValidated != Long.MIN_VALUE && tick != lastValidated && tick != lastValidated + 1) {
            startedAt = Long.MIN_VALUE;
        }
        if (startedAt == Long.MIN_VALUE) startedAt = tick;
        lastValidated = tick;
        if (!completed && tick - startedAt >= REQUIRED_TICKS) {
            completed = true;
            return true;
        }
        return false;
    }

    int progressTicks(long tick) {
        return startedAt == Long.MIN_VALUE ? 0 : (int) Math.min(REQUIRED_TICKS, Math.max(0, tick - startedAt));
    }
}
