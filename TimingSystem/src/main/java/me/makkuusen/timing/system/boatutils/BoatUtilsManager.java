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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class BoatUtilsManager {

    private static final long INITIAL_CHECK_DELAY_TICKS = 10 * 20L; // 10 seconds
    private static final long WARNING_INTERVAL_TICKS = 60 * 20L; // 60 seconds
    private static final String REALISTIC_MOD_DOWNLOAD_URL = "https://github.com/Kanorto/OpenBoatUtilsRealistic/releases/latest";

    public static Map<UUID, BoatUtilsMode> playerBoatUtilsMode = new HashMap<>();
    public static Map<UUID, Integer> playerCustomBoatUtilsModeId = new HashMap<>();
    private static final Map<UUID, BukkitTask> realisticModWarningTasks = new HashMap<>();

    public static void pluginMessageListener(@NotNull String channel, @NotNull Player player, byte[] message) {
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        short packetID = in.readShort();
        if (packetID == 0) {
            int version = in.readInt();
            TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());
            tPlayer.setBoatUtilsVersion(version);

            // Check for realistic mod identifier (appended after version)
            try {
                boolean isRealistic = in.readBoolean();
                tPlayer.setHasRealisticMod(isRealistic);
                if (isRealistic) {
                    cancelRealisticModWarning(player.getUniqueId());
                }
            } catch (Exception e) {
                // Regular OpenBoatUtils without realistic identifier
                tPlayer.setHasRealisticMod(false);
            }

            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            try {
                out.writeShort(29);
                out.writeBoolean(true);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> player.sendPluginMessage(TimingSystem.getPlugin(),"openboatutils:settings", b.toByteArray()), 20);
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
                                .clickEvent(ClickEvent.openUrl("https://modrinth.com/mod/openboatutils"));
                        player.sendMessage(boatUtilsWarning);
                    }
                }
            } else {
                // Need to update OpenBoatUtils
                if (tPlayer.getBoatUtilsVersion() < mode.getRequiredVersion()) {
                    var boatUtilsWarning = tPlayer.getTheme().warning(">> ").append(Text.get(player, Warning.TRACK_REQUIRES_NEWER_BOAT_UTILS)).append(tPlayer.getTheme().warning(" <<"))
                            .hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_OPEN)))
                            .clickEvent(ClickEvent.openUrl("https://modrinth.com/mod/openboatutils"));
                    player.sendMessage(boatUtilsWarning);
                    return;
                }
            }
        }

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            if (mode == BoatUtilsMode.VANILLA) {
                out.writeShort(0);
            } else {
                out.writeShort(8);
                out.writeShort(mode.getId());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", b.toByteArray());
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

    // ─── REALISTIC MOD WARNING ───

    public static void startRealisticModWarningTask(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(TimingSystem.getPlugin(), () -> {
            Player onlinePlayer = Bukkit.getPlayer(playerId);
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                cancelRealisticModWarning(playerId);
                return;
            }

            TPlayer tPlayer = TSDatabase.getPlayer(playerId);
            if (tPlayer != null && tPlayer.isHasRealisticMod()) {
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
                .append(Component.text("OpenBoatUtils Realistic", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" mod!", NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("You will not be able to use ", NamedTextColor.GRAY))
                .append(Component.text("Realistic mode", NamedTextColor.YELLOW))
                .append(Component.text(" and play on this server", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("until you download our version of OpenBoatUtils.", NamedTextColor.GRAY))
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

    public static List<BoatUtilsMode> getAvailableModes(int version) {
        return Arrays.stream(BoatUtilsMode.values()).filter(mode -> mode.getRequiredVersion() <= version).toList();
    }
}
