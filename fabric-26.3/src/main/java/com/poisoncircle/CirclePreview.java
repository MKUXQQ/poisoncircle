package com.poisoncircle;

/** A safe zone announced before it becomes the active shrink target. */
record CirclePreview(double x, double z, double radius) {
    CirclePreview promote() { return this; }
}
