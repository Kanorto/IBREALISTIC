package me.makkuusen.timing.system.tournament;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.economy.LevelManager;
import me.makkuusen.timing.system.economy.RallyCoinManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Central manager for the tournament system.
 * Handles lifecycle, scheduling, participation, and result calculation.
 */
public class TournamentManager {

    // ─── CONSTANTS ───

    private static final long CHECK_INTERVAL_TICKS = 6000L; // 5 minutes
    private static final int MAX_BRACKET_PLAYERS = 64;
    private static final int DEFAULT_MIN_PLAYERS = 3;

    // ─── STATE ───

    /** Active tournaments keyed by type — only one per type allowed. */
    private static final Map<TournamentType, Tournament> activeTournaments = new ConcurrentHashMap<>();

    /** Cached bracket matches for BRACKET tournaments. */
    private static final Map<Integer, List<BracketMatch>> bracketCache = new ConcurrentHashMap<>();

    private static BukkitTask schedulerTask;

    // ─── INITIALIZATION ───

    /**
     * Starts the tournament scheduler. Called from plugin onEnable.
     */
    public static void start() {
        loadActiveTournaments();
        schedulerTask = Bukkit.getScheduler().runTaskTimer(
            TimingSystem.getPlugin(), TournamentManager::checkScheduledTasks, 200L, CHECK_INTERVAL_TICKS
        );
        TimingSystem.getPlugin().getLogger().info("Tournament system initialized.");
    }

