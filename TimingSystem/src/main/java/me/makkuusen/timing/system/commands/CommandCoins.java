package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.economy.RallyCoinManager;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
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
            Text.send(player, Error.ECONOMY_DISABLED);
            return;
        }
        int balance = RallyCoinManager.getBalance(player.getUniqueId());
        int earned = RallyCoinManager.getTotalEarned(player.getUniqueId());
        int spent = RallyCoinManager.getTotalSpent(player.getUniqueId());

        Text.send(player, Info.COINS_TITLE);
        Text.send(player, Info.COINS_BALANCE, "%balance%", RallyCoinManager.format(balance));
        Text.send(player, Info.COINS_TOTAL_EARNED, "%earned%", RallyCoinManager.format(earned));
        Text.send(player, Info.COINS_TOTAL_SPENT, "%spent%", RallyCoinManager.format(spent));
    }

    @Subcommand("history")
    @CommandPermission("timingsystem.coins.balance")
    @Description("Show transaction history")
    public static void onHistory(Player player) {
        if (!RallyCoinManager.isEnabled()) {
            Text.send(player, Error.ECONOMY_DISABLED);
            return;
        }
        List<DbRow> history = RallyCoinManager.getHistory(player.getUniqueId(), 10);
        if (history.isEmpty()) {
            Text.send(player, Info.COINS_NO_HISTORY);
            return;
        }
        Text.send(player, Info.COINS_HISTORY_TITLE);
        for (DbRow row : history) {
            int amount = row.getInt("amount");
            String reason = row.getString("reason");
            String timestamp = row.getString("timestamp");
            String sign = amount >= 0 ? "+" : "";
            Text.send(player, Info.COINS_HISTORY_ENTRY,
                    "%sign%", sign,
                    "%amount%", String.valueOf(amount),
                    "%reason%", reason,
                    "%timestamp%", timestamp);
        }
    }

    @Subcommand("pay")
    @CommandPermission("timingsystem.coins.pay")
    @Syntax("<player> <amount>")
    @Description("Transfer coins to another player")
    public static void onPay(Player player, String targetName, int amount) {
        if (!RallyCoinManager.isEnabled()) {
            Text.send(player, Error.ECONOMY_DISABLED);
            return;
        }
        if (amount <= 0) {
            Text.send(player, Error.AMOUNT_MUST_BE_POSITIVE);
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Text.send(player, Error.PLAYER_NOT_FOUND);
            return;
        }
        if (target.equals(player)) {
            Text.send(player, Error.CANNOT_PAY_SELF);
            return;
        }
        if (RallyCoinManager.transfer(player.getUniqueId(), target.getUniqueId(), amount, "Player transfer")) {
            Text.send(player, Info.COINS_SENT, "%amount%", RallyCoinManager.format(amount), "%player%", target.getName());
            Text.send(target, Info.COINS_RECEIVED, "%amount%", RallyCoinManager.format(amount), "%player%", player.getName());
        } else {
            Text.send(player, Error.NOT_ENOUGH_COINS);
        }
    }

    @Subcommand("admin give")
    @CommandPermission("timingsystem.coins.admin")
    @Syntax("<player> <amount>")
    @Description("Give coins to a player (admin)")
    public static void onAdminGive(Player player, String targetName, int amount) {
        if (amount <= 0) {
            Text.send(player, Error.AMOUNT_MUST_BE_POSITIVE);
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Text.send(player, Error.PLAYER_NOT_FOUND);
            return;
        }
        RallyCoinManager.addCoins(target.getUniqueId(), amount, "Admin grant by " + player.getName());
        Text.send(player, Info.COINS_ADMIN_GAVE, "%amount%", RallyCoinManager.format(amount), "%player%", target.getName());
        Text.send(target, Info.COINS_ADMIN_RECEIVED, "%amount%", RallyCoinManager.format(amount));
    }

    @Subcommand("admin take")
    @CommandPermission("timingsystem.coins.admin")
    @Syntax("<player> <amount>")
    @Description("Take coins from a player (admin)")
    public static void onAdminTake(Player player, String targetName, int amount) {
        if (amount <= 0) {
            Text.send(player, Error.AMOUNT_MUST_BE_POSITIVE);
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Text.send(player, Error.PLAYER_NOT_FOUND);
            return;
        }
        if (RallyCoinManager.spendCoins(target.getUniqueId(), amount, "Admin take by " + player.getName())) {
            Text.send(player, Info.COINS_ADMIN_TOOK, "%amount%", RallyCoinManager.format(amount), "%player%", target.getName());
            Text.send(target, Info.COINS_ADMIN_TAKEN_FROM, "%amount%", RallyCoinManager.format(amount));
        } else {
            Text.send(player, Error.PLAYER_NOT_ENOUGH_COINS);
        }
    }

    @Subcommand("admin set")
    @CommandPermission("timingsystem.coins.admin")
    @Syntax("<player> <amount>")
    @Description("Set a player's coin balance (admin)")
    public static void onAdminSet(Player player, String targetName, int amount) {
        if (amount < 0) {
            Text.send(player, Error.AMOUNT_NOT_NEGATIVE);
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            Text.send(player, Error.PLAYER_NOT_FOUND);
            return;
        }
        RallyCoinManager.setBalance(target.getUniqueId(), amount);
        Text.send(player, Info.COINS_ADMIN_SET, "%player%", target.getName(), "%amount%", RallyCoinManager.format(amount));
    }
}
