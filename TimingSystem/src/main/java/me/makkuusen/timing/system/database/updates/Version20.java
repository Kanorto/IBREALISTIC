package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

/**
 * Version 20: Multi-stage rally event tables (Phase 12.6).
 */
public class Version20 {

    public static void updateMySQL() throws SQLException {
        DB.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `ts_rally_events` (
                  `id` int(11) NOT NULL AUTO_INCREMENT,
                  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                  `creator_uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                  `state` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SETUP',
                  `created_at` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """);
        DB.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `ts_rally_stages` (
                  `id` int(11) NOT NULL AUTO_INCREMENT,
                  `rally_id` int(11) NOT NULL,
                  `stage_index` int(11) NOT NULL DEFAULT 0,
                  `track_id` int(11) NOT NULL,
                  `stage_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                  PRIMARY KEY (`id`),
                  KEY `idx_rally_stages_rally_id` (`rally_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """);
    }

    public static void updateSQLite() throws SQLException {
        DB.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `ts_rally_events` (
                  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                  `name` TEXT NOT NULL,
                  `creator_uuid` TEXT NOT NULL,
                  `state` TEXT NOT NULL DEFAULT 'SETUP',
                  `created_at` TEXT DEFAULT NULL
                );
                """);
        DB.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `ts_rally_stages` (
                  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                  `rally_id` INTEGER NOT NULL,
                  `stage_index` INTEGER NOT NULL DEFAULT 0,
                  `track_id` INTEGER NOT NULL,
                  `stage_name` TEXT DEFAULT NULL
                );
                """);
    }
}
