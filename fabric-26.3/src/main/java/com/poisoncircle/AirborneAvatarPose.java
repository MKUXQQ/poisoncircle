package com.poisoncircle;

/** Pure presentation rules shared by the avatar and cape models. */
public final class AirborneAvatarPose {
    private AirborneAvatarPose() { }

    public static boolean controlled(String phase) {
        return "aboard".equals(phase) || "departing".equals(phase)
            || "diving".equals(phase) || "landing".equals(phase);
    }

    public static float seatedLegPitch(float standProgress) {
        return -(float) Math.PI / 2 * (1 - Math.clamp(standProgress, 0, 1));
    }

    // 26.3: renderer translates -1.501, avatar scales .9375, hip is model Y=12, leg half-depth=2.
    public static double seatedContactHeight() { return (1.501 - (12 + 2) / 16.0) * .9375; }
}
