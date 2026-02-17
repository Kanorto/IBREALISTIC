package me.makkuusen.timing.system.tournament;

/**
 * Visual ranks based on ELO/Glicko-2 rating.
 */
public enum RatingRank {
    BRONZE("Bronze", "🥉", 0, 999),
    SILVER("Silver", "🥈", 1000, 1299),
    GOLD("Gold", "🥇", 1300, 1599),
    DIAMOND("Diamond", "💎", 1600, 1899),
    CHAMPION("Champion", "👑", 1900, Integer.MAX_VALUE);

    private final String displayName;
    private final String icon;
    private final int minRating;
    private final int maxRating;

    RatingRank(String displayName, String icon, int minRating, int maxRating) {
        this.displayName = displayName;
        this.icon = icon;
        this.minRating = minRating;
        this.maxRating = maxRating;
    }

    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public int getMinRating() { return minRating; }
    public int getMaxRating() { return maxRating; }

    public static RatingRank fromRating(int rating) {
        for (RatingRank rank : values()) {
            if (rating >= rank.minRating && rating <= rank.maxRating) {
                return rank;
            }
        }
        return BRONZE;
    }
}
