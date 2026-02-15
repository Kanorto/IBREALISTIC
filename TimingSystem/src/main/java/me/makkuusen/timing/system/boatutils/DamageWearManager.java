package me.makkuusen.timing.system.boatutils;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.tplayer.TPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Server-side damage and wear manager.
 * Calculates tire wear, engine temperature, and body damage for each player,
 * then syncs the authoritative state to the client via packets.
 * <p>
 * Runs a periodic task (every TICK_INTERVAL ticks) to update all active players.
 * All sensitive data is calculated server-side to prevent cheating.
 */
public class DamageWearManager {

    // ─── TICK CONFIGURATION ───
    /** How often to update damage/wear (ticks). 5 ticks = 4 times/second */
    private static final long TICK_INTERVAL = 5L;
    /** How often to sync full state to client (ticks). 20 ticks = once/second */
    private static final long SYNC_INTERVAL = 20L;

    // ─── PACKET IDS (must match ClientboundPackets enum ordinals) ───
    private static final short PACKET_ID_SET_DAMAGE_ENABLED = 70;
    private static final short PACKET_ID_SYNC_TIRE_WEAR = 71;
    private static final short PACKET_ID_SYNC_ENGINE_TEMP = 72;
    private static final short PACKET_ID_SYNC_BODY_DAMAGE = 73;
    private static final short PACKET_ID_SET_SERVICE_ZONE = 74;
    private static final short PACKET_ID_DAMAGE_NOTIFICATION = 76;

    // ─── TIRE WEAR RATES ───
    /** Base tire wear per tick at 1 m/s on asphalt */
    private static final float BASE_TIRE_WEAR_RATE = 0.00002f;
    /** Multiplier for gravel/dirt surfaces */
    private static final float ROUGH_SURFACE_WEAR_MULTIPLIER = 2.5f;
    /** Multiplier for mud/sand surfaces */
    private static final float SOFT_SURFACE_WEAR_MULTIPLIER = 1.8f;
    /** Extra wear from aggressive steering (yaw rate above threshold) */
    private static final float STEERING_WEAR_MULTIPLIER = 3.0f;
    /** Yaw rate threshold for aggressive steering (deg/tick) */
    private static final float AGGRESSIVE_STEERING_THRESHOLD = 2.0f;
    /** Extra wear from wheel lockup (braking at speed) */
    private static final float LOCKUP_WEAR_MULTIPLIER = 2.0f;

    // ─── ENGINE TEMPERATURE ───
    /** Heating rate per tick at full throttle (detected by high speed) */
    private static final float ENGINE_HEAT_RATE = 0.0008f;
    /** Cooling rate per tick when not at full speed */
    private static final float ENGINE_COOL_RATE = 0.0012f;
    /** Speed threshold above which engine heats up (m/s, approx) */
    private static final float ENGINE_HEAT_SPEED_THRESHOLD = 0.3f;
    /** Minimum speed below which engine cools faster */
    private static final float ENGINE_FAST_COOL_THRESHOLD = 0.1f;
    /** Fast cooling multiplier when nearly stopped */
    private static final float ENGINE_FAST_COOL_MULTIPLIER = 2.0f;

    // ─── BODY DAMAGE ───
    /** Speed loss fraction that counts as a collision */
    private static final float COLLISION_THRESHOLD = 0.25f;
    /** Damage per unit of collision impact (normalized) */
    private static final float COLLISION_DAMAGE_FACTOR = 0.15f;
    /** Maximum damage from a single collision */
    private static final float MAX_SINGLE_COLLISION_DAMAGE = 0.3f;
    /** Minimum speed for collision detection (blocks/tick) */
    private static final float MIN_COLLISION_SPEED = 0.15f;

    // ─── SERVICE ZONE ───
    /** Repair rate per tick while in service zone */
    private static final float REPAIR_RATE_PER_TICK = 0.004f;

    // ─── NOTIFICATION THRESHOLDS ───
    private static final float TIRE_WARNING_THRESHOLD = 0.7f;
    private static final float ENGINE_WARNING_THRESHOLD = 0.8f;
    private static final float DAMAGE_WARNING_THRESHOLD = 0.6f;

