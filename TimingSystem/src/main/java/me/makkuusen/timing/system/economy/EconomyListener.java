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
        Player player = event.getPlayer();
        var config = TimingSystem.getPlugin().getConfig();

        // ─── COINS ───
        if (RallyCoinManager.isEnabled()) {
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

            int totalCoinReward = reward + recordBonus;
            RallyCoinManager.addCoins(player.getUniqueId(), totalCoinReward, reason);

            Component coinMsg = Component.text("+" + totalCoinReward + " \uD83E\uDE99", NamedTextColor.GOLD);
            if (recordBonus > 0) {
                coinMsg = coinMsg.append(Component.text(" (+" + recordBonus + " record bonus)", NamedTextColor.AQUA));
            }
            player.sendMessage(coinMsg);
        }

        // ─── XP ───
        if (LevelManager.isEnabled()) {
            int baseXP = config.getInt("levels.rewards.track_complete", 20);
            int xpReward = baseXP;

            // First completion bonus (×2)
            if (event.getOldBestTime() <= 0) {
                xpReward *= 2;
            }

            // Personal record bonus
            int xpRecordBonus = 0;
            if (event.isNewBestTime() && event.getOldBestTime() > 0) {
                xpRecordBonus = config.getInt("levels.rewards.personal_record", 30);
            }

            int totalXP = xpReward + xpRecordBonus;
            LevelManager.addXP(player.getUniqueId(), totalXP, "Track completion");

            Component xpMsg = Component.text("+" + totalXP + " XP", NamedTextColor.GREEN);
            if (xpRecordBonus > 0) {
                xpMsg = xpMsg.append(Component.text(" (+" + xpRecordBonus + " record)", NamedTextColor.DARK_GREEN));
            }
            player.sendMessage(xpMsg);
        }

        // ─── DAILY CHALLENGES ───
        boolean isNewRecord = event.isNewBestTime();
        DailyChallengeManager.onTrackComplete(player.getUniqueId(), isNewRecord, false, "track");
    }
}
