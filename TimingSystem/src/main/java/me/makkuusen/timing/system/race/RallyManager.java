package me.makkuusen.timing.system.race;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages multi-stage rally events: creation, participation, stage progression,
 * and standings calculation.
 */
public class RallyManager {

    private static final Map<Integer, RallyEvent> events = new ConcurrentHashMap<>();
    private static final Map<UUID, RallyParticipant> participants = new ConcurrentHashMap<>();

    private static long getSuperRallyPenaltyMs() {
        int minutes = TimingSystem.getPlugin().getConfig().getInt("race.rally.super_rally_penalty_minutes", 5);
        return minutes * 60 * 1000L;
    }

    // ─── EVENT MANAGEMENT ───

    public static RallyEvent createRally(String name, UUID creator) {
        try {
            long id = DB.executeInsert(
                    "INSERT INTO `ts_rally_events` (`name`, `creator_uuid`, `state`, `created_at`) VALUES (?, ?, ?, ?);",
                    name, creator.toString(), RallyEventState.SETUP.name(), ApiUtilities.getTimestamp()
            );
            RallyEvent event = new RallyEvent((int) id, name, creator);
            events.put(event.getId(), event);
            return event;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to create rally event", e);
            return null;
        }
    }

    public static boolean addStage(int rallyId, Track track) {
        RallyEvent event = events.get(rallyId);
        if (event == null || event.getState() != RallyEventState.SETUP) {
            return false;
        }
        int stageIndex = event.getTotalStages();
        String stageName = "SS" + (stageIndex + 1);
        try {
            DB.executeInsert(
                    "INSERT INTO `ts_rally_stages` (`rally_id`, `stage_index`, `track_id`, `stage_name`) VALUES (?, ?, ?, ?);",
                    rallyId, stageIndex, track.getId(), stageName
            );
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to add rally stage", e);
            return false;
        }
        event.addStage(track);
        return true;
    }

    public static boolean removeStage(int rallyId, int stageIndex) {
        RallyEvent event = events.get(rallyId);
        if (event == null || event.getState() != RallyEventState.SETUP) {
            return false;
        }
        if (!event.removeStage(stageIndex)) {
            return false;
        }
        try {
            DB.executeUpdate("DELETE FROM `ts_rally_stages` WHERE `rally_id` = ? AND `stage_index` = ?;",
                    rallyId, stageIndex);
            // Re-index remaining stages in DB
            for (RallyStage stage : event.getStages()) {
                DB.executeUpdate(
                        "UPDATE `ts_rally_stages` SET `stage_index` = ?, `stage_name` = ? WHERE `rally_id` = ? AND `track_id` = ?;",
                        stage.getStageIndex(), stage.getName(), rallyId, stage.getTrack().getId()
                );
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to remove rally stage", e);
        }
        return true;
    }

    public static boolean deleteRally(int rallyId) {
        RallyEvent event = events.remove(rallyId);
        if (event == null) {
            return false;
        }
        // Remove participants for this rally
        participants.values().removeIf(p -> p.getRallyId() == rallyId);
        try {
            DB.executeUpdate("DELETE FROM `ts_rally_stages` WHERE `rally_id` = ?;", rallyId);
            DB.executeUpdate("DELETE FROM `ts_rally_events` WHERE `id` = ?;", rallyId);
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to delete rally event", e);
        }
        return true;
    }

    // ─── RALLY LIFECYCLE ───

    public static boolean startRally(Player admin, int rallyId) {
        RallyEvent event = events.get(rallyId);
        if (event == null || event.getState() != RallyEventState.SETUP) {
            return false;
        }
        if (event.getTotalStages() == 0) {
            Text.send(admin, Error.RALLY_NO_STAGES);
            return false;
        }
        event.setState(RallyEventState.ACTIVE);
        event.setCurrentStageIndex(0);
        try {
            DB.executeUpdate("UPDATE `ts_rally_events` SET `state` = ? WHERE `id` = ?;",
                    RallyEventState.ACTIVE.name(), rallyId);
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to start rally", e);
        }
        return true;
    }

    public static boolean joinRally(Player player, int rallyId) {
        RallyEvent event = events.get(rallyId);
        if (event == null) {
            return false;
        }
        if (event.getState() != RallyEventState.ACTIVE) {
            Text.send(player, Error.RALLY_NOT_ACTIVE);
            return false;
        }
        if (participants.containsKey(player.getUniqueId())) {
            RallyParticipant existing = participants.get(player.getUniqueId());
            if (existing.getRallyId() == rallyId) {
                Text.send(player, Error.RALLY_ALREADY_JOINED);
                return false;
            }
        }
        RallyParticipant participant = new RallyParticipant(player.getUniqueId(), rallyId);
        participant.setCurrentStageIndex(event.getCurrentStageIndex());
        participants.put(player.getUniqueId(), participant);
        return true;
    }

    // ─── STAGE PROGRESSION ───

    public static void completeStage(UUID playerUuid, long stageTimeMs) {
        RallyParticipant participant = participants.get(playerUuid);
        if (participant == null) {
            return;
        }
        RallyEvent event = events.get(participant.getRallyId());
        if (event == null) {
            return;
        }

        participant.recordStageTime(participant.getCurrentStageIndex(), stageTimeMs);
        int completedIndex = participant.getCurrentStageIndex() + 1;
        participant.setCurrentStageIndex(completedIndex);

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            Text.send(player, Info.RALLY_STAGE_COMPLETE,
                    "%index%", String.valueOf(completedIndex),
                    "%time%", ApiUtilities.formatAsTime(stageTimeMs));
        }
    }

