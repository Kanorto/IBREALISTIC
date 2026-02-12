package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.economy.DailyChallengeManager;
import me.makkuusen.timing.system.economy.DailyChallengeManager.ChallengeType;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Info;
import org.bukkit.entity.Player;

import java.time.*;
import java.util.List;

@CommandAlias("daily")
public class CommandDaily extends BaseCommand {

    @Default
    @CommandPermission("timingsystem.daily.view")
    @Description("Show today's daily challenges")
    public static void onDaily(Player player) {
        List<ChallengeType> challenges = DailyChallengeManager.getTodayChallenges();
        if (challenges.isEmpty()) {
            Text.send(player, Info.DAILY_NO_CHALLENGES);
            return;
        }

        // Calculate time until reset
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        Duration remaining = Duration.between(now, midnight);
        long hours = remaining.toHours();
        long minutes = remaining.toMinutesPart();

        Text.send(player, Info.DAILY_TITLE);
        Text.send(player, Info.DAILY_RESET_TIME, "%time%", hours + "h " + minutes + "m");

        for (int i = 0; i < challenges.size(); i++) {
            ChallengeType ct = challenges.get(i);
            int progress = DailyChallengeManager.getProgress(player.getUniqueId(), i);
            boolean completed = progress >= ct.getTargetCount();

            String statusIcon = completed ? "✓" : "○";
            Text.send(player, Info.DAILY_CHALLENGE_ENTRY,
                    "%icon%", statusIcon,
                    "%description%", ct.getDescription(),
                    "%progress%", String.valueOf(Math.min(progress, ct.getTargetCount())),
                    "%target%", String.valueOf(ct.getTargetCount()));
            Text.send(player, Info.DAILY_CHALLENGE_REWARD,
                    "%coins%", String.valueOf(ct.getCoinReward()),
                    "%xp%", String.valueOf(ct.getXpReward()));
        }
    }

    @Subcommand("admin regenerate")
    @CommandPermission("timingsystem.daily.admin")
    @Description("Regenerate today's daily challenges (admin)")
    public static void onRegenerate(Player player) {
        DailyChallengeManager.regenerate();
        Text.send(player, Info.DAILY_REGENERATED);
    }
}
