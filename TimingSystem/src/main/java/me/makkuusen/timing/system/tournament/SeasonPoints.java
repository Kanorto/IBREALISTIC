package me.makkuusen.timing.system.tournament;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * A player's points accumulated during a season.
 */
@Getter
@Setter
public class SeasonPoints {

    /** Points awarded per position (1st=25, 2nd=18, ..., 10th=1). */
    public static final int[] POSITION_POINTS = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};

    @Expose private UUID uuid;
    @Expose private int seasonId;
    @Expose private int totalPoints;
    @Expose private int tournamentsPlayed;
    @Expose private int bestPosition;

    public SeasonPoints() {}

    public SeasonPoints(UUID uuid, int seasonId) {
        this.uuid = uuid;
        this.seasonId = seasonId;
    }

    /**
     * Returns points for a given position (1-based). Returns 0 for positions > 10.
     */
    public static int pointsForPosition(int position) {
        if (position < 1 || position > POSITION_POINTS.length) return 0;
        return POSITION_POINTS[position - 1];
    }

    public void addTournamentResult(int position) {
        int pts = pointsForPosition(position);
        this.totalPoints += pts;
        this.tournamentsPlayed++;
        if (this.bestPosition == 0 || position < this.bestPosition) {
            this.bestPosition = position;
        }
    }
}
