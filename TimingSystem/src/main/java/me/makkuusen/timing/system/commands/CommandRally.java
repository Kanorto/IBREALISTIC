package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.race.*;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.entity.Player;

/**
 * Commands for managing multi-stage rally events.
 */
@CommandAlias("rally")
public class CommandRally extends BaseCommand {

    @Subcommand("create")
    @CommandPermission("timingsystem.admin")
    @Description("Create a new rally event")
    @Syntax("<name>")
    @CommandCompletion("<name>")
    public void onCreate(Player player, String name) {
        RallyEvent event = RallyManager.createRally(name, player.getUniqueId());
        if (event == null) {
            Text.send(player, Error.RALLY_ALREADY_EXISTS);
            return;
        }
        Text.send(player, Success.RALLY_CREATED, "%name%", name, "%id%", String.valueOf(event.getId()));
    }

    @Subcommand("addstage")
    @CommandCompletion("@nothing @track")
    @CommandPermission("timingsystem.admin")
    @Description("Add a stage to a rally")
    @Syntax("<rallyId> <track>")
    public void onAddStage(Player player, int rallyId, Track track) {
        RallyEvent event = RallyManager.getRally(rallyId);
        if (event == null) {
            Text.send(player, Error.RALLY_NOT_FOUND);
            return;
        }
        if (!RallyManager.addStage(rallyId, track)) {
            Text.send(player, Error.RALLY_NOT_FOUND);
            return;
        }
        Text.send(player, Success.RALLY_STAGE_ADDED,
                "%index%", String.valueOf(event.getTotalStages()),
                "%track%", track.getDisplayName());
    }

    @Subcommand("removestage")
    @CommandPermission("timingsystem.admin")
    @Description("Remove a stage from a rally")
    @Syntax("<rallyId> <stageNumber>")
    @CommandCompletion("@nothing <stageNumber>")
    public void onRemoveStage(Player player, int rallyId, int stageNumber) {
        // Convert 1-based user input to 0-based index
        int stageIndex = stageNumber - 1;
        if (!RallyManager.removeStage(rallyId, stageIndex)) {
            Text.send(player, Error.RALLY_NOT_FOUND);
            return;
        }
        Text.send(player, Success.RALLY_STAGE_REMOVED, "%index%", String.valueOf(stageNumber));
    }

    @Subcommand("start")
    @CommandPermission("timingsystem.admin")
    @Description("Start a rally event")
    @Syntax("<rallyId>")
    @CommandCompletion("@nothing")
    public void onStart(Player player, int rallyId) {
        RallyEvent event = RallyManager.getRally(rallyId);
        if (event == null) {
            Text.send(player, Error.RALLY_NOT_FOUND);
            return;
        }
        if (!RallyManager.startRally(player, rallyId)) {
            Text.send(player, Error.RALLY_NOT_ACTIVE);
            return;
        }
        Text.send(player, Success.RALLY_STARTED, "%name%", event.getName());
    }

    @Subcommand("join")
    @CommandPermission("%permissionrace_solo")
    @Description("Join a rally event")
    @Syntax("<rallyId>")
    @CommandCompletion("@nothing")
    public void onJoin(Player player, int rallyId) {
        RallyEvent event = RallyManager.getRally(rallyId);
        if (event == null) {
            Text.send(player, Error.RALLY_NOT_FOUND);
            return;
        }
        if (!RallyManager.joinRally(player, rallyId)) {
            return; // Error messages sent inside joinRally
        }
        Text.send(player, Success.RALLY_JOINED, "%name%", event.getName());
    }

    @Subcommand("retire")
    @CommandPermission("%permissionrace_solo")
    @Description("Retire from current rally")
    public void onRetire(Player player) {
        RallyParticipant participant = RallyManager.getParticipant(player.getUniqueId());
        if (participant == null) {
            Text.send(player, Error.RALLY_NOT_JOINED);
            return;
        }
        RallyManager.superRally(player.getUniqueId());
        Text.send(player, Success.RALLY_RETIRED);
    }

    @Subcommand("results")
    @Description("View rally results")
    @Syntax("<rallyId>")
    @CommandCompletion("@nothing")
    public void onResults(Player player, int rallyId) {
        RallyManager.showResults(player, rallyId);
    }

    @Subcommand("info")
    @Description("View rally information")
    @Syntax("<rallyId>")
    @CommandCompletion("@nothing")
    public void onInfo(Player player, int rallyId) {
        RallyManager.showInfo(player, rallyId);
    }

    @Subcommand("delete")
    @CommandPermission("timingsystem.admin")
    @Description("Delete a rally event")
    @Syntax("<rallyId>")
    @CommandCompletion("@nothing")
    public void onDelete(Player player, int rallyId) {
        RallyEvent event = RallyManager.getRally(rallyId);
        if (event == null) {
            Text.send(player, Error.RALLY_NOT_FOUND);
            return;
        }
        String name = event.getName();
        if (!RallyManager.deleteRally(rallyId)) {
            Text.send(player, Error.RALLY_NOT_FOUND);
            return;
        }
        Text.send(player, Success.RALLY_DELETED, "%name%", name);
    }
}
