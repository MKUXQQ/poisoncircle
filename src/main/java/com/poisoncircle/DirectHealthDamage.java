package com.poisoncircle;

/** Calculates poison-circle health loss without invoking Minecraft's damage/armor event pipeline. */
public final class DirectHealthDamage {
    private DirectHealthDamage() {}

    public static float remainingHealth(float health, double damage) {
        return Math.max(0.0F, health - (float) Math.max(0.0D, damage));
    }
}
