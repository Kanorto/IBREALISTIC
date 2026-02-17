package me.makkuusen.timing.system.boatutils;

import co.aikar.idb.DB;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.economy.GarageManager;
import me.makkuusen.timing.system.economy.LevelManager;
import me.makkuusen.timing.system.economy.PlayerCar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Server-side anti-cheat manager for realistic rally racing.
 * Detects speed hacks, teleport exploits, and abnormal acceleration
 * by monitoring vehicle movement on every tick cycle.
 * <p>
 * Runs a periodic task (every TICK_INTERVAL ticks) to check all active players.
 * Configuration is read from the plugin config under the "anticheat" section.
 */
public class AntiCheatManager {

    // ─── TICK CONFIGURATION ───
    private static final long TICK_INTERVAL = 10L;

    // ─── DEFAULT THRESHOLDS ───
    private static final float DEFAULT_SPEED_TOLERANCE = 1.2f;
    private static final int DEFAULT_MAX_VIOLATIONS = 3;
    private static final float MAX_ACCELERATION_PER_TICK = 0.5f;
    private static final double TELEPORT_DETECTION_DISTANCE = 20.0;

    // ─── MAX SPEED PER VEHICLE TYPE (blocks/tick) ───
    private static final float MAX_SPEED_WRC_CAR = 2.5f;
    private static final float MAX_SPEED_GROUP_B = 2.8f;
    private static final float MAX_SPEED_CLASSIC_RALLY = 2.0f;
    private static final float MAX_SPEED_LIGHTWEIGHT = 2.2f;
    private static final float MAX_SPEED_TRUCK = 1.8f;
    private static final float MAX_SPEED_DEFAULT = 3.0f;

    // ─── VIOLATION COOLDOWN ───
    private static final long VIOLATION_COOLDOWN_MS = 2000L;
    /** Cooldown after disqualification before violations count again */
    private static final long DISQUALIFICATION_COOLDOWN_MS = 30000L;

    // ─── TEMP BAN ───
    private static final int DEFAULT_BAN_MINUTES = 30;
    private static final float RECORD_ANOMALY_THRESHOLD_SECONDS = 20.0f;
    /** Multiplier above normal max speed that triggers post-race ban */
    private static final float POST_RACE_SPEED_MULTIPLIER = 1.5f;

    // ─── CAR VALIDATION ───
    private static final String[] CAR_COMPONENTS = {
            "type", "tire", "suspension",
            "engine", "body", "steering",
            "brake", "weightdistribution"
    };

    // ─── RUNTIME CONFIG (read from plugin config on start) ───
    private static boolean enabled = true;
    private static long checkIntervalTicks = TICK_INTERVAL;
    private static float speedTolerance = DEFAULT_SPEED_TOLERANCE;
    private static int maxViolations = DEFAULT_MAX_VIOLATIONS;
    private static boolean notifyAdmins = true;
    private static boolean logViolations = true;

    // ─── PLAYER STATE ───
    private static final Map<UUID, PlayerAntiCheatData> playerData = new ConcurrentHashMap<>();
    /** UUID → ban expiry timestamp (ms) */
    private static final Map<UUID, Long> tempBans = new ConcurrentHashMap<>();
    private static BukkitTask tickTask = null;

    // ─── INNER CLASS: Per-Player Anti-Cheat Data ───
    private static class PlayerAntiCheatData {
        int violations = 0;
        int totalViolations = 0;
        volatile boolean enabled = false;
        double prevSpeed = 0;
        Location lastValidLocation = null;
        Location lastSafeLocation = null;
        short vehicleType = 0;
        long lastViolationTime = 0L;
        long disqualifiedUntil = 0L;

        void reset() {
            violations = 0;
            // totalViolations is intentionally NOT reset — tracks across sessions
            prevSpeed = 0;
            lastValidLocation = null;
            lastSafeLocation = null;
            lastViolationTime = 0L;
            disqualifiedUntil = 0L;
        }
    }

