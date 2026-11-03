package com.poisoncircle;

/** A horizontal safe cylinder; Y is retained as the lobby's reference position. */
record LobbySettings(boolean enabled, String dimension, double x, double y, double z, double radius) {
    LobbySettings {
        if (dimension == null || dimension.isBlank() || !Double.isFinite(x) || !Double.isFinite(y)
            || !Double.isFinite(z) || !Double.isFinite(radius) || radius < 1 || radius > 29_999_984)
            throw new IllegalArgumentException("Invalid lobby coordinates or radius");
    }
    boolean contains(String dimension, double x, double z) {
        double dx = x - this.x, dz = z - this.z;
        return enabled && this.dimension.equals(dimension) && dx * dx + dz * dz <= radius * radius;
    }
    com.google.gson.JsonObject toJson() {
        var json = new com.google.gson.JsonObject();
        json.addProperty("enabled", enabled); json.addProperty("dimension", dimension);
        json.addProperty("x", x); json.addProperty("y", y); json.addProperty("z", z); json.addProperty("radius", radius);
        return json;
    }
    static LobbySettings fromJson(com.google.gson.JsonObject json) {
        return new LobbySettings(json.get("enabled").getAsBoolean(), json.get("dimension").getAsString(),
            json.get("x").getAsDouble(), json.get("y").getAsDouble(), json.get("z").getAsDouble(), json.get("radius").getAsDouble());
    }
    LobbySettings disabled() { return new LobbySettings(false, dimension, x, y, z, radius); }
}
