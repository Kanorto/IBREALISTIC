package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.economy.GarageManager;
import me.makkuusen.timing.system.economy.PlayerCar;
import me.makkuusen.timing.system.track.Track;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages BossBar overlays during time trials.
 * Shows track name, elapsed time, and best time comparison.
 * Updated every 5 ticks (0.25s) via a repeating task.
 */
public class RaceBossBarManager {

    private static final int UPDATE_INTERVAL_TICKS = 5;

    private static final Map<UUID, BossBar> activeBars = new ConcurrentHashMap<>();
    private static BukkitTask updateTask;

    // ─── LIFECYCLE ───

    public static void start() {
        if (updateTask != null) return;
        updateTask = TimingSystem.getPlugin().getServer().getScheduler().runTaskTimer(
                TimingSystem.getPlugin(), RaceBossBarManager::updateAll,
                UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS
        );
    }

    public static void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        activeBars.forEach((uuid, bar) -> {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null) {
                player.hideBossBar(bar);
            }
        });
        activeBars.clear();
    }

    // ─── SHOW / HIDE ───

    public static void showForPlayer(Player player, Track track) {
        UUID uuid = player.getUniqueId();
        hideForPlayer(player);

        BossBar bar = BossBar.bossBar(
                buildTitle(player, track, 0),
                0f,
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS
        );
        activeBars.put(uuid, bar);
        player.showBossBar(bar);
    }

    public static void hideForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = activeBars.remove(uuid);
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    // ─── UPDATE LOOP ───

    private static void updateAll() {
        for (Map.Entry<UUID, BossBar> entry : activeBars.entrySet()) {
            UUID uuid = entry.getKey();
            BossBar bar = entry.getValue();

            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                activeBars.remove(uuid);
                continue;
            }

            TimeTrial timeTrial = TimeTrialController.timeTrials.get(uuid);
            if (timeTrial == null) {
                hideForPlayer(player);
                continue;
            }

            Track track = timeTrial.getTrack();
            long currentTime = timeTrial.getCurrentTime();

            // Update title with current time
            bar.name(buildTitle(player, track, currentTime));

            // Update progress based on best time comparison
            me.makkuusen.timing.system.tplayer.TPlayer tPlayer = me.makkuusen.timing.system.database.TSDatabase.getPlayer(uuid);
            if (tPlayer != null) {
                TimeTrialFinish bestFinish = track.getTimeTrials().getBestFinish(tPlayer);
                if (bestFinish != null && bestFinish.getTime() > 0) {
                    float progress = Math.min((float) currentTime / bestFinish.getTime(), 1.0f);
                    bar.progress(progress);

                    // Color based on pace
                    if (currentTime < bestFinish.getTime() * 0.95f) {
                        bar.color(BossBar.Color.GREEN);   // Ahead of PB
                    } else if (currentTime < bestFinish.getTime()) {
                        bar.color(BossBar.Color.YELLOW);  // Close to PB
                    } else {
                        bar.color(BossBar.Color.RED);     // Behind PB
                    }
                } else {
                    bar.progress(0f);
                    bar.color(BossBar.Color.WHITE);
                }
            }
        }
    }

    // ─── TITLE BUILDER ───

    private static Component buildTitle(Player player, Track track, long currentTimeMs) {
        Component trackName = Component.text(track.getDisplayName(), NamedTextColor.GOLD);
        Component separator = Component.text(" │ ", NamedTextColor.DARK_GRAY);
        Component time = Component.text(ApiUtilities.formatAsTime(currentTimeMs), NamedTextColor.WHITE);

        Component title = trackName.append(separator).append(time);

        // Add weather info
        if (track.getWeatherCondition() != null) {
            String weather = track.getWeatherCondition().getDisplayName();
            if (weather != null && !"Clear".equals(weather)) {
                title = title.append(separator)
                        .append(Component.text(weather, NamedTextColor.AQUA));
            }
        }

        // Add car name
        PlayerCar car = GarageManager.getActiveCar(player.getUniqueId());
        if (car != null) {
            title = title.append(separator)
                    .append(Component.text(car.getName(), NamedTextColor.GREEN));
        }

        return title;
    }
}
