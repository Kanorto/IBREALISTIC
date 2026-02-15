package me.makkuusen.timing.system.race;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.boatutils.BoatUtilsManager;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.locations.TrackLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages recce (reconnaissance) sessions for track exploration.
 */
public class RecceManager {

    private static final Map<UUID, RecceSession> activeSessions = new ConcurrentHashMap<>();

    public static boolean isEnabled() {
        return TimingSystem.getPlugin().getConfig().getBoolean("race.recce.enabled", true);
    }

    public static float getSpeedMultiplier() {
        return (float) TimingSystem.getPlugin().getConfig().getDouble("race.recce.speed_multiplier", 0.5);
    }

    public static Optional<RecceSession> getSession(UUID playerUuid) {
        return Optional.ofNullable(activeSessions.get(playerUuid));
    }

    public static boolean isInRecce(UUID playerUuid) {
        return activeSessions.containsKey(playerUuid);
    }

    /**
     * Starts a recce session for the given player on the given track.
     */
    public static boolean startRecce(Player player, Track track) {
        if (!isEnabled()) {
            Text.send(player, Error.RECCE_NOT_ENABLED);
            return false;
        }

        if (isInRecce(player.getUniqueId())) {
            Text.send(player, Error.RECCE_ALREADY_ACTIVE);
            return false;
        }

        if (SoloRaceManager.isInRace(player.getUniqueId())) {
            Text.send(player, Error.RACE_ALREADY_ACTIVE);
            return false;
        }

        if (!track.isOpen()) {
            Text.send(player, Error.TRACK_IS_CLOSED);
            return false;
        }

        RecceSession session = new RecceSession(player.getUniqueId(), track);
        session.setSpeedMultiplier(getSpeedMultiplier());
        activeSessions.put(player.getUniqueId(), session);

        teleportToStart(player, track);

        if (track.getBoatUtilsMode() != null) {
            BoatUtilsManager.sendBoatUtilsModePluginMessage(player, track.getBoatUtilsMode(), track, false);
        }

        player.setWalkSpeed(0.2f * session.getSpeedMultiplier());

        Text.send(player, Success.RECCE_STARTED, "%track%", track.getDisplayName());
        Text.send(player, Info.RECCE_SPEED_LIMIT,
                "%speed%", String.valueOf((int) (session.getSpeedMultiplier() * 100)));
        Text.send(player, Info.RECCE_INSTRUCTIONS);

        return true;
    }

    /**
     * Ends a recce session for the given player.
     */
    public static boolean endRecce(UUID playerUuid) {
        RecceSession session = activeSessions.remove(playerUuid);
        if (session == null) return false;

        session.setActive(false);
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            player.setWalkSpeed(0.2f);

            Text.send(player, Success.RECCE_ENDED, "%track%", session.getTrack().getDisplayName());

            if (!session.getPaceNotes().isEmpty()) {
                Text.send(player, Info.RECCE_NOTES_TITLE, "%count%", String.valueOf(session.getPaceNotes().size()));
                int idx = 1;
                for (String note : session.getPaceNotes()) {
                    Text.send(player, Info.RECCE_NOTE_ENTRY,
                            "%index%", String.valueOf(idx++),
                            "%note%", note);
                }
            }
        }

        return true;
    }

    /**
     * Adds a pace note to the player's current recce session.
     */
    public static boolean addPaceNote(Player player, String note) {
        Optional<RecceSession> maybeSession = getSession(player.getUniqueId());
        if (maybeSession.isEmpty()) {
            Text.send(player, Error.RECCE_NOT_ACTIVE);
            return false;
        }

        maybeSession.get().addPaceNote(note);
        Text.send(player, Success.RECCE_NOTE_ADDED, "%note%", note);
        return true;
    }

    private static void teleportToStart(Player player, Track track) {
        List<TrackLocation> grids = track.getTrackLocations().getLocations(TrackLocation.Type.GRID);
        if (!grids.isEmpty()) {
            Location startLoc = grids.get(0).getLocation();
            if (startLoc != null) {
                player.teleport(startLoc);
            }
        }
    }

    public static void onShutdown() {
        for (UUID uuid : new ArrayList<>(activeSessions.keySet())) {
            endRecce(uuid);
        }
    }
}
