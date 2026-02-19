package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.economy.GarageManager;
import me.makkuusen.timing.system.sounds.PlaySound;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Gui;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for selecting race difficulty before starting a time trial.
 * Shows 5 difficulty levels with reward multipliers and restrictions.
 * Higher difficulty = better rewards but more restrictions on custom parts.
 */
public class DifficultyGui extends BaseGui {

    private static final int ROWS = 3;
    private static final Runnable NO_OP = () -> {};

    // Difficulty level definitions
    private static final Material[] DIFFICULTY_MATERIALS = {
            Material.LIME_DYE, Material.GREEN_DYE, Material.YELLOW_DYE,
            Material.ORANGE_DYE, Material.RED_DYE
    };
    private static final Gui[] DIFFICULTY_LABELS = {
            Gui.DIFFICULTY_STAR_1, Gui.DIFFICULTY_STAR_2, Gui.DIFFICULTY_STAR_3,
            Gui.DIFFICULTY_STAR_4, Gui.DIFFICULTY_STAR_5
    };
    private static final float[] COIN_MULTIPLIERS = {1.0f, 1.3f, 1.6f, 2.0f, 2.5f};
    private static final float[] XP_MULTIPLIERS = {1.0f, 1.2f, 1.5f, 1.8f, 2.2f};

    // Maximum allowed preset level for restricted difficulties (0 = standard only)
    private static final int[] MAX_CUSTOM_LEVEL = {
            Integer.MAX_VALUE, // ★☆☆☆☆ Easy — no restrictions
            Integer.MAX_VALUE, // ★★☆☆☆ Normal — no restrictions
            10,                // ★★★☆☆ Hard — max preset level 10
            5,                 // ★★★★☆ Expert — max preset level 5
            0                  // ★★★★★ Extreme — standard parts only
    };

    private final TPlayer tPlayer;
    private final Player player;
    private final Track track;

    public DifficultyGui(TPlayer tPlayer, Track track) {
        super(Text.getGuiComponent(tPlayer.getPlayer(), Gui.DIFFICULTY_TITLE), ROWS);
        this.tPlayer = tPlayer;
        this.player = tPlayer.getPlayer();
        this.track = track;
        update();
    }

    private void update() {
        // Fill background
        for (int slot = 0; slot < ROWS * 9; slot++) {
            setItem(GuiCommon.getBorderGlassButton(), slot);
        }

        // Place 5 difficulty buttons centered in row 2 (slots 11-15)
        for (int i = 0; i < 5; i++) {
            setDifficultyButton(i, 11 + i);
        }

        // Return button at slot 18
        GuiButton returnButton = new GuiButton(new ItemBuilder(Material.ARROW).setName(
                Text.get(player, Gui.RETURN)
        ).build());
        returnButton.setAction(() -> {
            PlaySound.buttonClick(tPlayer);
            player.closeInventory();
        });
        setItem(returnButton, 18);
    }

    private void setDifficultyButton(int difficultyIndex, int slot) {
        int difficulty = difficultyIndex + 1;
        Component name = Text.get(player, DIFFICULTY_LABELS[difficultyIndex]);

        ItemStack item = new ItemBuilder(DIFFICULTY_MATERIALS[difficultyIndex])
                .setName(name).build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();

            // Reward multiplier
            String coinMult = String.format("%.1f", COIN_MULTIPLIERS[difficultyIndex]);
            lore.add(Text.get(player, Gui.DIFFICULTY_REWARD_MULTIPLIER, "%multiplier%", coinMult));

            lore.add(Component.empty());

            // Restrictions
            int maxLevel = MAX_CUSTOM_LEVEL[difficultyIndex];
            if (maxLevel == Integer.MAX_VALUE) {
                lore.add(Text.get(player, Gui.DIFFICULTY_RESTRICTION_NONE));
            } else if (maxLevel == 0) {
                lore.add(Text.get(player, Gui.DIFFICULTY_RESTRICTION_STANDARD_ONLY));
            } else {
                lore.add(Text.get(player, Gui.DIFFICULTY_RESTRICTION_LIMITED,
                        "%max_level%", String.valueOf(maxLevel)));
            }

            lore.add(Component.empty());
            lore.add(Text.get(player, Gui.DIFFICULTY_CLICK_TO_SELECT));

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        GuiButton button = new GuiButton(item);
        final int diff = difficulty;
        button.setAction(() -> {
            PlaySound.buttonClick(tPlayer);
            startRaceWithDifficulty(diff);
        });
        setItem(button, slot);
    }

    private void startRaceWithDifficulty(int difficulty) {
        if (!track.getSpawnLocation().isWorldLoaded()) {
            Text.send(player, me.makkuusen.timing.system.theme.messages.Error.WORLD_NOT_LOADED);
            return;
        }
        // Store selected difficulty in player data for SoloRaceManager to use
        tPlayer.setSelectedDifficulty(difficulty);
        ApiUtilities.teleportPlayerAndSpawnBoat(player, track, track.getSpawnLocation());
        player.closeInventory();
    }

    /**
     * Returns the maximum allowed preset level for a given difficulty.
     * Used by the race system to enforce restrictions.
     */
    public static int getMaxCustomLevel(int difficulty) {
        if (difficulty < 1 || difficulty > 5) return Integer.MAX_VALUE;
        return MAX_CUSTOM_LEVEL[difficulty - 1];
    }

    /**
     * Returns the coin reward multiplier for a given difficulty.
     */
    public static float getCoinMultiplier(int difficulty) {
        if (difficulty < 1 || difficulty > 5) return 1.0f;
        return COIN_MULTIPLIERS[difficulty - 1];
    }

    /**
     * Returns the XP reward multiplier for a given difficulty.
     */
    public static float getXpMultiplier(int difficulty) {
        if (difficulty < 1 || difficulty > 5) return 1.0f;
        return XP_MULTIPLIERS[difficulty - 1];
    }
}
