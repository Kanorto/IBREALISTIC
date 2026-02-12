package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

public class Version14 {

    public static void updateMySQL() throws SQLException {
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_player_coins` (
                `uuid` VARCHAR(36) NOT NULL PRIMARY KEY,
                `balance` INT NOT NULL DEFAULT 0,
                `total_earned` INT NOT NULL DEFAULT 0,
                `total_spent` INT NOT NULL DEFAULT 0
            )
        """);
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_coin_transactions` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `uuid` VARCHAR(36) NOT NULL,
                `amount` INT NOT NULL,
                `reason` VARCHAR(255) NOT NULL DEFAULT '',
                `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """);
    }

    public static void updateSQLite() throws SQLException {
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_player_coins` (
                `uuid` TEXT NOT NULL PRIMARY KEY,
                `balance` INTEGER NOT NULL DEFAULT 0,
                `total_earned` INTEGER NOT NULL DEFAULT 0,
                `total_spent` INTEGER NOT NULL DEFAULT 0
            )
        """);
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_coin_transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `uuid` TEXT NOT NULL,
                `amount` INTEGER NOT NULL,
                `reason` TEXT NOT NULL DEFAULT '',
                `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """);
    }
}
