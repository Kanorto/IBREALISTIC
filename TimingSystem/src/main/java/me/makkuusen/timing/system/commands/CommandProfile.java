package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.gui.ProfileGui;
import me.makkuusen.timing.system.tplayer.TPlayer;
import org.bukkit.entity.Player;

@CommandAlias("profile")
public class CommandProfile extends BaseCommand {

    @Default
    @CommandCompletion("@players")
    @CommandPermission("timingsystem.profile")
    @Description("View your profile or another player's profile")
    @Syntax("[player]")
    public static void onProfile(Player player, @Optional String targetName) {
        TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());

        if (targetName == null || targetName.isEmpty()) {
            new ProfileGui(tPlayer).show(player);
        } else {
            Player target = org.bukkit.Bukkit.getPlayerExact(targetName);
            if (target != null) {
                new ProfileGui(tPlayer, target.getUniqueId(), target.getName()).show(player);
            } else {
                new ProfileGui(tPlayer).show(player);
            }
        }
    }
}
