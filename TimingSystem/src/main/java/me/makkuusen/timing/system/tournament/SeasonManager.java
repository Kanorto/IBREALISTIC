package me.makkuusen.timing.system.tournament;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages competitive seasons spanning multiple tournaments.
 * Seasons track points, rankings, and provide seasonal rewards.
 */
public class SeasonManager {

    private static final long DEFAULT_DURATION_MS = 90L * 24 * 60 * 60 * 1000; // 90 days

    // ─── SEASON LIFECYCLE ───

    /**
     * Gets the current active season, or creates one if none exists.
     */
    public static Season getOrCreateCurrentSeason() {
        try {
            DbRow row = DB.getFirstRow(
                "SELECT * FROM ts_seasons WHERE state = 'ACTIVE' ORDER BY id DESC LIMIT 1"
            );
            if (row != null) {
                return rowToSeason(row);
            }
            // Create first season
            return createNewSeason("Season 1");
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get/create season", e);
            Season fallback = new Season();
            fallback.setId(1);
            fallback.setName("Season 1");
            fallback.setState("ACTIVE");
            return fallback;
        }
    }

    /**
     * Creates a new season.
     */
    public static Season createNewSeason(String name) {
        try {
            long now = System.currentTimeMillis();
            long end = now + DEFAULT_DURATION_MS;
            long id = DB.executeInsert(
                "INSERT INTO ts_seasons (name, start_timestamp, end_timestamp, state) VALUES (?, ?, ?, 'ACTIVE')",
                name, now, end
            );
            Season s = new Season();
            s.setId((int) id);
            s.setName(name);
            s.setStartTimestamp(now);
            s.setEndTimestamp(end);
            s.setState("ACTIVE");
            return s;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to create season: " + name, e);
            return null;
        }
    }

    /**
     * Ends the current season and starts a new one.
     */
    public static Season endCurrentAndStartNew() {
        Season current = getOrCreateCurrentSeason();
        try {
            // Finish current
            DB.executeUpdate(
                "UPDATE ts_seasons SET state = 'FINISHED' WHERE id = ?", current.getId()
            );
            // Soft-reset ratings
            int nextSeasonId = current.getId() + 1;
            RatingManager.softResetAll(nextSeasonId);
            // Create new
            return createNewSeason("Season " + nextSeasonId);
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to end season", e);
            return null;
        }
    }

    /**
     * Checks if the current season has expired and should be automatically ended.
     */
    public static void checkSeasonExpiry() {
        Season current = getOrCreateCurrentSeason();
        // endTimestamp == 0 means the season has no automatic expiry (infinite duration)
        if (current.getEndTimestamp() > 0 && System.currentTimeMillis() >= current.getEndTimestamp()) {
            endCurrentAndStartNew();
            TimingSystem.getPlugin().getLogger().info("Season " + current.getName() + " has ended. New season started.");
        }
    }

    // ─── SEASON POINTS ───

    /**
     * Adds tournament result points for a player in the current season.
     */
    public static void addTournamentPoints(UUID uuid, int position) {
        Season season = getOrCreateCurrentSeason();
        int points = SeasonPoints.pointsForPosition(position);
        try {
            DbRow existing = DB.getFirstRow(
                "SELECT * FROM ts_season_points WHERE uuid = ? AND season_id = ?",
                uuid.toString(), season.getId()
            );
            if (existing == null) {
                DB.executeInsert(
                    "INSERT INTO ts_season_points (uuid, season_id, total_points, tournaments_played, best_position) VALUES (?, ?, ?, 1, ?)",
                    uuid.toString(), season.getId(), points, position
                );
            } else {
                int newTotal = existing.getInt("total_points") + points;
                int played = existing.getInt("tournaments_played") + 1;
                int bestPos = existing.getInt("best_position");
                if (bestPos == 0 || position < bestPos) bestPos = position;
                DB.executeUpdate(
                    "UPDATE ts_season_points SET total_points = ?, tournaments_played = ?, best_position = ? WHERE uuid = ? AND season_id = ?",
                    newTotal, played, bestPos, uuid.toString(), season.getId()
                );
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to add season points for " + uuid, e);
        }
    }

    /**
     * Gets a player's season points.
     */
    public static SeasonPoints getPlayerPoints(UUID uuid, int seasonId) {
        try {
            DbRow row = DB.getFirstRow(
                "SELECT * FROM ts_season_points WHERE uuid = ? AND season_id = ?",
                uuid.toString(), seasonId
            );
            if (row != null) {
                SeasonPoints sp = new SeasonPoints(uuid, seasonId);
                sp.setTotalPoints(row.getInt("total_points"));
                sp.setTournamentsPlayed(row.getInt("tournaments_played"));
                sp.setBestPosition(row.getInt("best_position"));
                return sp;
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get season points", e);
        }
        return new SeasonPoints(uuid, seasonId);
    }

    /**
     * Gets top players by season points.
     */
    public static List<SeasonPoints> getTopSeasonPoints(int seasonId, int limit) {
        List<SeasonPoints> result = new ArrayList<>();
        try {
            List<DbRow> rows = DB.getResults(
                "SELECT * FROM ts_season_points WHERE season_id = ? ORDER BY total_points DESC LIMIT ?",
                seasonId, limit
            );
            for (DbRow row : rows) {
                UUID uuid = UUID.fromString(row.getString("uuid"));
                SeasonPoints sp = new SeasonPoints(uuid, seasonId);
                sp.setTotalPoints(row.getInt("total_points"));
                sp.setTournamentsPlayed(row.getInt("tournaments_played"));
                sp.setBestPosition(row.getInt("best_position"));
                result.add(sp);
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get top season points", e);
        }
        return result;
    }

    /**
     * Gets all finished seasons.
     */
    public static List<Season> getFinishedSeasons() {
        List<Season> result = new ArrayList<>();
        try {
            List<DbRow> rows = DB.getResults(
                "SELECT * FROM ts_seasons WHERE state = 'FINISHED' ORDER BY id DESC"
            );
            for (DbRow row : rows) {
                result.add(rowToSeason(row));
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get finished seasons", e);
        }
        return result;
    }

    // ─── HELPERS ───

    private static Season rowToSeason(DbRow row) {
        Season s = new Season();
        s.setId(row.getInt("id"));
        s.setName(row.getString("name"));
        s.setStartTimestamp(row.getLong("start_timestamp"));
        s.setEndTimestamp(row.getLong("end_timestamp"));
        s.setState(row.getString("state"));
        return s;
    }
}
