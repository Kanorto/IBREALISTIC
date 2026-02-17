package me.makkuusen.timing.system.team;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import co.aikar.taskchain.TaskChain;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.boatutils.BoatUtilsManager;
import me.makkuusen.timing.system.boatutils.CustomBoatUtilsMode;
import me.makkuusen.timing.system.boatutils.DamageWearManager;
import me.makkuusen.timing.system.economy.LevelManager;
import me.makkuusen.timing.system.economy.RallyCoinManager;
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
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages team race sessions — coordinating pilot and mechanics.
 * Handles race lifecycle: start → countdown → racing → pit stops → finish.
 */
public class TeamRaceManager {

    // ─── CONFIGURATION ───
    private static final int DEFAULT_TEAM_RACE_LAPS = 5;
    private static final int DEFAULT_REQUIRED_PITS = 1;
    private static final int TEAM_RACE_COINS = 50;
    private static final int TEAM_RACE_XP = 60;
    private static final int COUNTDOWN_SECONDS = 5;
    private static final int TIMEOUT_MINUTES = 15;

    // ─── ACTIVE SESSIONS ───
    /** Pilot UUID → TeamRaceSession */
    private static final Map<UUID, TeamRaceSession> activeSessions = new ConcurrentHashMap<>();

    /** Saved walk speeds for frozen pilots */
    private static final Map<UUID, Float> savedWalkSpeeds = new ConcurrentHashMap<>();

    // ─── CONFIGURATION GETTERS ───

    public static boolean isEnabled() {
        return TimingSystem.getPlugin().getConfig().getBoolean("team_race.enabled", true);
    }

    public static int getMinMechanics() {
        return TimingSystem.getPlugin().getConfig().getInt("team_race.min_mechanics", 1);
    }

    public static int getMaxMechanics() {
        return TimingSystem.getPlugin().getConfig().getInt("team_race.max_mechanics", 3);
    }

    // ─── SESSION MANAGEMENT ───

    public static Optional<TeamRaceSession> getSession(UUID pilotUuid) {
        return Optional.ofNullable(activeSessions.get(pilotUuid));
    }

    public static boolean isInTeamRace(UUID pilotUuid) {
        return activeSessions.containsKey(pilotUuid);
    }

    // ─── START TEAM RACE ───

    /**
     * Start a team race on a track.
     * @param pilot the pilot player
     * @param team the team
     * @param track the track to race on
     * @param laps number of laps
     * @param requiredPits number of mandatory pit stops
     * @return true if race started successfully
     */
    public static boolean startTeamRace(Player pilot, Team team, Track track,
                                        int laps, int requiredPits) {
        if (!isEnabled()) {
            Text.send(pilot, Error.TEAM_RACE_NOT_ENABLED);
            return false;
        }

        if (isInTeamRace(pilot.getUniqueId())) {
            Text.send(pilot, Error.RACE_ALREADY_ACTIVE);
            return false;
        }

        if (!track.isOpen()) {
            Text.send(pilot, Error.TRACK_IS_CLOSED);
            return false;
        }

        if (!track.getTrackRegions().hasRegion(TrackRegion.RegionType.START)) {
            Text.send(pilot, Error.GENERIC);
            return false;
        }

        // Validate team has a pilot
        if (!team.hasPilot()) {
            Text.send(pilot, Error.TEAM_NO_PILOT);
            return false;
        }

        // Validate pilot is the team's pilot
        UUID teamPilotUuid = team.getPilotUuid();
        if (!pilot.getUniqueId().equals(teamPilotUuid)) {
            Text.send(pilot, Error.TEAM_NOT_PILOT);
            return false;
        }

        // Validate enough mechanics
        List<UUID> mechanicUuids = team.getMechanicUuids();
        int onlineMechanics = 0;
        for (UUID uuid : mechanicUuids) {
            Player mech = Bukkit.getPlayer(uuid);
            if (mech != null && mech.isOnline()) {
                onlineMechanics++;
            }
        }

        if (onlineMechanics < getMinMechanics()) {
            Text.send(pilot, Error.TEAM_NOT_ENOUGH_MECHANICS,
                    "%required%", String.valueOf(getMinMechanics()),
                    "%online%", String.valueOf(onlineMechanics));
            return false;
        }

        // Validate pit stops vs laps
        if (requiredPits > 0 && !track.getTrackRegions().hasRegion(TrackRegion.RegionType.SERVICEPARK)) {
            Text.send(pilot, Error.TEAM_NO_PIT_ZONE);
            return false;
        }

        // Create session
        TeamRaceSession session = new TeamRaceSession(
                team.getId(), pilot.getUniqueId(), track, laps, requiredPits);
        activeSessions.put(pilot.getUniqueId(), session);

        // Teleport pilot to start
        teleportToStart(pilot, track);
        applyCarConfiguration(pilot, track);
        applyTrackEnvironment(pilot, track);

        // Enable damage system
        DamageWearManager.enableForPlayer(pilot);

        session.setState(TeamRaceSession.State.COUNTDOWN);
        startCountdown(pilot, session);

        // Notify mechanics
        for (UUID mechUuid : mechanicUuids) {
            Player mech = Bukkit.getPlayer(mechUuid);
            if (mech != null && mech.isOnline()) {
                Text.send(mech, Info.TEAM_RACE_STARTED_MECHANIC,
                        "%track%", track.getDisplayName(),
                        "%laps%", String.valueOf(laps),
                        "%pits%", String.valueOf(requiredPits));
            }
        }

        return true;
    }

