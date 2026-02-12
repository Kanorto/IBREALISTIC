package me.makkuusen.timing.system.economy;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages daily challenges — 3 per day, shared for all players.
 * Challenges reset at 00:00 UTC.
 */
public class DailyChallengeManager {

    // ─── CHALLENGE TYPES ───

    public enum ChallengeType {
        COMPLETE_TRACKS("Complete %d tracks", 3, 30, 20),
        BEAT_RECORD("Beat your personal record on any track", 1, 50, 30),
        COMPLETE_FIVE_TRACKS("Complete 5 tracks", 5, 60, 35),
        FIRST_PLACE("Finish with a new #1 time on any track", 1, 80, 50),
        COMPLETE_TEN_TRACKS("Complete 10 tracks in total", 10, 100, 60);

        private final String description;
        private final int targetCount;
        private final int coinReward;
        private final int xpReward;

        ChallengeType(String description, int targetCount, int coinReward, int xpReward) {
            this.description = description;
            this.targetCount = targetCount;
            this.coinReward = coinReward;
            this.xpReward = xpReward;
        }

        public String getDescription() {
            if (description.contains("%d")) return String.format(description, targetCount);
            return description;
        }

        public int getTargetCount() { return targetCount; }
        public int getCoinReward() { return coinReward; }
        public int getXpReward() { return xpReward; }
    }

    private static final int CHALLENGES_PER_DAY = 3;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Gets today's date string in UTC.
     */
    public static String getTodayDate() {
        return LocalDate.now(ZoneOffset.UTC).format(DATE_FORMAT);
    }

    /**
     * Generates daily challenges for today if they don't exist yet.
     */
    public static void ensureTodayChallenges() {
        String today = getTodayDate();
        try {
            DbRow existing = DB.getFirstRow("SELECT id FROM ts_daily_challenges WHERE date = ?", today);
            if (existing != null) return; // Already generated

            // Pick 3 random challenge types
            List<ChallengeType> types = new ArrayList<>(Arrays.asList(ChallengeType.values()));
            Collections.shuffle(types);

            for (int i = 0; i < Math.min(CHALLENGES_PER_DAY, types.size()); i++) {
                ChallengeType ct = types.get(i);
                DB.executeInsert(
                    "INSERT INTO ts_daily_challenges (date, challenge_type, slot) VALUES (?, ?, ?)",
                    today, ct.name(), i
                );
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to generate daily challenges", e);
        }
    }

    /**
     * Gets today's challenges as a list of ChallengeType.
     */
    public static List<ChallengeType> getTodayChallenges() {
        ensureTodayChallenges();
        String today = getTodayDate();
        List<ChallengeType> result = new ArrayList<>();
        try {
            List<DbRow> rows = DB.getResults(
                "SELECT challenge_type FROM ts_daily_challenges WHERE date = ? ORDER BY slot",
                today
            );
            for (DbRow row : rows) {
                try {
                    result.add(ChallengeType.valueOf(row.getString("challenge_type")));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to load daily challenges", e);
        }
        return result;
    }

    /**
     * Gets a player's progress on a specific challenge slot for today.
     */
    public static int getProgress(UUID uuid, int slot) {
        String today = getTodayDate();
        try {
            DbRow row = DB.getFirstRow(
                "SELECT progress FROM ts_player_daily_progress WHERE uuid = ? AND date = ? AND slot = ?",
                uuid.toString(), today, slot
            );
            return row != null ? row.getInt("progress") : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Checks if a player has completed a specific challenge slot today.
     */
    public static boolean isCompleted(UUID uuid, int slot) {
        List<ChallengeType> challenges = getTodayChallenges();
        if (slot < 0 || slot >= challenges.size()) return false;
        return getProgress(uuid, slot) >= challenges.get(slot).getTargetCount();
    }

    /**
     * Increments progress for applicable challenges.
     * Called when a player completes a time trial.
     *
     * @param uuid player UUID
     * @param isNewRecord whether this was a new personal best
     * @param isFirstPlace whether this achieved #1 on the leaderboard
     * @param trackName the track name (for different-tracks challenge)
     */
    public static void onTrackComplete(UUID uuid, boolean isNewRecord, boolean isFirstPlace, String trackName) {
        List<ChallengeType> challenges = getTodayChallenges();
        for (int slot = 0; slot < challenges.size(); slot++) {
            ChallengeType ct = challenges.get(slot);
            if (isCompleted(uuid, slot)) continue;

            boolean applies = switch (ct) {
                case COMPLETE_TRACKS, COMPLETE_FIVE_TRACKS, COMPLETE_TEN_TRACKS -> true;
                case BEAT_RECORD -> isNewRecord;
                case FIRST_PLACE -> isFirstPlace;
            };

            if (applies) {
                incrementProgress(uuid, slot, ct);
            }
        }
    }

    /**
     * Increments challenge progress and awards rewards if completed.
     */
    private static void incrementProgress(UUID uuid, int slot, ChallengeType ct) {
        String today = getTodayDate();
        try {
            DbRow existing = DB.getFirstRow(
                "SELECT progress FROM ts_player_daily_progress WHERE uuid = ? AND date = ? AND slot = ?",
                uuid.toString(), today, slot
            );
            int currentProgress;
            if (existing == null) {
                DB.executeInsert(
                    "INSERT INTO ts_player_daily_progress (uuid, date, slot, progress) VALUES (?, ?, ?, 1)",
                    uuid.toString(), today, slot
                );
                currentProgress = 1;
            } else {
                currentProgress = existing.getInt("progress") + 1;
                DB.executeUpdate(
                    "UPDATE ts_player_daily_progress SET progress = ? WHERE uuid = ? AND date = ? AND slot = ?",
                    currentProgress, uuid.toString(), today, slot
                );
            }

            // Check if just completed
            if (currentProgress == ct.getTargetCount()) {
                // Award rewards
                RallyCoinManager.addCoins(uuid, ct.getCoinReward(), "Daily challenge: " + ct.getDescription());
                LevelManager.addXP(uuid, ct.getXpReward(), "Daily challenge");

                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.sendMessage(
                        Component.text("✓ Daily Challenge Complete! ", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .append(Component.text("+" + ct.getCoinReward() + " 🪙 +" + ct.getXpReward() + " XP", NamedTextColor.GOLD))
                    );
                }
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to update challenge progress", e);
        }
    }

    /**
     * Regenerates today's challenges (admin command).
     */
    public static void regenerate() {
        String today = getTodayDate();
        try {
            DB.executeUpdate("DELETE FROM ts_daily_challenges WHERE date = ?", today);
            DB.executeUpdate("DELETE FROM ts_player_daily_progress WHERE date = ?", today);
            ensureTodayChallenges();
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to regenerate daily challenges", e);
        }
    }
}
