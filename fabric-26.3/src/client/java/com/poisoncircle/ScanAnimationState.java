package com.poisoncircle;

/** Server progress drives presentation; ending a hold always requires a physical release. */
public final class ScanAnimationState {
    private boolean completed;
    private boolean active;
    private int progressTicks;
    private int ageSinceSync;

    public void sync(boolean active, int progressTicks) {
        this.active = active;
        this.progressTicks = Math.clamp(progressTicks, 0, 100);
        ageSinceSync = 0;
        if (!active) completed = true;
    }

    public void tick(boolean useDown) {
        if (!useDown) completed = false;
        if (active) ageSinceSync++;
    }

    public void complete() { sync(false, progressTicks); }
    public boolean completed() { return completed; }
    public boolean active() { return active; }
    public int progressTicks() { return progressTicks; }
    public float age(float partialTick) { return progressTicks + Math.min(4, ageSinceSync + partialTick); }
    public float wrist(float partialTick) { return (float) Math.sin(age(partialTick) * 0.75F) * 0.20F; }
}
