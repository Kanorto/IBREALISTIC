package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

public class Version15 {

    public static void updateMySQL() throws SQLException {
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_player_levels` (
                `uuid` VARCHAR(36) NOT NULL PRIMARY KEY,
                `level` INT NOT NULL DEFAULT 1,
                `xp` INT NOT NULL DEFAULT 0,
                `total_xp` INT NOT NULL DEFAULT 0
            )
        """);
    }

    public static void updateSQLite() throws SQLException {
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_player_levels` (
                `uuid` TEXT NOT NULL PRIMARY KEY,
                `level` INTEGER NOT NULL DEFAULT 1,
                `xp` INTEGER NOT NULL DEFAULT 0,
                `total_xp` INTEGER NOT NULL DEFAULT 0
            )
        """);
    }
}
