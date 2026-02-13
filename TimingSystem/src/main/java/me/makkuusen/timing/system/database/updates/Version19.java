package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

/**
 * Version 19: Track settings — weather, time of day, difficulty (Phase 11).
 */
public class Version19 {

    public static void updateMySQL() throws SQLException {
        DB.executeUpdate("ALTER TABLE `ts_tracks` ADD COLUMN `weatherCondition` int(2) NOT NULL DEFAULT 0");
        DB.executeUpdate("ALTER TABLE `ts_tracks` ADD COLUMN `trackTime` int(11) DEFAULT NULL");
        DB.executeUpdate("ALTER TABLE `ts_tracks` ADD COLUMN `difficulty` int(2) NOT NULL DEFAULT 1");
        DB.executeUpdate("ALTER TABLE `ts_tracks` ADD COLUMN `dynamicWeather` tinyint(1) NOT NULL DEFAULT 0");
    }

    public static void updateSQLite() throws SQLException {
        DB.executeUpdate("ALTER TABLE `ts_tracks` ADD COLUMN `weatherCondition` INTEGER NOT NULL DEFAULT 0");
        DB.executeUpdate("ALTER TABLE `ts_tracks` ADD COLUMN `trackTime` INTEGER DEFAULT NULL");
        DB.executeUpdate("ALTER TABLE `ts_tracks` ADD COLUMN `difficulty` INTEGER NOT NULL DEFAULT 1");
        DB.executeUpdate("ALTER TABLE `ts_tracks` ADD COLUMN `dynamicWeather` INTEGER NOT NULL DEFAULT 0");
    }
}
