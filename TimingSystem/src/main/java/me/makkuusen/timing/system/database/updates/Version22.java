package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

/**
 * Version 22: Telemetry metadata table and validation status on race results.
 */
public class Version22 {

    public static void updateMySQL() throws SQLException {
        DB.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `ts_telemetry_meta` (
                  `id` int(11) NOT NULL AUTO_INCREMENT,
                  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
                  `race_result_id` int(11) NOT NULL DEFAULT 0,
                  `track_id` int(11) NOT NULL,
                  `file_path` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
                  `total_ticks` int(11) NOT NULL,
                  `finish_time_ms` bigint(20) NOT NULL DEFAULT 0,
                  `validation_status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
                  `validation_reason` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
                  `avg_speed_kmh` double NOT NULL DEFAULT 0,
                  `max_speed_kmh` double NOT NULL DEFAULT 0,
                  `drift_percent` double NOT NULL DEFAULT 0,
                  `checksum` bigint(20) NOT NULL,
                  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  KEY `idx_telemetry_uuid` (`uuid`),
                  KEY `idx_telemetry_track` (`track_id`),
                  KEY `idx_telemetry_race_result` (`race_result_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """);

        DB.executeUpdate("""
                ALTER TABLE `ts_race_results` ADD COLUMN `validation_status` varchar(32) DEFAULT 'VALID';
                """);
    }

    public static void updateSQLite() throws SQLException {
        DB.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `ts_telemetry_meta` (
                  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                  `uuid` TEXT NOT NULL,
                  `race_result_id` INTEGER NOT NULL DEFAULT 0,
                  `track_id` INTEGER NOT NULL,
                  `file_path` TEXT NOT NULL,
                  `total_ticks` INTEGER NOT NULL,
                  `finish_time_ms` INTEGER NOT NULL DEFAULT 0,
                  `validation_status` TEXT NOT NULL DEFAULT 'PENDING',
                  `validation_reason` TEXT NOT NULL DEFAULT '',
                  `avg_speed_kmh` REAL NOT NULL DEFAULT 0,
                  `max_speed_kmh` REAL NOT NULL DEFAULT 0,
                  `drift_percent` REAL NOT NULL DEFAULT 0,
                  `checksum` INTEGER NOT NULL,
                  `created_at` TEXT NOT NULL
                );
                """);

        DB.executeUpdate("""
                ALTER TABLE `ts_race_results` ADD COLUMN `validation_status` TEXT DEFAULT 'VALID';
                """);
    }
}
