package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.economy.GarageManager;
import me.makkuusen.timing.system.economy.LevelManager;
import me.makkuusen.timing.system.economy.PlayerCar;
import me.makkuusen.timing.system.economy.RallyCoinManager;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI for viewing a player's profile.
 * Displays level, XP, balance, statistics, and current car.
 */
public class ProfileGui extends BaseGui {

    private static final int ROWS = 4;
    private static final Runnable NO_OP = () -> {};

    private final TPlayer tPlayer;
    private final Player player;
    private final UUID targetUuid;
    private final String targetName;

    /**
     * Opens the profile for the player themselves.
     */
    public ProfileGui(TPlayer tPlayer) {
        this(tPlayer, tPlayer.getPlayer().getUniqueId(), tPlayer.getPlayer().getName());
    }

    /**
     * Opens the profile for a specific player (can view others).
     */
    public ProfileGui(TPlayer tPlayer, UUID targetUuid, String targetName) {
        super(Text.getGuiComponent(tPlayer.getPlayer(), Gui.PROFILE_TITLE), ROWS);
        this.tPlayer = tPlayer;
        this.player = tPlayer.getPlayer();
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        update();
    }

    private void update() {
        fillBackground();
        setPlayerHead();
        setLevelInfo();
        setEconomyInfo();
        setStatistics();
        setCarInfo();
        setNavigationRow();
    }

    private void fillBackground() {
        for (int slot = 0; slot < ROWS * 9; slot++) {
            setItem(GuiCommon.getBorderGlassButton(), slot);
        }
    }

    // ─── PLAYER HEAD (slot 4) ───

    private void setPlayerHead() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            Player target = org.bukkit.Bukkit.getPlayer(targetUuid);
            if (target != null) {
                meta.setOwningPlayer(target);
            }
            meta.displayName(Component.text(targetName, NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));