    // ─── LIFECYCLE ───

    /**
     * Starts the anti-cheat manager periodic task.
     * Reads configuration values from the plugin config.
     * Called when the plugin enables.
     */
    public static void start() {
        if (tickTask != null) return;

        readConfig();

        if (!enabled) {
            TimingSystem.getPlugin().getLogger().info("AntiCheatManager is disabled in config.");
            return;
        }

        tickTask = Bukkit.getScheduler().runTaskTimer(
                TimingSystem.getPlugin(),
                AntiCheatManager::tick,
                checkIntervalTicks, checkIntervalTicks
        );
        TimingSystem.getPlugin().getLogger().info("AntiCheatManager started (interval: " + checkIntervalTicks + " ticks).");
    }

    /**
     * Stops the anti-cheat manager and clears all tracked data.
     * Called when the plugin disables.
     */
    public static void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        playerData.clear();
    }

    /**
     * Enables anti-cheat tracking for a player with the given vehicle type.
     * Called when a player enters a realistic mode.
     *
     * @param player      the player to track
     * @param vehicleType vehicle type index (0=WRC, 1=GROUP_B, 2=CLASSIC, 3=LIGHTWEIGHT, 4=TRUCK)
     */
    public static void enableForPlayer(Player player, short vehicleType) {
        if (!enabled) return;
        UUID uuid = player.getUniqueId();
        PlayerAntiCheatData data = playerData.computeIfAbsent(uuid, k -> new PlayerAntiCheatData());
        data.enabled = true;
        data.vehicleType = vehicleType;
        data.reset();
        data.lastValidLocation = player.getLocation().clone();
        data.lastSafeLocation = player.getLocation().clone();
    }

    /**
     * Disables anti-cheat tracking for a player.
     * Called when a player leaves realistic mode.
     */
    public static void disableForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerAntiCheatData data = playerData.get(uuid);
        if (data != null) {
            data.enabled = false;
            data.reset();
        }
    }

    /**
     * Removes all data for a player (on disconnect).
     */
    public static void removePlayer(UUID uuid) {
        playerData.remove(uuid);
    }

    // ─── CONFIGURATION ───

    private static void readConfig() {
        var config = TimingSystem.getPlugin().getConfig();
        enabled = config.getBoolean("anticheat.enabled", true);
        checkIntervalTicks = Math.max(1L, config.getLong("anticheat.check_interval_ticks", TICK_INTERVAL));
        speedTolerance = Math.max(1.0f, (float) config.getDouble("anticheat.speed_tolerance", DEFAULT_SPEED_TOLERANCE));
        maxViolations = Math.max(1, config.getInt("anticheat.max_violations", DEFAULT_MAX_VIOLATIONS));
        notifyAdmins = config.getBoolean("anticheat.notify_admins", true);
        logViolations = config.getBoolean("anticheat.log_violations", true);
    }

    // ─── MAIN TICK ───

    private static void tick() {
        for (Map.Entry<UUID, PlayerAntiCheatData> entry : playerData.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerAntiCheatData data = entry.getValue();
            if (!data.enabled) continue;

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;

            Entity vehicle = player.getVehicle();
            if (vehicle == null) {
                data.prevSpeed = 0;
                continue;
            }

            checkPlayer(player, vehicle, data);
        }
    }

    // ─── PLAYER CHECKS ───

    private static void checkPlayer(Player player, Entity vehicle, PlayerAntiCheatData data) {
        // Skip if in disqualification cooldown
        if (System.currentTimeMillis() < data.disqualifiedUntil) {
            return;
        }

        Vector velocity = vehicle.getVelocity();
        double currentSpeed = velocity.length();
        Location currentLocation = vehicle.getLocation();
        boolean violated = false;

        // ─── SPEED CHECK ───
        float maxSpeed = getMaxSpeed(data.vehicleType) * speedTolerance;
        if (currentSpeed > maxSpeed) {
            onViolation(player, data, "Speed: " + String.format("%.2f", currentSpeed)
                    + " > max " + String.format("%.2f", maxSpeed) + " blocks/tick");
            violated = true;
        }

        // ─── ACCELERATION CHECK ───
        // Normalize by interval: speed delta per tick
        // Skip on first check (prevSpeed == 0) to avoid false positives
        if (data.prevSpeed > 0) {
            double accelerationPerTick = (currentSpeed - data.prevSpeed) / checkIntervalTicks;
            if (accelerationPerTick > MAX_ACCELERATION_PER_TICK) {
                onViolation(player, data, "Acceleration: " + String.format("%.3f", accelerationPerTick)
                        + " > limit " + String.format("%.3f", (double) MAX_ACCELERATION_PER_TICK) + " blocks/tick²");
                violated = true;
            }
        }

        // ─── TELEPORT DETECTION ───
        if (data.lastValidLocation != null
                && currentLocation.getWorld() != null
                && data.lastValidLocation.getWorld() != null
                && currentLocation.getWorld().equals(data.lastValidLocation.getWorld())) {
            // Scale threshold by interval and max possible speed
            double maxTravelDistance = getMaxSpeed(data.vehicleType) * checkIntervalTicks * speedTolerance;
            double effectiveThreshold = Math.max(TELEPORT_DETECTION_DISTANCE, maxTravelDistance);
            double distance = currentLocation.distance(data.lastValidLocation);
            if (distance > effectiveThreshold) {
                String details = "Teleport: " + String.format("%.1f", distance)
                        + " blocks (threshold: " + String.format("%.1f", effectiveThreshold) + ")";
                TimingSystem.getPlugin().getLogger().warning(
                        "[AntiCheat] Suspicious teleport by " + player.getName() + ": " + details);
                if (notifyAdmins) {
                    notifyAdmins(player, details);
                }
                if (logViolations) {
                    logViolation(player.getUniqueId(), "TELEPORT", details);
                }
                // Don't auto-punish for teleports — may be legitimate server teleport
            }
        }

        // ─── UPDATE TRACKING STATE ───
        data.prevSpeed = currentSpeed;
        data.lastValidLocation = currentLocation.clone();
        if (!violated) {
            data.lastSafeLocation = currentLocation.clone();
        }
    }

    // ─── VIOLATION HANDLING ───

    private static void onViolation(Player player, PlayerAntiCheatData data, String reason) {
        long now = System.currentTimeMillis();
        if (now - data.lastViolationTime < VIOLATION_COOLDOWN_MS) {
            return;
        }
        data.lastViolationTime = now;
        data.violations++;
        data.totalViolations++;

        TimingSystem.getPlugin().getLogger().warning(
                "[AntiCheat] Violation #" + data.violations + " for " + player.getName() + ": " + reason);

        // Always notify admins and Discord on any suspicion
        if (notifyAdmins) {
            notifyAdmins(player, reason);
        }
        DiscordWebhookManager.sendSuspicionAlert(player.getName(), "Violation #" + data.violations, reason);

        if (data.violations == 1) {
            // Warning
            player.sendMessage(Component.text()
                    .append(Component.text("[AntiCheat] ", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text("Warning: suspicious vehicle behavior detected.", NamedTextColor.YELLOW))
                    .build());
        } else if (data.violations == 2) {
            // Teleport back to last safe (non-violating) location
            if (data.lastSafeLocation != null) {
                player.teleport(data.lastSafeLocation);
            }
            player.sendMessage(Component.text()
                    .append(Component.text("[AntiCheat] ", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text("Violation detected. You have been moved to your last valid position.", NamedTextColor.RED))
                    .build());
        } else if (data.violations >= maxViolations) {
            // Temp ban: kick player
            int banMinutes = TimingSystem.getPlugin().getConfig().getInt(
                    "anticheat.temp_ban_minutes", DEFAULT_BAN_MINUTES);
            tempBan(player, banMinutes, reason);
            data.disqualifiedUntil = now + DISQUALIFICATION_COOLDOWN_MS;
            data.violations = 0;
        }

        if (logViolations) {
            logViolation(player.getUniqueId(), "VIOLATION", reason);
        }
    }

    // ─── ADMIN NOTIFICATION ───

    private static void notifyAdmins(Player suspect, String details) {
        Component message = Component.text()
                .append(Component.text("[AC] ", NamedTextColor.GOLD))
                .append(Component.text(suspect.getName(), NamedTextColor.WHITE))
                .append(Component.text(": " + details, NamedTextColor.GRAY))
                .build();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("timingsystem.admin")) {
                online.sendMessage(message);
            }
        }
    }

    // ─── DATABASE LOGGING ───

    /**
     * Logs a violation to the ts_anticheat_violations table.
     * SQL errors are caught and logged without propagation.
     */
    private static void logViolation(UUID uuid, String type, String details) {
        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> {
            try {
                DB.executeUpdate(
                        "INSERT INTO ts_anticheat_violations (uuid, type, details, timestamp) VALUES (?, ?, ?, ?)",
                        uuid.toString(), type, details, System.currentTimeMillis()
                );
            } catch (SQLException e) {
                TimingSystem.getPlugin().getLogger().log(Level.WARNING,
                        "Failed to log anticheat violation for " + uuid, e);
            }
        });
    }

    // ─── CAR VALIDATION ───

    /**
     * Validates a player's active car configuration.
     * Checks that all presets meet level requirements and that balance is non-negative.
     * Called when a player enters realistic mode.
     *
     * @param player the player to validate
     * @return true if the car is valid, false if it was reset
     */
    public static boolean validatePlayerCar(Player player) {
        if (!GarageManager.isEnabled()) return true;

        UUID uuid = player.getUniqueId();
        PlayerCar car = GarageManager.getActiveCar(uuid);
        if (car == null) return true;

        int playerLevel = LevelManager.isEnabled() ? LevelManager.getLevel(uuid) : Integer.MAX_VALUE;
        boolean valid = true;

        for (String component : CAR_COMPONENTS) {
            short presetId = GarageManager.getCurrentPreset(car, component);
            if (presetId < 0) {
                // Unknown component mapping — shouldn't happen with correct CAR_COMPONENTS
                continue;
            }
            int requiredLevel = GarageManager.getPresetLevel(component, presetId);
            if (requiredLevel < 0) {
                // Preset ID is out of range — invalid/hacked preset
                TimingSystem.getPlugin().getLogger().warning(
                        "[AntiCheat] Player " + player.getName() + " has invalid " + component
                                + " preset ID " + presetId + " (out of range). Resetting to 0.");
                GarageManager.upgradeComponent(uuid, car.getId(), component, (short) 0);
                valid = false;
            } else if (requiredLevel > playerLevel) {
                TimingSystem.getPlugin().getLogger().warning(
                        "[AntiCheat] Player " + player.getName() + " has " + component
                                + " preset " + presetId + " requiring level " + requiredLevel
                                + " but is only level " + playerLevel + ". Resetting to 0.");
                GarageManager.upgradeComponent(uuid, car.getId(), component, (short) 0);
                valid = false;
            }
        }

        if (!valid) {
            player.sendMessage(Component.text()
                    .append(Component.text("[AntiCheat] ", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text("Your car configuration contains invalid presets. Resetting to defaults.", NamedTextColor.YELLOW))
                    .build());
            if (logViolations) {
                logViolation(uuid, "CAR_VALIDATION", "Invalid presets reset to defaults");
            }
        }

        return valid;
    }

    // ─── SPEED LIMITS ───

    /**
     * Returns the maximum allowed speed (blocks/tick) for the given vehicle type.
     *
     * @param vehicleType vehicle type index (0=WRC, 1=GROUP_B, 2=CLASSIC, 3=LIGHTWEIGHT, 4=TRUCK)
     * @return max speed in blocks/tick
     */
    private static float getMaxSpeed(short vehicleType) {
        switch (vehicleType) {
            case 0: return MAX_SPEED_WRC_CAR;
            case 1: return MAX_SPEED_GROUP_B;
            case 2: return MAX_SPEED_CLASSIC_RALLY;
            case 3: return MAX_SPEED_LIGHTWEIGHT;
            case 4: return MAX_SPEED_TRUCK;
            default: return MAX_SPEED_DEFAULT;
        }
    }

    // ─── PUBLIC GETTERS FOR COMMANDS ───

    /**
     * Returns the current violation count for a player.
     */
    public static int getViolations(UUID uuid) {
        PlayerAntiCheatData data = playerData.get(uuid);
        return data != null ? data.violations : 0;
    }

    /**
     * Returns the total session violation count for a player.
     */
    public static int getTotalViolations(UUID uuid) {
        PlayerAntiCheatData data = playerData.get(uuid);
        return data != null ? data.totalViolations : 0;
    }

    /**
     * Checks if anti-cheat is currently enabled for a player.
     */
    public static boolean isEnabledForPlayer(UUID uuid) {
        PlayerAntiCheatData data = playerData.get(uuid);
        return data != null && data.enabled;
    }

    /**
     * Resets the violation counter for a player (admin command).
     */
    public static void resetViolations(UUID uuid) {
        PlayerAntiCheatData data = playerData.get(uuid);
        if (data != null) {
            data.violations = 0;
        }
    }

    // ─── TEMP BAN ───

    /**
     * Temporarily ban a player. Kicks them and records in DB.
     */
    public static void tempBan(Player player, int minutes, String reason) {
        UUID uuid = player.getUniqueId();
        long expiresAt = System.currentTimeMillis() + (long) minutes * 60 * 1000;
        tempBans.put(uuid, expiresAt);

        // Store in DB
        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> {
            try {
                DB.executeUpdate(
                        "INSERT INTO ts_anticheat_violations (uuid, type, details, timestamp) VALUES (?, ?, ?, ?)",
                        uuid.toString(), "TEMP_BAN",
                        "Duration: " + minutes + "min, Reason: " + reason,
                        System.currentTimeMillis()
                );
            } catch (SQLException e) {
                TimingSystem.getPlugin().getLogger().log(Level.WARNING,
                        "Failed to log temp ban for " + uuid, e);
            }
        });

        // Discord notification
        DiscordWebhookManager.sendBanAlert(player.getName(), reason,
                "Violations: " + getTotalViolations(uuid), minutes);

        // Notify admins
        notifyAdmins(player, "TEMP BANNED (" + minutes + " min): " + reason);

        // Kick
        Bukkit.getScheduler().runTask(TimingSystem.getPlugin(), () ->
                player.kick(Component.text()
                        .append(Component.text("[TimingSystem AntiCheat]\n", NamedTextColor.RED, TextDecoration.BOLD))
                        .append(Component.text("You have been temporarily banned for " + minutes + " minutes.\n", NamedTextColor.YELLOW))
                        .append(Component.text("Reason: " + reason, NamedTextColor.GRAY))
                        .build())
        );
    }

    /**
     * Check if a player is currently temp-banned.
     * @return remaining ban time in ms, or 0 if not banned
     */
    public static long getTempBanRemaining(UUID uuid) {
        Long expiresAt = tempBans.get(uuid);
        if (expiresAt == null) return 0;
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0) {
            tempBans.remove(uuid);
            return 0;
        }
        return remaining;
    }

    /**
     * Check if a player is temp-banned. Call on join to reject.
     */
    public static boolean isTempBanned(UUID uuid) {
        return getTempBanRemaining(uuid) > 0;
    }

    // ─── POST-RACE CHECKS ───

    /**
     * Check if a race finish time is anomalous (world record by suspicious margin).
     * Called after a race finishes.
     *
     * @param player          the player who finished
     * @param trackName       track display name
     * @param finishTimeMs    the player's finish time in milliseconds
     * @param previousBestMs  the previous best time on this track (0 if no record)
     * @param totalParticipants number of participants in the race
     * @return true if anomaly detected (player was banned)
     */
    public static boolean checkRecordAnomaly(Player player, String trackName,
                                              long finishTimeMs, long previousBestMs,
                                              int totalParticipants) {
        if (!enabled || previousBestMs <= 0) return false;

        long marginMs = previousBestMs - finishTimeMs;
        float marginSeconds = marginMs / 1000.0f;

        float threshold = (float) TimingSystem.getPlugin().getConfig().getDouble(
                "anticheat.record_anomaly_threshold_seconds", RECORD_ANOMALY_THRESHOLD_SECONDS);

        // Only flag if significant margin AND enough participants to be suspicious
        if (marginSeconds >= threshold && totalParticipants >= 3) {
            String newTimeStr = formatTime(finishTimeMs);
            String prevTimeStr = formatTime(previousBestMs);

            String reason = "World record anomaly: " + newTimeStr
                    + " vs previous " + prevTimeStr
                    + " (margin: " + String.format("%.1f", marginSeconds) + "s"
                    + ", participants: " + totalParticipants + ")";

            TimingSystem.getPlugin().getLogger().warning("[AntiCheat] " + reason + " by " + player.getName());

            // Discord alert
            DiscordWebhookManager.sendRecordAnomalyAlert(player.getName(), trackName,
                    newTimeStr, prevTimeStr, marginSeconds, totalParticipants);

            // Admin notification
            notifyAdmins(player, reason);

            // Log violation
            logViolation(player.getUniqueId(), "RECORD_ANOMALY", reason);

            // Temp ban
            int banMinutes = TimingSystem.getPlugin().getConfig().getInt(
                    "anticheat.record_anomaly_ban_minutes", DEFAULT_BAN_MINUTES);
            tempBan(player, banMinutes, reason);
            return true;
        }

        // Still alert for smaller margins (suspicion, no ban)
        if (marginSeconds >= threshold / 2) {
            String details = "New record on " + trackName + ": "
                    + formatTime(finishTimeMs) + " (prev: " + formatTime(previousBestMs)
                    + ", margin: " + String.format("%.1f", marginSeconds) + "s)";
            DiscordWebhookManager.sendSuspicionAlert(player.getName(), "Unusual record margin", details);
            notifyAdmins(player, "Suspicious record: " + details);
        }

        return false;
    }

    /**
     * Check if a player's max speed during a race was too high.
     * Called after a race finishes.
     */
    public static boolean checkPostRaceSpeed(Player player, float maxSpeedReached, short vehicleType) {
        if (!enabled) return false;

        float absoluteMax = getMaxSpeed(vehicleType) * speedTolerance * POST_RACE_SPEED_MULTIPLIER;
        if (maxSpeedReached > absoluteMax) {
            String reason = "Post-race speed check: max speed " + String.format("%.2f", maxSpeedReached)
                    + " exceeded absolute limit " + String.format("%.2f", absoluteMax);

            DiscordWebhookManager.sendSuspicionAlert(player.getName(), "Excessive speed during race", reason);
            notifyAdmins(player, reason);
            logViolation(player.getUniqueId(), "POST_RACE_SPEED", reason);

            int banMinutes = TimingSystem.getPlugin().getConfig().getInt(
                    "anticheat.temp_ban_minutes", DEFAULT_BAN_MINUTES);
            tempBan(player, banMinutes, reason);
            return true;
        }
        return false;
    }

    private static String formatTime(long ms) {
        long secs = ms / 1000;
        long millis = ms % 1000;
        long mins = secs / 60;
        secs %= 60;
        return String.format("%d:%02d.%03d", mins, secs, millis);
    }
}
