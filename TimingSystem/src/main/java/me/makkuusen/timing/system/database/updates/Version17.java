package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

/**
 * Version 17: Player garage tables for vehicle customization (Phase 9).
 */
public class Version17 {

    public static void updateMySQL() throws SQLException {
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_player_garage` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `uuid` VARCHAR(36) NOT NULL,
                `name` VARCHAR(64) NOT NULL,
                `vehicle_type` INT NOT NULL DEFAULT 0,
                `tire_preset` INT NOT NULL DEFAULT 0,
                `suspension_preset` INT NOT NULL DEFAULT 0,
                `engine_preset` INT NOT NULL DEFAULT 0,
                `body_preset` INT NOT NULL DEFAULT 0,
                `steering_preset` INT NOT NULL DEFAULT 0,
                `brake_preset` INT NOT NULL DEFAULT 0,
                `weight_distribution_preset` INT NOT NULL DEFAULT 0,
                `active` INT NOT NULL DEFAULT 0,
                INDEX idx_uuid (`uuid`),
                INDEX idx_uuid_active (`uuid`, `active`)
            )
        """);
    }

    public static void updateSQLite() throws SQLException {
        DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_player_garage` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `uuid` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `vehicle_type` INTEGER NOT NULL DEFAULT 0,
                `tire_preset` INTEGER NOT NULL DEFAULT 0,
                `suspension_preset` INTEGER NOT NULL DEFAULT 0,
                `engine_preset` INTEGER NOT NULL DEFAULT 0,
                `body_preset` INTEGER NOT NULL DEFAULT 0,
                `steering_preset` INTEGER NOT NULL DEFAULT 0,
                `brake_preset` INTEGER NOT NULL DEFAULT 0,
                `weight_distribution_preset` INTEGER NOT NULL DEFAULT 0,
                `active` INTEGER NOT NULL DEFAULT 0
            )
        """);
    }
}