            List<Component> lore = new ArrayList<>();
            int level = LevelManager.isEnabled() ? LevelManager.getLevel(targetUuid) : 0;
            lore.add(Text.get(player, Gui.PROFILE_LEVEL, "%level%", String.valueOf(level)));
            meta.lore(lore);
            head.setItemMeta(meta);
        }

        GuiButton button = new GuiButton(head);
        button.setAction(NO_OP);
        setItem(button, 4);
    }

    // ─── LEVEL INFO (slot 10) ───

    private void setLevelInfo() {
        int level = LevelManager.isEnabled() ? LevelManager.getLevel(targetUuid) : 0;
        int xp = LevelManager.isEnabled() ? LevelManager.getXP(targetUuid) : 0;
        int totalXP = LevelManager.isEnabled() ? LevelManager.getTotalXP(targetUuid) : 0;
        int nextLevelXP = LevelManager.getXPForLevel(level + 1);
        float progress = LevelManager.getProgress(targetUuid);

        ItemStack item = new ItemBuilder(Material.EXPERIENCE_BOTTLE).setName(
                Text.get(player, Gui.PROFILE_LEVEL_TITLE)
        ).build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Text.get(player, Gui.PROFILE_LEVEL, "%level%", String.valueOf(level)));
            lore.add(Text.get(player, Gui.PROFILE_XP_PROGRESS, "%xp%", String.valueOf(xp), "%required%", String.valueOf(nextLevelXP)));

            // Progress bar
            int barLength = 20;
            int filled = (int) (progress * barLength);
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < barLength; i++) {
                bar.append(i < filled ? "█" : "░");
            }
            lore.add(Component.text(bar.toString(), filled > barLength / 2 ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Text.get(player, Gui.PROFILE_TOTAL_XP, "%xp%", String.valueOf(totalXP)));
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        item.setAmount(Math.max(1, Math.min(level, 64)));
        GuiButton button = new GuiButton(item);
        button.setAction(NO_OP);
        setItem(button, 10);
    }

    // ─── ECONOMY INFO (slot 12) ───

    private void setEconomyInfo() {
        int balance = RallyCoinManager.isEnabled() ? RallyCoinManager.getBalance(targetUuid) : 0;
        int totalEarned = RallyCoinManager.isEnabled() ? RallyCoinManager.getTotalEarned(targetUuid) : 0;
        int totalSpent = RallyCoinManager.isEnabled() ? RallyCoinManager.getTotalSpent(targetUuid) : 0;

        ItemStack item = new ItemBuilder(Material.GOLD_INGOT).setName(
                Text.get(player, Gui.PROFILE_BALANCE, "%balance%", RallyCoinManager.format(balance))
        ).build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Text.get(player, Gui.PROFILE_TOTAL_EARNED, "%amount%", RallyCoinManager.format(totalEarned)));
            lore.add(Text.get(player, Gui.PROFILE_TOTAL_SPENT, "%amount%", RallyCoinManager.format(totalSpent)));
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        GuiButton button = new GuiButton(item);
        button.setAction(NO_OP);
        setItem(button, 12);
    }

    // ─── STATISTICS (slot 14) ───

    private void setStatistics() {
        int tracksCompleted = 0;
        long totalTimeSpent = 0;
        int totalFinishes = 0;

        for (Track track : TrackDatabase.tracks) {
            int finishes = track.getTimeTrials().getPlayerTotalFinishes(targetUuid);
            if (finishes > 0) {
                tracksCompleted++;
                totalFinishes += finishes;
            }
            totalTimeSpent += track.getPlayerTotalTimeSpent(targetUuid);
        }

        ItemStack item = new ItemBuilder(Material.BOOK).setName(
                Text.get(player, Gui.PROFILE_STATISTICS)
        ).build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Text.get(player, Gui.PROFILE_TRACKS_COMPLETED, "%count%", String.valueOf(tracksCompleted)));
            lore.add(Text.get(player, Gui.PROFILE_TOTAL_FINISHES, "%count%", String.valueOf(totalFinishes)));
            lore.add(Text.get(player, Gui.PROFILE_TIME_SPENT, "%time%", ApiUtilities.formatAsTimeSpent(totalTimeSpent)));
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        GuiButton button = new GuiButton(item);
        button.setAction(NO_OP);
        setItem(button, 14);
    }

    // ─── CAR INFO (slot 16) ───

    private void setCarInfo() {
        PlayerCar activeCar = GarageManager.getActiveCar(targetUuid);

        ItemStack item;
        if (activeCar != null) {
            item = new ItemBuilder(Material.MINECART).setName(
                    Text.get(player, Gui.PROFILE_CAR, "%name%", activeCar.getName())
            ).build();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Type: " + GarageManager.getPresetName("type", activeCar.getVehicleType()), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Tires: " + GarageManager.getPresetName("tire", activeCar.getTirePreset()), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Engine: " + GarageManager.getPresetName("engine", activeCar.getEnginePreset()), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                item.setItemMeta(meta);
            }
        } else {
            item = new ItemBuilder(Material.BARRIER).setName(
                    Text.get(player, Gui.PROFILE_NO_CAR)
            ).build();
        }

        GuiButton button = new GuiButton(item);
        boolean isOwnProfile = targetUuid.equals(player.getUniqueId());
        if (isOwnProfile && activeCar != null) {
            button.setAction(() -> {
                PlaySound.buttonClick(tPlayer);
                new GarageGui(tPlayer).show(player);
            });
        } else {
            button.setAction(NO_OP);
        }
        setItem(button, 16);
    }

    // ─── NAVIGATION ROW (slots 27-35) ───

    private void setNavigationRow() {
        GuiButton returnButton = new GuiButton(new ItemBuilder(Material.ARROW).setName(
                Text.get(player, Gui.RETURN)
        ).build());
        returnButton.setAction(() -> {
            PlaySound.buttonClick(tPlayer);
            player.closeInventory();
        });
        setItem(returnButton, 27);
    }
}
