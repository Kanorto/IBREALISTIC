package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;
import java.sql.SQLException;

/**
 * Version 24: Tournament system tables.
 * Adds tournaments, results, brackets, ratings, seasons, and season points.
 */
public class Version24 {

    public static void updateMySQL() throws SQLException {
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_tournaments` (
              `id` int(11) NOT NULL AUTO_INCREMENT,
              `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
              `type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SPRINT',
              `state` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SCHEDULED',
              `start_timestamp` bigint(20) NOT NULL DEFAULT 0,
              `end_timestamp` bigint(20) NOT NULL DEFAULT 0,
              `track_ids` TEXT,
              `car_restriction` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ALL',
              `season_id` int(11) NOT NULL DEFAULT 1,
              `rewards` TEXT,
              `min_players` int(11) NOT NULL DEFAULT 3,
              `min_difficulty` int(11) NOT NULL DEFAULT 1,
              `max_difficulty` int(11) NOT NULL DEFAULT 5,
              `created_at` bigint(20) NOT NULL DEFAULT 0,
              PRIMARY KEY (`id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """);

        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_tournament_results` (
              `id` int(11) NOT NULL AUTO_INCREMENT,
              `tournament_id` int(11) NOT NULL,
              `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
              `track_times` TEXT,
              `total_time_ms` bigint(20) NOT NULL DEFAULT 0,
              `position` int(11) NOT NULL DEFAULT 0,
              `reward_claimed` tinyint(1) NOT NULL DEFAULT 0,
              PRIMARY KEY (`id`),
              KEY `idx_tournament` (`tournament_id`),
              KEY `idx_uuid` (`uuid`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """);

        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_bracket_matches` (
              `id` int(11) NOT NULL AUTO_INCREMENT,
              `tournament_id` int(11) NOT NULL,
              `round_number` int(11) NOT NULL,
              `match_index` int(11) NOT NULL,
              `upper_bracket` tinyint(1) NOT NULL DEFAULT 1,
              `player1_uuid` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
              `player2_uuid` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
              `winner_uuid` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
              `player1_time_ms` bigint(20) NOT NULL DEFAULT 0,
              `player2_time_ms` bigint(20) NOT NULL DEFAULT 0,
              `track_id` int(11) NOT NULL DEFAULT 0,
              `state` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
              PRIMARY KEY (`id`),
              KEY `idx_bracket_tournament` (`tournament_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """);

        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_player_rating` (
              `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
              `rating` int(11) NOT NULL DEFAULT 1000,
              `deviation` int(11) NOT NULL DEFAULT 350,
              `volatility` double NOT NULL DEFAULT 0.06,
              `games_played` int(11) NOT NULL DEFAULT 0,
              `peak_rating` int(11) NOT NULL DEFAULT 1000,
              `season_id` int(11) NOT NULL DEFAULT 1,
              `updated_at` bigint(20) NOT NULL DEFAULT 0,
              PRIMARY KEY (`uuid`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """);

        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_seasons` (
              `id` int(11) NOT NULL AUTO_INCREMENT,
              `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
              `start_timestamp` bigint(20) NOT NULL DEFAULT 0,
              `end_timestamp` bigint(20) NOT NULL DEFAULT 0,
              `state` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
              PRIMARY KEY (`id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """);

        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_season_points` (
              `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
              `season_id` int(11) NOT NULL,
              `total_points` int(11) NOT NULL DEFAULT 0,
              `tournaments_played` int(11) NOT NULL DEFAULT 0,
              `best_position` int(11) NOT NULL DEFAULT 0,
              PRIMARY KEY (`uuid`, `season_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """);
    }

    public static void updateSQLite() throws SQLException {
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_tournaments` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT,
              `name` TEXT NOT NULL,
              `type` TEXT NOT NULL DEFAULT 'SPRINT',
              `state` TEXT NOT NULL DEFAULT 'SCHEDULED',
              `start_timestamp` INTEGER NOT NULL DEFAULT 0,
              `end_timestamp` INTEGER NOT NULL DEFAULT 0,
              `track_ids` TEXT,
              `car_restriction` TEXT NOT NULL DEFAULT 'ALL',
              `season_id` INTEGER NOT NULL DEFAULT 1,
              `rewards` TEXT,
              `min_players` INTEGER NOT NULL DEFAULT 3,
              `min_difficulty` INTEGER NOT NULL DEFAULT 1,
              `max_difficulty` INTEGER NOT NULL DEFAULT 5,
              `created_at` INTEGER NOT NULL DEFAULT 0
            );
        """);

        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_tournament_results` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT,
              `tournament_id` INTEGER NOT NULL,
              `uuid` TEXT NOT NULL,
              `track_times` TEXT,
              `total_time_ms` INTEGER NOT NULL DEFAULT 0,
              `position` INTEGER NOT NULL DEFAULT 0,
              `reward_claimed` INTEGER NOT NULL DEFAULT 0
            );
        """);

        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_bracket_matches` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT,
              `tournament_id` INTEGER NOT NULL,
              `round_number` INTEGER NOT NULL,
              `match_index` INTEGER NOT NULL,
              `upper_bracket` INTEGER NOT NULL DEFAULT 1,
              `player1_uuid` TEXT,
              `player2_uuid` TEXT,
              `winner_uuid` TEXT,
              `player1_time_ms` INTEGER NOT NULL DEFAULT 0,
              `player2_time_ms` INTEGER NOT NULL DEFAULT 0,
              `track_id` INTEGER NOT NULL DEFAULT 0,
              `state` TEXT NOT NULL DEFAULT 'PENDING'
            );
        """);

        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_player_rating` (
              `uuid` TEXT NOT NULL PRIMARY KEY,
              `rating` INTEGER NOT NULL DEFAULT 1000,
              `deviation` INTEGER NOT NULL DEFAULT 350,
              `volatility` REAL NOT NULL DEFAULT 0.06,
              `games_played` INTEGER NOT NULL DEFAULT 0,
              `peak_rating` INTEGER NOT NULL DEFAULT 1000,
              `season_id` INTEGER NOT NULL DEFAULT 1,
              `updated_at` INTEGER NOT NULL DEFAULT 0
            );
        """);

        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_seasons` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT,
              `name` TEXT NOT NULL,
              `start_timestamp` INTEGER NOT NULL DEFAULT 0,
              `end_timestamp` INTEGER NOT NULL DEFAULT 0,
              `state` TEXT NOT NULL DEFAULT 'ACTIVE'
            );
        """);

        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_season_points` (
              `uuid` TEXT NOT NULL,
              `season_id` INTEGER NOT NULL,
              `total_points` INTEGER NOT NULL DEFAULT 0,
              `tournaments_played` INTEGER NOT NULL DEFAULT 0,
              `best_position` INTEGER NOT NULL DEFAULT 0,
              PRIMARY KEY (`uuid`, `season_id`)
            );
        """);
    }
}