    // ─── COUNTDOWN ───

    private static void startCountdown(Player pilot, TeamRaceSession session) {
        savedWalkSpeeds.putIfAbsent(pilot.getUniqueId(), pilot.getWalkSpeed());
        pilot.setWalkSpeed(0f);

        long goTimeMs = System.currentTimeMillis() + (COUNTDOWN_SECONDS * 1000L);
        CustomBoatUtilsMode.sendRaceCountdownPacket(pilot, goTimeMs, COUNTDOWN_SECONDS);

        TaskChain<?> chain = TimingSystem.newChain();

        for (int i = COUNTDOWN_SECONDS; i > 0; i--) {
            int count = i;
            chain.sync(() -> {
                if (!session.isActive()) return;
                Player p = Bukkit.getPlayer(session.getPilotUuid());
                if (p == null) {
                    cancelTeamRace(session.getPilotUuid());
                    return;
                }
                if (count >= 4) {
                    Text.send(p, Broadcast.RACE_COUNTDOWN_RED, "%count%", String.valueOf(count));
                } else {
                    Text.send(p, Broadcast.RACE_COUNTDOWN_YELLOW, "%count%", String.valueOf(count));
                }
            }).delay(20);
        }

        chain.execute((finished) -> {
            if (!session.isActive()) return;
            Player p = Bukkit.getPlayer(session.getPilotUuid());
            if (p == null) {
                cancelTeamRace(session.getPilotUuid());
                return;
            }

            Float savedSpeed = savedWalkSpeeds.remove(p.getUniqueId());
            p.setWalkSpeed(savedSpeed != null ? savedSpeed : 0.2f);

            session.setState(TeamRaceSession.State.RACING);
            session.setStartTime(TimingSystem.currentTime);
            Text.send(p, Broadcast.RACE_GO);
            Text.send(p, Success.TEAM_RACE_STARTED,
                    "%track%", session.getTrack().getDisplayName(),
                    "%laps%", String.valueOf(session.getTotalLaps()));

            scheduleTimeout(session);
        });
    }

    // ─── PIT STOP INTEGRATION ───

    /**
     * Handle pilot entering a SERVICEPARK region during a team race.
     */
    public static void handleServiceParkEntry(Player pilot) {
        UUID pilotUuid = pilot.getUniqueId();
        TeamRaceSession session = activeSessions.get(pilotUuid);
        if (session == null || session.getState() != TeamRaceSession.State.RACING) return;

        if (PitStopManager.isPilotInPitStop(pilotUuid)) return;

        Optional<Team> maybeTeam = TeamManager.getTeam(session.getTeamId());
        if (maybeTeam.isEmpty()) return;

        Team team = maybeTeam.get();

        // Start pit stop
        session.setState(TeamRaceSession.State.IN_PIT_STOP);
        PitStopManager.startPitStop(pilotUuid, session.getTeamId(), team);

        // Set damage service zone
        DamageWearManager.setServiceZone(pilot, true);
    }

