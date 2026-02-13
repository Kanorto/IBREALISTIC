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
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.TrackWeather;
import me.makkuusen.timing.system.track.locations.TrackLocation;
import me.makkuusen.timing.system.track.regions.TrackRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages race sessions — both solo (isolated) and multiplayer.
 * Solo races use player hiding + NOCOL for isolation.
 * Multiplayer races use the existing QuickRace/Heat system as a foundation.
 */
public class SoloRaceManager {

    // ─── CONFIGURATION DEFAULTS ───
    private static final int DEFAULT_MAX_CONCURRENT = 20;
    private static final int DEFAULT_COUNTDOWN_SECONDS = 5;
    private static final int DEFAULT_TIMEOUT_MINUTES = 10;
    private static final int RACE_COMPLETE_COINS = 30;
    private static final int RACE_COMPLETE_XP = 40;

    // ─── ACTIVE SESSIONS ───
    private static final Map<UUID, RaceSession> activeSessions = new ConcurrentHashMap<>();

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

    // ─── SESSION MANAGEMENT ───

    /**
     * Gets the active race session for a player, if any.
     */
    public static Optional<RaceSession> getSession(UUID playerUuid) {
        return Optional.ofNullable(activeSessions.get(playerUuid));
    }

    /**
     * Returns true if the player is currently in a race session.
     */
    public static boolean isInRace(UUID playerUuid) {
        return activeSessions.containsKey(playerUuid);
    }

    /**
     * Returns the number of currently active race sessions.
     */
    public static int getActiveSessionCount() {
        return (int) activeSessions.values().stream()
                .filter(RaceSession::isActive)
                .count();
    }

    // ─── SOLO RACE START ───

    /**
     * Starts a solo race for a player on the given track.
     * Solo races isolate the player — hiding other players and disabling collisions.
     *
     * @param player  the player starting the race
     * @param track   the track to race on
     * @param carType "SYSTEM" or "CUSTOM"
     * @return true if the race started successfully
     */
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

        // Validate custom car if selected
        if ("CUSTOM".equals(carType)) {
            PlayerCar activeCar = GarageManager.getActiveCar(player.getUniqueId());
            if (activeCar == null) {
                carType = "SYSTEM"; // Fall back to system car
            }
        }

        // Create session
        RaceSession session = new RaceSession(player.getUniqueId(), track, RaceType.SOLO, carType);
        activeSessions.put(player.getUniqueId(), session);

        // Hide other players (solo isolation)
        hideOtherPlayers(player);

        // Teleport to start position
        teleportToStart(player, track);

        // Apply car configuration
        applyCarConfiguration(player, track, carType);

        // Apply track weather and time settings
        applyTrackEnvironment(player, track);

        // Start countdown
        session.setState(RaceState.COUNTDOWN);
        startCountdown(player, session);

