package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.economy.LevelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            player.sendMessage(Component.text("Level system is disabled.", NamedTextColor.RED));
            return;
        }
        int level = LevelManager.getLevel(player.getUniqueId());
        int xp = LevelManager.getXP(player.getUniqueId());
        int totalXP = LevelManager.getTotalXP(player.getUniqueId());
        int nextLevelXP = LevelManager.getXPForLevel(level + 1);
        float progress = LevelManager.getProgress(player.getUniqueId());

        player.sendMessage(Component.empty()
                .append(Component.text("━━━ ", NamedTextColor.DARK_PURPLE))
                .append(Component.text("Rally Level", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(" ━━━", NamedTextColor.DARK_PURPLE)));
        player.sendMessage(Component.text("Level: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(level), NamedTextColor.YELLOW)));

        if (level < LevelManager.getMaxLevel()) {
            int progressPercent = (int) (progress * 100);
            String bar = buildProgressBar(progress, 20);
            player.sendMessage(Component.text("XP: ", NamedTextColor.GRAY)
                    .append(Component.text(xp + "/" + nextLevelXP, NamedTextColor.GREEN))
                    .append(Component.text(" (" + progressPercent + "%)", NamedTextColor.DARK_GREEN)));
            player.sendMessage(Component.text(bar, NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("MAX LEVEL!", NamedTextColor.GOLD));
        }
        player.sendMessage(Component.text("Total XP: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(totalXP), NamedTextColor.AQUA)));
    }

    @Subcommand("top")
    @CommandPermission("timingsystem.level.top")
    @Description("Show top players by level")
    public static void onTop(Player player) {
        if (!LevelManager.isEnabled()) {
            player.sendMessage(Component.text("Level system is disabled.", NamedTextColor.RED));
            return;
        }
        List<DbRow> top = LevelManager.getTopPlayers(10);
        if (top.isEmpty()) {
            player.sendMessage(Component.text("No players ranked yet.", NamedTextColor.GRAY));
            return;
        }
        player.sendMessage(Component.text("━━━ Top Rally Levels ━━━", NamedTextColor.DARK_PURPLE));
        int rank = 1;
        for (DbRow row : top) {
            String name = row.getString("name");
            if (name == null) name = "Unknown";
            int level = row.getInt("level");
            int totalXP = row.getInt("total_xp");
            NamedTextColor rankColor = rank <= 3 ? NamedTextColor.GOLD : NamedTextColor.GRAY;
            player.sendMessage(Component.text("#" + rank + " ", rankColor)
                    .append(Component.text(name, NamedTextColor.WHITE))
                    .append(Component.text(" — Lvl " + level, NamedTextColor.YELLOW))
                    .append(Component.text(" (" + totalXP + " XP)", NamedTextColor.DARK_GRAY)));
            rank++;
        }
    }

    @Subcommand("admin setlevel")
    @CommandPermission("timingsystem.level.admin")
    @Syntax("<player> <level>")
    @Description("Set a player's level (admin)")
    public static void onAdminSetLevel(Player player, String targetName, int level) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        if (LevelManager.setLevel(target.getUniqueId(), level)) {
            player.sendMessage(Component.text("Set " + target.getName() + "'s level to " + level, NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Invalid level (1-" + LevelManager.getMaxLevel() + ").", NamedTextColor.RED));
        }
    }

    @Subcommand("admin addxp")
    @CommandPermission("timingsystem.level.admin")
    @Syntax("<player> <xp>")
    @Description("Add XP to a player (admin)")
    public static void onAdminAddXP(Player player, String targetName, int xp) {
        if (xp <= 0) {
            player.sendMessage(Component.text("XP must be positive.", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        int levelsUp = LevelManager.addXP(target.getUniqueId(), xp, "Admin grant by " + player.getName());
        player.sendMessage(Component.text("Gave " + xp + " XP to " + target.getName(), NamedTextColor.GREEN)
                .append(levelsUp > 0 ? Component.text(" (+" + levelsUp + " levels!)", NamedTextColor.GOLD) : Component.empty()));
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
