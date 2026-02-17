package me.makkuusen.timing.system.team;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.boatutils.CustomBoatUtilsMode;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.theme.messages.Warning;
import me.makkuusen.timing.system.track.TrackWeather;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dynamic weather during team races (F1-style).
 * Shows forecast in BossBar, warns 30 seconds before change.
 */
public class RaceWeatherManager {

    // ─── CONFIGURATION ───
    private static final int DEFAULT_WEATHER_INTERVAL_SECONDS = 300; // 5 minutes
    private static final int WARNING_SECONDS = 30;

    // ─── WEATHER SCHEDULE ───
    /** Valid race weather sequence */
    private static final TrackWeather[] WEATHER_CYCLE = {
            TrackWeather.CLEAR, TrackWeather.RAIN, TrackWeather.HEAVY_RAIN,
            TrackWeather.RAIN, TrackWeather.CLEAR
    };

    // ─── ACTIVE SESSIONS ───
    /** Pilot UUID → RaceWeatherSession */
    private static final Map<UUID, RaceWeatherSession> activeSessions = new ConcurrentHashMap<>();

    // ─── INNER CLASS ───
    private static class RaceWeatherSession {
        final UUID pilotUuid;
        TrackWeather currentWeather;
        TrackWeather nextWeather;
        int cycleIndex;
        int ticksUntilChange;
        int ticksSinceWarning;
        boolean warningGiven;
        boolean dynamicEnabled;
        BossBar weatherBar;
        int taskId = -1;

        RaceWeatherSession(UUID pilotUuid, TrackWeather initial, boolean dynamic) {
            this.pilotUuid = pilotUuid;
            this.currentWeather = initial;
            this.dynamicEnabled = dynamic;
            this.cycleIndex = findCycleIndex(initial);
            this.nextWeather = dynamic ? getNextInCycle() : initial;
            this.ticksUntilChange = getIntervalTicks();
            this.ticksSinceWarning = 0;
            this.warningGiven = false;
        }

        private int findCycleIndex(TrackWeather w) {
            for (int i = 0; i < WEATHER_CYCLE.length; i++) {
                if (WEATHER_CYCLE[i] == w) return i;
            }
            return 0;
        }

        TrackWeather getNextInCycle() {
            int next = (cycleIndex + 1) % WEATHER_CYCLE.length;
            return WEATHER_CYCLE[next];
        }

        void advanceCycle() {
            cycleIndex = (cycleIndex + 1) % WEATHER_CYCLE.length;
            currentWeather = WEATHER_CYCLE[cycleIndex];
            nextWeather = getNextInCycle();
            ticksUntilChange = getIntervalTicks();
            warningGiven = false;
        }
    }

    // ─── LIFECYCLE ───

    /**
     * Start weather tracking for a team race pilot.
     */
    public static void startForPilot(UUID pilotUuid, TrackWeather initialWeather, boolean dynamicWeather) {
        RaceWeatherSession session = new RaceWeatherSession(pilotUuid, initialWeather, dynamicWeather);

        // Create BossBar
        session.weatherBar = BossBar.bossBar(
                buildWeatherTitle(session),
                1.0f,
                getBarColor(session.currentWeather),
                BossBar.Overlay.PROGRESS
        );

        Player player = Bukkit.getPlayer(pilotUuid);
        if (player != null) {
            player.showBossBar(session.weatherBar);
        }

        activeSessions.put(pilotUuid, session);

        if (dynamicWeather) {
            // Schedule tick task
            session.taskId = Bukkit.getScheduler().runTaskTimer(TimingSystem.getPlugin(), () -> {
                tickWeather(pilotUuid);
            }, 20L, 20L).getTaskId(); // tick every second
        }
    }

    /**
     * Stop weather tracking for a pilot.
     */
    public static void stopForPilot(UUID pilotUuid) {
        RaceWeatherSession session = activeSessions.remove(pilotUuid);
        if (session == null) return;

        if (session.taskId >= 0) {
            Bukkit.getScheduler().cancelTask(session.taskId);
        }

        Player player = Bukkit.getPlayer(pilotUuid);
        if (player != null && session.weatherBar != null) {
            player.hideBossBar(session.weatherBar);
        }
    }

    /**
     * Get current weather for a pilot.
     */
    public static TrackWeather getCurrentWeather(UUID pilotUuid) {
        RaceWeatherSession session = activeSessions.get(pilotUuid);
        return session != null ? session.currentWeather : TrackWeather.CLEAR;
    }

