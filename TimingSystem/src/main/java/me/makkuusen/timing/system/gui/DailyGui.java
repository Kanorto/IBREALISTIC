package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.economy.DailyChallengeManager;
import me.makkuusen.timing.system.economy.DailyChallengeManager.ChallengeType;
import me.makkuusen.timing.system.economy.RallyCoinManager;
import me.makkuusen.timing.system.sounds.PlaySound;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Gui;
import me.makkuusen.timing.system.tplayer.TPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI for viewing daily challenges.
 * Shows 3 daily challenges with progress bars and rewards.
 */
public class DailyGui extends BaseGui {

    private static final int ROWS = 3;
    private static final int[] CHALLENGE_SLOTS = {11, 13, 15};
    private static final Runnable NO_OP = () -> {};

    private final TPlayer tPlayer;
    private final Player player;

    public DailyGui(TPlayer tPlayer) {
        super(Text.getGuiComponent(tPlayer.getPlayer(), Gui.DAILY_TITLE), ROWS);
        this.tPlayer = tPlayer;
        this.player = tPlayer.getPlayer();
        update();
    }

    private void update() {
        fillBackground();
        setChallengeSlots();
        setTimerSlot();
        setNavigationRow();
    }

    private void fillBackground() {
        for (int slot = 0; slot < ROWS * 9; slot++) {
            setItem(GuiCommon.getBorderGlassButton(), slot);
        }
    }

    // ─── CHALLENGE SLOTS ───

    private void setChallengeSlots() {
        List<ChallengeType> challenges = DailyChallengeManager.getTodayChallenges();
        UUID uuid = player.getUniqueId();

        for (int i = 0; i < CHALLENGE_SLOTS.length; i++) {
            if (i < challenges.size()) {
                setChallengeButton(CHALLENGE_SLOTS[i], challenges.get(i), i, uuid);
            }
        }
    }

    private void setChallengeButton(int slot, ChallengeType challenge, int challengeSlot, UUID uuid) {
        int progress = DailyChallengeManager.getProgress(uuid, challengeSlot);
        int target = challenge.getTargetCount();
        boolean completed = progress >= target;

        Material mat = completed ? Material.LIME_CONCRETE : Material.YELLOW_CONCRETE;
        NamedTextColor titleColor = completed ? NamedTextColor.GREEN : NamedTextColor.YELLOW;

        Component name = Component.text(challenge.getDescription(), titleColor)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true);

        ItemStack item = new ItemBuilder(mat).setName(name).build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();

            // Progress bar
            int barLength = 20;
            int capped = Math.min(progress, target);
            float ratio = target > 0 ? (float) capped / target : 1.0f;
            int filled = (int) (ratio * barLength);
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < barLength; i++) {
                bar.append(i < filled ? "█" : "░");
            }
            NamedTextColor barColor = completed ? NamedTextColor.GREEN : NamedTextColor.GOLD;
            lore.add(Component.text(bar.toString() + " " + capped + "/" + target, barColor)
                    .decoration(TextDecoration.ITALIC, false));

            // Reward info
            lore.add(Component.empty());
            lore.add(Text.get(player, Gui.DAILY_REWARD,
                    "%coins%", String.valueOf(challenge.getCoinReward()),
                    "%xp%", String.valueOf(challenge.getXpReward())));

            // Status
            lore.add(Component.empty());
            if (completed) {
                lore.add(Text.get(player, Gui.DAILY_COMPLETED));
            } else {
                lore.add(Text.get(player, Gui.DAILY_IN_PROGRESS));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        GuiButton button = new GuiButton(item);
        button.setAction(NO_OP);
        setItem(button, slot);
    }

    // ─── TIMER (slot 4) ───

    private void setTimerSlot() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        Duration remaining = Duration.between(now, midnight);
        long hours = remaining.toHours();
        long minutes = remaining.toMinutesPart();

        ItemStack item = new ItemBuilder(Material.CLOCK).setName(
                Text.get(player, Gui.DAILY_RESET_TIMER, "%hours%", String.valueOf(hours), "%minutes%", String.valueOf(minutes))
        ).build();

        GuiButton button = new GuiButton(item);
        button.setAction(NO_OP);
        setItem(button, 4);
    }

    // ─── NAVIGATION ROW ───

    private void setNavigationRow() {
        GuiButton returnButton = new GuiButton(new ItemBuilder(Material.ARROW).setName(
                Text.get(player, Gui.RETURN)
        ).build());
        returnButton.setAction(() -> {
            PlaySound.buttonClick(tPlayer);
            player.closeInventory();
        });
        setItem(returnButton, 18);
    }
}
