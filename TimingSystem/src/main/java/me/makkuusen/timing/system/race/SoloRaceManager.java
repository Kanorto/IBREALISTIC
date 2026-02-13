package me.makkuusen.timing.system.race;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import co.aikar.taskchain.TaskChain;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.boatutils.BoatUtilsManager;
import me.makkuusen.timing.system.boatutils.CustomBoatUtilsMode;
import me.makkuusen.timing.system.economy.GarageManager;
import me.makkuusen.timing.system.economy.LevelManager;
import me.makkuusen.timing.system.economy.PlayerCar;
import me.makkuusen.timing.system.economy.RallyCoinManager;
import me.makkuusen.timing.system.loneliness.LonelinessController;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Broadcast;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.theme.messages.Warning;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackWeather;
import me.makkuusen.timing.system.track.locations.TrackLocation;
import me.makkuusen.timing.system.track.regions.TrackRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages race sessions — both solo (isolated) and multiplayer.
 * Solo races use player hiding + NOCOL for isolation.
 *
 * Phase 12 additions:
 * - Enhanced countdown with color stages (red → yellow → green)
 * - False start detection and penalties
 * - Time control region handling
 */
public class SoloRaceManager {

    // ─── CONFIGURATION DEFAULTS ───
    private static final int DEFAULT_MAX_CONCURRENT = 20;
    private static final int DEFAULT_COUNTDOWN_SECONDS = 5;
    private static final int DEFAULT_TIMEOUT_MINUTES = 10;
    private static final int RACE_COMPLETE_COINS = 30;
    private static final int RACE_COMPLETE_XP = 40;
    private static final double FALSE_START_MOVE_THRESHOLD = 0.5;

    // ─── ACTIVE SESSIONS ───
    private static final Map<UUID, RaceSession> activeSessions = new ConcurrentHashMap<>();

    // ─── SAVED WALK SPEEDS ───
    private static final Map<UUID, Float> savedWalkSpeeds = new ConcurrentHashMap<>();

    // ─── CONFIGURATION ───

    public static boolean isEnabled() {
        return TimingSystem.getPlugin().getConfig().getBoolean("race.enabled", true);
    }

    public static int getMaxConcurrent() {
        return TimingSystem.getPlugin().getConfig().getInt("race.max_concurrent", DEFAULT_MAX_CONCURRENT);
    }

    public static int getCountdownSeconds() {
        return TimingSystem.getPlugin().getConfig().getInt("race.countdown_seconds", DEFAULT_COUNTDOWN_SECONDS);
    }

    public static int getTimeoutMinutes() {
        return TimingSystem.getPlugin().getConfig().getInt("race.timeout_minutes", DEFAULT_TIMEOUT_MINUTES);
    }

    public static boolean isFalseStartEnabled() {
        return TimingSystem.getPlugin().getConfig().getBoolean("race.false_start.enabled", true);
    }

    public static boolean isTimeControlEnabled() {
        return TimingSystem.getPlugin().getConfig().getBoolean("race.time_control.enabled", true);
    }

    public static int getTimeControlWindowSeconds() {
        return TimingSystem.getPlugin().getConfig().getInt("race.time_control.window_seconds", 60);
    }

    public static int getTimeControlLatePenaltyPerMinute() {
        return TimingSystem.getPlugin().getConfig().getInt("race.time_control.late_penalty_seconds", 10);
    }

    public static int getTimeControlEarlyPenaltyPerMinute() {
        return TimingSystem.getPlugin().getConfig().getInt("race.time_control.early_penalty_seconds", 60);
    }

    // ─── SESSION MANAGEMENT ───

    public static Optional<RaceSession> getSession(UUID playerUuid) {
        return Optional.ofNullable(activeSessions.get(playerUuid));
    }

    public static boolean isInRace(UUID playerUuid) {
        return activeSessions.containsKey(playerUuid);
    }

    public static int getActiveSessionCount() {
        return (int) activeSessions.values().stream()
                .filter(RaceSession::isActive)
                .count();
    }

    // ─── SOLO RACE START ───

