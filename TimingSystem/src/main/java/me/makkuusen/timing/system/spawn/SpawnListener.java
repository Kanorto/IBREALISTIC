package me.makkuusen.timing.system.spawn;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.theme.MessageParser;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import net.kyori.adventure.text.Component;
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
import org.bukkit.event.player.PlayerQuitEvent;
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
    private static final int HOTBAR_MAX_SLOT = 8;
    private static final long JOIN_DELAY_TICKS = 5L;
    private static final long RESPAWN_DELAY_TICKS = 1L;

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
        }, JOIN_DELAY_TICKS);
    }

    // ─── PLAYER QUIT (cleanup) ───

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        resetCooldowns.remove(uuid);
        boatCooldowns.remove(uuid);
        tracksCooldowns.remove(uuid);
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
        Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(),
                () -> SpawnManager.giveHotbarItems(player), RESPAWN_DELAY_TICKS);
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

        String itemType = SpawnManager.getSpawnItemType(event.getItem());
        if (itemType == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        switch (itemType) {
            case SpawnManager.ITEM_TYPE_TRACKS -> executeWithCooldown(player, tracksCooldowns, COOLDOWN_TRACKS_MS, "tt");
            case SpawnManager.ITEM_TYPE_RESET -> executeWithCooldown(player, resetCooldowns, COOLDOWN_RESET_MS, "reset");
            case SpawnManager.ITEM_TYPE_BOAT -> executeWithCooldown(player, boatCooldowns, COOLDOWN_BOAT_MS, "b");
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

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if (!SpawnManager.isSpawnItem(currentItem) && !SpawnManager.isSpawnItem(cursorItem)) {
            return;
        }

        // Allow normal clicks within hotbar (slots 0-8) for rearranging items
        if (event.getClickedInventory() == event.getWhoClicked().getInventory()
                && event.getSlot() >= 0 && event.getSlot() <= HOTBAR_MAX_SLOT
                && !event.isShiftClick()) {
            return;
        }

        event.setCancelled(true);
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

    // ─── COOLDOWN UTILITY ───

    /**
     * Executes a command for the player if not on cooldown.
     * Shows a cooldown message if the action is still cooling down.
     */
    private void executeWithCooldown(Player player, Map<UUID, Long> cooldownMap, long cooldownMs, String command) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUse = cooldownMap.get(uuid);

        if (lastUse != null && (now - lastUse) < cooldownMs) {
            sendSpawnCooldownMessage(player);
            return;
        }

        cooldownMap.put(uuid, now);
        player.performCommand(command);
    }

    private void sendSpawnCooldownMessage(Player player) {
        String locale = TimingSystem.isTritonEnabled() ? "triton" : getPlayerLocale(player);
        String text = TimingSystem.getLanguageManager().getNewValue("spawn.cooldown", locale);
        if (text == null) {
            text = "Please wait before using this again.";
        }
        if (text.contains("&")) {
            player.sendMessage(MessageParser.getComponentWithColors(text, Success.CREATED, Theme.getTheme(player)));
        } else {
            player.sendMessage(Component.text(text));
        }
    }

    private static String getPlayerLocale(Player player) {
        try {
            return player.getClientOption(com.destroystokyo.paper.ClientOption.LOCALE);
        } catch (Exception e) {
            return "en_us";
        }
    }
}
