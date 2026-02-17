package me.makkuusen.timing.system.tournament;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Player's Glicko-2 rating data.
 */
@Getter
@Setter
public class PlayerRating {

    private static final int DEFAULT_RATING = 1000;
    private static final int DEFAULT_DEVIATION = 350;
    private static final double DEFAULT_VOLATILITY = 0.06;

    @Expose private UUID uuid;
    @Expose private int rating = DEFAULT_RATING;
    @Expose private int deviation = DEFAULT_DEVIATION;
    @Expose private double volatility = DEFAULT_VOLATILITY;
    @Expose private int gamesPlayed;
    @Expose private int peakRating = DEFAULT_RATING;
    @Expose private int seasonId = 1;
    @Expose private long updatedAt;

    public PlayerRating() {}

    public PlayerRating(UUID uuid) {
        this.uuid = uuid;
        this.updatedAt = System.currentTimeMillis();
    }

    public RatingRank getRank() {
        return RatingRank.fromRating(rating);
    }

    public void updateRating(int newRating) {
        this.rating = Math.max(0, newRating);
        if (this.rating > this.peakRating) {
            this.peakRating = this.rating;
        }
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Soft reset for new season: newRating = (rating - 1000) * 0.5 + 1000
     */
    public void softReset(int newSeasonId) {
        this.rating = (int) ((this.rating - DEFAULT_RATING) * 0.5 + DEFAULT_RATING);
        this.deviation = DEFAULT_DEVIATION;
        this.volatility = DEFAULT_VOLATILITY;
        this.seasonId = newSeasonId;
        this.updatedAt = System.currentTimeMillis();
    }
}