    public static boolean advanceToNextStage(int rallyId) {
        RallyEvent event = events.get(rallyId);
        if (event == null || event.getState() != RallyEventState.ACTIVE) {
            return false;
        }
        if (!event.hasNextStage()) {
            // Rally finished
            event.setState(RallyEventState.FINISHED);
            try {
                DB.executeUpdate("UPDATE `ts_rally_events` SET `state` = ? WHERE `id` = ?;",
                        RallyEventState.FINISHED.name(), rallyId);
            } catch (SQLException e) {
                TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to finish rally", e);
            }
            return true;
        }
        event.advanceStage();
        return true;
    }

    public static void superRally(UUID playerUuid) {
        RallyParticipant participant = participants.get(playerUuid);
        if (participant == null) {
            return;
        }
        participant.setRetired(true);
        participant.setTotalPenaltyMs(participant.getTotalPenaltyMs() + getSuperRallyPenaltyMs());
        // Record a zero time for current stage — penalty is tracked separately
        participant.recordStageTime(participant.getCurrentStageIndex(), 0);
        int completedIndex = participant.getCurrentStageIndex() + 1;
        participant.setCurrentStageIndex(completedIndex);
        participant.setRetired(false);
    }

    // ─── STANDINGS & RESULTS ───

    public static List<RallyParticipant> getStandings(int rallyId) {
        return participants.values().stream()
                .filter(p -> p.getRallyId() == rallyId)
                .sorted(Comparator.comparingLong(RallyParticipant::getTotalTimeMs))
                .collect(Collectors.toList());
    }

    public static void showResults(Player player, int rallyId) {
        RallyEvent event = events.get(rallyId);
        if (event == null) {
            Text.send(player, Error.RALLY_NOT_FOUND);
            return;
        }
        List<RallyParticipant> standings = getStandings(rallyId);
        if (standings.isEmpty()) {
            Text.send(player, Info.RALLY_NO_RESULTS);
            return;
        }
        Text.send(player, Info.RALLY_RESULTS_TITLE, "%name%", event.getName());
        int pos = 1;
        for (RallyParticipant p : standings) {
            String playerName = Bukkit.getOfflinePlayer(p.getPlayerUuid()).getName();
            if (playerName == null) playerName = p.getPlayerUuid().toString();
            Text.send(player, Info.RALLY_RESULTS_ENTRY,
                    "%pos%", String.valueOf(pos),
                    "%player%", playerName,
                    "%time%", ApiUtilities.formatAsTime(p.getTotalTimeMs()),
                    "%stages%", String.valueOf(p.getCompletedStages()));
            pos++;
        }
    }

    // ─── INFO ───

    public static void showInfo(Player player, int rallyId) {
        RallyEvent event = events.get(rallyId);
        if (event == null) {
            Text.send(player, Error.RALLY_NOT_FOUND);
            return;
        }
        Text.send(player, Info.RALLY_INFO_TITLE, "%name%", event.getName());
        for (RallyStage stage : event.getStages()) {
            String stateStr = stage.getStageIndex() < event.getCurrentStageIndex() ? "done"
                    : stage.getStageIndex() == event.getCurrentStageIndex() ? "current"
                    : "upcoming";
            Text.send(player, Info.RALLY_INFO_STAGE,
                    "%index%", String.valueOf(stage.getStageIndex() + 1),
                    "%track%", stage.getTrack().getDisplayName(),
                    "%state%", stateStr);
        }
    }

    // ─── QUERIES ───

    public static RallyEvent getRally(int rallyId) {
        return events.get(rallyId);
    }

    public static RallyParticipant getParticipant(UUID playerUuid) {
        return participants.get(playerUuid);
    }

    // ─── LIFECYCLE ───

    public static void loadFromDatabase() {
        try {
            List<DbRow> rows = DB.getResults("SELECT * FROM `ts_rally_events`;");
            for (DbRow row : rows) {
                int id = row.getInt("id");
                String name = row.getString("name");
                UUID creator = UUID.fromString(row.getString("creator_uuid"));
                RallyEvent event = new RallyEvent(id, name, creator);
                event.setState(RallyEventState.valueOf(row.getString("state")));
                events.put(id, event);
            }
            List<DbRow> stageRows = DB.getResults("SELECT * FROM `ts_rally_stages` ORDER BY `rally_id`, `stage_index`;");
            for (DbRow row : stageRows) {
                int rallyId = row.getInt("rally_id");
                int trackId = row.getInt("track_id");
                RallyEvent event = events.get(rallyId);
                if (event == null) continue;
                Track track = TrackDatabase.getTrackById(trackId).orElse(null);
                if (track == null) continue;
                int stageIndex = row.getInt("stage_index");
                String stageName = row.getString("stage_name");
                event.getStages().add(new RallyStage(stageIndex, track, stageName));
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to load rally events from database", e);
        }
    }

    public static void onShutdown() {
        events.clear();
        participants.clear();
    }
}
