package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.permissions.PermissionTeam;
import me.makkuusen.timing.system.team.*;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.theme.messages.Warning;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Command handler for team management operations.
 * Phase 20: Extended with invite, role, race, and pit stop commands.
 */
@CommandAlias("team")
public class CommandTeam extends BaseCommand {

    @Subcommand("create")
    @CommandPermission("%permissionteam_create")
    @Syntax("<teamName>")
    @Description("Create a new team")
    public void onTeamCreate(Player player, String teamName) {
        if (!Team.isValidTeamName(teamName)) {
            Text.send(player, Error.INVALID_NAME);
            return;
        }

        if (!TeamManager.isTeamNameAvailable(teamName)) {
            Text.send(player, Error.TEAM_NAME_TAKEN, "%name%", teamName);
            return;
        }

        // Check if player already owns a team
        java.util.Optional<Team> existing = TeamManager.getPlayerTeam(player.getUniqueId());
        if (existing.isPresent()) {
            Text.send(player, Error.TEAM_ALREADY_IN_TEAM);
            return;
        }

        Team team = TeamManager.createTeam(teamName, player.getUniqueId());
        if (team == null) {
            Text.send(player, Error.FAILED_TO_CREATE_TEAM);
            return;
        }

        // Add creator as a member with PILOT role
        TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        if (tPlayer != null) {
            TeamManager.addPlayerToTeam(team, tPlayer);
            TeamManager.setPlayerRole(team, player.getUniqueId(), TeamRole.PILOT);
        }

        Text.send(player, Success.TEAM_CREATED, "%name%", teamName);
    }

    @Subcommand("delete|disband")
    @CommandCompletion("@teams")
    @CommandPermission("%permissionteam_delete")
    @Syntax("<team>")
    @Description("Delete/disband a team")
    public void onTeamDelete(Player player, Team team) {
        // Only owner or admin can delete
        if (!team.getCreatorUuid().equals(player.getUniqueId())
                && !player.hasPermission("timingsystem.team.admin")) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }

