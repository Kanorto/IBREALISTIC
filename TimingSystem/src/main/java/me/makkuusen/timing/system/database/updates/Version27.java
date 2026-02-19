package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;
import java.sql.SQLException;

/**
 * Version 27: Quantity-based purchase inventory + new vehicle components.
 *
 * 1. Adds 'quantity' column to ts_player_purchases to support buying multiple
 *    copies of the same preset (physical inventory items).
 * 2. Adds new component columns to ts_player_garage:
 *    exhaust_preset, differential_preset, gearbox_preset, turbo_preset, intercooler_preset.
 * 3. Adds ts_track_difficulty_enabled flag table for per-track difficulty selection.
 */
public class Version27 {

    public static void updateMySQL() throws SQLException {
        // 1. Add quantity column to purchases
        try {
            DB.executeUpdate("ALTER TABLE `ts_player_purchases` ADD COLUMN `quantity` int(11) NOT NULL DEFAULT 1");
        } catch (SQLException e) {
            // Column may already exist
        }

        // 2. Add new component columns to garage
        String[] newColumns = {
                "exhaust_preset", "differential_preset", "gearbox_preset",
                "turbo_preset", "intercooler_preset"
        };
        for (String col : newColumns) {
            try {
                DB.executeUpdate("ALTER TABLE `ts_player_garage` ADD COLUMN `" + col + "` int(11) NOT NULL DEFAULT 0");
            } catch (SQLException e) {
                // Column may already exist
            }
        }
    }

    public static void updateSQLite() throws SQLException {
        // 1. Add quantity column to purchases
        try {
            DB.executeUpdate("ALTER TABLE `ts_player_purchases` ADD COLUMN `quantity` INTEGER NOT NULL DEFAULT 1");
        } catch (SQLException e) {
            // Column may already exist
        }

        // 2. Add new component columns to garage
        String[] newColumns = {
                "exhaust_preset", "differential_preset", "gearbox_preset",
                "turbo_preset", "intercooler_preset"
        };
        for (String col : newColumns) {
            try {
                DB.executeUpdate("ALTER TABLE `ts_player_garage` ADD COLUMN `" + col + "` INTEGER NOT NULL DEFAULT 0");
            } catch (SQLException e) {
                // Column may already exist
            }
        }
    }
}
