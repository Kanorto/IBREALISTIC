package me.makkuusen.timing.system.economy;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages the player leveling and XP system.
 * XP is displayed in the Minecraft XP bar.
 * Levels unlock vehicles, customizations, and other features.
 */
public class LevelManager {

    private static final int MAX_LEVEL = 100;
    private static final int XP_FORMULA_BASE = 100;
    private static final float XP_FORMULA_GROWTH = 0.1f;

    /**
     * Initialize the level system — update XP bars for all online players.
     */
    public static void initialize() {
        if (!isEnabled()) return;
        Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                updateXPBar(p);
            }
        }, 40L); // 2 seconds after startup
    }

    /**
     * Returns whether the level system is enabled in config.
     */
    public static boolean isEnabled() {
        return TimingSystem.getPlugin().getConfig().getBoolean("levels.enabled", true);
    }

    // ─── XP FORMULA ───

    /**
     * Calculates XP required to go from level (n-1) to level n.
     * Formula: base × n × (1 + growth × (n - 1))
     */
    public static int getXPForLevel(int level) {
        if (level <= 1) return 0;
        int base = TimingSystem.getPlugin().getConfig().getInt("levels.xp_formula_base", XP_FORMULA_BASE);
        float growth = (float) TimingSystem.getPlugin().getConfig().getDouble("levels.xp_formula_growth", XP_FORMULA_GROWTH);
        return (int) (base * level * (1 + growth * (level - 1)));
    }

    /**
     * Calculates total XP needed to reach a given level from level 1.
     */
    public static int getTotalXPForLevel(int level) {
        int total = 0;
        for (int i = 2; i <= level; i++) {
            total += getXPForLevel(i);
        }
        return total;
    }

    // ─── DATABASE OPERATIONS ───

    /**
     * Ensures a player record exists in ts_player_levels.
     */
    private static void ensureRecord(UUID uuid) {
        try {
            DbRow row = DB.getFirstRow("SELECT uuid FROM ts_player_levels WHERE uuid = ?", uuid.toString());
            if (row == null) {
                DB.executeInsert("INSERT INTO ts_player_levels (uuid, level, xp, total_xp) VALUES (?, 1, 0, 0)", uuid.toString());
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to ensure level record for " + uuid, e);
        }
    }

    /**
     * Gets the current level of a player.
     */
    public static int getLevel(UUID uuid) {
        try {
            DbRow row = DB.getFirstRow("SELECT level FROM ts_player_levels WHERE uuid = ?", uuid.toString());
            if (row == null) {
                ensureRecord(uuid);
                return 1;
            }
            return row.getInt("level");
        } catch (SQLException e) {
            return 1;
        }
    }

    /**
     * Gets the current XP within the current level.
     */
    public static int getXP(UUID uuid) {
        try {
            DbRow row = DB.getFirstRow("SELECT xp FROM ts_player_levels WHERE uuid = ?", uuid.toString());
            if (row == null) {
                ensureRecord(uuid);
                return 0;
            }
            return row.getInt("xp");
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Gets the total accumulated XP.
     */
    public static int getTotalXP(UUID uuid) {
        try {
            DbRow row = DB.getFirstRow("SELECT total_xp FROM ts_player_levels WHERE uuid = ?", uuid.toString());
            if (row == null) {
                ensureRecord(uuid);
                return 0;
            }
            return row.getInt("total_xp");
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Gets the progress to the next level (0.0 to 1.0).
     */
    public static float getProgress(UUID uuid) {
        int level = getLevel(uuid);
        if (level >= getMaxLevel()) return 1.0f;
        int xp = getXP(uuid);
        int required = getXPForLevel(level + 1);
        if (required <= 0) return 1.0f;
        return Math.min(1.0f, (float) xp / required);
    }

    /**
     * Gets the configured max level.
     */
    public static int getMaxLevel() {
        return TimingSystem.getPlugin().getConfig().getInt("levels.max_level", MAX_LEVEL);
    }

    /**
     * Adds XP to a player. Handles level-ups automatically.
     * @return number of level-ups that occurred
     */
    public static int addXP(UUID uuid, int amount, String reason) {
        if (amount <= 0 || !isEnabled()) return 0;
        ensureRecord(uuid);

        int currentLevel = getLevel(uuid);
        int currentXP = getXP(uuid);
        int maxLevel = getMaxLevel();

        if (currentLevel >= maxLevel) return 0;

        int newXP = currentXP + amount;
        int levelsGained = 0;

        // Check for level-ups
        while (currentLevel < maxLevel) {
            int required = getXPForLevel(currentLevel + 1);
            if (newXP >= required) {
                newXP -= required;
                currentLevel++;
                levelsGained++;
            } else {
                break;
            }
        }

        // Cap at max level
        if (currentLevel >= maxLevel) {
            currentLevel = maxLevel;
            newXP = 0;
        }

        try {
            DB.executeUpdate("UPDATE ts_player_levels SET level = ?, xp = ?, total_xp = total_xp + ? WHERE uuid = ?",
                    currentLevel, newXP, amount, uuid.toString());
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to add XP for " + uuid, e);
            return 0;
        }

        // Update XP bar for online player
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            updateXPBar(player);

            // Notify about level-ups
            if (levelsGained > 0) {
                player.sendMessage(
                        net.kyori.adventure.text.Component.text("⬆ Level Up! ", net.kyori.adventure.text.format.NamedTextColor.GOLD)
                                .append(net.kyori.adventure.text.Component.text("You are now level " + currentLevel, net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                );
            }
        }

        return levelsGained;
    }

    /**
     * Sets the level directly (admin command).
     */
    public static boolean setLevel(UUID uuid, int level) {
        if (level < 1 || level > getMaxLevel()) return false;
        ensureRecord(uuid);
        try {
            int totalXP = getTotalXPForLevel(level);
            DB.executeUpdate("UPDATE ts_player_levels SET level = ?, xp = 0, total_xp = ? WHERE uuid = ?",
                    level, totalXP, uuid.toString());
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) updateXPBar(player);
            return true;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to set level for " + uuid, e);
            return false;
        }
    }

    /**
     * Gets the top players by level/total_xp.
     */
    public static List<DbRow> getTopPlayers(int limit) {
        try {
            return DB.getResults(
                    "SELECT l.uuid, l.level, l.total_xp, p.name FROM ts_player_levels l " +
                    "LEFT JOIN ts_players p ON l.uuid = p.uuid " +
                    "ORDER BY l.level DESC, l.total_xp DESC LIMIT ?", limit);
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get top players", e);
            return List.of();
        }
    }

    // ─── XP BAR DISPLAY ───

    /**
     * Updates the Minecraft XP bar to reflect the player's rally level.
     */
    public static void updateXPBar(Player player) {
        if (!isEnabled()) return;
        if (!TimingSystem.getPlugin().getConfig().getBoolean("levels.show_in_xp_bar", true)) return;

        int level = getLevel(player.getUniqueId());
        float progress = getProgress(player.getUniqueId());

        player.setLevel(level);
        player.setExp(Math.max(0f, Math.min(progress, 0.999f)));
    }
}
