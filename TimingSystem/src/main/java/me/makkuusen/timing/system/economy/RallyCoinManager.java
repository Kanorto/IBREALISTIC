package me.makkuusen.timing.system.economy;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages internal Rally Coins currency.
 * Coins are stored in the database (ts_player_coins table).
 * <p>
 * Includes anti-cheat protections:
 * - Rate limiting (max transactions per minute per player)
 * - Daily transfer limit
 * - Max single transaction amount
 * - Dupe detection (total supply audit)
 */
public class RallyCoinManager {

    private static final String CURRENCY_SYMBOL = "\uD83E\uDE99";

    // ─── ECONOMY PROTECTION DEFAULTS ───
    private static final int DEFAULT_MAX_TRANSACTIONS_PER_MINUTE = 30;
    private static final int DEFAULT_MAX_DAILY_TRANSFER = 10000;
    private static final int DEFAULT_MAX_SINGLE_TRANSACTION = 50000;

    // ─── RATE LIMITING STATE ───
    private static final Map<UUID, RateLimitData> rateLimitMap = new ConcurrentHashMap<>();

    private static class RateLimitData {
        int transactionsThisMinute = 0;
        long minuteStart = System.currentTimeMillis();
        int transferredToday = 0;
        long dayStart = System.currentTimeMillis();

        void resetMinuteIfNeeded() {
            long now = System.currentTimeMillis();
            if (now - minuteStart > 60_000L) {
                transactionsThisMinute = 0;
                minuteStart = now;
            }
        }

        void resetDayIfNeeded() {
            long now = System.currentTimeMillis();
            if (now - dayStart > 86_400_000L) {
                transferredToday = 0;
                dayStart = now;
            }
        }
    }

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
     * Subject to rate limiting and max transaction checks.
     * @return true if successful
     */
    public static boolean addCoins(UUID uuid, int amount, String reason) {
        if (amount <= 0) return false;
        if (amount > getMaxSingleTransaction()) {
            TimingSystem.getPlugin().getLogger().warning(
                    "[Economy] Blocked oversized addCoins: " + amount + " for " + uuid + " reason: " + reason);
            return false;
        }
        if (!checkRateLimit(uuid)) {
            TimingSystem.getPlugin().getLogger().warning(
                    "[Economy] Rate limit exceeded for " + uuid + " reason: " + reason);
            return false;
        }
        try {
            getBalance(uuid); // ensure record exists
            DB.executeUpdate("UPDATE ts_player_coins SET balance = balance + ?, total_earned = total_earned + ? WHERE uuid = ?",
                    amount, amount, uuid.toString());
            DB.executeInsert("INSERT INTO ts_coin_transactions (uuid, amount, reason) VALUES (?, ?, ?)",
                    uuid.toString(), amount, reason);
            incrementRateLimit(uuid);
            return true;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to add coins for " + uuid, e);
            return false;
        }
    }

    /**
     * Spends coins from a player's balance. Only succeeds if player has enough.
     * Subject to rate limiting and max transaction checks.
     * @return true if successful (player had enough coins)
     */
    public static boolean spendCoins(UUID uuid, int amount, String reason) {
        if (amount <= 0) return false;
        if (amount > getMaxSingleTransaction()) {
            TimingSystem.getPlugin().getLogger().warning(
                    "[Economy] Blocked oversized spendCoins: " + amount + " for " + uuid + " reason: " + reason);
            return false;
        }
        if (!checkRateLimit(uuid)) {
            TimingSystem.getPlugin().getLogger().warning(
                    "[Economy] Rate limit exceeded for " + uuid + " reason: " + reason);
            return false;
        }
        int balance = getBalance(uuid);
        if (balance < amount) return false;
        try {
            int rows = DB.executeUpdate("UPDATE ts_player_coins SET balance = balance - ?, total_spent = total_spent + ? WHERE uuid = ? AND balance >= ?",
                    amount, amount, uuid.toString(), amount);
            if (rows == 0) return false; // balance changed between check and update
            DB.executeInsert("INSERT INTO ts_coin_transactions (uuid, amount, reason) VALUES (?, ?, ?)",
                    uuid.toString(), -amount, reason);
            incrementRateLimit(uuid);
            return true;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to spend coins for " + uuid, e);
            return false;
        }
    }

