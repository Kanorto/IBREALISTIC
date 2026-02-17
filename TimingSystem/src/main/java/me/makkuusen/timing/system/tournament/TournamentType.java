package me.makkuusen.timing.system.tournament;

/**
 * Types of tournaments available in the system.
 * Each server can run multiple tournaments simultaneously, but each must be a different type.
 */
public enum TournamentType {
    /** Best time on a single track. */
    SPRINT("Sprint", 1, 1),
    /** Cumulative best times across 3-5 tracks. */
    RALLY("Rally", 3, 5),
    /** Maximum laps within a fixed time window. */
    ENDURANCE("Endurance", 1, 1),
    /** Double-elimination bracket tournament with qualification. Max 64 players. */
    BRACKET("Bracket", 1, 3);

    private final String displayName;
    private final int minTracks;
    private final int maxTracks;

    TournamentType(String displayName, int minTracks, int maxTracks) {
        this.displayName = displayName;
        this.minTracks = minTracks;
        this.maxTracks = maxTracks;
    }

    public String getDisplayName() { return displayName; }
    public int getMinTracks() { return minTracks; }
    public int getMaxTracks() { return maxTracks; }
}
