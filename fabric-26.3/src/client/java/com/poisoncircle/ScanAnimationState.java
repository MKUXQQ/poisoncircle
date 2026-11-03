package com.poisoncircle;

/** Presentation only: elapsed time never grants a scan result. */
public final class ScanAnimationState {
    private boolean completed;
    private int ticks;

    public void tick(boolean validTarget, boolean useDown) {
        if (!useDown) completed = false;
        ticks = validTarget && useDown && !completed ? ticks + 1 : 0;
    }

    public void complete() {
        completed = true;
        ticks = 0;
    }

    public boolean completed() { return completed; }
    public boolean shouldLockMovement(boolean validTarget, boolean useDown) { return validTarget && useDown; }
    public boolean active() { return ticks > 0; }
    public float wrist(float partialTick) { return (float) Math.sin((ticks + partialTick) * 0.55) * 0.055F; }
}
