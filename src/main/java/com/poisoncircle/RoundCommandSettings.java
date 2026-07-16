package com.poisoncircle;

import java.util.HashMap;
import java.util.Map;

final class RoundCommandSettings {
    private final Map<Integer, String> commands = new HashMap<>();
    void set(int round, String command) { commands.put(round, command); }
    String get(int round) { return commands.get(round); }
}
