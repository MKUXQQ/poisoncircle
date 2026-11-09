package com.poisoncircle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** Bounded presentation decisions, independent of the game renderer. */
public final class AirborneClientRules {
    public record Marker(double x, double y, double z) { }
    private AirborneClientRules() { }

    public static List<Marker> markers(JsonObject snapshot, boolean hasSetter, String dimension, double x, double y, double z) {
        List<Marker> result = new ArrayList<>();
        try {
            if (!hasSetter || !snapshot.has("operator") || !snapshot.get("operator").getAsBoolean()) return result;
            String selected = snapshot.get("selectedGroup").getAsString();
            com.google.gson.JsonArray working = selected.isEmpty() ? snapshot.getAsJsonArray("draftPoints") : null;
            if (!selected.isEmpty()) {
                int groups = 0;
                for (JsonElement value : snapshot.getAsJsonArray("groups")) {
                    if (++groups > 8) break;
                    JsonObject group = value.getAsJsonObject();
                    if (selected.equals(group.get("id").getAsString())) { working = group.getAsJsonArray("points"); break; }
                }
            }
            if (working == null) return result;
            int points = 0;
            for (JsonElement entry : working) {
                if (++points > 8) break;
                try {
                    JsonObject point = entry.getAsJsonObject();
                    if (!dimension.equals(point.get("dimension").getAsString())) continue;
                    double px = point.get("x").getAsDouble(), py = point.get("y").getAsDouble(), pz = point.get("z").getAsDouble();
                    double distance = (px-x)*(px-x)+(py-y)*(py-y)+(pz-z)*(pz-z);
                    if (Double.isFinite(distance) && distance <= 128*128) result.add(new Marker(px, py, pz));
                } catch (RuntimeException ignored) { }
            }
        } catch (RuntimeException ignored) { }
        return result;
    }

    public static float legPitch(boolean landing) { return landing ? -.65F : .08F; }
    public static boolean warningVisible(long elapsedNanos) { return elapsedNanos >= 0 && elapsedNanos < 3_000_000_000L; }
}