        String teamName = team.getDisplayName();
        if (TeamManager.deleteTeam(team)) {
            Text.send(player, Success.TEAM_DELETED, "%name%", teamName);
        } else {
            Text.send(player, Error.FAILED_TO_DELETE_TEAM, "%name%", teamName);
        }
    }

    @Subcommand("invite")
    @CommandCompletion("@players")
    @CommandPermission("%permissionteam_invite")
    @Syntax("<player>")
    @Description("Invite a player to your team")
    public void onTeamInvite(Player player, String playerName) {
        java.util.Optional<Team> maybeTeam = TeamManager.getPlayerTeam(player.getUniqueId());
        if (maybeTeam.isEmpty()) {
            Text.send(player, Error.TEAM_NOT_FOUND);
            return;
        }

        Team team = maybeTeam.get();

        // Only owner can invite
        if (!team.getCreatorUuid().equals(player.getUniqueId())
                && !player.hasPermission("timingsystem.team.admin")) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }

        if (team.isFull()) {
            Text.send(player, Error.TEAM_MAX_MEMBERS);
            return;
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            Text.send(player, Error.PLAYER_NOT_FOUND);
            return;
        }

        TPlayer tTarget = TSDatabase.getPlayer(target.getUniqueId());
        if (tTarget != null && team.hasPlayer(tTarget)) {
            Text.send(player, Error.PLAYER_ALREADY_IN_TEAM, "%player%", playerName, "%team%", team.getDisplayName());
            return;
        }

        if (TeamManager.invitePlayer(team, target.getUniqueId())) {
            Text.send(player, Success.TEAM_INVITE_SENT, "%player%", target.getName(), "%team%", team.getDisplayName());
            Text.send(target, Info.TEAM_INVITE_RECEIVED, "%team%", team.getDisplayName(), "%player%", player.getName());
        } else {
            Text.send(player, Error.GENERIC);
        }
    }

    @Subcommand("accept")
    @CommandPermission("%permissionteam_create")
    @Description("Accept a team invite")
    public void onTeamAccept(Player player) {
        if (!TeamManager.hasPendingInvite(player.getUniqueId())) {
            Text.send(player, Error.TEAM_NO_PENDING_INVITE);
            return;
        }

        Team team = TeamManager.acceptInvite(player.getUniqueId());
        if (team != null) {
            Text.send(player, Success.TEAM_INVITE_ACCEPTED, "%team%", team.getDisplayName());
            // Notify team owner
            Player owner = Bukkit.getPlayer(team.getCreatorUuid());
            if (owner != null) {
                Text.send(owner, Success.PLAYER_ADDED_TO_TEAM,
                        "%player%", player.getName(), "%team%", team.getDisplayName());
            }
        } else {
            Text.send(player, Error.GENERIC);
        }
    }

    @Subcommand("decline")
    @CommandPermission("%permissionteam_create")
    @Description("Decline a team invite")
    public void onTeamDecline(Player player) {
        if (!TeamManager.hasPendingInvite(player.getUniqueId())) {
            Text.send(player, Error.TEAM_NO_PENDING_INVITE);
            return;
        }

        TeamManager.declineInvite(player.getUniqueId());
        Text.send(player, Success.TEAM_INVITE_DECLINED);
    }

    @Subcommand("kick")
    @CommandCompletion("@teamplayers")
    @CommandPermission("%permissionteam_manage")
    @Syntax("<playerName>")
    @Description("Kick a player from your team")
    public void onTeamKick(Player player, String playerName) {
        java.util.Optional<Team> maybeTeam = TeamManager.getPlayerTeam(player.getUniqueId());
        if (maybeTeam.isEmpty()) {
            Text.send(player, Error.TEAM_NOT_FOUND);
            return;
        }

        Team team = maybeTeam.get();

        if (!team.getCreatorUuid().equals(player.getUniqueId())
                && !player.hasPermission("timingsystem.team.admin")) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }

        TPlayer tTarget = TSDatabase.getPlayer(playerName);
        if (tTarget == null) {
            Text.send(player, Error.PLAYER_NOT_FOUND);
            return;
        }

        if (!team.hasPlayer(tTarget)) {
            Text.send(player, Error.PLAYER_NOT_IN_TEAM, "%player%", playerName, "%team%", team.getDisplayName());
            return;
        }

        if (TeamManager.removePlayerFromTeam(team, tTarget)) {
            Text.send(player, Success.PLAYER_REMOVED_FROM_TEAM,
                    "%player%", tTarget.getName(), "%team%", team.getDisplayName());
        } else {
            Text.send(player, Error.GENERIC);
        }
    }

    @Subcommand("role")
    @CommandCompletion("@teamplayers pilot|mechanic")
    @CommandPermission("%permissionteam_role")
    @Syntax("<player> <pilot|mechanic>")
    @Description("Set a team member's role")
    public void onTeamRole(Player player, String playerName, String roleName) {
        java.util.Optional<Team> maybeTeam = TeamManager.getPlayerTeam(player.getUniqueId());
        if (maybeTeam.isEmpty()) {
            Text.send(player, Error.TEAM_NOT_FOUND);
            return;
        }

        Team team = maybeTeam.get();

        if (!team.getCreatorUuid().equals(player.getUniqueId())
                && !player.hasPermission("timingsystem.team.admin")) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }

        TeamRole role = TeamRole.fromString(roleName);
        if (role == null) {
            Text.send(player, Error.INVALID_VALUE);
            return;
        }

        TPlayer tTarget = TSDatabase.getPlayer(playerName);
        if (tTarget == null) {
            Text.send(player, Error.PLAYER_NOT_FOUND);
            return;
        }

        if (!team.hasPlayer(tTarget)) {
            Text.send(player, Error.PLAYER_NOT_IN_TEAM, "%player%", playerName, "%team%", team.getDisplayName());
            return;
        }

        if (TeamManager.setPlayerRole(team, tTarget.getUniqueId(), role)) {
            Text.send(player, Success.TEAM_ROLE_SET,
                    "%player%", tTarget.getName(), "%role%", role.name());
        } else {
            Text.send(player, Error.GENERIC);
        }
    }

    @Subcommand("leave")
    @CommandPermission("%permissionteam_create")
    @Description("Leave your current team")
    public void onTeamLeave(Player player) {
        java.util.Optional<Team> maybeTeam = TeamManager.getPlayerTeam(player.getUniqueId());
        if (maybeTeam.isEmpty()) {
            Text.send(player, Error.TEAM_NOT_FOUND);
            return;
        }

        Team team = maybeTeam.get();

        // Owner can't leave — must disband
        if (team.getCreatorUuid().equals(player.getUniqueId())) {
            Text.send(player, Error.PERMISSION_DENIED);
            return;
        }

        TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        if (tPlayer != null && TeamManager.removePlayerFromTeam(team, tPlayer)) {
            Text.send(player, Success.TEAM_LEFT, "%team%", team.getDisplayName());
        } else {
            Text.send(player, Error.GENERIC);
        }
    }

    @Subcommand("info")
    @CommandCompletion("@teams")
    @CommandPermission("%permissionteam_info")
    @Syntax("[team]")
    @Description("Show team information")
    public void onTeamInfo(CommandSender sender, @Optional Team team) {
        if (team == null && sender instanceof Player player) {
            java.util.Optional<Team> myTeam = TeamManager.getPlayerTeam(player.getUniqueId());
            if (myTeam.isEmpty()) {
                Text.send(player, Error.TEAM_NOT_FOUND);
                return;
            }
            team = myTeam.get();
        } else if (team == null) {
            sender.sendMessage("§cPlease specify a team name.");
            return;
        }

        Text.send(sender, Info.TEAM_INFO_TITLE, "%name%", team.getDisplayName(), "%id%", String.valueOf(team.getId()));
        Text.send(sender, Info.TEAM_INFO_CREATOR, "%creator%",
                team.getCreator() != null ? team.getCreator().getName() : "Unknown");
        Text.send(sender, Info.TEAM_INFO_DATE_CREATED, "%date%", ApiUtilities.niceDate(team.getDateCreated()));
        Text.send(sender, Info.TEAM_INFO_PLAYER_COUNT, "%count%", String.valueOf(team.getPlayerCount()));

        if (team.isEmpty()) {
            sender.sendMessage("§7No players in this team.");
        } else {
            StringBuilder playerList = new StringBuilder();
            for (int i = 0; i < team.getPlayers().size(); i++) {
                TPlayer p = team.getPlayers().get(i);
                TeamRole role = team.getMemberRole(p.getUniqueId());
                String roleTag = role == TeamRole.PILOT ? " §b[PILOT]" : " §7[MECHANIC]";
                if (i > 0) {
                    playerList.append("§f, ");
                }
                playerList.append("§f").append(p.getName()).append(roleTag);
            }
            Text.send(sender, Info.TEAM_INFO_PLAYERS, "%players%", playerList.toString());
        }
    }

    @Subcommand("list")
    @CommandPermission("%permissionteam_list")
    @Description("List all teams")
    public void onTeamList(CommandSender sender) {
        List<Team> teams = TeamManager.getAllTeams();

        Text.send(sender, Info.TEAM_LIST_TITLE);

        if (teams.isEmpty()) {
            Text.send(sender, Info.TEAM_LIST_EMPTY);
            return;
        }

        for (Team team : teams) {
            String playerCount = String.valueOf(team.getPlayerCount());
            UUID pilotUuid = team.getPilotUuid();
            String pilotName = "none";
            if (pilotUuid != null) {
                TPlayer pilot = TSDatabase.getPlayer(pilotUuid);
                if (pilot != null) pilotName = pilot.getName();
            }
            sender.sendMessage("§f• " + team.getDisplayName()
                    + " §7(" + playerCount + " players, pilot: " + pilotName + ")");
        }
    }

    // ─── TEAM RACE COMMANDS ───

    @Subcommand("race")
    @CommandCompletion("@track")
    @CommandPermission("%permissionteam_race")
    @Syntax("<track> [laps] [pits]")
    @Description("Start a team race on a track")
    public void onTeamRace(Player player, Track track, @Optional Integer laps, @Optional Integer pits) {
        java.util.Optional<Team> maybeTeam = TeamManager.getPlayerTeam(player.getUniqueId());
        if (maybeTeam.isEmpty()) {
            Text.send(player, Error.TEAM_NOT_FOUND);
            return;
        }

        Team team = maybeTeam.get();
        int raceLaps = laps != null && laps > 0 ? laps : 5;
        int requiredPits = pits != null && pits >= 0 ? pits : 1;
        requiredPits = Math.min(requiredPits, raceLaps); // Can't have more pits than laps

        TeamRaceManager.startTeamRace(player, team, track, raceLaps, requiredPits);
    }

    @Subcommand("results")
    @CommandCompletion("@track")
    @CommandPermission("%permissionteam_info")
    @Syntax("<track>")
    @Description("View team race results for a track")
    public void onTeamResults(Player player, Track track) {
        List<DbRow> results = TeamRaceManager.getTopTeamResults(track.getId(), 10);
        if (results.isEmpty()) {
            Text.send(player, Info.TEAM_RACE_NO_RESULTS);
            return;
        }

        Text.send(player, Info.TEAM_RACE_RESULTS_TITLE, "%track%", track.getDisplayName());
        int pos = 1;
        for (DbRow row : results) {
            int teamId = row.getInt("team_id");
            long bestTime = row.getLong("best_time");
            java.util.Optional<Team> t = TeamManager.getTeam(teamId);
            String teamName = t.map(Team::getDisplayName).orElse("Unknown");
            String timeFormatted = ApiUtilities.formatAsTime(bestTime);

            Text.send(player, Info.TEAM_RACE_RESULTS_ENTRY,
                    "%pos%", String.valueOf(pos++),
                    "%team%", teamName,
                    "%time%", timeFormatted);
        }
    }

    // ─── ADMIN/DEBUG ───

    @Subcommand("add")
    @CommandCompletion("@teams @players")
    @CommandPermission("%permissionteam_admin")
    @Syntax("<team> <playerName>")
    @Description("Admin: Add a player to a team directly")
    public void onTeamAddPlayer(CommandSender sender, Team team, String playerName) {
        TPlayer player = TSDatabase.getPlayer(playerName);
        if (player == null) {
            Text.send(sender, Error.PLAYER_NOT_FOUND);
            return;
        }

        if (team.hasPlayer(player)) {
            Text.send(sender, Error.PLAYER_ALREADY_IN_TEAM,
                    "%player%", playerName, "%team%", team.getDisplayName());
            return;
        }

        if (TeamManager.addPlayerToTeam(team, player)) {
            TeamManager.setPlayerRole(team, player.getUniqueId(), TeamRole.MECHANIC);
            Text.send(sender, Success.PLAYER_ADDED_TO_TEAM,
                    "%player%", player.getName(), "%team%", team.getDisplayName());
        } else {
            Text.send(sender, Error.GENERIC);
        }
    }

    @Subcommand("remove")
    @CommandCompletion("@teams @teamplayers")
    @CommandPermission("%permissionteam_admin")
    @Syntax("<team> <playerName>")
    @Description("Admin: Remove a player from a team")
    public void onTeamRemovePlayer(CommandSender sender, Team team, String playerName) {
        TPlayer player = TSDatabase.getPlayer(playerName);
        if (player == null) {
            Text.send(sender, Error.PLAYER_NOT_FOUND);
            return;
        }

        if (!team.hasPlayer(player)) {
            Text.send(sender, Error.PLAYER_NOT_IN_TEAM,
                    "%player%", playerName, "%team%", team.getDisplayName());
            return;
        }

        if (TeamManager.removePlayerFromTeam(team, player)) {
            Text.send(sender, Success.PLAYER_REMOVED_FROM_TEAM,
                    "%player%", player.getName(), "%team%", team.getDisplayName());
        } else {
            Text.send(sender, Error.GENERIC);
        }
    }
}