    /**
     * Sets a player's balance directly (admin command).
     * Admin operations bypass rate limiting.
     * Records a transaction to keep audit trail consistent.
     */
    public static boolean setBalance(UUID uuid, int amount) {
        if (amount < 0) return false;
        try {
            int currentBalance = getBalance(uuid); // ensure record exists
            DB.executeUpdate("UPDATE ts_player_coins SET balance = ? WHERE uuid = ?", amount, uuid.toString());
            // Record adjustment transaction for audit trail consistency
            int diff = amount - currentBalance;
            if (diff != 0) {
                DB.executeInsert("INSERT INTO ts_coin_transactions (uuid, amount, reason) VALUES (?, ?, ?)",
                        uuid.toString(), diff, "Admin: setBalance to " + amount);
                // Update totals to keep them in sync
                if (diff > 0) {
                    DB.executeUpdate("UPDATE ts_player_coins SET total_earned = total_earned + ? WHERE uuid = ?",
                            diff, uuid.toString());
                } else {
                    DB.executeUpdate("UPDATE ts_player_coins SET total_spent = total_spent + ? WHERE uuid = ?",
                            -diff, uuid.toString());
                }
            }
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
     * Subject to daily transfer limit and rate limiting.
     * @return true if successful
     */
    public static boolean transfer(UUID from, UUID to, int amount, String reason) {
        if (amount <= 0) return false;

        // Check daily transfer limit
        if (!checkDailyTransferLimit(from, amount)) {
            TimingSystem.getPlugin().getLogger().warning(
                    "[Economy] Daily transfer limit exceeded for " + from);
            return false;
        }

        if (!spendCoins(from, amount, "Transfer to " + to + ": " + reason)) return false;
        if (!addCoins(to, amount, "Transfer from " + from + ": " + reason)) {
            // Rollback
            addCoins(from, amount, "Rollback: failed transfer to " + to);
            return false;
        }

        // Track daily transfer
        RateLimitData data = rateLimitMap.computeIfAbsent(from, k -> new RateLimitData());
        data.resetDayIfNeeded();
        data.transferredToday += amount;
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

    // ─── RATE LIMITING ───

    /**
     * Checks if a player is within the rate limit.
     * @return true if the player can perform another transaction
     */
    private static boolean checkRateLimit(UUID uuid) {
        RateLimitData data = rateLimitMap.computeIfAbsent(uuid, k -> new RateLimitData());
        data.resetMinuteIfNeeded();
        int maxPerMinute = TimingSystem.getPlugin().getConfig().getInt(
                "anticheat.economy.max_transactions_per_minute", DEFAULT_MAX_TRANSACTIONS_PER_MINUTE);
        return data.transactionsThisMinute < maxPerMinute;
    }

    private static void incrementRateLimit(UUID uuid) {
        RateLimitData data = rateLimitMap.computeIfAbsent(uuid, k -> new RateLimitData());
        data.transactionsThisMinute++;
    }

    /**
     * Checks if a player is within the daily transfer limit.
     * @return true if the transfer is within limits
     */
    private static boolean checkDailyTransferLimit(UUID uuid, int amount) {
        RateLimitData data = rateLimitMap.computeIfAbsent(uuid, k -> new RateLimitData());
        data.resetDayIfNeeded();
        int maxDaily = TimingSystem.getPlugin().getConfig().getInt(
                "anticheat.economy.max_daily_transfer", DEFAULT_MAX_DAILY_TRANSFER);
        return (data.transferredToday + amount) <= maxDaily;
    }

    private static int getMaxSingleTransaction() {
        return TimingSystem.getPlugin().getConfig().getInt(
                "anticheat.economy.max_single_transaction", DEFAULT_MAX_SINGLE_TRANSACTION);
    }

    // ─── DUPE DETECTION ───

    /**
     * Audits the total coin supply in the system.
     * Compares sum of all balances against sum of all transactions.
     * Logs a warning if there's a discrepancy (potential dupe).
     *
     * @return true if the audit passes (no discrepancy)
     */
    public static boolean auditCoinSupply() {
        try {
            DbRow balanceRow = DB.getFirstRow("SELECT COALESCE(SUM(balance), 0) AS total_balance FROM ts_player_coins");
            DbRow txRow = DB.getFirstRow("SELECT COALESCE(SUM(amount), 0) AS net_amount FROM ts_coin_transactions");

            long totalBalance = balanceRow != null ? balanceRow.getLong("total_balance") : 0;
            long netTransactions = txRow != null ? txRow.getLong("net_amount") : 0;

            // net transactions should equal total balance (adds positive, spends negative)
            if (totalBalance != netTransactions) {
                TimingSystem.getPlugin().getLogger().severe(
                        "[Economy Audit] DISCREPANCY DETECTED! Total balances: " + totalBalance
                                + " vs Net transactions: " + netTransactions
                                + " (diff: " + (totalBalance - netTransactions) + ")");
                return false;
            }
            return true;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to audit coin supply", e);
            return false;
        }
    }

    /**
     * Cleans up rate limit data for a player (on disconnect).
     */
    public static void removePlayer(UUID uuid) {
        rateLimitMap.remove(uuid);
    }

    /**
     * Returns the remaining daily transfer amount for a player.
     */
    public static int getRemainingDailyTransfer(UUID uuid) {
        RateLimitData data = rateLimitMap.get(uuid);
        if (data == null) return getMaxDailyTransfer();
        data.resetDayIfNeeded();
        int maxDaily = getMaxDailyTransfer();
        return Math.max(0, maxDaily - data.transferredToday);
    }

    private static int getMaxDailyTransfer() {
        return TimingSystem.getPlugin().getConfig().getInt(
                "anticheat.economy.max_daily_transfer", DEFAULT_MAX_DAILY_TRANSFER);
    }
}
