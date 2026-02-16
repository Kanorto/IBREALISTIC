package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.economy.DailyChallengeManager;
import me.makkuusen.timing.system.gui.DailyGui;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.tplayer.TPlayer;
import org.bukkit.entity.Player;

@CommandAlias("daily")
public class CommandDaily extends BaseCommand {

    @Default
    @CommandPermission("timingsystem.daily.view")
    @Description("Show today's daily challenges")
    public static void onDaily(Player player) {
        TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        new DailyGui(tPlayer).show(player);
    }

    @Subcommand("admin regenerate")
    @CommandPermission("timingsystem.daily.admin")
    @Description("Regenerate today's daily challenges (admin)")
    public static void onRegenerate(Player player) {
        DailyChallengeManager.regenerate();
        Text.send(player, Info.DAILY_REGENERATED);
    }
}
