package me.makkuusen.timing.system.economy;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.events.TimeTrialFinishEvent;
import me.makkuusen.timing.system.api.events.driver.DriverFinishHeatEvent;
import me.makkuusen.timing.system.participant.Driver;
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

    // ─── EVENT/HEAT REWARDS ───

    @EventHandler
    public void onDriverFinishHeat(DriverFinishHeatEvent event) {
        Driver driver = event.getDriver();
        Player player = driver.getTPlayer().getPlayer();
        if (player == null) return;
        var config = TimingSystem.getPlugin().getConfig();

        // XP for event participation
        if (LevelManager.isEnabled()) {
            int participationXP = config.getInt("levels.rewards.event_participation", 50);
            LevelManager.addXP(player.getUniqueId(), participationXP, "Event participation");
            player.sendMessage(Component.text("+" + participationXP + " XP (event)", NamedTextColor.GREEN));
        }

        // Coins for event participation
        if (RallyCoinManager.isEnabled()) {
            int participationCoins = 30;
            RallyCoinManager.addCoins(player.getUniqueId(), participationCoins, "Event participation");
            player.sendMessage(Component.text("+" + participationCoins + " \uD83E\uDE99 (event)", NamedTextColor.GOLD));
        }

        // Bonus for position (top 3)
        int position = driver.getPosition();
        if (position >= 1 && position <= 3) {
            int positionBonus = switch (position) {
                case 1 -> config.getInt("economy.coins.leaderboard_top1", 100);
                case 2 -> config.getInt("economy.coins.leaderboard_top2", 50);
                case 3 -> config.getInt("economy.coins.leaderboard_top3", 25);
                default -> 0;
            };
            if (positionBonus > 0 && RallyCoinManager.isEnabled()) {
                RallyCoinManager.addCoins(player.getUniqueId(), positionBonus, "Event position #" + position);
                player.sendMessage(Component.text("+" + positionBonus + " \uD83E\uDE99 (#" + position + " finish!)", NamedTextColor.GOLD));
            }

            // Extra XP for winning
            if (position == 1 && LevelManager.isEnabled()) {
                int winXP = config.getInt("levels.rewards.event_win", 100);
                LevelManager.addXP(player.getUniqueId(), winXP, "Event win");
                player.sendMessage(Component.text("+" + winXP + " XP (event win!)", NamedTextColor.GREEN));
            }
        }
    }
}