    /**
     * Handle pilot leaving a SERVICEPARK region during a team race.
     */
    public static void handleServiceParkExit(Player pilot) {
        UUID pilotUuid = pilot.getUniqueId();
        TeamRaceSession session = activeSessions.get(pilotUuid);
        if (session == null) return;

        if (PitStopManager.isPilotInPitStop(pilotUuid)) {
            // Force end the pit stop if pilot leaves the zone
            PitStopManager.forceEndPitStop(pilotUuid, false);
        }

        if (session.getState() == TeamRaceSession.State.IN_PIT_STOP) {
            session.setState(TeamRaceSession.State.RACING);
        }

        DamageWearManager.setServiceZone(pilot, false);
    }

    /**
     * Called by PitStopManager when a pit stop completes.
     */
    public static void recordPitStop(UUID pilotUuid, long pitTimeMs, long penaltyMs) {
        TeamRaceSession session = activeSessions.get(pilotUuid);
        if (session == null) return;

        session.recordPitStop(pitTimeMs, penaltyMs);

        if (session.getState() == TeamRaceSession.State.IN_PIT_STOP) {
            session.setState(TeamRaceSession.State.RACING);
        }

        Player pilot = Bukkit.getPlayer(pilotUuid);
        if (pilot != null) {
            Text.send(pilot, Info.TEAM_RACE_PIT_RECORDED,
                    "%pitTime%", ApiUtilities.formatAsTime(pitTimeMs),
                    "%pitCount%", String.valueOf(session.getPitStopsCompleted()),
                    "%required%", String.valueOf(session.getRequiredPitStops()));
        }
    }

    // ─── LAP COMPLETION ───