    // ─── NOTIFICATION COLORS ───
    private static final int COLOR_WARNING = 0xFFFFFF55;
    private static final int COLOR_DANGER = 0xFFFF5555;
    private static final int COLOR_SERVICE = 0xFF55FFFF;
    private static final long NOTIFICATION_DURATION_MS = 3000;

    // ─── PLAYER STATE ───
    private static final Map<UUID, PlayerDamageData> playerData = new ConcurrentHashMap<>();
    private static BukkitTask tickTask = null;
    private static long tickCounter = 0;

    // ─── INNER CLASS: Per-Player Damage Data ───
    private static class PlayerDamageData {
        float[] tireWear = new float[4];
        float engineTemp = 0f;
        float bodyDamage = 0f;
        boolean inServiceZone = false;
        float repairProgress = 0f;
        boolean enabled = false;

        // Previous tick tracking for collision detection
        double prevSpeed = 0;
        float prevYaw = 0f;

        // Notification cooldowns (prevent spam)
        long lastTireWarningMs = 0;
        long lastEngineWarningMs = 0;
        long lastDamageWarningMs = 0;
        boolean wasInServiceZone = false;

        void reset() {
            tireWear = new float[4];
            engineTemp = 0f;
            bodyDamage = 0f;
            inServiceZone = false;
            repairProgress = 0f;
            prevSpeed = 0;
            prevYaw = 0f;
            lastTireWarningMs = 0;
            lastEngineWarningMs = 0;
            lastDamageWarningMs = 0;
            wasInServiceZone = false;
        }
    }

    // ─── LIFECYCLE ───

    /**
     * Starts the damage/wear manager periodic task.
     * Called when the plugin enables.
     */
    public static void start() {
        if (tickTask != null) return;
        tickTask = Bukkit.getScheduler().runTaskTimer(
                TimingSystem.getPlugin(),
                DamageWearManager::tick,
                TICK_INTERVAL, TICK_INTERVAL
        );
    }