    public static boolean startSoloRace(Player player, Track track, String carType) {
        if (!isEnabled()) {
            Text.send(player, Error.RACE_NOT_ENABLED);
            return false;
        }

        if (isInRace(player.getUniqueId())) {
            Text.send(player, Error.RACE_ALREADY_ACTIVE);
            return false;
        }

        if (getActiveSessionCount() >= getMaxConcurrent()) {
            Text.send(player, Error.RACE_SERVER_FULL);
            return false;
        }

        if (!track.isOpen()) {
            Text.send(player, Error.TRACK_IS_CLOSED);
            return false;
        }

        if (!track.getTrackRegions().hasRegion(TrackRegion.RegionType.START)) {
            Text.send(player, Error.GENERIC);
            return false;
        }

        if ("CUSTOM".equals(carType)) {
            PlayerCar activeCar = GarageManager.getActiveCar(player.getUniqueId());
            if (activeCar == null) {
                carType = "SYSTEM";
            }
        }

        RaceSession session = new RaceSession(player.getUniqueId(), track, RaceType.SOLO, carType);
        activeSessions.put(player.getUniqueId(), session);

        hideOtherPlayers(player);
        teleportToStart(player, track);
        applyCarConfiguration(player, track, carType);
        applyTrackEnvironment(player, track);

        session.setState(RaceState.COUNTDOWN);
        startCountdown(player, session);

        return true;
    }

    // ─── MULTIPLAYER RACE ───

    public static boolean startMultiplayerRace(Player host, Track track, List<Player> players, int laps) {
        if (!isEnabled()) {
            Text.send(host, Error.RACE_NOT_ENABLED);
            return false;
        }

        if (!track.isOpen()) {
            Text.send(host, Error.TRACK_IS_CLOSED);
            return false;
        }

        if (!track.getTrackRegions().hasRegion(TrackRegion.RegionType.START)) {
            Text.send(host, Error.GENERIC);
            return false;
        }

        if (!track.getTrackLocations().hasLocation(TrackLocation.Type.GRID)) {
            Text.send(host, Error.GENERIC);
            return false;
        }

        for (Player p : players) {
            if (isInRace(p.getUniqueId())) {
                Text.send(host, Error.RACE_ALREADY_ACTIVE);
                return false;
            }
        }

        for (Player p : players) {
            RaceSession session = new RaceSession(p.getUniqueId(), track, RaceType.MULTIPLAYER, "SYSTEM");
            activeSessions.put(p.getUniqueId(), session);
        }

        return true;
    }

    // ─── COUNTDOWN ───

    private static void startCountdown(Player player, RaceSession session) {
        int countdownSeconds = getCountdownSeconds();

        // Record position for false start detection
        session.setCountdownLocation(player.getLocation().clone());

        // Freeze player during countdown
        freezePlayer(player);

        TaskChain<?> chain = TimingSystem.newChain();

        for (int i = countdownSeconds; i > 0; i--) {
            int count = i;
            chain.sync(() -> {
                if (!session.isActive()) return;
                Player p = Bukkit.getPlayer(session.getPlayerUuid());
                if (p == null) {
                    cancelRace(session.getPlayerUuid());
                    return;
                }

                // Color-coded countdown: red for 5-4, yellow for 3-2-1
                if (count >= 4) {
                    Text.send(p, Broadcast.RACE_COUNTDOWN_RED, "%count%", String.valueOf(count));
                } else {
                    Text.send(p, Broadcast.RACE_COUNTDOWN_YELLOW, "%count%", String.valueOf(count));
                }

                // Sound: short beep on each second
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);

                // False start detection during countdown
                if (isFalseStartEnabled()) {
                    checkFalseStart(p, session);
                }
            }).delay(20);
        }

