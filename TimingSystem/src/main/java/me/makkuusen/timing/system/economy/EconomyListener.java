package me.makkuusen.timing.system.economy;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.events.TimeTrialFinishEvent;
import me.makkuusen.timing.system.api.events.driver.DriverFinishHeatEvent;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Info;
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

            Text.send(player, Info.ECONOMY_COINS_REWARD, "%amount%", String.valueOf(totalCoinReward));
            if (recordBonus > 0) {
                Text.send(player, Info.ECONOMY_COINS_RECORD_BONUS, "%bonus%", String.valueOf(recordBonus));
            }
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

            Text.send(player, Info.ECONOMY_XP_REWARD, "%amount%", String.valueOf(totalXP));
            if (xpRecordBonus > 0) {
                Text.send(player, Info.ECONOMY_XP_RECORD_BONUS, "%bonus%", String.valueOf(xpRecordBonus));
            }
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
            Text.send(player, Info.ECONOMY_EVENT_XP, "%amount%", String.valueOf(participationXP));
        }

        // Coins for event participation
        if (RallyCoinManager.isEnabled()) {
            int participationCoins = config.getInt("economy.coins.event_participation", 30);
            RallyCoinManager.addCoins(player.getUniqueId(), participationCoins, "Event participation");
            Text.send(player, Info.ECONOMY_EVENT_COINS, "%amount%", String.valueOf(participationCoins));
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
                Text.send(player, Info.ECONOMY_POSITION_BONUS,
                        "%amount%", String.valueOf(positionBonus),
                        "%position%", String.valueOf(position));
            }

            // Extra XP for winning
            if (position == 1 && LevelManager.isEnabled()) {
                int winXP = config.getInt("levels.rewards.event_win", 100);
                LevelManager.addXP(player.getUniqueId(), winXP, "Event win");
                Text.send(player, Info.ECONOMY_EVENT_WIN_XP, "%amount%", String.valueOf(winXP));
            }
        }
    }
}