    /**
     * Stops the tournament scheduler. Called from plugin onDisable.
     */
    public static void stop() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }
    }

    /**
     * Loads active tournaments from the database.
     */
    private static void loadActiveTournaments() {
        activeTournaments.clear();
        try {
            List<DbRow> rows = DB.getResults(
                "SELECT * FROM ts_tournaments WHERE state IN ('SCHEDULED', 'QUALIFYING', 'ACTIVE') ORDER BY id"
            );
            for (DbRow row : rows) {
                Tournament t = rowToTournament(row);
                activeTournaments.put(t.getType(), t);
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to load active tournaments", e);
        }
    }

    // ─── SCHEDULED TASKS ───

    /**
     * Periodic check for tournament state transitions.
     */
    private static void checkScheduledTasks() {
        long now = System.currentTimeMillis();

        for (Tournament t : new ArrayList<>(activeTournaments.values())) {
            // Check if SCHEDULED tournament should start
            if (t.getState() == TournamentState.SCHEDULED && now >= t.getStartTimestamp()) {
                if (t.getType() == TournamentType.BRACKET) {
                    transitionToQualifying(t);
                } else {
                    transitionToActive(t);
                }
            }
            // Check if active tournament should end
            if (t.isActive() && now >= t.getEndTimestamp()) {
                calculateAndFinish(t);
            }
        }

        // Check season expiry
        SeasonManager.checkSeasonExpiry();
    }

    // ─── TOURNAMENT LIFECYCLE ───

    /**
     * Creates a new tournament.
     * @return the created tournament, or null if one of that type already exists
     */
    public static Tournament createTournament(String name, TournamentType type, List<Integer> trackIds,
                                               long startTimestamp, long endTimestamp,
                                               CarRestriction restriction, int minDifficulty, int maxDifficulty) {
        if (activeTournaments.containsKey(type)) {
            return null; // Already an active tournament of this type
        }

        Tournament t = new Tournament();
        t.setName(name);
        t.setType(type);
        t.setState(TournamentState.SCHEDULED);
        t.setStartTimestamp(startTimestamp);
        t.setEndTimestamp(endTimestamp);
        t.setTrackIds(trackIds);
        t.setCarRestriction(restriction);
        t.setMinDifficulty(minDifficulty);
        t.setMaxDifficulty(maxDifficulty);
        t.setRewards(Tournament.defaultRewards());

        Season season = SeasonManager.getOrCreateCurrentSeason();
        t.setSeasonId(season.getId());

        try {
            long id = DB.executeInsert(
                "INSERT INTO ts_tournaments (name, type, state, start_timestamp, end_timestamp, track_ids, car_restriction, season_id, rewards, min_players, min_difficulty, max_difficulty, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                t.getName(), t.getType().name(), t.getState().name(),
                t.getStartTimestamp(), t.getEndTimestamp(),
                t.trackIdsToJson(), t.getCarRestriction().name(),
                t.getSeasonId(), t.rewardsToJson(), t.getMinPlayers(),
                t.getMinDifficulty(), t.getMaxDifficulty(), System.currentTimeMillis()
            );
            t.setId((int) id);
            activeTournaments.put(type, t);
            return t;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to create tournament", e);
            return null;
        }
    }

    /**
     * Transitions a tournament from SCHEDULED to ACTIVE.
     */
    private static void transitionToActive(Tournament t) {
        t.setState(TournamentState.ACTIVE);
        updateState(t);
        broadcastTournamentStart(t);
    }

    /**
     * Transitions a BRACKET tournament to QUALIFYING phase.
     */
    private static void transitionToQualifying(Tournament t) {
        t.setState(TournamentState.QUALIFYING);
        updateState(t);
        broadcastTournamentQualifying(t);
    }

    /**
     * Calculates results and finishes a tournament.
     */
    public static void calculateAndFinish(Tournament t) {
        t.setState(TournamentState.CALCULATING);
        updateState(t);

        try {
            List<TournamentResult> results = getResults(t.getId());

            // Check minimum participants
            if (results.size() < t.getMinPlayers()) {
                cancelTournament(t, "Not enough participants (minimum: " + t.getMinPlayers() + ")");
                return;
            }

            // Sort results by total time (ascending)
            results.sort(Comparator.comparingLong(TournamentResult::getTotalTimeMs));

            // Assign positions
            for (int i = 0; i < results.size(); i++) {
                TournamentResult r = results.get(i);
                r.setPosition(i + 1);
                saveResult(r);
            }

            // Update ELO ratings
            RatingManager.updateRatingsForTournament(results);

            // Update season points
            for (TournamentResult r : results) {
                SeasonManager.addTournamentPoints(r.getPlayerUuid(), r.getPosition());
            }

            // Distribute rewards
            distributeRewards(t, results);

            t.setState(TournamentState.FINISHED);
            updateState(t);
            activeTournaments.remove(t.getType());

            broadcastTournamentResults(t, results);
        } catch (Exception e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to calculate tournament results", e);
            t.setState(TournamentState.ACTIVE);
            updateState(t);
        }
    }

    /**
     * Cancels a tournament.
     */
    public static void cancelTournament(Tournament t, String reason) {
        t.setState(TournamentState.CANCELLED);
        updateState(t);
        activeTournaments.remove(t.getType());
        Bukkit.broadcast(
            Component.text("🏆 Tournament \"" + t.getName() + "\" cancelled: " + reason, NamedTextColor.RED)
        );
    }

    // ─── PARTICIPATION ───

    /**
     * Records a race result for a player in any active tournament on the given track.
     * Called automatically when a player finishes a time trial.
     */
    public static void onTrackComplete(UUID playerUuid, int trackId, long timeMs) {
        for (Tournament t : activeTournaments.values()) {
            if (!t.isActive()) continue;
            if (!t.getTrackIds().contains(trackId)) continue;
            if (t.getType() == TournamentType.BRACKET) continue; // Brackets handled separately

            recordResult(t, playerUuid, trackId, timeMs);
        }
    }

    /**
     * Records or updates a player's result for a tournament.
     */
    private static void recordResult(Tournament t, UUID playerUuid, int trackId, long timeMs) {
        try {
            DbRow existing = DB.getFirstRow(
                "SELECT * FROM ts_tournament_results WHERE tournament_id = ? AND uuid = ?",
                t.getId(), playerUuid.toString()
            );

            if (existing == null) {
                // New participant
                Map<Integer, Long> trackTimes = new HashMap<>();
                trackTimes.put(trackId, timeMs);
                long totalTime = timeMs;
                TournamentResult tr = new TournamentResult();
                tr.setTrackTimes(trackTimes);

                DB.executeInsert(
                    "INSERT INTO ts_tournament_results (tournament_id, uuid, track_times, total_time_ms, position, reward_claimed) VALUES (?, ?, ?, ?, 0, 0)",
                    t.getId(), playerUuid.toString(), tr.trackTimesToJson(), totalTime
                );
            } else {
                // Update existing result
                Map<Integer, Long> trackTimes = TournamentResult.trackTimesFromJson(existing.getString("track_times"));
                Long existingTime = trackTimes.get(trackId);
                if (existingTime == null || timeMs < existingTime) {
                    trackTimes.put(trackId, timeMs);
                    long totalTime = trackTimes.values().stream().mapToLong(Long::longValue).sum();
                    TournamentResult tr = new TournamentResult();
                    tr.setTrackTimes(trackTimes);

                    DB.executeUpdate(
                        "UPDATE ts_tournament_results SET track_times = ?, total_time_ms = ? WHERE id = ?",
                        tr.trackTimesToJson(), totalTime, existing.getInt("id")
                    );
                }
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to record tournament result", e);
        }
    }

    // ─── BRACKET MANAGEMENT ───

    /**
     * Generates a double-elimination bracket from qualification results.
     */
    public static void generateBracket(Tournament t) {
        if (t.getType() != TournamentType.BRACKET) return;

        List<TournamentResult> qualResults = getResults(t.getId());
        qualResults.sort(Comparator.comparingLong(TournamentResult::getTotalTimeMs));

        // Limit to max bracket size
        int bracketSize = Math.min(qualResults.size(), MAX_BRACKET_PLAYERS);
        // Round down to power of 2
        int actualSize = Integer.highestOneBit(bracketSize);
        if (actualSize < 2) {
            cancelTournament(t, "Not enough qualified players for bracket");
            return;
        }

        List<UUID> seeds = qualResults.stream()
            .limit(actualSize)
            .map(TournamentResult::getPlayerUuid)
            .collect(Collectors.toList());

        // Generate upper bracket round 1
        List<BracketMatch> matches = new ArrayList<>();
        int matchCount = actualSize / 2;
        for (int i = 0; i < matchCount; i++) {
            BracketMatch m = new BracketMatch();
            m.setTournamentId(t.getId());
            m.setRoundNumber(1);
            m.setMatchIndex(i);
            m.setUpperBracket(true);
            m.setPlayer1Uuid(seeds.get(i));
            m.setPlayer2Uuid(seeds.get(actualSize - 1 - i));
            m.setState("ACTIVE");
            // Assign track from tournament's track list
            int trackIdx = i % t.getTrackIds().size();
            m.setTrackId(t.getTrackIds().get(trackIdx));
            saveBracketMatch(m);
            matches.add(m);
        }

        bracketCache.put(t.getId(), matches);
        t.setState(TournamentState.ACTIVE);
        updateState(t);
        broadcastTournamentBracketStart(t, actualSize);
    }

    /**
     * Records a bracket match result for a player.
     */
    public static void onBracketRaceComplete(UUID playerUuid, int trackId, long timeMs) {
        for (Tournament t : activeTournaments.values()) {
            if (t.getType() != TournamentType.BRACKET || t.getState() != TournamentState.ACTIVE) continue;

            List<BracketMatch> matches = getBracketMatches(t.getId());
            for (BracketMatch m : matches) {
                if (!m.isActive()) continue;
                if (m.getTrackId() != trackId) continue;
                if (!m.hasPlayer(playerUuid)) continue;

                // Record time
                if (playerUuid.equals(m.getPlayer1Uuid())) {
                    if (m.getPlayer1TimeMs() <= 0 || timeMs < m.getPlayer1TimeMs()) {
                        m.setPlayer1TimeMs(timeMs);
                    }
                } else if (playerUuid.equals(m.getPlayer2Uuid())) {
                    if (m.getPlayer2TimeMs() <= 0 || timeMs < m.getPlayer2TimeMs()) {
                        m.setPlayer2TimeMs(timeMs);
                    }
                }
                updateBracketMatch(m);

                // Check if both players have raced
                if (m.getPlayer1TimeMs() > 0 && m.getPlayer2TimeMs() > 0) {
                    m.setWinnerUuid(m.determineWinner());
                    m.setState("COMPLETED");
                    updateBracketMatch(m);
                    advanceBracket(t, m);
                }
            }
        }
    }

    /**
     * Advances the bracket after a match is completed.
     * In double elimination: winners go to upper bracket next round, losers to lower bracket.
     */
    private static void advanceBracket(Tournament t, BracketMatch completedMatch) {
        List<BracketMatch> allMatches = getBracketMatches(t.getId());
        UUID winner = completedMatch.getWinnerUuid();
        UUID loser = completedMatch.determineLoser();

        // Check if all matches in current round are done
        int round = completedMatch.getRoundNumber();
        boolean isUpper = completedMatch.isUpperBracket();

        List<BracketMatch> roundMatches = allMatches.stream()
            .filter(m -> m.getRoundNumber() == round && m.isUpperBracket() == isUpper)
            .collect(Collectors.toList());

        boolean allDone = roundMatches.stream().allMatch(BracketMatch::isCompleted);

        if (allDone) {
            List<UUID> winners = roundMatches.stream().map(BracketMatch::getWinnerUuid).collect(Collectors.toList());
            List<UUID> losers = roundMatches.stream().map(BracketMatch::determineLoser).filter(Objects::nonNull).collect(Collectors.toList());

            if (winners.size() == 1) {
                // Tournament over — this was the final
                finishBracketTournament(t, winners.get(0));
                return;
            }

            // Create next round of upper bracket
            int nextRound = round + 1;
            for (int i = 0; i < winners.size() / 2; i++) {
                BracketMatch next = new BracketMatch();
                next.setTournamentId(t.getId());
                next.setRoundNumber(nextRound);
                next.setMatchIndex(i);
                next.setUpperBracket(true);
                next.setPlayer1Uuid(winners.get(i * 2));
                next.setPlayer2Uuid(winners.get(i * 2 + 1));
                next.setState("ACTIVE");
                int trackIdx = i % t.getTrackIds().size();
                next.setTrackId(t.getTrackIds().get(trackIdx));
                saveBracketMatch(next);
            }

            // Create lower bracket round for losers (double elimination)
            if (isUpper && losers.size() >= 2) {
                for (int i = 0; i < losers.size() / 2; i++) {
                    BracketMatch lower = new BracketMatch();
                    lower.setTournamentId(t.getId());
                    lower.setRoundNumber(nextRound);
                    lower.setMatchIndex(i);
                    lower.setUpperBracket(false);
                    lower.setPlayer1Uuid(losers.get(i * 2));
                    lower.setPlayer2Uuid(losers.get(i * 2 + 1));
                    lower.setState("ACTIVE");
                    int trackIdx = i % t.getTrackIds().size();
                    lower.setTrackId(t.getTrackIds().get(trackIdx));
                    saveBracketMatch(lower);
                }
            }

            // Refresh cache
            bracketCache.put(t.getId(), getBracketMatchesFromDb(t.getId()));
        }
    }

    /**
     * Finishes a bracket tournament with the given winner.
     */
    private static void finishBracketTournament(Tournament t, UUID winnerUuid) {
        // Build results from bracket matches
        List<BracketMatch> allMatches = getBracketMatchesFromDb(t.getId());
        Map<UUID, Integer> playerWins = new HashMap<>();
        Set<UUID> allPlayers = new HashSet<>();

        for (BracketMatch m : allMatches) {
            if (m.getPlayer1Uuid() != null) allPlayers.add(m.getPlayer1Uuid());
            if (m.getPlayer2Uuid() != null) allPlayers.add(m.getPlayer2Uuid());
            if (m.getWinnerUuid() != null) {
                playerWins.merge(m.getWinnerUuid(), 1, Integer::sum);
            }
        }

        // Sort by wins (descending) to determine positions
        List<Map.Entry<UUID, Integer>> sorted = playerWins.entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .collect(Collectors.toList());

        List<TournamentResult> results = new ArrayList<>();
        int pos = 1;
        for (Map.Entry<UUID, Integer> entry : sorted) {
            TournamentResult r = new TournamentResult();
            r.setTournamentId(t.getId());
            r.setPlayerUuid(entry.getKey());
            r.setPosition(pos++);
            r.setTotalTimeMs(entry.getValue()); // store wins as "time" for bracket
            results.add(r);
            saveResult(r);
        }

        // Update ratings and season points
        if (results.size() >= 2) {
            RatingManager.updateRatingsForTournament(results);
        }
        for (TournamentResult r : results) {
            SeasonManager.addTournamentPoints(r.getPlayerUuid(), r.getPosition());
        }

        distributeRewards(t, results);

        t.setState(TournamentState.FINISHED);
        updateState(t);
        activeTournaments.remove(t.getType());
        broadcastTournamentResults(t, results);
    }

    // ─── REWARDS ───

    private static void distributeRewards(Tournament t, List<TournamentResult> results) {
        for (TournamentResult r : results) {
            TournamentReward reward = t.getRewardForPosition(r.getPosition());
            if (reward == null) {
                reward = t.getRewardForPosition(0); // participation reward
            }
            if (reward == null) continue;

            RallyCoinManager.addCoins(r.getPlayerUuid(), reward.getCoins(), "Tournament: " + t.getName() + " (pos " + r.getPosition() + ")");
            LevelManager.addXP(r.getPlayerUuid(), reward.getXp(), "Tournament: " + t.getName());

            Player player = Bukkit.getPlayer(r.getPlayerUuid());
            if (player != null) {
                player.sendMessage(
                    Component.text("🏆 Tournament \"" + t.getName() + "\" — Position #" + r.getPosition() + "! ", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text("+" + reward.getCoins() + " 🪙 +" + reward.getXp() + " XP", NamedTextColor.GREEN))
                );
            }
            r.setRewardClaimed(true);
        }
    }

    // ─── QUERIES ───

    /**
     * Gets all active tournaments.
     */
    public static Collection<Tournament> getActiveTournaments() {
        return activeTournaments.values();
    }

    /**
     * Gets an active tournament by type.
     */
    public static Tournament getActiveTournament(TournamentType type) {
        return activeTournaments.get(type);
    }

    /**
     * Gets a tournament by ID.
     */
    public static Tournament getTournamentById(int id) {
        try {
            DbRow row = DB.getFirstRow("SELECT * FROM ts_tournaments WHERE id = ?", id);
            return row != null ? rowToTournament(row) : null;
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Gets results for a tournament, sorted by total time.
     */
    public static List<TournamentResult> getResults(int tournamentId) {
        List<TournamentResult> results = new ArrayList<>();
        try {
            List<DbRow> rows = DB.getResults(
                "SELECT * FROM ts_tournament_results WHERE tournament_id = ? ORDER BY total_time_ms ASC",
                tournamentId
            );
            for (DbRow row : rows) {
                TournamentResult r = new TournamentResult();
                r.setId(row.getInt("id"));
                r.setTournamentId(row.getInt("tournament_id"));
                r.setPlayerUuid(UUID.fromString(row.getString("uuid")));
                r.setTrackTimes(TournamentResult.trackTimesFromJson(row.getString("track_times")));
                r.setTotalTimeMs(row.getLong("total_time_ms"));
                r.setPosition(row.getInt("position"));
                r.setRewardClaimed(row.getInt("reward_claimed") == 1);
                results.add(r);
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get tournament results", e);
        }
        return results;
    }

    /**
     * Gets bracket matches for a tournament.
     */
    public static List<BracketMatch> getBracketMatches(int tournamentId) {
        List<BracketMatch> cached = bracketCache.get(tournamentId);
        if (cached != null) return cached;
        List<BracketMatch> matches = getBracketMatchesFromDb(tournamentId);
        bracketCache.put(tournamentId, matches);
        return matches;
    }

    /**
     * Gets recent finished tournaments.
     */
    public static List<Tournament> getRecentTournaments(int limit) {
        List<Tournament> result = new ArrayList<>();
        try {
            List<DbRow> rows = DB.getResults(
                "SELECT * FROM ts_tournaments WHERE state IN ('FINISHED', 'ARCHIVED') ORDER BY end_timestamp DESC LIMIT ?",
                limit
            );
            for (DbRow row : rows) {
                result.add(rowToTournament(row));
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get recent tournaments", e);
        }
        return result;
    }

    // ─── DATABASE HELPERS ───

    private static void updateState(Tournament t) {
        try {
            DB.executeUpdate(
                "UPDATE ts_tournaments SET state = ? WHERE id = ?", t.getState().name(), t.getId()
            );
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to update tournament state", e);
        }
    }

    private static void saveResult(TournamentResult r) {
        try {
            if (r.getId() > 0) {
                DB.executeUpdate(
                    "UPDATE ts_tournament_results SET track_times = ?, total_time_ms = ?, position = ?, reward_claimed = ? WHERE id = ?",
                    r.trackTimesToJson(), r.getTotalTimeMs(), r.getPosition(),
                    r.isRewardClaimed() ? 1 : 0, r.getId()
                );
            } else {
                long id = DB.executeInsert(
                    "INSERT INTO ts_tournament_results (tournament_id, uuid, track_times, total_time_ms, position, reward_claimed) VALUES (?, ?, ?, ?, ?, ?)",
                    r.getTournamentId(), r.getPlayerUuid().toString(), r.trackTimesToJson(),
                    r.getTotalTimeMs(), r.getPosition(), r.isRewardClaimed() ? 1 : 0
                );
                r.setId((int) id);
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to save tournament result", e);
        }
    }

    private static void saveBracketMatch(BracketMatch m) {
        try {
            long id = DB.executeInsert(
                "INSERT INTO ts_bracket_matches (tournament_id, round_number, match_index, upper_bracket, player1_uuid, player2_uuid, winner_uuid, player1_time_ms, player2_time_ms, track_id, state) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                m.getTournamentId(), m.getRoundNumber(), m.getMatchIndex(),
                m.isUpperBracket() ? 1 : 0,
                m.getPlayer1Uuid() != null ? m.getPlayer1Uuid().toString() : null,
                m.getPlayer2Uuid() != null ? m.getPlayer2Uuid().toString() : null,
                m.getWinnerUuid() != null ? m.getWinnerUuid().toString() : null,
                m.getPlayer1TimeMs(), m.getPlayer2TimeMs(), m.getTrackId(), m.getState()
            );
            m.setId((int) id);
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to save bracket match", e);
        }
    }

    private static void updateBracketMatch(BracketMatch m) {
        try {
            DB.executeUpdate(
                "UPDATE ts_bracket_matches SET winner_uuid = ?, player1_time_ms = ?, player2_time_ms = ?, state = ? WHERE id = ?",
                m.getWinnerUuid() != null ? m.getWinnerUuid().toString() : null,
                m.getPlayer1TimeMs(), m.getPlayer2TimeMs(), m.getState(), m.getId()
            );
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to update bracket match", e);
        }
    }

    private static List<BracketMatch> getBracketMatchesFromDb(int tournamentId) {
        List<BracketMatch> matches = new ArrayList<>();
        try {
            List<DbRow> rows = DB.getResults(
                "SELECT * FROM ts_bracket_matches WHERE tournament_id = ? ORDER BY round_number, match_index",
                tournamentId
            );
            for (DbRow row : rows) {
                BracketMatch m = new BracketMatch();
                m.setId(row.getInt("id"));
                m.setTournamentId(row.getInt("tournament_id"));
                m.setRoundNumber(row.getInt("round_number"));
                m.setMatchIndex(row.getInt("match_index"));
                m.setUpperBracket(row.getInt("upper_bracket") == 1);
                String p1 = row.getString("player1_uuid");
                String p2 = row.getString("player2_uuid");
                String w = row.getString("winner_uuid");
                if (p1 != null && !p1.isEmpty()) m.setPlayer1Uuid(UUID.fromString(p1));
                if (p2 != null && !p2.isEmpty()) m.setPlayer2Uuid(UUID.fromString(p2));
                if (w != null && !w.isEmpty()) m.setWinnerUuid(UUID.fromString(w));
                m.setPlayer1TimeMs(row.getLong("player1_time_ms"));
                m.setPlayer2TimeMs(row.getLong("player2_time_ms"));
                m.setTrackId(row.getInt("track_id"));
                m.setState(row.getString("state"));
                matches.add(m);
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get bracket matches", e);
        }
        return matches;
    }

    private static Tournament rowToTournament(DbRow row) {
        Tournament t = new Tournament();
        t.setId(row.getInt("id"));
        t.setName(row.getString("name"));
        try { t.setType(TournamentType.valueOf(row.getString("type"))); } catch (Exception e) { t.setType(TournamentType.SPRINT); }
        try { t.setState(TournamentState.valueOf(row.getString("state"))); } catch (Exception e) { t.setState(TournamentState.SCHEDULED); }
        t.setStartTimestamp(row.getLong("start_timestamp"));
        t.setEndTimestamp(row.getLong("end_timestamp"));
        t.setTrackIds(Tournament.trackIdsFromJson(row.getString("track_ids")));
        try { t.setCarRestriction(CarRestriction.valueOf(row.getString("car_restriction"))); } catch (Exception e) { t.setCarRestriction(CarRestriction.ALL); }
        t.setSeasonId(row.getInt("season_id"));
        t.setRewards(Tournament.rewardsFromJson(row.getString("rewards")));
        t.setMinPlayers(row.getInt("min_players"));
        t.setMinDifficulty(row.getInt("min_difficulty"));
        t.setMaxDifficulty(row.getInt("max_difficulty"));
        return t;
    }

    // ─── BROADCASTS ───

    private static void broadcastTournamentStart(Tournament t) {
        Bukkit.broadcast(
            Component.text("🏆 ", NamedTextColor.GOLD)
                .append(Component.text("Tournament \"" + t.getName() + "\" has started! ", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text("Type: " + t.getType().getDisplayName() + " | /tournament for details", NamedTextColor.GRAY))
        );
    }

    private static void broadcastTournamentQualifying(Tournament t) {
        Bukkit.broadcast(
            Component.text("🏆 ", NamedTextColor.GOLD)
                .append(Component.text("Tournament \"" + t.getName() + "\" — Qualification phase! ", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text("Race on tournament tracks to qualify!", NamedTextColor.GRAY))
        );
    }

    private static void broadcastTournamentBracketStart(Tournament t, int playerCount) {
        Bukkit.broadcast(
            Component.text("🏆 ", NamedTextColor.GOLD)
                .append(Component.text("Tournament \"" + t.getName() + "\" — Bracket phase! ", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(playerCount + " players in double elimination!", NamedTextColor.GRAY))
        );
    }

    private static void broadcastTournamentResults(Tournament t, List<TournamentResult> results) {
        Component msg = Component.text("🏆 ", NamedTextColor.GOLD)
            .append(Component.text("Tournament \"" + t.getName() + "\" finished!\n", NamedTextColor.GREEN, TextDecoration.BOLD));

        int showCount = Math.min(3, results.size());
        String[] medals = {"🥇", "🥈", "🥉"};
        for (int i = 0; i < showCount; i++) {
            TournamentResult r = results.get(i);
            String name = Bukkit.getOfflinePlayer(r.getPlayerUuid()).getName();
            if (name == null) name = r.getPlayerUuid().toString().substring(0, 8);
            msg = msg.append(Component.text(medals[i] + " #" + (i + 1) + " " + name + "\n", NamedTextColor.YELLOW));
        }

        Bukkit.broadcast(msg);
    }
}
