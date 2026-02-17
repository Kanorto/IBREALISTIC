package me.makkuusen.timing.system.tournament;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages player ratings using a simplified Glicko-2 algorithm.
 * Ratings are updated after each tournament based on relative performance.
 */
public class RatingManager {

    // ─── GLICKO-2 CONSTANTS ───

    private static final double TAU = 0.5;
    private static final double GLICKO2_SCALE = 173.7178;
    private static final int DEFAULT_RATING = 1000;
    private static final int DEFAULT_DEVIATION = 350;
    private static final double DEFAULT_VOLATILITY = 0.06;
    private static final double CONVERGENCE_TOLERANCE = 0.000001;

    // ─── DATABASE OPERATIONS ───

    /**
     * Gets or creates a player's rating.
     */
    public static PlayerRating getOrCreateRating(UUID uuid) {
        try {
            DbRow row = DB.getFirstRow(
                "SELECT * FROM ts_player_rating WHERE uuid = ?", uuid.toString()
            );
            if (row != null) {
                PlayerRating pr = new PlayerRating(uuid);
                pr.setRating(row.getInt("rating"));
                pr.setDeviation(row.getInt("deviation"));
                pr.setVolatility(row.get("volatility") instanceof Number n ? n.doubleValue() : DEFAULT_VOLATILITY);
                pr.setGamesPlayed(row.getInt("games_played"));
                pr.setPeakRating(row.getInt("peak_rating"));
                pr.setSeasonId(row.getInt("season_id"));
                pr.setUpdatedAt(row.getLong("updated_at"));
                return pr;
            }
            // Create new rating
            PlayerRating pr = new PlayerRating(uuid);
            DB.executeInsert(
                "INSERT INTO ts_player_rating (uuid, rating, deviation, volatility, games_played, peak_rating, season_id, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                uuid.toString(), pr.getRating(), pr.getDeviation(), pr.getVolatility(),
                pr.getGamesPlayed(), pr.getPeakRating(), pr.getSeasonId(), pr.getUpdatedAt()
            );
            return pr;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get/create rating for " + uuid, e);
            return new PlayerRating(uuid);
        }
    }

    /**
     * Saves a player's rating to the database.
     */
    public static void saveRating(PlayerRating pr) {
        try {
            DB.executeUpdate(
                "UPDATE ts_player_rating SET rating = ?, deviation = ?, volatility = ?, games_played = ?, peak_rating = ?, season_id = ?, updated_at = ? WHERE uuid = ?",
                pr.getRating(), pr.getDeviation(), pr.getVolatility(),
                pr.getGamesPlayed(), pr.getPeakRating(), pr.getSeasonId(), pr.getUpdatedAt(),
                pr.getUuid().toString()
            );
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to save rating for " + pr.getUuid(), e);
        }
    }