    /**
     * Handle pilot completing a lap (crossing END region).
     */
    public static void handleLapCompletion(Player pilot) {
        UUID pilotUuid = pilot.getUniqueId();
        TeamRaceSession session = activeSessions.get(pilotUuid);
        if (session == null || session.getState() != TeamRaceSession.State.RACING) return;

        boolean finalLap = session.completeLap();

        if (finalLap) {
            // Check mandatory pit stops
            if (session.needsMorePitStops()) {
                Text.send(pilot, Warning.TEAM_RACE_PITS_REQUIRED,
                        "%completed%", String.valueOf(session.getPitStopsCompleted()),
                        "%required%", String.valueOf(session.getRequiredPitStops()));
                // Don't finish — roll back the lap
                session.setCurrentLap(session.getCurrentLap() - 1);
                return;
            }
            finishTeamRace(pilotUuid);
        } else {
            Text.send(pilot, Info.TEAM_RACE_LAP_COMPLETE,
                    "%lap%", String.valueOf(session.getCurrentLap()),
                    "%total%", String.valueOf(session.getTotalLaps()));
            pilot.playSound(pilot.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        }
    }

    // ─── RACE FINISH ───

    public static void finishTeamRace(UUID pilotUuid) {
        TeamRaceSession session = activeSessions.get(pilotUuid);
        if (session == null || session.getState() == TeamRaceSession.State.FINISHED) return;

        session.setState(TeamRaceSession.State.FINISHED);
        session.setEndTime(TimingSystem.currentTime);

        Player pilot = Bukkit.getPlayer(pilotUuid);
        if (pilot != null) {
            long raceTimeMs = session.getRaceTimeMs();
            long totalTimeMs = session.getTotalTimeMs();
            long pitTimeMs = session.getTotalPitTimeMs();
            long penaltyMs = session.getTotalPenaltyMs();

            Text.send(pilot, Success.TEAM_RACE_FINISHED,
                    "%time%", ApiUtilities.formatAsTime(totalTimeMs),
                    "%track%", session.getTrack().getDisplayName());

            Text.send(pilot, Info.TEAM_RACE_SUMMARY,
                    "%raceTime%", ApiUtilities.formatAsTime(raceTimeMs),
                    "%pitTime%", ApiUtilities.formatAsTime(pitTimeMs),
                    "%penalty%", ApiUtilities.formatAsTime(penaltyMs),
                    "%totalTime%", ApiUtilities.formatAsTime(totalTimeMs),
                    "%pitCount%", String.valueOf(session.getPitStopsCompleted()));

            if (session.getBestPitTimeMs() > 0) {
                Text.send(pilot, Info.TEAM_RACE_PIT_STATS,
                        "%best%", ApiUtilities.formatAsTime(session.getBestPitTimeMs()),
                        "%avg%", ApiUtilities.formatAsTime(session.getAveragePitTimeMs()));
            }

            saveTeamRaceResult(session);
            awardTeamRaceRewards(pilot, session);
            resetTrackEnvironment(pilot);
            DamageWearManager.disableForPlayer(pilot);
            pilot.playSound(pilot.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        // Notify mechanics
        notifyMechanics(session, "Race finished!");

        activeSessions.remove(pilotUuid);
    }

    // ─── RACE CANCEL ───

    public static boolean cancelTeamRace(UUID pilotUuid) {
        TeamRaceSession session = activeSessions.remove(pilotUuid);
        if (session == null) return false;

        session.setState(TeamRaceSession.State.CANCELLED);

        // Cancel any active pit stop
        PitStopManager.cancelForPlayer(pilotUuid);

        Player pilot = Bukkit.getPlayer(pilotUuid);
        if (pilot != null) {
            Float savedSpeed = savedWalkSpeeds.remove(pilotUuid);
            pilot.setWalkSpeed(savedSpeed != null ? savedSpeed : 0.2f);

            CustomBoatUtilsMode.sendRaceCountdownPacket(pilot, 0, 0);
            resetTrackEnvironment(pilot);
            DamageWearManager.disableForPlayer(pilot);
            Text.send(pilot, Success.RACE_CANCELLED);
        }

        notifyMechanics(session, "Team race cancelled.");
        savedWalkSpeeds.remove(pilotUuid);
        return true;
    }

    // ─── TIMEOUT ───

    private static void scheduleTimeout(TeamRaceSession session) {
        int timeoutTicks = TIMEOUT_MINUTES * 60 * 20;
        Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> {
            if (session.isActive()) {
                Player p = Bukkit.getPlayer(session.getPilotUuid());
                if (p != null) {
                    Text.send(p, Error.RACE_TIMEOUT);
                }
                cancelTeamRace(session.getPilotUuid());
            }
        }, timeoutTicks);
    }

    // ─── RESULTS ───

    private static void saveTeamRaceResult(TeamRaceSession session) {
        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> {
            try {
                DB.executeInsert(
                        "INSERT INTO ts_team_race_results (team_id, track_id, pilot_uuid, race_time_ms, pit_time_ms, penalty_ms, total_time_ms, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        session.getTeamId(),
                        session.getTrack().getId(),
                        session.getPilotUuid().toString(),
                        session.getRaceTimeMs(),
                        session.getTotalPitTimeMs(),
                        session.getTotalPenaltyMs(),
                        session.getTotalTimeMs(),
                        ApiUtilities.getTimestamp()
                );
            } catch (SQLException e) {
                TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to save team race result", e);
            }
        });
    }

    /**
     * Get top team race results for a track.
     */
    public static List<DbRow> getTopTeamResults(int trackId, int limit) {
        try {
            return DB.getResults(
                    "SELECT team_id, pilot_uuid, MIN(total_time_ms) as best_time, race_time_ms, pit_time_ms, penalty_ms FROM ts_team_race_results WHERE track_id = ? GROUP BY team_id ORDER BY best_time ASC LIMIT ?",
                    trackId, limit
            );
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get team race results", e);
            return Collections.emptyList();
        }
    }

    // ─── REWARDS ───

    private static void awardTeamRaceRewards(Player pilot, TeamRaceSession session) {
        boolean economyEnabled = TimingSystem.getPlugin().getConfig().getBoolean("economy.enabled", true);
        boolean levelsEnabled = TimingSystem.getPlugin().getConfig().getBoolean("levels.enabled", true);
        Track track = session.getTrack();

        if (economyEnabled) {
            int baseCoins = TimingSystem.getPlugin().getConfig().getInt("team_race.rewards.coins", TEAM_RACE_COINS);
            int coins = Math.round(baseCoins * track.getDifficultyCoinMultiplier());
            RallyCoinManager.addCoins(pilot.getUniqueId(), coins, "Team Race: " + track.getDisplayName());
            Text.send(pilot, Info.ECONOMY_COINS_REWARD, "%amount%", String.valueOf(coins));
        }

        if (levelsEnabled) {
            int baseXp = TimingSystem.getPlugin().getConfig().getInt("team_race.rewards.xp", TEAM_RACE_XP);
            int xp = Math.round(baseXp * track.getDifficultyXpMultiplier());
            LevelManager.addXP(pilot.getUniqueId(), xp, "Team Race: " + track.getDisplayName());
            Text.send(pilot, Info.ECONOMY_XP_REWARD, "%amount%", String.valueOf(xp));
        }
    }

    // ─── HELPERS ───

    private static void teleportToStart(Player pilot, Track track) {
        List<TrackLocation> grids = track.getTrackLocations().getLocations(TrackLocation.Type.GRID);
        if (!grids.isEmpty()) {
            Location startLoc = grids.get(0).getLocation();
            if (startLoc != null) {
                pilot.teleport(startLoc);
            }
        }
    }

    private static void applyCarConfiguration(Player pilot, Track track) {
        if (track.getBoatUtilsMode() != null) {
            BoatUtilsManager.sendBoatUtilsModePluginMessage(pilot, track.getBoatUtilsMode(), track, false);
        }
        Integer customModeId = track.getCustomBoatUtilsModeId();
        if (customModeId != null) {
            CustomBoatUtilsMode mode = TimingSystem.getTrackDatabase().getCustomBoatUtilsModeFromId(customModeId);
            if (mode != null) {
                mode.applyToPlayer(pilot);
            }
        }
    }

    private static void applyTrackEnvironment(Player pilot, Track track) {
        if (track.getWeatherCondition() != TrackWeather.CLEAR) {
            CustomBoatUtilsMode.sendWeatherConditionPacket(pilot, (short) track.getWeatherCondition().getId());
            if (track.getWeatherCondition() == TrackWeather.RAIN
                    || track.getWeatherCondition() == TrackWeather.HEAVY_RAIN
                    || track.getWeatherCondition() == TrackWeather.SNOW) {
                pilot.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL);
            }
        }
        if (track.getTrackTime() != null) {
            pilot.setPlayerTime(track.getTrackTime(), false);
        }
    }

    private static void resetTrackEnvironment(Player pilot) {
        CustomBoatUtilsMode.sendWeatherConditionPacket(pilot, (short) TrackWeather.CLEAR.getId());
        pilot.resetPlayerWeather();
        pilot.resetPlayerTime();
    }

    private static void notifyMechanics(TeamRaceSession session, String message) {
        Optional<Team> maybeTeam = TeamManager.getTeam(session.getTeamId());
        if (maybeTeam.isEmpty()) return;
        Team team = maybeTeam.get();
        for (UUID mechUuid : team.getMechanicUuids()) {
            Player mech = Bukkit.getPlayer(mechUuid);
            if (mech != null && mech.isOnline()) {
                mech.sendMessage("§b[Team Race] §f" + message);
            }
        }
    }

    // ─── CLEANUP ───

    public static void onShutdown() {
        for (UUID uuid : new ArrayList<>(activeSessions.keySet())) {
            cancelTeamRace(uuid);
        }
        PitStopManager.shutdown();
        savedWalkSpeeds.clear();
    }
}
