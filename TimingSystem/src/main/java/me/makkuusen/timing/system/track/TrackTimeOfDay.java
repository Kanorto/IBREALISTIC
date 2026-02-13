package me.makkuusen.timing.system.track;

/**
 * Preset time-of-day values for tracks.
 * Tick values correspond to Minecraft's day cycle (0–24000).
 */
public enum TrackTimeOfDay {
    DAWN(0, "Dawn"),
    NOON(6000, "Noon"),
    SUNSET(12000, "Sunset"),
    NIGHT(18000, "Night"),
    MIDNIGHT(13000, "Midnight");

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
