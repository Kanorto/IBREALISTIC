package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.economy.RallyCoinManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("coins")
public class CommandCoins extends BaseCommand {

    @Default
    @CommandPermission("timingsystem.coins.balance")
    @Description("Show your coin balance")
    public static void onBalance(Player player) {
        if (!RallyCoinManager.isEnabled()) {
            player.sendMessage(Component.text("Economy is disabled.", NamedTextColor.RED));
            return;
        }
        int balance = RallyCoinManager.getBalance(player.getUniqueId());
        int earned = RallyCoinManager.getTotalEarned(player.getUniqueId());
        int spent = RallyCoinManager.getTotalSpent(player.getUniqueId());

        player.sendMessage(Component.empty()
                .append(Component.text("━━━ ", NamedTextColor.GOLD))
                .append(Component.text("Rally Coins", NamedTextColor.YELLOW))
                .append(Component.text(" ━━━", NamedTextColor.GOLD)));
        player.sendMessage(Component.text("Balance: ", NamedTextColor.GRAY)
                .append(Component.text(RallyCoinManager.format(balance), NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Total earned: ", NamedTextColor.GRAY)
                .append(Component.text(RallyCoinManager.format(earned), NamedTextColor.AQUA)));
        player.sendMessage(Component.text("Total spent: ", NamedTextColor.GRAY)
                .append(Component.text(RallyCoinManager.format(spent), NamedTextColor.RED)));
    }

    @Subcommand("history")
    @CommandPermission("timingsystem.coins.balance")
    @Description("Show transaction history")
    public static void onHistory(Player player) {
        if (!RallyCoinManager.isEnabled()) {
            player.sendMessage(Component.text("Economy is disabled.", NamedTextColor.RED));
            return;
        }
        List<DbRow> history = RallyCoinManager.getHistory(player.getUniqueId(), 10);
        if (history.isEmpty()) {
            player.sendMessage(Component.text("No transactions yet.", NamedTextColor.GRAY));
            return;
        }
        player.sendMessage(Component.text("━━━ Transaction History ━━━", NamedTextColor.GOLD));
        for (DbRow row : history) {
            int amount = row.getInt("amount");
            String reason = row.getString("reason");
            String timestamp = row.getString("timestamp");
            NamedTextColor color = amount >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
            String sign = amount >= 0 ? "+" : "";
            player.sendMessage(Component.text(sign + amount + " \uD83E\uDE99 ", color)
                    .append(Component.text(reason, NamedTextColor.GRAY))
                    .append(Component.text(" [" + timestamp + "]", NamedTextColor.DARK_GRAY)));
        }
    }

    @Subcommand("pay")
    @CommandPermission("timingsystem.coins.pay")
    @Syntax("<player> <amount>")
    @Description("Transfer coins to another player")
    public static void onPay(Player player, String targetName, int amount) {
        if (!RallyCoinManager.isEnabled()) {
            player.sendMessage(Component.text("Economy is disabled.", NamedTextColor.RED));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(Component.text("Amount must be positive.", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(Component.text("You cannot pay yourself.", NamedTextColor.RED));
            return;
        }
        if (RallyCoinManager.transfer(player.getUniqueId(), target.getUniqueId(), amount, "Player transfer")) {
            player.sendMessage(Component.text("Sent " + RallyCoinManager.format(amount) + " to " + target.getName(), NamedTextColor.GREEN));
            target.sendMessage(Component.text("Received " + RallyCoinManager.format(amount) + " from " + player.getName(), NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Not enough coins.", NamedTextColor.RED));
        }
    }

    @Subcommand("admin give")
    @CommandPermission("timingsystem.coins.admin")
    @Syntax("<player> <amount>")
    @Description("Give coins to a player (admin)")
    public static void onAdminGive(Player player, String targetName, int amount) {
        if (amount <= 0) {
            player.sendMessage(Component.text("Amount must be positive.", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        RallyCoinManager.addCoins(target.getUniqueId(), amount, "Admin grant by " + player.getName());
        player.sendMessage(Component.text("Gave " + RallyCoinManager.format(amount) + " to " + target.getName(), NamedTextColor.GREEN));
        target.sendMessage(Component.text("Received " + RallyCoinManager.format(amount) + " from admin", NamedTextColor.GREEN));
    }

    @Subcommand("admin take")
    @CommandPermission("timingsystem.coins.admin")
    @Syntax("<player> <amount>")
    @Description("Take coins from a player (admin)")
    public static void onAdminTake(Player player, String targetName, int amount) {
        if (amount <= 0) {
            player.sendMessage(Component.text("Amount must be positive.", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        if (RallyCoinManager.spendCoins(target.getUniqueId(), amount, "Admin take by " + player.getName())) {
            player.sendMessage(Component.text("Took " + RallyCoinManager.format(amount) + " from " + target.getName(), NamedTextColor.GREEN));
            target.sendMessage(Component.text("Admin took " + RallyCoinManager.format(amount) + " from your balance", NamedTextColor.RED));
        } else {
            player.sendMessage(Component.text("Player doesn't have enough coins.", NamedTextColor.RED));
        }
    }

    @Subcommand("admin set")
    @CommandPermission("timingsystem.coins.admin")
    @Syntax("<player> <amount>")
    @Description("Set a player's coin balance (admin)")
    public static void onAdminSet(Player player, String targetName, int amount) {
        if (amount < 0) {
            player.sendMessage(Component.text("Amount must not be negative.", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        RallyCoinManager.setBalance(target.getUniqueId(), amount);
        player.sendMessage(Component.text("Set " + target.getName() + "'s balance to " + RallyCoinManager.format(amount), NamedTextColor.GREEN));
    }
}
