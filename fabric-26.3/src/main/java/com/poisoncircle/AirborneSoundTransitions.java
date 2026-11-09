package com.poisoncircle;

/** Edges only: late tracking/reload must not replay a door already open. */
final class AirborneSoundTransitions {
    private boolean doorObserved;
    private boolean doorOpen;
    private String phase = "none";

    boolean door(float progress) {
        boolean open = Float.isFinite(progress) && progress > 0;
        boolean trigger = doorObserved && !doorOpen && open;
        doorObserved = true;
        doorOpen = open;
        return trigger;
    }

    boolean phase(String next) {
        boolean trigger = (phase.equals("aboard") || phase.equals("departing")) && "diving".equals(next);
        phase = next == null ? "none" : next;
        return trigger;
    }
}
