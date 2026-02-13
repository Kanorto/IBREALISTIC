package me.makkuusen.timing.system.track;

/**
 * Weather conditions for tracks.
 * IDs must match the WeatherCondition enum in OpenBoatUtilsRealistic mod.
 */
public enum TrackWeather {
    CLEAR(0, "Clear"),
    RAIN(1, "Rain"),
    HEAVY_RAIN(2, "Heavy Rain"),
    SNOW(3, "Snow"),
    FOG(4, "Fog");

    private final int id;
    private final String displayName;

    TrackWeather(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TrackWeather fromId(int id) {
        for (TrackWeather w : values()) {
            if (w.id == id) return w;
        }
        return CLEAR;
    }

    public static TrackWeather fromName(String name) {
        if (name == null) return null;
        for (TrackWeather w : values()) {
            if (w.name().equalsIgnoreCase(name)) return w;
        }
        return null;
    }
}
