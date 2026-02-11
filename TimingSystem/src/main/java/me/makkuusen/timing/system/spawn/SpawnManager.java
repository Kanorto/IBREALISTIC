package me.makkuusen.timing.system.spawn;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.MessageParser;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Success;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Manages player spawn location and hotbar items.
 * Players are teleported to a configured spawn location on join/respawn
 * and receive persistent hotbar items that execute commands on right-click.
 */
public class SpawnManager {

    private static final int SLOT_TRACKS = 0;
    private static final int SLOT_RESET = 1;
    private static final int SLOT_BOAT = 8;

    private static NamespacedKey spawnItemKey;

    private static boolean enabled;
    private static String spawnWorldName;
    private static double spawnX;
    private static double spawnY;
    private static double spawnZ;
    private static float spawnYaw;
    private static float spawnPitch;

    private static boolean worldWarningLogged;

    public static void initialize() {
        spawnItemKey = new NamespacedKey(TimingSystem.getPlugin(), "spawn_item");
        worldWarningLogged = false;
        loadConfig();
    }

    public static void loadConfig() {
        var config = TimingSystem.getPlugin().getConfig();
        enabled = config.getBoolean("spawn.enabled", false);
        spawnWorldName = config.getString("spawn.world", "world");
        spawnX = config.getDouble("spawn.x", 0);
        spawnY = config.getDouble("spawn.y", 100);
        spawnZ = config.getDouble("spawn.z", 0);
        spawnYaw = (float) config.getDouble("spawn.yaw", 0);
        spawnPitch = (float) config.getDouble("spawn.pitch", 0);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static NamespacedKey getSpawnItemKey() {
        return spawnItemKey;
    }

    /**
     * Returns the configured spawn location, or null if the world is not loaded.
     */
    public static Location getSpawnLocation() {
        World world = Bukkit.getWorld(spawnWorldName);
        if (world == null) {
            if (!worldWarningLogged) {
                TimingSystem.getPlugin().getLogger().warning("[SpawnManager] Spawn world '" + spawnWorldName + "' is not loaded!");
                worldWarningLogged = true;
            }
            return null;
        }
        return new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
    }

    /**
     * Teleports the player to the configured spawn location.
     */
    public static void teleportToSpawn(Player player) {
        Location spawnLoc = getSpawnLocation();
        if (spawnLoc != null) {
            player.teleportAsync(spawnLoc);
        }
    }

    /**
     * Gives the player the persistent hotbar items.
     * Item names are resolved from the language system for Triton support.
     */
    public static void giveHotbarItems(Player player) {
        player.getInventory().setItem(SLOT_TRACKS, createTracksItem(player));
        player.getInventory().setItem(SLOT_RESET, createResetItem(player));
        player.getInventory().setItem(SLOT_BOAT, createBoatItem(player));
    }

    /**
     * Checks if an ItemStack is a spawn-managed hotbar item.
     */
    public static boolean isSpawnItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(spawnItemKey, PersistentDataType.STRING);
    }

    /**
     * Gets the spawn item type identifier from an ItemStack.
     * Returns null if the item is not a spawn item.
     */
    public static String getSpawnItemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(spawnItemKey, PersistentDataType.STRING);
    }

    // ─── ITEM CREATION ───

    private static ItemStack createTracksItem(Player player) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(getTranslatedComponent(player, "spawn.item_tracks_name")
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                getTranslatedComponent(player, "spawn.item_tracks_lore")
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(spawnItemKey, PersistentDataType.STRING, "tracks");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createResetItem(Player player) {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(getTranslatedComponent(player, "spawn.item_reset_name")
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                getTranslatedComponent(player, "spawn.item_reset_lore")
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(spawnItemKey, PersistentDataType.STRING, "reset");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createBoatItem(Player player) {
        ItemStack item = new ItemStack(Material.CHERRY_BOAT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(getTranslatedComponent(player, "spawn.item_boat_name")
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                getTranslatedComponent(player, "spawn.item_boat_lore")
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(spawnItemKey, PersistentDataType.STRING, "boat");
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Gets a translated component from the language system.
     * Supports Triton placeholders when Triton is enabled.
     */
    private static Component getTranslatedComponent(Player player, String key) {
        String locale = TimingSystem.isTritonEnabled() ? "triton" : getPlayerLocale(player);
        String text = TimingSystem.getLanguageManager().getNewValue(key, locale);
        if (text == null) {
            return Component.text(key);
        }
        if (text.contains("&")) {
            return MessageParser.getComponentWithColors(text, Success.CREATED, Theme.getTheme(player));
        }
        return Component.text(text);
    }

    private static String getPlayerLocale(Player player) {
        try {
            return player.getClientOption(com.destroystokyo.paper.ClientOption.LOCALE);
        } catch (Exception e) {
            return "en_us";
        }
    }
}
