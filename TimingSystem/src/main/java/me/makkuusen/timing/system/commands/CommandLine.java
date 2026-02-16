package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.ghost.GhostDisplayMode;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.tplayer.Settings;
import me.makkuusen.timing.system.tplayer.TPlayer;
import org.bukkit.entity.Player;

@CommandAlias("line|l")
public class CommandLine extends BaseCommand {

    @Default
    @CommandPermission("%permissiontimingsystem_settings")
    @Description("Toggle ghost line display on/off")
    public static void onToggle(Player player) {
        TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        tPlayer.getSettings().toggleGhostLine();
        GhostDisplayMode mode = tPlayer.getSettings().getGhostDisplayMode();
        if (mode == GhostDisplayMode.OFF) {
            Text.send(player, Success.GHOST_LINE_OFF);
        } else {
            Text.send(player, Success.GHOST_LINE_ON);
        }
    }

    @Subcommand("mode")
    @CommandCompletion("OFF|LINE|BOAT|COMPETITION")
    @CommandPermission("%permissiontimingsystem_settings")
    @Description("Set ghost display mode")
    public static void onMode(Player player, String modeName) {
        GhostDisplayMode mode = GhostDisplayMode.fromName(modeName);
        TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        tPlayer.getSettings().setGhostDisplayMode(mode);
        Text.send(player, Success.GHOST_MODE_SET);
    }

    @Subcommand("count")
    @CommandCompletion("1|2|3|4|5|6")
    @CommandPermission("%permissiontimingsystem_settings")
    @Description("Set max ghost count for competition mode")
    public static void onCount(Player player, int count) {
        if (count < Settings.MIN_GHOST_COUNT || count > Settings.MAX_GHOST_COUNT) {
            Text.send(player, Error.NUMBER_FORMAT);
            return;
        }
        TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        tPlayer.getSettings().setGhostCount(count);
        Text.send(player, Success.GHOST_COUNT_SET);
    }

    @Subcommand("info")
    @CommandPermission("%permissiontimingsystem_settings")
    @Description("Show current ghost line settings")
    public static void onInfo(Player player) {
        TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        GhostDisplayMode mode = tPlayer.getSettings().getGhostDisplayMode();
        int count = tPlayer.getSettings().getGhostCount();
        player.sendMessage("§7Ghost Line Settings:");
        player.sendMessage("§7  Mode: §f" + mode.name());
        player.sendMessage("§7  Max ghosts (competition): §f" + count);
    }
}
