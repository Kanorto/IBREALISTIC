package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.economy.LevelManager;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("level|lvl")
public class CommandLevel extends BaseCommand {

    @Default
    @CommandPermission("timingsystem.level.view")
    @Description("Show your current level and XP")
    public static void onLevel(Player player) {
        if (!LevelManager.isEnabled()) {
            Text.send(player, Error.LEVEL_DISABLED);
            return;
        }
        int level = LevelManager.getLevel(player.getUniqueId());
        int xp = LevelManager.getXP(player.getUniqueId());
        int totalXP = LevelManager.getTotalXP(player.getUniqueId());
        int nextLevelXP = LevelManager.getXPForLevel(level + 1);
        float progress = LevelManager.getProgress(player.getUniqueId());

        Text.send(player, Info.LEVEL_TITLE);
        Text.send(player, Info.LEVEL_CURRENT, "%level%", String.valueOf(level));

        if (level < LevelManager.getMaxLevel()) {
            int progressPercent = (int) (progress * 100);
            String bar = buildProgressBar(progress, 20);
            Text.send(player, Info.LEVEL_XP_PROGRESS,
                    "%xp%", String.valueOf(xp),
                    "%next%", String.valueOf(nextLevelXP),
                    "%percent%", String.valueOf(progressPercent));
            Text.send(player, Info.LEVEL_XP_BAR, "%bar%", bar);
        } else {
            Text.send(player, Info.LEVEL_MAX);
        }
        Text.send(player, Info.LEVEL_TOTAL_XP, "%total%", String.valueOf(totalXP));
    }

    @Subcommand("top")
    @CommandPermission("timingsystem.level.top")
    @Description("Show top players by level")
    public static void onTop(Player player) {
        if (!LevelManager.isEnabled()) {
            Text.send(player, Error.LEVEL_DISABLED);
            return;
        }
        List<DbRow> top = LevelManager.getTopPlayers(10);
        if (top.isEmpty()) {
            Text.send(player, Info.LEVEL_NO_PLAYERS);
            return;
        }
        Text.send(player, Info.LEVEL_TOP_TITLE);
        int rank = 1;
        for (DbRow row : top) {
            String name = row.getString("name");
            if (name == null) name = "Unknown";
            int level = row.getInt("level");
            int totalXP = row.getInt("total_xp");
            Text.send(player, Info.LEVEL_TOP_ENTRY,
                    "%rank%", String.valueOf(rank),
                    "%name%", name,
                    "%level%", String.valueOf(level),
                    "%xp%", String.valueOf(totalXP));
            rank++;
        }
    }

    @Subcommand("admin setlevel")
    @CommandPermission("timingsystem.level.admin")
    @CommandCompletion("@players <level>")
    @Syntax("<player> <level>")
    @Description("Set a player's level (admin)")
    public static void onAdminSetLevel(Player player, String targetName, int level) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Text.send(player, Error.PLAYER_NOT_FOUND);
            return;
        }
        if (LevelManager.setLevel(target.getUniqueId(), level)) {
            Text.send(player, Info.LEVEL_ADMIN_SET,
                    "%player%", target.getName(),
                    "%level%", String.valueOf(level));
        } else {
            Text.send(player, Error.INVALID_LEVEL, "%max%", String.valueOf(LevelManager.getMaxLevel()));
        }
    }

    @Subcommand("admin addxp")
    @CommandPermission("timingsystem.level.admin")
    @CommandCompletion("@players <xp>")
    @Syntax("<player> <xp>")
    @Description("Add XP to a player (admin)")
    public static void onAdminAddXP(Player player, String targetName, int xp) {
        if (xp <= 0) {
            Text.send(player, Error.AMOUNT_MUST_BE_POSITIVE);
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Text.send(player, Error.PLAYER_NOT_FOUND);
            return;
        }
        int levelsUp = LevelManager.addXP(target.getUniqueId(), xp, "Admin grant by " + player.getName());
        Text.send(player, Info.LEVEL_ADMIN_GAVE_XP,
                "%xp%", String.valueOf(xp),
                "%player%", target.getName());
        if (levelsUp > 0) {
            Text.send(player, Info.LEVEL_ADMIN_GAVE_XP_LEVELUP,
                    "%levels%", String.valueOf(levelsUp),
                    "%player%", target.getName());
        }
    }

    private static String buildProgressBar(float progress, int length) {
        int filled = (int) (progress * length);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < length; i++) {
            sb.append(i < filled ? "█" : "░");
        }
        sb.append("]");
        return sb.toString();
    }
}
