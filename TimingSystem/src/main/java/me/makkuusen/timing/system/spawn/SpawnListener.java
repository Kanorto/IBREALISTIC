package me.makkuusen.timing.system.spawn;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for spawn system events: item interactions, drop prevention,
 * join/respawn teleportation, and cooldown management.
 */
public class SpawnListener implements Listener {

    private static final long COOLDOWN_RESET_MS = 2000;
    private static final long COOLDOWN_BOAT_MS = 2000;
    private static final long COOLDOWN_TRACKS_MS = 1000;

    private final Map<UUID, Long> resetCooldowns = new HashMap<>();
    private final Map<UUID, Long> boatCooldowns = new HashMap<>();
    private final Map<UUID, Long> tracksCooldowns = new HashMap<>();

    // ─── PLAYER JOIN ───

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!SpawnManager.isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> {
            SpawnManager.teleportToSpawn(player);
            SpawnManager.giveHotbarItems(player);
        }, 5L);
    }

    // ─── PLAYER RESPAWN ───

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!SpawnManager.isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        var spawnLoc = SpawnManager.getSpawnLocation();
        if (spawnLoc != null) {
            event.setRespawnLocation(spawnLoc);
        }
        Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> {
            SpawnManager.giveHotbarItems(player);
        }, 1L);
    }

    // ─── ITEM RIGHT-CLICK INTERACTION ───

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!SpawnManager.isEnabled()) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        String itemType = SpawnManager.getSpawnItemType(item);
        if (itemType == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        switch (itemType) {
            case "tracks" -> handleTracksClick(player);
            case "reset" -> handleResetClick(player);
            case "boat" -> handleBoatClick(player);
        }
    }

    // ─── PREVENT DROPPING SPAWN ITEMS ───

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!SpawnManager.isEnabled()) {
            return;
        }
        if (SpawnManager.isSpawnItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    // ─── PREVENT MOVING SPAWN ITEMS OUT OF HOTBAR ───

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!SpawnManager.isEnabled()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        // Check if the clicked item or cursor item is a spawn item
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if (SpawnManager.isSpawnItem(currentItem) || SpawnManager.isSpawnItem(cursorItem)) {
            // Allow moving within hotbar (slots 0-8 in player inventory bottom row)
            // Player inventory slot mapping: 0-8 = hotbar, 9-35 = main inventory, 36-39 = armor, 40 = offhand
            int rawSlot = event.getRawSlot();
            int slot = event.getSlot();

            // If the click is in the player's own inventory
            if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
                // Allow clicks within hotbar (slots 0-8)
                if (slot >= 0 && slot <= 8) {
                    // Allow shift-click only within hotbar
                    if (event.isShiftClick()) {
                        event.setCancelled(true);
                    }
                    // Normal click within hotbar is allowed (swap positions)
                    return;
                }
            }

            // Block all other inventory interactions with spawn items
            event.setCancelled(true);
        }
    }

    // ─── PREVENT OFFHAND SWAP OF SPAWN ITEMS ───

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (!SpawnManager.isEnabled()) {
            return;
        }
        if (SpawnManager.isSpawnItem(event.getOffHandItem()) || SpawnManager.isSpawnItem(event.getMainHandItem())) {
            event.setCancelled(true);
        }
    }

    // ─── COMMAND HANDLERS ───

    private void handleTracksClick(Player player) {
        if (isOnCooldown(player, tracksCooldowns, COOLDOWN_TRACKS_MS)) {
            Text.send(player, Error.NOT_NOW);
            return;
        }
        player.performCommand("tt");
    }

    private void handleResetClick(Player player) {
        if (isOnCooldown(player, resetCooldowns, COOLDOWN_RESET_MS)) {
            Text.send(player, Error.NOT_NOW);
            return;
        }
        player.performCommand("reset");
    }

    private void handleBoatClick(Player player) {
        if (isOnCooldown(player, boatCooldowns, COOLDOWN_BOAT_MS)) {
            Text.send(player, Error.NOT_NOW);
            return;
        }
        player.performCommand("b");
    }

    // ─── COOLDOWN UTILITY ───

    private boolean isOnCooldown(Player player, Map<UUID, Long> cooldownMap, long cooldownMs) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUse = cooldownMap.get(uuid);
        if (lastUse != null && (now - lastUse) < cooldownMs) {
            return true;
        }
        cooldownMap.put(uuid, now);
        return false;
    }
}