        chain.execute((finished) -> {
            if (!session.isActive()) return;
            Player p = Bukkit.getPlayer(session.getPlayerUuid());
            if (p == null) {
                cancelRace(session.getPlayerUuid());
                return;
            }

            // Unfreeze player
            unfreezePlayer(p);

            // GO! — Green
            session.setState(RaceState.RACING);
            session.setStartTime(TimingSystem.currentTime);
            Text.send(p, Broadcast.RACE_GO);
            // Long beep on GO
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            Text.send(p, Success.RACE_STARTED);

            scheduleTimeout(session);
        });
    }

    // ─── FALSE START DETECTION ───

    /**
     * Checks if a player has moved during the countdown.
     * Called externally from TSListener on player move events.
     */
    public static void handleCountdownMovement(Player player) {
        Optional<RaceSession> maybeSession = getSession(player.getUniqueId());
        if (maybeSession.isEmpty()) return;

        RaceSession session = maybeSession.get();
        if (session.getState() != RaceState.COUNTDOWN) return;
        if (!isFalseStartEnabled()) return;

        checkFalseStart(player, session);
    }

    private static void checkFalseStart(Player player, RaceSession session) {
        Location startLoc = session.getCountdownLocation();
        if (startLoc == null) return;

        Location currentLoc = player.getLocation();
        double dx = currentLoc.getX() - startLoc.getX();
        double dz = currentLoc.getZ() - startLoc.getZ();
        double distSq = dx * dx + dz * dz;

        if (distSq > FALSE_START_MOVE_THRESHOLD * FALSE_START_MOVE_THRESHOLD) {
            FalseStartResult result = session.recordFalseStart();

            switch (result) {
                case PENALTY:
                    Text.send(player, Warning.FALSE_START_WARNING);
                    Text.send(player, Error.FALSE_START_PENALTY,
                            "%seconds%", String.valueOf(session.getFalseStartPenaltySeconds()));
                    player.teleport(startLoc);
                    session.setCountdownLocation(startLoc.clone());
                    break;

                case RESTART:
                    Text.send(player, Error.FALSE_START_RESTART);
                    player.teleport(startLoc);
                    session.setCountdownLocation(startLoc.clone());
                    session.setState(RaceState.COUNTDOWN);
                    startCountdown(player, session);
                    break;

                case DISQUALIFIED:
                    Text.send(player, Error.FALSE_START_DISQUALIFIED);
                    cancelRace(session.getPlayerUuid());
                    break;
            }
        }
    }

    // ─── PLAYER FREEZE ───

    private static void freezePlayer(Player player) {
        savedWalkSpeeds.put(player.getUniqueId(), player.getWalkSpeed());
        player.setWalkSpeed(0f);
    }

    private static void unfreezePlayer(Player player) {
        Float savedSpeed = savedWalkSpeeds.remove(player.getUniqueId());
        player.setWalkSpeed(savedSpeed != null ? savedSpeed : 0.2f);
    }

    // ─── TIME CONTROL ───

    /**
     * Handles a player entering a TIME_CONTROL region during a race.
     * Calculates penalty based on arrival time relative to target.
     */
    public static void handleTimeControl(Player player, TrackRegion region) {
        if (!isTimeControlEnabled()) return;

        Optional<RaceSession> maybeSession = getSession(player.getUniqueId());
        if (maybeSession.isEmpty()) return;

        RaceSession session = maybeSession.get();
        if (session.getState() != RaceState.RACING) return;

        if (session.hasPassedTimeControl(region.getId())) return;

        long elapsedMs = session.getElapsedMs();
        int elapsedSeconds = (int) (elapsedMs / 1000);

        int windowSeconds = getTimeControlWindowSeconds();
        int targetSeconds = (region.getRegionIndex() + 1) * windowSeconds;
        int lateSeconds = elapsedSeconds - (targetSeconds + windowSeconds);
        int earlySeconds = (targetSeconds - windowSeconds) - elapsedSeconds;

        int penaltySeconds = 0;

        if (lateSeconds > 0) {
            int minutesLate = Math.max(1, (lateSeconds + 59) / 60);
            penaltySeconds = minutesLate * getTimeControlLatePenaltyPerMinute();
            Text.send(player, Error.TIME_CONTROL_LATE,
                    "%penalty%", String.valueOf(penaltySeconds));
        } else if (earlySeconds > 0) {
            int minutesEarly = Math.max(1, (earlySeconds + 59) / 60);
            penaltySeconds = minutesEarly * getTimeControlEarlyPenaltyPerMinute();
            Text.send(player, Error.TIME_CONTROL_EARLY,
                    "%penalty%", String.valueOf(penaltySeconds));
        } else {
            Text.send(player, Info.TIME_CONTROL_PASSED);
        }

        session.recordTimeControlPass(region.getId(), penaltySeconds);

        if (penaltySeconds > 0) {
            Text.send(player, Info.TIME_CONTROL_PENALTY,
                    "%total%", String.valueOf(session.getTimeControlPenaltySeconds()));
        }
    }

    private static void scheduleTimeout(RaceSession session) {
        int timeoutTicks = getTimeoutMinutes() * 60 * 20;
        Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> {
            if (session.isActive()) {
                Player p = Bukkit.getPlayer(session.getPlayerUuid());
                if (p != null) {
                    Text.send(p, Error.RACE_TIMEOUT);
                }
                cancelRace(session.getPlayerUuid());
            }
        }, timeoutTicks);
    }

    // ─── RACE FINISH ───

    public static void finishSoloRace(UUID playerUuid) {
        RaceSession session = activeSessions.get(playerUuid);
        if (session == null || session.getState() != RaceState.RACING) return;

        session.setState(RaceState.FINISHED);
        session.setEndTime(TimingSystem.currentTime);

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            long timeMs = session.getElapsedMs();
            long totalPenaltyMs = session.getTotalPenaltyMs();
            long adjustedTimeMs = session.getAdjustedTimeMs();
            String timeFormatted = ApiUtilities.formatAsTime(adjustedTimeMs);

            Text.send(player, Success.RACE_SOLO_FINISH, "%time%", timeFormatted, "%track%", session.getTrack().getDisplayName());

            if (totalPenaltyMs > 0) {
                String penaltyFormatted = ApiUtilities.formatAsTime(totalPenaltyMs);
                String rawTimeFormatted = ApiUtilities.formatAsTime(timeMs);
                Text.send(player, Info.RACE_PENALTY_SUMMARY,
                        "%rawTime%", rawTimeFormatted,
                        "%penalty%", penaltyFormatted,
                        "%totalTime%", timeFormatted);
            }

            saveRaceResult(session, adjustedTimeMs);
            awardRaceRewards(player, session, adjustedTimeMs);
            showAllPlayers(player);
            resetTrackEnvironment(player);
        }

        activeSessions.remove(playerUuid);
    }

    // ─── RACE CANCEL ───

    public static boolean cancelRace(UUID playerUuid) {
        RaceSession session = activeSessions.remove(playerUuid);
        if (session == null) return false;

        session.setState(RaceState.CANCELLED);
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            unfreezePlayer(player);

            if (session.getRaceType() == RaceType.SOLO) {
                showAllPlayers(player);
            }
            resetTrackEnvironment(player);
            Text.send(player, Success.RACE_CANCELLED);
        }
        savedWalkSpeeds.remove(playerUuid);
        return true;
    }

    // ─── PLAYER VISIBILITY ───

    private static void hideOtherPlayers(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId())) {
                player.hideEntity(TimingSystem.getPlugin(), other);
                if (other.isInsideVehicle() && (other.getVehicle() instanceof Boat)) {
                    player.hideEntity(TimingSystem.getPlugin(), other.getVehicle());
                }
            }
        }
    }

    private static void showAllPlayers(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId())) {
                player.showEntity(TimingSystem.getPlugin(), other);
                if (other.isInsideVehicle() && (other.getVehicle() instanceof Boat)) {
                    player.showEntity(TimingSystem.getPlugin(), other.getVehicle());
                }
            }
        }
        LonelinessController.updatePlayersVisibility(player);
        LonelinessController.updatePlayerVisibility(player);
    }

    // ─── CAR CONFIGURATION ───

    private static void applyCarConfiguration(Player player, Track track, String carType) {
        if (track.getBoatUtilsMode() != null) {
            BoatUtilsManager.sendBoatUtilsModePluginMessage(player, track.getBoatUtilsMode(), track, false);
        }

        if ("CUSTOM".equals(carType)) {
            PlayerCar activeCar = GarageManager.getActiveCar(player.getUniqueId());
            if (activeCar != null) {
                Integer customModeId = track.getCustomBoatUtilsModeId();
                if (customModeId != null) {
                    CustomBoatUtilsMode mode = TimingSystem.getTrackDatabase().getCustomBoatUtilsModeFromId(customModeId);
                    if (mode != null) {
                        GarageManager.applyCarToMode(activeCar, mode);
                        mode.applyToPlayer(player);
                    }
                }
            }
        }
    }

    // ─── TELEPORT ───

    private static void teleportToStart(Player player, Track track) {
        List<TrackLocation> grids = track.getTrackLocations().getLocations(TrackLocation.Type.GRID);
        if (!grids.isEmpty()) {
            Location startLoc = grids.get(0).getLocation();
            if (startLoc != null) {
                player.teleport(startLoc);
            }
        }
    }

    // ─── TRACK ENVIRONMENT ───

    private static void applyTrackEnvironment(Player player, Track track) {
        if (track.getWeatherCondition() != TrackWeather.CLEAR) {
            CustomBoatUtilsMode.sendWeatherConditionPacket(player, (short) track.getWeatherCondition().getId());

            if (track.getWeatherCondition() == TrackWeather.RAIN
                    || track.getWeatherCondition() == TrackWeather.HEAVY_RAIN
                    || track.getWeatherCondition() == TrackWeather.SNOW) {
                player.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL);
            }
        }

        if (track.getTrackTime() != null) {
            player.setPlayerTime(track.getTrackTime(), false);
        }
    }

    private static void resetTrackEnvironment(Player player) {
        CustomBoatUtilsMode.sendWeatherConditionPacket(player, (short) TrackWeather.CLEAR.getId());
        player.resetPlayerWeather();
        player.resetPlayerTime();
    }

    // ─── RESULTS ───

    private static void saveRaceResult(RaceSession session, long timeMs) {
        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> {
            try {
                DB.executeInsert(
                        "INSERT INTO ts_race_results (uuid, track_id, time_ms, race_type, car_type, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        session.getPlayerUuid().toString(),
                        session.getTrack().getId(),
                        timeMs,
                        session.getRaceType().name(),
                        session.getCarType(),
                        ApiUtilities.getTimestamp()
                );
            } catch (SQLException e) {
                TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to save race result", e);
            }
        });
    }

    // ─── REWARDS ───

    private static void awardRaceRewards(Player player, RaceSession session, long timeMs) {
        boolean economyEnabled = TimingSystem.getPlugin().getConfig().getBoolean("economy.enabled", true);
        boolean levelsEnabled = TimingSystem.getPlugin().getConfig().getBoolean("levels.enabled", true);
        Track track = session.getTrack();

        if (economyEnabled) {
            int baseCoins = TimingSystem.getPlugin().getConfig().getInt("race.rewards.coins", RACE_COMPLETE_COINS);
            int coins = Math.round(baseCoins * track.getDifficultyCoinMultiplier());
            RallyCoinManager.addCoins(player.getUniqueId(), coins, "Race: " + track.getDisplayName());
            Text.send(player, Info.ECONOMY_COINS_REWARD, "%amount%", String.valueOf(coins));
        }

        if (levelsEnabled) {
            int baseXp = TimingSystem.getPlugin().getConfig().getInt("race.rewards.xp", RACE_COMPLETE_XP);
            int xp = Math.round(baseXp * track.getDifficultyXpMultiplier());
            LevelManager.addXP(player.getUniqueId(), xp, "Race: " + track.getDisplayName());
            Text.send(player, Info.ECONOMY_XP_REWARD, "%amount%", String.valueOf(xp));
        }
    }

    // ─── RESULTS QUERY ───

    public static List<DbRow> getTopResults(int trackId, String carType, int limit) {
        try {
            if (carType != null) {
                return DB.getResults(
                        "SELECT uuid, MIN(time_ms) as best_time FROM ts_race_results WHERE track_id = ? AND car_type = ? GROUP BY uuid ORDER BY best_time ASC LIMIT ?",
                        trackId, carType, limit
                );
            }
            return DB.getResults(
                    "SELECT uuid, MIN(time_ms) as best_time FROM ts_race_results WHERE track_id = ? GROUP BY uuid ORDER BY best_time ASC LIMIT ?",
                    trackId, limit
            );
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get race results", e);
            return Collections.emptyList();
        }
    }

    public static List<DbRow> getPlayerResults(UUID playerUuid, int trackId, int limit) {
        try {
            return DB.getResults(
                    "SELECT * FROM ts_race_results WHERE uuid = ? AND track_id = ? ORDER BY time_ms ASC LIMIT ?",
                    playerUuid.toString(), trackId, limit
            );
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get player race results", e);
            return Collections.emptyList();
        }
    }

    // ─── CLEANUP ───

    public static void onShutdown() {
        for (UUID uuid : new ArrayList<>(activeSessions.keySet())) {
            cancelRace(uuid);
        }
        savedWalkSpeeds.clear();
    }
}
