package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

/**
 * Version 21: Anti-cheat violation log table (Phase 14).
 */
public class Version21 {

    public static void updateMySQL() throws SQLException {
        DB.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `ts_anticheat_violations` (
                  `id` int(11) NOT NULL AUTO_INCREMENT,
                  `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
                  `type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
                  `details` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
                  `timestamp` bigint(20) NOT NULL DEFAULT 0,
                  PRIMARY KEY (`id`),
                  KEY `idx_ac_violations_uuid` (`uuid`),
                  KEY `idx_ac_violations_type` (`type`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """);
    }

    public static void updateSQLite() throws SQLException {
        DB.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `ts_anticheat_violations` (
                  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                  `uuid` TEXT NOT NULL,
                  `type` TEXT NOT NULL,
                  `details` TEXT NOT NULL DEFAULT '',
                  `timestamp` INTEGER NOT NULL DEFAULT 0
                );
                """);
    }
}