    /**
     * Stops the damage/wear manager.
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
     * Enables damage tracking for a player.
     * Called when the player enters a realistic mode with damage enabled.
     */
    public static void enableForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerDamageData data = playerData.computeIfAbsent(uuid, k -> new PlayerDamageData());
        data.enabled = true;
        data.reset();
        sendDamageEnabled(player, true);
    }

    /**
     * Disables damage tracking for a player.
     * Called when the player leaves realistic mode or disconnects.
     */
    public static void disableForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerDamageData data = playerData.get(uuid);
        if (data != null) {
            data.enabled = false;
            data.reset();
        }
        sendDamageEnabled(player, false);
    }

    /**
     * Removes all data for a player (on disconnect).
     */
    public static void removePlayer(UUID uuid) {
        playerData.remove(uuid);
    }

    /**
     * Checks if damage system is enabled for a player.
     */
    public static boolean isEnabledForPlayer(UUID uuid) {
        PlayerDamageData data = playerData.get(uuid);
        return data != null && data.enabled;
    }

    /**
     * Sets the service zone state for a player.
     * Call from region enter/exit events.
     */
    public static void setServiceZone(Player player, boolean inZone) {
        UUID uuid = player.getUniqueId();
        PlayerDamageData data = playerData.get(uuid);
        if (data == null || !data.enabled) return;
        data.inServiceZone = inZone;
        if (!inZone) {
            data.repairProgress = 0f;
        }
    }

    // ─── MAIN TICK ───

    private static void tick() {
        tickCounter++;
        boolean syncTick = (tickCounter % (SYNC_INTERVAL / TICK_INTERVAL)) == 0;

        for (Map.Entry<UUID, PlayerDamageData> entry : playerData.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerDamageData data = entry.getValue();
            if (!data.enabled) continue;

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;

            Entity vehicle = player.getVehicle();
            if (vehicle == null) continue;

            updatePlayerDamage(player, vehicle, data);

            if (syncTick) {
                syncToClient(player, data);
            }
        }
    }

    // ─── UPDATE LOGIC ───

    private static void updatePlayerDamage(Player player, Entity vehicle, PlayerDamageData data) {
        Location loc = vehicle.getLocation();
        Vector velocity = vehicle.getVelocity();
        double speed = velocity.length();
        float yaw = loc.getYaw();

        float dtFactor = TICK_INTERVAL; // scale by tick interval

        // ─── TIRE WEAR ───
        if (speed > 0.02) {
            float surfaceMultiplier = getSurfaceWearMultiplier(loc);

            // Yaw rate (steering aggressiveness)
            float yawDelta = Math.abs(yaw - data.prevYaw);
            if (yawDelta > 180f) yawDelta = 360f - yawDelta;
            float steeringMultiplier = yawDelta > AGGRESSIVE_STEERING_THRESHOLD
                    ? STEERING_WEAR_MULTIPLIER : 1.0f;

            float wearIncrement = BASE_TIRE_WEAR_RATE * (float) speed * surfaceMultiplier
                    * steeringMultiplier * dtFactor;

            // Apply wear to all 4 tires (front tires wear faster during steering)
            boolean steering = yawDelta > 1.0f;
            for (int i = 0; i < 4; i++) {
                float wheelMultiplier = (steering && i < 2) ? 1.3f : 1.0f;
                data.tireWear[i] = Math.min(1.0f, data.tireWear[i] + wearIncrement * wheelMultiplier);
            }
        }

        // ─── ENGINE TEMPERATURE ───
        if (speed > ENGINE_HEAT_SPEED_THRESHOLD) {
            // Heating: proportional to speed
            float heatRate = ENGINE_HEAT_RATE * (float) (speed / 0.5) * dtFactor;
            data.engineTemp = Math.min(1.0f, data.engineTemp + heatRate);
        } else {
            // Cooling
            float coolRate = ENGINE_COOL_RATE * dtFactor;
            if (speed < ENGINE_FAST_COOL_THRESHOLD) {
                coolRate *= ENGINE_FAST_COOL_MULTIPLIER;
            }
            data.engineTemp = Math.max(0f, data.engineTemp - coolRate);
        }

        // ─── BODY DAMAGE (collision detection) ───
        if (data.prevSpeed > MIN_COLLISION_SPEED) {
            double speedLoss = data.prevSpeed - speed;
            double lossRatio = speedLoss / data.prevSpeed;
            if (lossRatio > COLLISION_THRESHOLD) {
                float impact = (float) Math.min(MAX_SINGLE_COLLISION_DAMAGE,
                        lossRatio * COLLISION_DAMAGE_FACTOR * data.prevSpeed * 5.0);
                data.bodyDamage = Math.min(1.0f, data.bodyDamage + impact);
            }
        }

        // ─── SERVICE ZONE REPAIR ───
        if (data.inServiceZone) {
            float totalDamage = data.bodyDamage + data.engineTemp + averageTireWear(data.tireWear);
            if (totalDamage > 0.01f) {
                data.repairProgress = Math.min(1.0f, data.repairProgress + REPAIR_RATE_PER_TICK * dtFactor);

                // Apply repair proportionally
                float repairFraction = REPAIR_RATE_PER_TICK * dtFactor;
                for (int i = 0; i < 4; i++) {
                    data.tireWear[i] = Math.max(0f, data.tireWear[i] - repairFraction);
                }
                data.engineTemp = Math.max(0f, data.engineTemp - repairFraction);
                data.bodyDamage = Math.max(0f, data.bodyDamage - repairFraction);

                // Check if fully repaired
                if (averageTireWear(data.tireWear) < 0.01f && data.engineTemp < 0.01f && data.bodyDamage < 0.01f) {
                    data.repairProgress = 1.0f;
                }
            }

            // Notify on service zone entry
            if (!data.wasInServiceZone) {
                sendNotification(player, "Entering service zone...", COLOR_SERVICE, NOTIFICATION_DURATION_MS);
            }
        }
        data.wasInServiceZone = data.inServiceZone;

        // ─── NOTIFICATIONS ───
        long now = System.currentTimeMillis();
        long notifCooldown = 10000; // 10 seconds between repeated warnings

        if (averageTireWear(data.tireWear) > TIRE_WARNING_THRESHOLD
                && now - data.lastTireWarningMs > notifCooldown) {
            int pct = (int) (averageTireWear(data.tireWear) * 100);
            sendNotification(player, "Tires worn " + pct + "%!", COLOR_WARNING, NOTIFICATION_DURATION_MS);
            data.lastTireWarningMs = now;
        }

        if (data.engineTemp > ENGINE_WARNING_THRESHOLD
                && now - data.lastEngineWarningMs > notifCooldown) {
            sendNotification(player, "Engine overheating!", COLOR_DANGER, NOTIFICATION_DURATION_MS);
            data.lastEngineWarningMs = now;
        }

        if (data.bodyDamage > DAMAGE_WARNING_THRESHOLD
                && now - data.lastDamageWarningMs > notifCooldown) {
            int pct = (int) (data.bodyDamage * 100);
            sendNotification(player, "Body damage " + pct + "%!", COLOR_DANGER, NOTIFICATION_DURATION_MS);
            data.lastDamageWarningMs = now;
        }

        // Store for next tick
        data.prevSpeed = speed;
        data.prevYaw = yaw;
    }

    // ─── SURFACE DETECTION ───

    /**
     * Returns the tire wear multiplier based on the block beneath the vehicle.
     * Rough surfaces (gravel, cobblestone) = higher wear.
     */
    private static float getSurfaceWearMultiplier(Location loc) {
        Block below = loc.clone().subtract(0, 0.5, 0).getBlock();
        String blockType = below.getType().name().toLowerCase();

        if (blockType.contains("gravel") || blockType.contains("cobblestone")) {
            return ROUGH_SURFACE_WEAR_MULTIPLIER;
        }
        if (blockType.contains("mud") || blockType.contains("sand") || blockType.contains("soul")) {
            return SOFT_SURFACE_WEAR_MULTIPLIER;
        }
        if (blockType.contains("dirt") || blockType.contains("grass") || blockType.contains("podzol")) {
            return SOFT_SURFACE_WEAR_MULTIPLIER * 0.8f;
        }
        if (blockType.contains("snow") || blockType.contains("ice")) {
            return 0.5f; // Ice/snow is gentle on tires
        }
        return 1.0f; // Default: asphalt-like
    }

    // ─── SYNC TO CLIENT ───

    private static void syncToClient(Player player, PlayerDamageData data) {
        sendTireWear(player, data.tireWear);
        sendEngineTemp(player, data.engineTemp);
        sendBodyDamage(player, data.bodyDamage);
        sendServiceZone(player, data.inServiceZone, data.repairProgress);
    }

    // ─── PACKET SENDING ───

    private static void sendDamageEnabled(Player player, boolean enabled) {
        sendBooleanPacket(player, PACKET_ID_SET_DAMAGE_ENABLED, enabled);
    }

    private static void sendTireWear(Player player, float[] wear) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(PACKET_ID_SYNC_TIRE_WEAR);
            out.writeFloat(wear[0]);
            out.writeFloat(wear[1]);
            out.writeFloat(wear[2]);
            out.writeFloat(wear[3]);
            player.sendPluginMessage(TimingSystem.getPlugin(),
                    CustomBoatUtilsMode.CHANNEL_IBREALISTIC, byteStream.toByteArray());
        } catch (IOException e) {
            logError("SYNC_TIRE_WEAR", player, e);
        }
    }

    private static void sendEngineTemp(Player player, float temp) {
        sendFloatPacket(player, PACKET_ID_SYNC_ENGINE_TEMP, temp);
    }

    private static void sendBodyDamage(Player player, float damage) {
        sendFloatPacket(player, PACKET_ID_SYNC_BODY_DAMAGE, damage);
    }

    private static void sendServiceZone(Player player, boolean inZone, float progress) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(PACKET_ID_SET_SERVICE_ZONE);
            out.writeBoolean(inZone);
            out.writeFloat(progress);
            player.sendPluginMessage(TimingSystem.getPlugin(),
                    CustomBoatUtilsMode.CHANNEL_IBREALISTIC, byteStream.toByteArray());
        } catch (IOException e) {
            logError("SET_SERVICE_ZONE", player, e);
        }
    }

    private static void sendNotification(Player player, String message, int color, long durationMs) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(PACKET_ID_DAMAGE_NOTIFICATION);
            // Write string manually (UTF-8 length-prefixed)
            byte[] bytes = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            writeVarInt(out, bytes.length);
            out.write(bytes);
            out.writeInt(color);
            out.writeLong(durationMs);
            player.sendPluginMessage(TimingSystem.getPlugin(),
                    CustomBoatUtilsMode.CHANNEL_IBREALISTIC, byteStream.toByteArray());
        } catch (IOException e) {
            logError("DAMAGE_NOTIFICATION", player, e);
        }
    }

    // ─── LOW-LEVEL HELPERS ───

    private static void sendBooleanPacket(Player player, short packetId, boolean value) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeBoolean(value);
            player.sendPluginMessage(TimingSystem.getPlugin(),
                    CustomBoatUtilsMode.CHANNEL_IBREALISTIC, byteStream.toByteArray());
        } catch (IOException e) {
            logError("packet " + packetId, player, e);
        }
    }

    private static void sendFloatPacket(Player player, short packetId, float value) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeFloat(value);
            player.sendPluginMessage(TimingSystem.getPlugin(),
                    CustomBoatUtilsMode.CHANNEL_IBREALISTIC, byteStream.toByteArray());
        } catch (IOException e) {
            logError("packet " + packetId, player, e);
        }
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static void logError(String packetName, Player player, IOException e) {
        TimingSystem.getPlugin().getLogger().log(Level.WARNING,
                "Failed to send " + packetName + " to " + player.getName(), e);
    }

    private static float averageTireWear(float[] wear) {
        return (wear[0] + wear[1] + wear[2] + wear[3]) / 4f;
    }

    // ─── PUBLIC GETTERS FOR COMMANDS ───

    public static float[] getTireWear(UUID uuid) {
        PlayerDamageData data = playerData.get(uuid);
        return data != null ? data.tireWear.clone() : new float[4];
    }

    public static float getEngineTemp(UUID uuid) {
        PlayerDamageData data = playerData.get(uuid);
        return data != null ? data.engineTemp : 0f;
    }

    public static float getBodyDamage(UUID uuid) {
        PlayerDamageData data = playerData.get(uuid);
        return data != null ? data.bodyDamage : 0f;
    }

    /**
     * Manually sets body damage for a player (for testing/admin commands).
     */
    public static void setBodyDamage(UUID uuid, float damage) {
        PlayerDamageData data = playerData.get(uuid);
        if (data != null) {
            data.bodyDamage = Math.max(0f, Math.min(1f, damage));
        }
    }

    /**
     * Manually sets engine temperature for a player (for testing/admin commands).
     */
    public static void setEngineTemp(UUID uuid, float temp) {
        PlayerDamageData data = playerData.get(uuid);
        if (data != null) {
            data.engineTemp = Math.max(0f, Math.min(1f, temp));
        }
    }

    /**
     * Manually sets tire wear for a player (for testing/admin commands).
     */
    public static void setTireWear(UUID uuid, float wear) {
        PlayerDamageData data = playerData.get(uuid);
        if (data != null) {
            for (int i = 0; i < 4; i++) {
                data.tireWear[i] = Math.max(0f, Math.min(1f, wear));
            }
        }
    }

    /**
     * Fully repairs a player's vehicle.
     */
    public static void repairFull(UUID uuid) {
        PlayerDamageData data = playerData.get(uuid);
        if (data != null) {
            data.tireWear = new float[4];
            data.engineTemp = 0f;
            data.bodyDamage = 0f;
            data.repairProgress = 0f;
        }
    }
}
