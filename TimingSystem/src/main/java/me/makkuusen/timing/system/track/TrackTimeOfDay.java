package me.makkuusen.timing.system.track;

/**
 * Preset time-of-day values for tracks.
 * Tick values correspond to Minecraft's day cycle (0–24000).
 */
public enum TrackTimeOfDay {
    DAWN(23000, "Dawn"),      // Early morning, just before sunrise
    NOON(6000, "Noon"),       // Midday
    SUNSET(12000, "Sunset"),  // Evening
    NIGHT(13000, "Night"),    // Night begins
    MIDNIGHT(18000, "Midnight"); // Middle of the night

    private final long ticks;
    private final String displayName;

    TrackTimeOfDay(long ticks, String displayName) {
        this.ticks = ticks;
        this.displayName = displayName;
    }

    public long getTicks() {
        return ticks;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TrackTimeOfDay fromName(String name) {
        if (name == null) return null;
        for (TrackTimeOfDay t : values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return null;
    }
}
