package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

public class Version16 {

    public static void updateMySQL() throws SQLException {
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_daily_challenges` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `date` VARCHAR(10) NOT NULL,
                `challenge_type` VARCHAR(64) NOT NULL,
                `slot` INT NOT NULL DEFAULT 0
            )
        """);
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_player_daily_progress` (
                `uuid` VARCHAR(36) NOT NULL,
                `date` VARCHAR(10) NOT NULL,
                `slot` INT NOT NULL DEFAULT 0,
                `progress` INT NOT NULL DEFAULT 0,
                PRIMARY KEY (`uuid`, `date`, `slot`)
            )
        """);
    }

    public static void updateSQLite() throws SQLException {
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_daily_challenges` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `date` TEXT NOT NULL,
                `challenge_type` TEXT NOT NULL,
                `slot` INTEGER NOT NULL DEFAULT 0
            )
        """);
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_player_daily_progress` (
                `uuid` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `slot` INTEGER NOT NULL DEFAULT 0,
                `progress` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (`uuid`, `date`, `slot`)
            )
        """);
    }
}
