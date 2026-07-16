package com.poisoncircle;

/** Documents the one-recipient rule used by the detector packet. */
final class DetectorRevealScope {
    private final String recipient;
    DetectorRevealScope(String recipient) { this.recipient = recipient; }
    boolean visibleTo(String playerId) { return recipient.equals(playerId); }
}
