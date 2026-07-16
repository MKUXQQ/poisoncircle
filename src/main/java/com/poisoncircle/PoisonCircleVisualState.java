package com.poisoncircle;

/** Immutable server state used to update the non-colliding visual entity. */
record PoisonCircleVisualState(double x, double z, float radius) {
    PoisonCircleVisualState moveTo(double x, double z, float radius) { return new PoisonCircleVisualState(x, z, radius); }
    boolean collides() { return false; }
}
