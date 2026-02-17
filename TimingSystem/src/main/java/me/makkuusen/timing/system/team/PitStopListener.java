package me.makkuusen.timing.system.team;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for pit stop mechanics during team races.
 * Handles mechanic hotbar item clicks and player disconnect cleanup.
 */
public class PitStopListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Only handle mechanics in pit stops
        if (!PitStopManager.isMechanicInPitStop(player.getUniqueId())) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        String itemType = PitStopManager.getPitStopItemType(item);
        if (itemType == null) return;

        event.setCancelled(true);

        // Handle refuel (right-click hold)
        if (PitStopManager.ITEM_TYPE_REFUEL.equals(itemType)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR
                    || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                PitStopManager.startRefueling(player);
            }
            return;
        }

        // Handle left/right click for other items
        if (event.getAction() == Action.LEFT_CLICK_AIR
                || event.getAction() == Action.LEFT_CLICK_BLOCK
                || event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            PitStopManager.handlePitStopClick(player, itemType);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PitStopManager.cancelForPlayer(event.getPlayer().getUniqueId());
        TeamRaceManager.cancelTeamRace(event.getPlayer().getUniqueId());
    }
}
