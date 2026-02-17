package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;
import java.sql.SQLException;

/**
 * Version 26: Persistent purchase inventory.
 * Adds ts_player_purchases table to track which component presets
 * a player has purchased. Purchased presets can be freely re-equipped
 * on any car without spending coins again.
 */
public class Version26 {

    public static void updateMySQL() throws SQLException {
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_player_purchases` (
              `id` int(11) NOT NULL AUTO_INCREMENT,
              `uuid` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
              `component` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
              `preset_id` int(11) NOT NULL,
              `purchased_at` bigint(20) NOT NULL DEFAULT 0,
              PRIMARY KEY (`id`),
              UNIQUE KEY `uk_player_purchase` (`uuid`, `component`, `preset_id`),
              KEY `idx_purchases_uuid` (`uuid`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """);
    }

    public static void updateSQLite() throws SQLException {
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_player_purchases` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT,
              `uuid` TEXT NOT NULL,
              `component` TEXT NOT NULL,
              `preset_id` INTEGER NOT NULL,
              `purchased_at` INTEGER NOT NULL DEFAULT 0,
              UNIQUE(`uuid`, `component`, `preset_id`)
            );
        """);
        // Index for fast lookups by player UUID
        try {
            DB.executeUpdate("CREATE INDEX IF NOT EXISTS `idx_purchases_uuid` ON `ts_player_purchases` (`uuid`)");
        } catch (SQLException e) {
            // Index may already exist
        }
    }
}
