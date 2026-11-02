package com.poisoncircle;

/** Persistent server-session configuration used when a new poison circle is started. */
public final class RoundDurationSettings {
    private final int[] waitSeconds;
    private final int[] shrinkSeconds;

    public RoundDurationSettings(int rounds, int defaultSeconds) {
        this.waitSeconds = new int[rounds];
        this.shrinkSeconds = new int[rounds];
        for (int i = 0; i < rounds; i++) { waitSeconds[i] = defaultSeconds; shrinkSeconds[i] = defaultSeconds; }
    }

    public void setTimes(int round, int wait, int shrink) {
        waitSeconds[round - 1] = Math.max(0, wait);
        shrinkSeconds[round - 1] = Math.max(1, shrink);
    }
    public int secondsForRound(int round) { return shrinkSeconds[round - 1]; }
    public int[] copyShrinkTicks() {
        int[] ticks = new int[shrinkSeconds.length];
        for (int i = 0; i < shrinkSeconds.length; i++) ticks[i] = shrinkSeconds[i] * 20;
        return ticks;
    }
    public int[] copyWaitTicks() {
        int[] ticks = new int[waitSeconds.length];
        for (int i = 0; i < waitSeconds.length; i++) ticks[i] = waitSeconds[i] * 20;
        return ticks;
    }
}
