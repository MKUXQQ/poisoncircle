package com.poisoncircle;

final class DetectorUseRules {
    private DetectorUseRules() {}
    static boolean shouldConsume(boolean creative) { return !creative; }
}
