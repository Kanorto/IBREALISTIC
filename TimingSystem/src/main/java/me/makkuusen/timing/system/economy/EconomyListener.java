package me.makkuusen.timing.system.economy;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.events.TimeTrialFinishEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class EconomyListener implements Listener {

    @EventHandler
    public void onTimeTrialFinish(TimeTrialFinishEvent event) {
        if (!RallyCoinManager.isEnabled()) return;

        Player player = event.getPlayer();
        var config = TimingSystem.getPlugin().getConfig();
        int baseReward = config.getInt("economy.coins.track_complete", 20);

        int reward = baseReward;
        String reason = "Track completion";

        // First completion bonus
        if (event.getOldBestTime() <= 0) {
            int multiplier = config.getInt("economy.coins.first_completion_multiplier", 3);
            reward *= multiplier;
            reason = "First track completion";
        }

        // Personal record bonus
        int recordBonus = 0;
        if (event.isNewBestTime() && event.getOldBestTime() > 0) {
            recordBonus = config.getInt("economy.coins.personal_record_bonus", 25);
        }

        int totalReward = reward + recordBonus;
        RallyCoinManager.addCoins(player.getUniqueId(), totalReward, reason);

        // Notify player
        Component message = Component.text("+" + totalReward + " \uD83E\uDE99", NamedTextColor.GOLD);
        if (recordBonus > 0) {
            message = message.append(Component.text(" (+" + recordBonus + " record bonus)", NamedTextColor.AQUA));
        }
        player.sendMessage(message);
    }
}