        return true;
    }

    // ─── MULTIPLAYER RACE ───

    /**
     * Starts a multiplayer race for a group of players on the given track.
     * Uses the existing QuickRace/Heat system under the hood but
     * wraps it with RaceSession tracking for the race subsystem.
     *
     * @param host    the host player
     * @param track   the track to race on
     * @param players the players joining the race
     * @param laps    number of laps
     * @return true if the race was created successfully
     */
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

        // Check if any player is already in a race
        for (Player p : players) {
            if (isInRace(p.getUniqueId())) {
                Text.send(host, Error.RACE_ALREADY_ACTIVE);
                return false;
            }
        }

        // Create sessions for all participants
        for (Player p : players) {
            RaceSession session = new RaceSession(p.getUniqueId(), track, RaceType.MULTIPLAYER, "SYSTEM");
            activeSessions.put(p.getUniqueId(), session);
        }

        return true;
    }

    // ─── COUNTDOWN ───

    private static void startCountdown(Player player, RaceSession session) {
        int countdownSeconds = getCountdownSeconds();
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
                // Title display
                p.showTitle(Title.title(
                        Component.text("§e§l" + count),
                        Component.text("§7Get ready..."),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ofMillis(200))
                ));
                // Sound
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
            }).delay(20);
        }

        chain.execute((finished) -> {
            if (!session.isActive()) return;
            Player p = Bukkit.getPlayer(session.getPlayerUuid());
            if (p == null) {
                cancelRace(session.getPlayerUuid());
                return;
            }

            // GO!
            session.setState(RaceState.RACING);
            session.setStartTime(TimingSystem.currentTime);
            p.showTitle(Title.title(
                    Component.text("§a§lGO!"),
                    Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(400))
            ));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            Text.send(p, Success.RACE_STARTED);

            // Schedule timeout
            scheduleTimeout(session);
        });
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

    /**
     * Finishes a solo race session. Called when the player crosses the finish line.
     */
    public static void finishSoloRace(UUID playerUuid) {
        RaceSession session = activeSessions.get(playerUuid);
        if (session == null || session.getState() != RaceState.RACING) return;

        session.setState(RaceState.FINISHED);
        session.setEndTime(TimingSystem.currentTime);

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            long timeMs = session.getElapsedMs();
            String timeFormatted = ApiUtilities.formatAsTime(timeMs);

            // Show finish title
            player.showTitle(Title.title(
                    Component.text("§a§l" + timeFormatted),
                    Component.text("§7Race finished!"),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));

            Text.send(player, Success.RACE_SOLO_FINISH, "%time%", timeFormatted, "%track%", session.getTrack().getDisplayName());

            // Save result
            saveRaceResult(session, timeMs);

            // Award rewards
            awardRaceRewards(player, session, timeMs);

            // Restore visibility
            showAllPlayers(player);

            // Reset track environment (weather/time)
            resetTrackEnvironment(player);
        }

        activeSessions.remove(playerUuid);
    }

    // ─── RACE CANCEL ───

    /**
     * Cancels a race session for a player.
     */
    public static boolean cancelRace(UUID playerUuid) {
        RaceSession session = activeSessions.remove(playerUuid);
        if (session == null) return false;

        session.setState(RaceState.CANCELLED);
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            if (session.getRaceType() == RaceType.SOLO) {
                showAllPlayers(player);
            }
            resetTrackEnvironment(player);
            Text.send(player, Success.RACE_CANCELLED);
        }
        return true;
    }

    // ─── PLAYER VISIBILITY ───

    private static void hideOtherPlayers(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId())) {
                player.hideEntity(TimingSystem.getPlugin(), other);
                // Also hide their boat
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
        // Let the normal visibility controller take over
        LonelinessController.updatePlayersVisibility(player);
        LonelinessController.updatePlayerVisibility(player);
    }

    // ─── CAR CONFIGURATION ───

    private static void applyCarConfiguration(Player player, Track track, String carType) {
        // Apply the track's boat utils mode
        if (track.getBoatUtilsMode() != null) {
            BoatUtilsManager.sendBoatUtilsModePluginMessage(player, track.getBoatUtilsMode(), track, false);
        }

        // For custom cars, apply garage presets via custom mode
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

    /**
     * Applies track weather and time-of-day settings to the player.
     */
    private static void applyTrackEnvironment(Player player, Track track) {
        // Apply weather condition via BoatUtils packet
        if (track.getWeatherCondition() != TrackWeather.CLEAR) {
            CustomBoatUtilsMode.sendWeatherConditionPacket(player, (short) track.getWeatherCondition().getId());

            // Set client-side visual weather
            if (track.getWeatherCondition() == TrackWeather.RAIN
                    || track.getWeatherCondition() == TrackWeather.HEAVY_RAIN
                    || track.getWeatherCondition() == TrackWeather.SNOW) {
                player.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL);
            }
        }

        // Apply time of day
        if (track.getTrackTime() != null) {
            player.setPlayerTime(track.getTrackTime(), false);
        }
    }

    /**
     * Resets track environment (weather/time) back to server defaults.
     */
    private static void resetTrackEnvironment(Player player) {
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

    /**
     * Gets the top N results for a track, optionally filtered by car type.
     */
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

    /**
     * Gets a player's results for a specific track.
     */
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

    /**
     * Cleans up all active sessions (called on plugin shutdown).
     */
    public static void onShutdown() {
        for (UUID uuid : new ArrayList<>(activeSessions.keySet())) {
            cancelRace(uuid);
        }
    }
}
