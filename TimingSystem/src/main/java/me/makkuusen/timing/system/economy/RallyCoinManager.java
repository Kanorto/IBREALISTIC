package me.makkuusen.timing.system.economy;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages internal Rally Coins currency.
 * Coins are stored in the database (ts_player_coins table).
 */
public class RallyCoinManager {

    private static final String CURRENCY_SYMBOL = "\uD83E\uDE99";

    /**
     * Gets the coin balance for a player. Creates a record if not exists.
     */
    public static int getBalance(UUID uuid) {
        try {
            DbRow row = DB.getFirstRow("SELECT balance FROM ts_player_coins WHERE uuid = ?", uuid.toString());
            if (row == null) {
                DB.executeInsert("INSERT INTO ts_player_coins (uuid, balance, total_earned, total_spent) VALUES (?, 0, 0, 0)", uuid.toString());
                return 0;
            }
            return row.getInt("balance");
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get coin balance for " + uuid, e);
            return 0;
        }
    }

    /**
     * Adds coins to a player's balance. Records the transaction.
     * @return true if successful
     */
    public static boolean addCoins(UUID uuid, int amount, String reason) {
        if (amount <= 0) return false;
        try {
            getBalance(uuid); // ensure record exists
            DB.executeUpdate("UPDATE ts_player_coins SET balance = balance + ?, total_earned = total_earned + ? WHERE uuid = ?",
                    amount, amount, uuid.toString());
            DB.executeInsert("INSERT INTO ts_coin_transactions (uuid, amount, reason) VALUES (?, ?, ?)",
                    uuid.toString(), amount, reason);
            return true;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to add coins for " + uuid, e);
            return false;
        }
    }

    /**
     * Spends coins from a player's balance. Only succeeds if player has enough.
     * @return true if successful (player had enough coins)
     */
    public static boolean spendCoins(UUID uuid, int amount, String reason) {
        if (amount <= 0) return false;
        int balance = getBalance(uuid);
        if (balance < amount) return false;
        try {
            int rows = DB.executeUpdate("UPDATE ts_player_coins SET balance = balance - ?, total_spent = total_spent + ? WHERE uuid = ? AND balance >= ?",
                    amount, amount, uuid.toString(), amount);
            if (rows == 0) return false; // balance changed between check and update
            DB.executeInsert("INSERT INTO ts_coin_transactions (uuid, amount, reason) VALUES (?, ?, ?)",
                    uuid.toString(), -amount, reason);
            return true;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to spend coins for " + uuid, e);
            return false;
        }
    }

    /**
     * Sets a player's balance directly (admin command).
     */
    public static boolean setBalance(UUID uuid, int amount) {
        if (amount < 0) return false;
        try {
            getBalance(uuid); // ensure record exists
            DB.executeUpdate("UPDATE ts_player_coins SET balance = ? WHERE uuid = ?", amount, uuid.toString());
            return true;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to set balance for " + uuid, e);
            return false;
        }
    }

    /**
     * Gets the total earned coins for a player.
     */
    public static int getTotalEarned(UUID uuid) {
        try {
            DbRow row = DB.getFirstRow("SELECT total_earned FROM ts_player_coins WHERE uuid = ?", uuid.toString());
            return row != null ? row.getInt("total_earned") : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Gets the total spent coins for a player.
     */
    public static int getTotalSpent(UUID uuid) {
        try {
            DbRow row = DB.getFirstRow("SELECT total_spent FROM ts_player_coins WHERE uuid = ?", uuid.toString());
            return row != null ? row.getInt("total_spent") : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Gets recent transaction history for a player.
     */
    public static List<DbRow> getHistory(UUID uuid, int limit) {
        try {
            return DB.getResults("SELECT amount, reason, timestamp FROM ts_coin_transactions WHERE uuid = ? ORDER BY timestamp DESC LIMIT ?",
                    uuid.toString(), limit);
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get coin history for " + uuid, e);
            return List.of();
        }
    }

    /**
     * Transfers coins between players.
     * @return true if successful
     */
    public static boolean transfer(UUID from, UUID to, int amount, String reason) {
        if (amount <= 0) return false;
        if (!spendCoins(from, amount, "Transfer to " + to + ": " + reason)) return false;
        if (!addCoins(to, amount, "Transfer from " + from + ": " + reason)) {
            // Rollback
            addCoins(from, amount, "Rollback: failed transfer to " + to);
            return false;
        }
        return true;
    }

    /**
     * Formats coins with currency symbol.
     */
    public static String format(int amount) {
        return amount + " " + CURRENCY_SYMBOL;
    }

    /**
     * Returns whether the economy system is enabled in config.
     */
    public static boolean isEnabled() {
        return TimingSystem.getPlugin().getConfig().getBoolean("economy.enabled", true);
    }
}
