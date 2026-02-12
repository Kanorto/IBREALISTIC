package me.makkuusen.timing.system.spawn;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.economy.GarageManager;
import me.makkuusen.timing.system.economy.PlayerCar;
import me.makkuusen.timing.system.theme.MessageParser;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Success;
import net.kyori.adventure.text.Component;
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
public final class SpawnManager {

    static final int SLOT_TRACKS = 0;
    static final int SLOT_RESET = 1;
    static final int SLOT_BOAT = 8;

    static final String ITEM_TYPE_TRACKS = "tracks";
    static final String ITEM_TYPE_RESET = "reset";
    static final String ITEM_TYPE_BOAT = "boat";

    private static NamespacedKey spawnItemKey;

    private static boolean enabled;
    private static String spawnWorldName;
    private static double spawnX;
    private static double spawnY;
    private static double spawnZ;
    private static float spawnYaw;
    private static float spawnPitch;
    private static boolean worldWarningLogged;

    private SpawnManager() {
    }

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
        worldWarningLogged = false;
    }

    public static boolean isEnabled() {
        return enabled;
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
     * If the player has an active garage car, the boat item shows the car name.
     */
    public static void giveHotbarItems(Player player) {
        player.getInventory().setItem(SLOT_TRACKS, createSpawnItem(player, Material.NETHER_STAR,
                ITEM_TYPE_TRACKS, "spawn.item_tracks_name", "spawn.item_tracks_lore"));
        player.getInventory().setItem(SLOT_RESET, createSpawnItem(player, Material.RECOVERY_COMPASS,
                ITEM_TYPE_RESET, "spawn.item_reset_name", "spawn.item_reset_lore"));

        // Check for active garage car
        if (GarageManager.isEnabled()) {
            PlayerCar activeCar = GarageManager.getActiveCar(player.getUniqueId());
            if (activeCar != null) {
                player.getInventory().setItem(SLOT_BOAT, createGarageCarItem(player, activeCar));
                return;
            }
        }
        player.getInventory().setItem(SLOT_BOAT, createSpawnItem(player, Material.CHERRY_BOAT,
                ITEM_TYPE_BOAT, "spawn.item_boat_name", "spawn.item_boat_lore"));
    }

    /**
     * Creates a hotbar item representing the player's active garage car.
     */
    private static ItemStack createGarageCarItem(Player player, PlayerCar car) {
        ItemStack item = new ItemStack(Material.CHERRY_BOAT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("🏎 " + car.getName())
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(car.getComponentSummary())
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(spawnItemKey, PersistentDataType.STRING, ITEM_TYPE_BOAT);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Gets the spawn item type identifier from an ItemStack.
     *
     * @return the item type string ("tracks", "reset", "boat"), or null if not a spawn item
     */
    public static String getSpawnItemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(spawnItemKey, PersistentDataType.STRING);
    }

    /**
     * Checks if an ItemStack is a spawn-managed hotbar item.
     */
    public static boolean isSpawnItem(ItemStack item) {
        return getSpawnItemType(item) != null;
    }

    // ─── ITEM CREATION ───

    private static ItemStack createSpawnItem(Player player, Material material, String type,
                                             String nameKey, String loreKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(getTranslatedComponent(player, nameKey)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                getTranslatedComponent(player, loreKey)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(spawnItemKey, PersistentDataType.STRING, type);
        item.setItemMeta(meta);
        return item;
    }

    // ─── TRANSLATION ───

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