    /**
     * Gets the top N players by rating.
     */
    public static List<PlayerRating> getTopRatings(int limit) {
        List<PlayerRating> result = new ArrayList<>();
        try {
            List<DbRow> rows = DB.getResults(
                "SELECT * FROM ts_player_rating ORDER BY rating DESC LIMIT ?", limit
            );
            for (DbRow row : rows) {
                UUID uuid = UUID.fromString(row.getString("uuid"));
                PlayerRating pr = new PlayerRating(uuid);
                pr.setRating(row.getInt("rating"));
                pr.setDeviation(row.getInt("deviation"));
                pr.setGamesPlayed(row.getInt("games_played"));
                pr.setPeakRating(row.getInt("peak_rating"));
                pr.setSeasonId(row.getInt("season_id"));
                result.add(pr);
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get top ratings", e);
        }
        return result;
    }

    // ─── GLICKO-2 UPDATE ───

    /**
     * Updates ratings for all participants in a tournament based on their positions.
     * Uses Glicko-2 pairwise comparison: each pair of participants is treated as a match.
     *
     * @param results sorted list of tournament results (position 1 = first)
     */
    public static void updateRatingsForTournament(List<TournamentResult> results) {
        if (results.size() < 2) return;

        // Load all ratings
        Map<UUID, PlayerRating> ratings = new HashMap<>();
        for (TournamentResult r : results) {
            ratings.put(r.getPlayerUuid(), getOrCreateRating(r.getPlayerUuid()));
        }

        // Calculate new ratings using Glicko-2 pairwise comparison
        Map<UUID, int[]> winLoss = new HashMap<>();
        for (TournamentResult r : results) {
            winLoss.put(r.getPlayerUuid(), new int[]{0, 0}); // [wins, losses]
        }

        // Pairwise comparison: higher position = winner
        for (int i = 0; i < results.size(); i++) {
            for (int j = i + 1; j < results.size(); j++) {
                UUID winner = results.get(i).getPlayerUuid();
                UUID loser = results.get(j).getPlayerUuid();
                winLoss.get(winner)[0]++;
                winLoss.get(loser)[1]++;
            }
        }

        // Apply simplified Glicko-2 update per player
        for (TournamentResult r : results) {
            PlayerRating pr = ratings.get(r.getPlayerUuid());
            int[] wl = winLoss.get(r.getPlayerUuid());
            int totalGames = wl[0] + wl[1];
            double score = totalGames > 0 ? (double) wl[0] / totalGames : 0.5;

            // Calculate average opponent rating
            double avgOpponentRating = 0;
            double avgOpponentDeviation = 0;
            int opponentCount = 0;
            for (TournamentResult other : results) {
                if (!other.getPlayerUuid().equals(r.getPlayerUuid())) {
                    PlayerRating opp = ratings.get(other.getPlayerUuid());
                    avgOpponentRating += opp.getRating();
                    avgOpponentDeviation += opp.getDeviation();
                    opponentCount++;
                }
            }
            if (opponentCount > 0) {
                avgOpponentRating /= opponentCount;
                avgOpponentDeviation /= opponentCount;
            }

            int newRating = calculateGlicko2(
                pr.getRating(), pr.getDeviation(), pr.getVolatility(),
                avgOpponentRating, avgOpponentDeviation, score
            );

            // Update deviation (decreases with more games)
            int newDeviation = Math.max(30, (int) (pr.getDeviation() * 0.9));

            pr.updateRating(newRating);
            pr.setDeviation(newDeviation);
            pr.setGamesPlayed(pr.getGamesPlayed() + 1);
            saveRating(pr);
        }
    }

    /**
     * Simplified Glicko-2 calculation for a single player against an average opponent.
     * Uses 1000 as center rating instead of standard 1500 to match our rating system.
     */
    private static int calculateGlicko2(
        double playerRating, double playerDeviation, double playerVolatility,
        double opponentRating, double opponentDeviation, double score
    ) {
        // Convert to Glicko-2 scale (centered at DEFAULT_RATING = 1000)
        double mu = (playerRating - DEFAULT_RATING) / GLICKO2_SCALE;
        double phi = playerDeviation / GLICKO2_SCALE;
        double muJ = (opponentRating - DEFAULT_RATING) / GLICKO2_SCALE;
        double phiJ = opponentDeviation / GLICKO2_SCALE;

        // g(phi)
        double gPhiJ = 1.0 / Math.sqrt(1.0 + 3.0 * phiJ * phiJ / (Math.PI * Math.PI));

        // E(mu, muJ, phiJ)
        double eMu = 1.0 / (1.0 + Math.exp(-gPhiJ * (mu - muJ)));

        // Variance v
        double v = 1.0 / (gPhiJ * gPhiJ * eMu * (1.0 - eMu));

        // Delta
        double delta = v * gPhiJ * (score - eMu);

        // New volatility (simplified — use current)
        double newSigma = playerVolatility;

        // New phi
        double phiStar = Math.sqrt(phi * phi + newSigma * newSigma);
        double newPhi = 1.0 / Math.sqrt(1.0 / (phiStar * phiStar) + 1.0 / v);

        // New mu
        double newMu = mu + newPhi * newPhi * gPhiJ * (score - eMu);

        // Convert back (centered at DEFAULT_RATING = 1000)
        return (int) Math.round(GLICKO2_SCALE * newMu + DEFAULT_RATING);
    }

    /**
     * Performs soft reset for all ratings in a season transition.
     */
    public static void softResetAll(int newSeasonId) {
        try {
            List<DbRow> rows = DB.getResults("SELECT * FROM ts_player_rating");
            for (DbRow row : rows) {
                UUID uuid = UUID.fromString(row.getString("uuid"));
                PlayerRating pr = getOrCreateRating(uuid);
                pr.softReset(newSeasonId);
                saveRating(pr);
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to soft reset ratings", e);
        }
    }
}
