package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;
import java.sql.SQLException;

/**
 * Version 25: Team Racing & Pit Stops tables.
 * Adds team_race_results table and role column to team_players.
 */
public class Version25 {

    public static void updateMySQL() throws SQLException {
        // Add role column to ts_team_players (if not exists)
        try {
            DB.executeUpdate("""
                ALTER TABLE `ts_team_players`
                ADD COLUMN `role` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MECHANIC'
            """);
        } catch (SQLException e) {
            // Column may already exist — ignore
            if (!e.getMessage().contains("Duplicate column")) {
                throw e;
            }
        }

        // Add tasks column to ts_team_players (if not exists)
        try {
            DB.executeUpdate("""
                ALTER TABLE `ts_team_players`
                ADD COLUMN `tasks` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT ''
            """);
        } catch (SQLException e) {
            if (!e.getMessage().contains("Duplicate column")) {
                throw e;
            }
        }

        // Add maxMembers column to ts_teams (if not exists)
        try {
            DB.executeUpdate("""
                ALTER TABLE `ts_teams`
                ADD COLUMN `maxMembers` int(11) NOT NULL DEFAULT 0
            """);
        } catch (SQLException e) {
            if (!e.getMessage().contains("Duplicate column")) {
                throw e;
            }
        }

        // Create team race results table
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_team_race_results` (
              `id` int(11) NOT NULL AUTO_INCREMENT,
              `team_id` int(11) NOT NULL,
              `track_id` int(11) NOT NULL,
              `pilot_uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
              `race_time_ms` bigint(20) NOT NULL DEFAULT 0,
              `pit_time_ms` bigint(20) NOT NULL DEFAULT 0,
              `penalty_ms` bigint(20) NOT NULL DEFAULT 0,
              `total_time_ms` bigint(20) NOT NULL DEFAULT 0,
              `created_at` bigint(20) NOT NULL DEFAULT 0,
              PRIMARY KEY (`id`),
              KEY `idx_team` (`team_id`),
              KEY `idx_track` (`track_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """);
    }

    public static void updateSQLite() throws SQLException {
        // Add role column to ts_team_players (if not exists)
        try {
            DB.executeUpdate("ALTER TABLE `ts_team_players` ADD COLUMN `role` TEXT NOT NULL DEFAULT 'MECHANIC'");
        } catch (SQLException e) {
            // Column may already exist
            if (!e.getMessage().contains("duplicate column")) {
                throw e;
            }
        }

        // Add tasks column to ts_team_players (if not exists)
        try {
            DB.executeUpdate("ALTER TABLE `ts_team_players` ADD COLUMN `tasks` TEXT NOT NULL DEFAULT ''");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column")) {
                throw e;
            }
        }

        // Add maxMembers column to ts_teams (if not exists)
        try {
            DB.executeUpdate("ALTER TABLE `ts_teams` ADD COLUMN `maxMembers` INTEGER NOT NULL DEFAULT 0");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column")) {
                throw e;
            }
        }

        // Create team race results table
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_team_race_results` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT,
              `team_id` INTEGER NOT NULL,
              `track_id` INTEGER NOT NULL,
              `pilot_uuid` TEXT NOT NULL,
              `race_time_ms` INTEGER NOT NULL DEFAULT 0,
              `pit_time_ms` INTEGER NOT NULL DEFAULT 0,
              `penalty_ms` INTEGER NOT NULL DEFAULT 0,
              `total_time_ms` INTEGER NOT NULL DEFAULT 0,
              `created_at` INTEGER NOT NULL DEFAULT 0
            );
        """);
    }
}