    /**
     * Check if current weather is wet (rain or heavy rain).
     */
    public static boolean isWetCondition(UUID pilotUuid) {
        TrackWeather w = getCurrentWeather(pilotUuid);
        return w == TrackWeather.RAIN || w == TrackWeather.HEAVY_RAIN || w == TrackWeather.SNOW;
    }

    // ─── TICK ───

    private static void tickWeather(UUID pilotUuid) {
        RaceWeatherSession session = activeSessions.get(pilotUuid);
        if (session == null || !session.dynamicEnabled) return;

        session.ticksUntilChange--;

        // Update BossBar progress
        float progress = Math.max(0f, (float) session.ticksUntilChange / getIntervalTicks());
        if (session.weatherBar != null) {
            session.weatherBar.progress(progress);
            session.weatherBar.name(buildWeatherTitle(session));
        }

        // 30-second warning
        if (!session.warningGiven && session.ticksUntilChange <= WARNING_SECONDS) {
            session.warningGiven = true;
            Player player = Bukkit.getPlayer(pilotUuid);
            if (player != null) {
                Text.send(player, Warning.WEATHER_CHANGE_WARNING,
                        "%weather%", session.nextWeather.getDisplayName(),
                        "%seconds%", String.valueOf(WARNING_SECONDS));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.8f);
                // Flash BossBar
                if (session.weatherBar != null) {
                    session.weatherBar.color(BossBar.Color.RED);
                }
            }
        }

        // Weather change
        if (session.ticksUntilChange <= 0) {
            session.advanceCycle();

            Player player = Bukkit.getPlayer(pilotUuid);
            if (player != null) {
                // Send weather packet to client
                CustomBoatUtilsMode.sendWeatherConditionPacket(player,
                        (short) session.currentWeather.getId());

                // Update Bukkit weather
                if (session.currentWeather == TrackWeather.RAIN
                        || session.currentWeather == TrackWeather.HEAVY_RAIN
                        || session.currentWeather == TrackWeather.SNOW) {
                    player.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL);
                } else {
                    player.resetPlayerWeather();
                }

                Text.send(player, Info.WEATHER_CHANGED,
                        "%weather%", session.currentWeather.getDisplayName());
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.0f);

                // Reset BossBar color
                if (session.weatherBar != null) {
                    session.weatherBar.color(getBarColor(session.currentWeather));
                }
            }
        }
    }

    // ─── DISPLAY ───

    private static Component buildWeatherTitle(RaceWeatherSession session) {
        String icon = getWeatherIcon(session.currentWeather);
        String current = session.currentWeather.getDisplayName();

        if (!session.dynamicEnabled) {
            return Component.text(icon + " " + current, NamedTextColor.WHITE);
        }

        int secsLeft = session.ticksUntilChange;
        String nextIcon = getWeatherIcon(session.nextWeather);
        String next = session.nextWeather.getDisplayName();

        String timeStr;
        if (secsLeft >= 60) {
            timeStr = (secsLeft / 60) + "m " + (secsLeft % 60) + "s";
        } else {
            timeStr = secsLeft + "s";
        }

        NamedTextColor timeColor = secsLeft <= WARNING_SECONDS ? NamedTextColor.RED : NamedTextColor.GRAY;

        return Component.text(icon + " " + current, NamedTextColor.WHITE)
                .append(Component.text(" │ ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Next: " + nextIcon + " " + next, NamedTextColor.GRAY))
                .append(Component.text(" │ ", NamedTextColor.DARK_GRAY))
                .append(Component.text(timeStr, timeColor));
    }

    private static String getWeatherIcon(TrackWeather weather) {
        return switch (weather) {
            case CLEAR -> "☀";
            case RAIN -> "🌧";
            case HEAVY_RAIN -> "⛈";
            case SNOW -> "❄";
            case FOG -> "🌫";
        };
    }

    private static BossBar.Color getBarColor(TrackWeather weather) {
        return switch (weather) {
            case CLEAR -> BossBar.Color.YELLOW;
            case RAIN -> BossBar.Color.BLUE;
            case HEAVY_RAIN -> BossBar.Color.PURPLE;
            case SNOW -> BossBar.Color.WHITE;
            case FOG -> BossBar.Color.WHITE;
        };
    }

    private static int getIntervalTicks() {
        return TimingSystem.getPlugin().getConfig().getInt(
                "team_race.weather.interval_seconds", DEFAULT_WEATHER_INTERVAL_SECONDS);
    }

    // ─── CLEANUP ───

    public static void stopAll() {
        for (UUID uuid : new ArrayList<>(activeSessions.keySet())) {
            stopForPilot(uuid);
        }
    }
}
