package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.economy.DailyChallengeManager;
import me.makkuusen.timing.system.economy.DailyChallengeManager.ChallengeType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
            player.sendMessage(Component.text("No daily challenges available.", NamedTextColor.GRAY));
            return;
        }

        // Calculate time until reset
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        Duration remaining = Duration.between(now, midnight);
        long hours = remaining.toHours();
        long minutes = remaining.toMinutesPart();

        player.sendMessage(Component.empty()
                .append(Component.text("━━━ ", NamedTextColor.GOLD))
                .append(Component.text("Daily Challenges", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" ━━━", NamedTextColor.GOLD)));
        player.sendMessage(Component.text("Resets in: ", NamedTextColor.GRAY)
                .append(Component.text(hours + "h " + minutes + "m", NamedTextColor.WHITE)));
        player.sendMessage(Component.empty());

        for (int i = 0; i < challenges.size(); i++) {
            ChallengeType ct = challenges.get(i);
            int progress = DailyChallengeManager.getProgress(player.getUniqueId(), i);
            boolean completed = progress >= ct.getTargetCount();

            String statusIcon = completed ? "✓" : "○";
            NamedTextColor statusColor = completed ? NamedTextColor.GREEN : NamedTextColor.WHITE;
            NamedTextColor descColor = completed ? NamedTextColor.DARK_GREEN : NamedTextColor.GRAY;

            player.sendMessage(Component.text(statusIcon + " ", statusColor)
                    .append(Component.text(ct.getDescription(), descColor))
                    .append(Component.text(" [" + Math.min(progress, ct.getTargetCount()) + "/" + ct.getTargetCount() + "]",
                            completed ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY)));
            player.sendMessage(Component.text("   Reward: ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(ct.getCoinReward() + " 🪙 ", NamedTextColor.GOLD))
                    .append(Component.text("+" + ct.getXpReward() + " XP", NamedTextColor.AQUA)));
        }
    }

    @Subcommand("admin regenerate")
    @CommandPermission("timingsystem.daily.admin")
    @Description("Regenerate today's daily challenges (admin)")
    public static void onRegenerate(Player player) {
        DailyChallengeManager.regenerate();
        player.sendMessage(Component.text("Daily challenges regenerated.", NamedTextColor.GREEN));
    }
}
