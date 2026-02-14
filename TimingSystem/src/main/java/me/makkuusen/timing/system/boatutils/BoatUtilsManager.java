package me.makkuusen.timing.system.boatutils;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import me.makkuusen.timing.system.api.events.BoatUtilsAppliedEvent;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Hover;
import me.makkuusen.timing.system.theme.messages.Warning;
import me.makkuusen.timing.system.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class BoatUtilsManager {

    private static final long INITIAL_CHECK_DELAY_TICKS = 10 * 20L; // 10 seconds
    private static final long WARNING_INTERVAL_TICKS = 60 * 20L; // 60 seconds
    private static final String REALISTIC_MOD_DOWNLOAD_URL = "https://github.com/Kanorto/IBREALISTIC/releases/latest";

    public static Map<UUID, BoatUtilsMode> playerBoatUtilsMode = new HashMap<>();
    public static Map<UUID, Integer> playerCustomBoatUtilsModeId = new HashMap<>();
    private static final Map<UUID, BukkitTask> realisticModWarningTasks = new HashMap<>();

    public static void pluginMessageListener(@NotNull String channel, @NotNull Player player, byte[] message) {
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        short packetID = in.readShort();
        if (packetID == 0) {
            int version = in.readInt();
            TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());
            // Use MAX to prevent race condition: IBRealistic sends version 21 on
            // ibrealistic:settings, OBU sends version 18 on openboatutils:settings.
            // If OBU's packet arrives second, it must not downgrade 21 → 18.
            Integer currentVersion = tPlayer.getBoatUtilsVersion();
            tPlayer.setBoatUtilsVersion(currentVersion != null ? Math.max(currentVersion, version) : version);

            // Check for realistic mod identifier (appended after version)
            boolean isRealistic = false;
            String buildHash = null;
            try {
                isRealistic = in.readBoolean();
                tPlayer.setRealisticMod(isRealistic);
                if (isRealistic) {
                    cancelRealisticModWarning(player.getUniqueId());
                }
                // Read build hash (appended after realistic flag)
                buildHash = readString(in);
            } catch (IllegalStateException e) {
                // Packet has no realistic flag — this is a regular OBU version packet.
                // Only set realisticMod=false if this came from the IBRealistic channel.
                // OBU's version packet naturally lacks this field, so we must NOT let it
                // overwrite the realistic=true flag set by a prior IBRealistic packet.
                if (CustomBoatUtilsMode.CHANNEL_IBREALISTIC.equalsIgnoreCase(channel)) {
                    tPlayer.setRealisticMod(false);
                }
            } catch (Exception e) {
                // Hash not present or malformed — older realistic client
            }

            // Validate build hash
            if (isRealistic && buildHash != null) {
                validateBuildHash(player, buildHash);
            }

            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            try {
                out.writeShort(29);
                out.writeBoolean(true);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> {
                player.sendPluginMessage(TimingSystem.getPlugin(), CustomBoatUtilsMode.CHANNEL_OBU, b.toByteArray());

                // Send REALISTIC_SERVER_INFO to realistic clients
                if (tPlayer.isRealisticMod()) {
                    sendRealisticServerInfoToPlayer(player);
                }
            }, 20);
        } else if (packetID == 1) {
            // REALISTIC_CLIENT_INFO (C2S)
            handleRealisticClientInfo(player, in);
        }
    }

    // ─── REALISTIC SERVER INFO ───

    /**
     * Sends REALISTIC_SERVER_INFO packet to a player.
     * Reads version from plugin metadata, features and server name from config.
     */
    private static void sendRealisticServerInfoToPlayer(Player player) {
        String realisticVersion = getServerRealisticVersion();
        int featureFlags = getServerFeatureFlags();
        String serverName = getServerName();
        CustomBoatUtilsMode.sendRealisticServerInfo(player, realisticVersion, featureFlags, serverName);
        TimingSystem.getPlugin().getLogger().info(
                "Sent REALISTIC_SERVER_INFO to " + player.getName()
                + ": version=" + realisticVersion
                + " features=" + featureFlags
                + " name=" + serverName);
    }

    /**
     * Returns the realistic version of this plugin, extracted from plugin metadata.
     * Format: "{ts_base_version}-{realistic_version}" → extracts realistic_version.
     * <p>
     * Fallback: if version has no dash (e.g. during development), returns the full version string.
     */
    public static String getServerRealisticVersion() {
        String fullVersion = TimingSystem.getPlugin().getPluginMeta().getVersion();
        // Format: {ts_base_version}-{realistic_version}
        int dashIdx = fullVersion.indexOf('-');
        if (dashIdx < 0) return fullVersion;
        return fullVersion.substring(dashIdx + 1);
    }

    /**
     * Returns the server's supported feature flags as a bitfield.
     * Read from config.yml realistic.features section.
     */
    public static int getServerFeatureFlags() {
        var config = TimingSystem.getPlugin().getConfig();
        int flags = 0;
        if (config.getBoolean("realistic.features.fourWheel", true)) {
            flags |= RealisticFeature.FOUR_WHEEL.getMask();
        }
        if (config.getBoolean("realistic.features.weather", true)) {
            flags |= RealisticFeature.WEATHER.getMask();
        }
        if (config.getBoolean("realistic.features.economy", false)) {
            flags |= RealisticFeature.ECONOMY.getMask();
        }
        if (config.getBoolean("realistic.features.soloRace", false)) {
            flags |= RealisticFeature.SOLO_RACE.getMask();
        }
        if (config.getBoolean("realistic.features.customCars", false)) {
            flags |= RealisticFeature.CUSTOM_CARS.getMask();
        }
        if (config.getBoolean("realistic.features.multiplayerRace", false)) {
            flags |= RealisticFeature.MULTIPLAYER_RACE.getMask();
        }
        return flags;
    }

    /**
     * Returns the server name from config, defaulting to Bukkit server name.
     */
    public static String getServerName() {
        return TimingSystem.getPlugin().getConfig().getString("realistic.serverName",
                Bukkit.getServer().getName());
    }

    /**
     * Handles REALISTIC_CLIENT_INFO packet (C2S, packet ID 1).
     * Stores the client's realistic version and feature flags in TPlayer.
     */
    private static void handleRealisticClientInfo(Player player, ByteArrayDataInput in) {
        try {
            String clientVersion = readString(in);
            int clientFeatures = in.readInt();
            TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());
            tPlayer.setClientRealisticVersion(clientVersion);
            tPlayer.setClientFeatures(clientFeatures);
            TimingSystem.getPlugin().getLogger().info(
                    "Received REALISTIC_CLIENT_INFO from " + player.getName()
                    + ": version=" + clientVersion
                    + " features=" + clientFeatures);
        } catch (Exception e) {
            TimingSystem.getPlugin().getLogger().warning(
                    "Failed to parse REALISTIC_CLIENT_INFO from " + player.getName() + ": " + e.getMessage());
        }
    }

    public static void sendBoatUtilsModePluginMessage(Player player, BoatUtilsMode mode, Track track, boolean sameAsLastTrack){
        TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        if (mode != BoatUtilsMode.VANILLA) {
            if (!tPlayer.hasBoatUtils()) {
                if (track != null) {
                    if (track.isBoatUtils()) {
                        var boatUtilsWarning = tPlayer.getTheme().warning(">> ").append(Text.get(player, Warning.TRACK_REQUIRES_BOAT_UTILS)).append(tPlayer.getTheme().warning(" <<"))
                                .hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_OPEN)))
                                .clickEvent(ClickEvent.openUrl(REALISTIC_MOD_DOWNLOAD_URL));
                        player.sendMessage(boatUtilsWarning);
                    }
                }
            } else {
                // Check if mode requires the realistic mod
                if (mode.requiresRealisticMod() && !tPlayer.isRealisticMod()) {
                    var boatUtilsWarning = tPlayer.getTheme().warning(">> ").append(Text.get(player, Warning.TRACK_REQUIRES_NEWER_BOAT_UTILS)).append(tPlayer.getTheme().warning(" <<"))
                            .hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_OPEN)))
                            .clickEvent(ClickEvent.openUrl(REALISTIC_MOD_DOWNLOAD_URL));
                    player.sendMessage(boatUtilsWarning);
                    return;
                }
                // Need to update IBRealistic
                if (tPlayer.getBoatUtilsVersion() < mode.getRequiredVersion()) {
                    var boatUtilsWarning = tPlayer.getTheme().warning(">> ").append(Text.get(player, Warning.TRACK_REQUIRES_NEWER_BOAT_UTILS)).append(tPlayer.getTheme().warning(" <<"))
                            .hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_OPEN)))
                            .clickEvent(ClickEvent.openUrl(REALISTIC_MOD_DOWNLOAD_URL));
                    player.sendMessage(boatUtilsWarning);
                    return;
                }
            }
        }

        byte[] modePacket = buildModePacket(mode);

        if (mode == BoatUtilsMode.VANILLA) {
            // RESET: send to OBU channel (clears OBU state) AND IBRealistic channel (clears realistic state)
            player.sendPluginMessage(TimingSystem.getPlugin(), CustomBoatUtilsMode.CHANNEL_OBU, modePacket);
            player.sendPluginMessage(TimingSystem.getPlugin(), CustomBoatUtilsMode.CHANNEL_IBREALISTIC, modePacket);
        } else if (mode.requiresRealisticMod()) {
            // Realistic modes: route SET_MODE to IBRealistic channel.
            // IBRealistic's Modes enum includes realistic entries (25+) and its handler
            // sets BOTH OBU fields and IBRealistic physics state.
            // OBU's Modes enum only has 25 entries (0-24), so sending mode>=25 to OBU would crash.
            player.sendPluginMessage(TimingSystem.getPlugin(), CustomBoatUtilsMode.CHANNEL_IBREALISTIC, modePacket);
        } else {
            // Non-realistic modes: route SET_MODE to OBU channel (OBU handles it).
            // Also send RESET to IBRealistic channel to clear any leftover realistic state
            // from a previously active realistic mode.
            player.sendPluginMessage(TimingSystem.getPlugin(), CustomBoatUtilsMode.CHANNEL_OBU, modePacket);
            player.sendPluginMessage(TimingSystem.getPlugin(), CustomBoatUtilsMode.CHANNEL_IBREALISTIC,
                    buildModePacket(BoatUtilsMode.VANILLA));
        }
        if (tPlayer.getSettings().isVerbose() && !(playerBoatUtilsMode.get(player.getUniqueId()) != null && playerBoatUtilsMode.get(player.getUniqueId()) == mode)) {
            player.sendMessage(Component.text("BU Mode: " + mode.name(), tPlayer.getTheme().getPrimary()));
        }
        playerBoatUtilsMode.put(player.getUniqueId(), mode);
        new BoatUtilsAppliedEvent(player, mode, track).callEvent();
    }

    public static void clearPlayerModes(UUID playerId) {
        playerBoatUtilsMode.remove(playerId);
        playerCustomBoatUtilsModeId.remove(playerId);
        cancelRealisticModWarning(playerId);
    }

    /**
     * Builds the byte array for a SET_MODE or RESET packet.
     * VANILLA → RESET (short 0), otherwise → SET_MODE (short 8, short modeId).
     */
    private static byte[] buildModePacket(BoatUtilsMode mode) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            if (mode == BoatUtilsMode.VANILLA) {
                out.writeShort(0); // RESET
            } else {
                out.writeShort(8); // SET_MODE
                out.writeShort(mode.getId());
            }
            return byteStream.toByteArray();
        } catch (IOException e) {
            TimingSystem.getPlugin().getLogger().log(java.util.logging.Level.SEVERE,
                    "Failed to build mode packet for " + mode.name(), e);
            return new byte[]{0, 0}; // Fallback: RESET packet (safe no-op)
        }
    }

    // ─── BUILD HASH VERIFICATION ───

    /** Hashes loaded from valid_hashes.txt in the plugin's data folder */
    private static Set<String> validHashes = new HashSet<>();

    /**
     * Loads valid hashes from the plugin's data folder (valid_hashes.txt).
     * Called during plugin initialization.
     */
    public static void loadValidHashes() {
        validHashes.clear();
        File hashFile = new File(TimingSystem.getPlugin().getDataFolder(), "valid_hashes.txt");
        if (!hashFile.exists()) {
            TimingSystem.getPlugin().getLogger().warning(
                    "valid_hashes.txt not found in plugin folder. Build verification will rely on config.yml only.");
            return;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(hashFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    validHashes.add(line);
                }
            }
            TimingSystem.getPlugin().getLogger().info(
                    "Loaded " + validHashes.size() + " valid build hashes from valid_hashes.txt");
        } catch (IOException e) {
            TimingSystem.getPlugin().getLogger().warning(
                    "Failed to read valid_hashes.txt: " + e.getMessage());
        }
    }

    /**
     * Validates the client's build hash against the hash file and config whitelist.
     * Notifies online admins if the hash is unknown.
     */
    private static void validateBuildHash(Player player, String clientHash) {
        if (!TimingSystem.getPlugin().getConfig().getBoolean("build_verification.enabled", true)) {
            return;
        }

        // Check hashes from valid_hashes.txt (placed by admin from CI artifacts)
        if (validHashes.contains(clientHash)) {
            TimingSystem.getPlugin().getLogger().info(
                    "Build hash OK for " + player.getName() + ": " + clientHash);
            return;
        }

        // Check config hashes (manually added by admin)
        List<String> configHashes = TimingSystem.getPlugin().getConfig().getStringList("build_verification.valid_hashes");
        if (configHashes.contains(clientHash)) {
            TimingSystem.getPlugin().getLogger().info(
                    "Build hash OK for " + player.getName() + ": " + clientHash);
            return;
        }

        // Unknown hash — notify admins
        TimingSystem.getPlugin().getLogger().warning(
                "Unknown build hash from " + player.getName() + ": " + clientHash);

        Component adminMessage = Component.text("[IBRealistic] ", NamedTextColor.RED)
                .append(Component.text("Unknown build hash from ", NamedTextColor.YELLOW))
                .append(Component.text(player.getName(), NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(": ", NamedTextColor.YELLOW))
                .append(Component.text(clientHash, NamedTextColor.GRAY));

        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("timingsystem.admin")) {
                admin.sendMessage(adminMessage);
            }
        }
    }

    // ─── REALISTIC MOD WARNING ───

    public static void startRealisticModWarningTask(Player player) {
        UUID playerId = player.getUniqueId();
        cancelRealisticModWarning(playerId);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(TimingSystem.getPlugin(), () -> {
            Player onlinePlayer = Bukkit.getPlayer(playerId);
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                cancelRealisticModWarning(playerId);
                return;
            }

            TPlayer tPlayer = TSDatabase.getPlayer(playerId);
            if (tPlayer != null && tPlayer.isRealisticMod()) {
                cancelRealisticModWarning(playerId);
                return;
            }

            sendRealisticModWarning(onlinePlayer);
        }, INITIAL_CHECK_DELAY_TICKS, WARNING_INTERVAL_TICKS);

        realisticModWarningTasks.put(playerId, task);
    }

    public static void cancelRealisticModWarning(UUID playerId) {
        BukkitTask task = realisticModWarningTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private static void sendRealisticModWarning(Player player) {
        Component separator = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.RED);

        Component warningMessage = Component.empty()
                .append(separator)
                .append(Component.newline())
                .append(Component.text("⚠ ", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text("You joined without ", NamedTextColor.RED))
                .append(Component.text("IBRealistic", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" mod!", NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("You will not be able to use ", NamedTextColor.GRAY))
                .append(Component.text("Realistic mode", NamedTextColor.YELLOW))
                .append(Component.text(" and play on this server", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("until you install our modified version of IBRealistic.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("Replace your current mod with the one below.", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("▶ ", NamedTextColor.GREEN))
                .append(Component.text("[Click here to download]", NamedTextColor.GREEN, TextDecoration.BOLD, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(REALISTIC_MOD_DOWNLOAD_URL))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to open download page", NamedTextColor.GREEN))))
                .append(Component.newline())
                .append(Component.text(REALISTIC_MOD_DOWNLOAD_URL, NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.openUrl(REALISTIC_MOD_DOWNLOAD_URL)))
                .append(Component.newline())
                .append(separator);

        player.sendMessage(warningMessage);
    }

    public static boolean isPlayerUsingCorrectMode(Player player, Track track) {
        BoatUtilsMode playerMode = playerBoatUtilsMode.get(player.getUniqueId());
        Integer playerCustomModeId = playerCustomBoatUtilsModeId.get(player.getUniqueId());

        Integer trackCustomModeId = track.getCustomBoatUtilsModeId();
        if (trackCustomModeId != null) {
            return Objects.equals(playerCustomModeId, trackCustomModeId);
        } else {
            return playerCustomModeId == null && Objects.equals(playerMode, track.getBoatUtilsMode());
        }
    }

    public static List<BoatUtilsMode> getAvailableModes(int version, boolean isRealisticMod) {
        return Arrays.stream(BoatUtilsMode.values())
                .filter(mode -> mode.getRequiredVersion() <= version)
                .filter(mode -> !mode.requiresRealisticMod() || isRealisticMod)
                .toList();
    }

    // ─── VARINT STRING READING ───

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    /**
     * Reads a VarInt-encoded string from ByteArrayDataInput.
     * Compatible with Minecraft's PacketByteBuf.writeString format.
     */
    private static String readString(ByteArrayDataInput in) {
        int length = readVarInt(in);
        if (length < 0 || length > 32767) {
            throw new RuntimeException("VarInt string length out of bounds: " + length + " (valid range: 0-32767)");
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static int readVarInt(ByteArrayDataInput in) {
        int value = 0;
        int position = 0;
        byte currentByte;
        while (true) {
            currentByte = in.readByte();
            value |= (currentByte & SEGMENT_BITS) << position;
            if ((currentByte & CONTINUE_BIT) == 0) break;
            position += 7;
            if (position >= 32) throw new RuntimeException("VarInt is too big");
        }
        return value;
    }
}
