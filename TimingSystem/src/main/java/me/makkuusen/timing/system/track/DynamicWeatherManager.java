package me.makkuusen.timing.system.track;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.boatutils.CustomBoatUtilsMode;
import me.makkuusen.timing.system.race.RaceState;
import me.makkuusen.timing.system.race.SoloRaceManager;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Info;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dynamic weather transitions for tracks that have dynamicWeather enabled.
 * Weather cycles: CLEAR → RAIN → HEAVY_RAIN → RAIN → CLEAR
 */
public class DynamicWeatherManager {

    // ─── CONFIGURATION DEFAULTS ───
    private static final int DEFAULT_CHANGE_INTERVAL_MINUTES = 5;

    // ─── WEATHER CYCLE ───
    private static final TrackWeather[] WEATHER_CYCLE = {
            TrackWeather.CLEAR, TrackWeather.RAIN, TrackWeather.HEAVY_RAIN, TrackWeather.RAIN, TrackWeather.CLEAR
    };

    // ─── STATE ───
    private static final Map<Integer, Integer> trackWeatherIndex = new ConcurrentHashMap<>();
    private static BukkitTask weatherTask;

    /**
     * Starts the dynamic weather scheduler.
     * Called during plugin startup.
     */
    public static void start() {
        int intervalTicks = getChangeIntervalMinutes() * 60 * 20;
        weatherTask = Bukkit.getScheduler().runTaskTimer(TimingSystem.getPlugin(), DynamicWeatherManager::tick, intervalTicks, intervalTicks);
    }

    /**
     * Stops the dynamic weather scheduler.
     * Called during plugin shutdown.
     */
    public static void stop() {
        if (weatherTask != null) {
            weatherTask.cancel();
            weatherTask = null;
        }
        trackWeatherIndex.clear();
    }

    /**
     * Gets the current dynamic weather for a track, or its base weather if dynamic is disabled.
     */
    public static TrackWeather getCurrentWeather(Track track) {
        if (!track.isDynamicWeather()) {
            return track.getWeatherCondition();
        }
        int index = trackWeatherIndex.getOrDefault(track.getId(), 0);
        return WEATHER_CYCLE[index % WEATHER_CYCLE.length];
    }

    /**
     * Called periodically to advance weather for all tracks with dynamic weather.
     */
    private static void tick() {
        // Find all tracks with dynamic weather that have active racers
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            var session = SoloRaceManager.getSession(uuid);
            if (session.isEmpty()) continue;
            if (session.get().getState() != RaceState.RACING) continue;

            Track track = session.get().getTrack();
            if (!track.isDynamicWeather()) continue;

            // Advance weather index for this track
            int currentIndex = trackWeatherIndex.getOrDefault(track.getId(), 0);
            int nextIndex = (currentIndex + 1) % WEATHER_CYCLE.length;
            trackWeatherIndex.put(track.getId(), nextIndex);

            TrackWeather newWeather = WEATHER_CYCLE[nextIndex];

            // Send weather update to player
            CustomBoatUtilsMode.sendWeatherConditionPacket(player, (short) newWeather.getId());

            // Update visual weather
            if (newWeather == TrackWeather.RAIN || newWeather == TrackWeather.HEAVY_RAIN || newWeather == TrackWeather.SNOW) {
                player.setPlayerWeather(WeatherType.DOWNFALL);
            } else {
                player.resetPlayerWeather();
            }

            // Notify player
            Text.send(player, Info.DYNAMIC_WEATHER_CHANGE, "%weather%", newWeather.getDisplayName());
        }
    }

    private static int getChangeIntervalMinutes() {
        return TimingSystem.getPlugin().getConfig().getInt("dynamic_weather.change_interval_minutes", DEFAULT_CHANGE_INTERVAL_MINUTES);
    }
}
