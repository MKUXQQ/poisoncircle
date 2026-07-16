package com.poisoncircle;

/** Client-side interpolation between authoritative server circle snapshots. */
final class CirclePrediction {
    private CirclePrediction() {}

    static State advance(State state, int elapsedTicks) {
        return advance(state, elapsedTicks, true);
    }

    static State advance(State state, int elapsedTicks, boolean shrinking) {
        if (!shrinking) return state;
        if (state.remainingTicks <= 0 || elapsedTicks <= 0) return state;
        double progress = Math.min(1.0, (double) elapsedTicks / state.remainingTicks);
        return new State(
            lerp(state.x, state.targetX, progress),
            lerp(state.z, state.targetZ, progress),
            lerp(state.radius, state.targetRadius, progress),
            state.targetX, state.targetZ, state.targetRadius,
            Math.max(0, state.remainingTicks - elapsedTicks)
        );
    }

    private static double lerp(double from, double to, double progress) { return from + (to - from) * progress; }

    record State(double x, double z, double radius, double targetX, double targetZ, double targetRadius, int remainingTicks) {}
}
