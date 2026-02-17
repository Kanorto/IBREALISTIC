package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;
import java.sql.SQLException;

/**
 * Version 23: Ghost display settings on player table.
 */
public class Version23 {

    public static void updateMySQL() throws SQLException {
        DB.executeUpdate("ALTER TABLE `ts_players` ADD COLUMN `ghostDisplayMode` varchar(16) NOT NULL DEFAULT 'OFF';");
        DB.executeUpdate("ALTER TABLE `ts_players` ADD COLUMN `ghostCount` int(11) NOT NULL DEFAULT 2;");
    }

    public static void updateSQLite() throws SQLException {
        DB.executeUpdate("ALTER TABLE `ts_players` ADD COLUMN `ghostDisplayMode` TEXT NOT NULL DEFAULT 'OFF';");
        DB.executeUpdate("ALTER TABLE `ts_players` ADD COLUMN `ghostCount` INTEGER NOT NULL DEFAULT 2;");
    }
}
