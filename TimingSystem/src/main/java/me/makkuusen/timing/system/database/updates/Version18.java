package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

/**
 * Version 18: Race results table for solo and multiplayer races (Phase 10).
 */
public class Version18 {

    public static void updateMySQL() throws SQLException {
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_race_results` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `uuid` VARCHAR(36) NOT NULL,
                `track_id` INT NOT NULL,
                `time_ms` BIGINT NOT NULL,
                `race_type` VARCHAR(16) NOT NULL DEFAULT 'SOLO',
                `car_type` VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
                `created_at` VARCHAR(255),
                INDEX idx_track_id (`track_id`),
                INDEX idx_uuid (`uuid`),
                INDEX idx_track_car (`track_id`, `car_type`),
                INDEX idx_track_race_type (`track_id`, `race_type`)
            )
        """);
    }

    public static void updateSQLite() throws SQLException {
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_race_results` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `uuid` TEXT NOT NULL,
                `track_id` INTEGER NOT NULL,
                `time_ms` INTEGER NOT NULL,
                `race_type` TEXT NOT NULL DEFAULT 'SOLO',
                `car_type` TEXT NOT NULL DEFAULT 'SYSTEM',
                `created_at` TEXT
            )
        """);
    }
}
