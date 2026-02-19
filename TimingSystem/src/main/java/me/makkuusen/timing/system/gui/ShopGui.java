package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.economy.GarageManager;
import me.makkuusen.timing.system.economy.LevelManager;
import me.makkuusen.timing.system.economy.PlayerCar;
import me.makkuusen.timing.system.economy.RallyCoinManager;
import me.makkuusen.timing.system.sounds.PlaySound;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Gui;
import me.makkuusen.timing.system.tplayer.TPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI for the vehicle upgrade shop.
 * Displays component categories in the top row and presets for the selected category.
 * Players can browse and purchase upgrades for their active car.
 */
public class ShopGui extends BaseGui {

    private static final int ROWS = 6;
    private static final int CATEGORY_ROW_START = 0;
    private static final int PRESET_ROW_START = 18; // Row 3 onwards (rows 0-1 for categories)
    private static final int PRESET_ROW_END = 44;
    private static final int NAV_ROW_START = 45;
    private static final int MAX_LEVEL_WHEN_DISABLED = 999;
    private static final Runnable NO_OP = () -> {};

    // Category definitions: component key, material, gui enum
    private static final String[] CATEGORY_KEYS = {
            "tire", "engine", "body", "suspension", "steering", "brake", "weight", "type",
            "exhaust", "differential", "gearbox", "turbo", "intercooler"
    };
    private static final Material[] CATEGORY_MATERIALS = {
            Material.LEATHER_HORSE_ARMOR, Material.PISTON, Material.IRON_CHESTPLATE,
            Material.CHAIN, Material.COMPASS, Material.REDSTONE,
            Material.ANVIL, Material.OAK_BOAT,
            Material.CAMPFIRE, Material.HOPPER, Material.LEVER,
            Material.FIREWORK_ROCKET, Material.PACKED_ICE
    };
    private static final Gui[] CATEGORY_LABELS = {
            Gui.SHOP_CATEGORY_TIRES, Gui.SHOP_CATEGORY_ENGINE, Gui.SHOP_CATEGORY_BODY,
            Gui.SHOP_CATEGORY_SUSPENSION, Gui.SHOP_CATEGORY_STEERING, Gui.SHOP_CATEGORY_BRAKES,
            Gui.SHOP_CATEGORY_WEIGHT, Gui.SHOP_CATEGORY_VEHICLE_TYPE,
            Gui.SHOP_CATEGORY_EXHAUST, Gui.SHOP_CATEGORY_DIFFERENTIAL, Gui.SHOP_CATEGORY_GEARBOX,
            Gui.SHOP_CATEGORY_TURBO, Gui.SHOP_CATEGORY_INTERCOOLER
    };

    private final TPlayer tPlayer;
    private final Player player;
    private String selectedCategory;

    public ShopGui(TPlayer tPlayer) {
        this(tPlayer, "tire");
    }

    public ShopGui(TPlayer tPlayer, String initialCategory) {
        super(Text.getGuiComponent(tPlayer.getPlayer(), Gui.SHOP_TITLE), ROWS);
        this.tPlayer = tPlayer;
        this.player = tPlayer.getPlayer();
        this.selectedCategory = initialCategory;
        update();
    }

    private void update() {
        setCategoryButtons();
        setPresetButtons();
        setNavigationRow();
    }

    // ─── CATEGORY ROWS (slots 0-8 + 9-12) ───

    private void setCategoryButtons() {
        // Fill row 2 with glass (between categories and presets)
        for (int slot = 9; slot < 18; slot++) {
            setItem(GuiCommon.getBorderGlassButton(), slot);
        }

        for (int i = 0; i < CATEGORY_KEYS.length; i++) {
            final String categoryKey = CATEGORY_KEYS[i];
            Material mat = CATEGORY_MATERIALS[i];
            Component name = Text.get(player, CATEGORY_LABELS[i]);

            // Add inventory summary to category lore
            UUID uuid = player.getUniqueId();
            List<Short> owned = GarageManager.getPurchasedPresets(uuid, categoryKey);
            int total = GarageManager.getPresetCount(categoryKey);

            // Highlight selected category
            if (categoryKey.equals(selectedCategory)) {
                mat = Material.GLOWSTONE_DUST;
            }

            ItemStack item = new ItemBuilder(mat).setName(name).build();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (categoryKey.equals(selectedCategory)) {
                    meta.setEnchantmentGlintOverride(true);
                }
                // Show inventory summary
                List<Component> lore = new ArrayList<>();
                lore.add(Text.get(player, Gui.SHOP_INVENTORY_SUMMARY,
                        "%owned%", String.valueOf(owned.size()),
                        "%total%", String.valueOf(total)));
                meta.lore(lore);
                item.setItemMeta(meta);
            }

            GuiButton button = new GuiButton(item);
            final int idx = i;
            button.setAction(() -> {
                PlaySound.buttonClick(tPlayer);
                new ShopGui(tPlayer, CATEGORY_KEYS[idx]).show(player);
            });
            // First 9 categories in row 0, next in row 1
            int slot = i < 9 ? (CATEGORY_ROW_START + i) : (9 + (i - 9));
            setItem(button, slot);
        }
        }

        // Slot 8: Active car info
        setActiveCarDisplay();
    }

    private void setActiveCarDisplay() {
        UUID uuid = player.getUniqueId();
        PlayerCar activeCar = GarageManager.getActiveCar(uuid);

        ItemStack item;
        if (activeCar != null) {
            item = new ItemBuilder(Material.MINECART).setName(
                    Component.text(activeCar.getName(), NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false)
            ).build();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                lore.add(Text.get(player, Gui.SHOP_ACTIVE_CAR));
                lore.add(Component.text(activeCar.getComponentSummary(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                item.setItemMeta(meta);
            }
        } else {
            item = new ItemBuilder(Material.BARRIER).setName(
                    Text.get(player, Gui.SHOP_NO_CAR)
            ).build();
        }

        GuiButton button = new GuiButton(item);
        button.setAction(NO_OP);
        setItem(button, 8);
    }

    // ─── PRESET ROWS (slots 18-44) ───

    private void setPresetButtons() {
        // Clear preset area
        for (int slot = PRESET_ROW_START; slot <= PRESET_ROW_END; slot++) {
            setItem(GuiCommon.getBorderGlassButton(), slot);
        }

        String[] names = GarageManager.getNamesForComponent(selectedCategory);
        if (names == null) return;

        UUID uuid = player.getUniqueId();
        PlayerCar activeCar = GarageManager.getActiveCar(uuid);
        int playerLevel = LevelManager.isEnabled() ? LevelManager.getLevel(uuid) : MAX_LEVEL_WHEN_DISABLED;
        int playerBalance = RallyCoinManager.isEnabled() ? RallyCoinManager.getBalance(uuid) : Integer.MAX_VALUE;
        short currentPreset = activeCar != null ? GarageManager.getCurrentPreset(activeCar, selectedCategory) : -1;
        List<Short> ownedPresets = GarageManager.getPurchasedPresets(uuid, selectedCategory);

        for (short i = 0; i < names.length && (PRESET_ROW_START + i) <= PRESET_ROW_END; i++) {
            int price = GarageManager.getPresetPrice(selectedCategory, i);
            int requiredLevel = GarageManager.getPresetLevel(selectedCategory, i);
            boolean isInstalled = (i == currentPreset);
            boolean isOwned = ownedPresets.contains(i);
            boolean hasLevel = playerLevel >= requiredLevel;
            boolean hasCoins = playerBalance >= price || price == 0;

            // Quantity-based: check available inventory
            int totalOwned = GarageManager.getPurchasedQuantity(uuid, selectedCategory, i);
            int installed = GarageManager.getInstalledCount(uuid, selectedCategory, i);
            int available = GarageManager.getAvailableQuantity(uuid, selectedCategory, i);
            boolean hasAvailable = available > 0 || i == 0 || price == 0;

            boolean canEquip = isOwned && !isInstalled && hasAvailable && activeCar != null;
            boolean canBuy = hasLevel && hasCoins && !isInstalled && activeCar != null;

            // Determine item color/material based on state
            Material presetMat;
            NamedTextColor nameColor;
            if (isInstalled) {
                presetMat = Material.LIME_DYE;
                nameColor = NamedTextColor.GREEN;
            } else if (canEquip) {
                presetMat = Material.LIGHT_BLUE_DYE;
                nameColor = NamedTextColor.AQUA;
            } else if (canBuy) {
                presetMat = Material.YELLOW_DYE;
                nameColor = NamedTextColor.YELLOW;
            } else {
                presetMat = Material.RED_DYE;
                nameColor = NamedTextColor.RED;
            }

            Component displayName = Component.text(names[i], nameColor)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, isInstalled);

            ItemStack item = new ItemBuilder(presetMat).setName(displayName).build();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();

                // Price
                if (price > 0) {
                    lore.add(Text.get(player, Gui.SHOP_PRESET_PRICE, "%price%", RallyCoinManager.format(price)));
                } else {
                    lore.add(Text.get(player, Gui.SHOP_PRESET_PRICE_FREE));
                }

                // Level requirement
                if (requiredLevel > 0) {
                    NamedTextColor levelColor = hasLevel ? NamedTextColor.GREEN : NamedTextColor.RED;
                    lore.add(Text.get(player, Gui.SHOP_PRESET_LEVEL_REQ, "%level%", String.valueOf(requiredLevel))
                            .color(levelColor));
                }

                // Quantity info (for non-free, non-default presets)
                if (i > 0 && price > 0) {
                    lore.add(Component.empty());
                    String ownedStr = totalOwned == Integer.MAX_VALUE ? "∞" : String.valueOf(totalOwned);
                    String installedStr = String.valueOf(installed);
                    String availableStr = available == Integer.MAX_VALUE ? "∞" : String.valueOf(available);
                    lore.add(Text.get(player, Gui.SHOP_QUANTITY_OWNED, "%qty%", ownedStr));
                    lore.add(Text.get(player, Gui.SHOP_QUANTITY_INSTALLED, "%qty%", installedStr));
                    lore.add(Text.get(player, Gui.SHOP_QUANTITY_AVAILABLE, "%qty%", availableStr));
                }

                // Status line
                lore.add(Component.empty());
                if (isInstalled) {
                    lore.add(Text.get(player, Gui.SHOP_PRESET_OWNED));
                } else if (canEquip) {
                    lore.add(Text.get(player, Gui.SHOP_PRESET_IN_INVENTORY));
                } else if (canBuy) {
                    lore.add(Text.get(player, Gui.SHOP_CLICK_TO_BUY));
                } else if (activeCar == null) {
                    lore.add(Text.get(player, Gui.SHOP_NO_CAR));
                } else if (!hasLevel) {
                    lore.add(Text.get(player, Gui.SHOP_LOCKED_LEVEL));
                } else if (!hasCoins) {
                    lore.add(Text.get(player, Gui.SHOP_LOCKED_COINS));
                }

                meta.lore(lore);
                item.setItemMeta(meta);
            }

            GuiButton button = new GuiButton(item);
            if (canEquip && !isInstalled) {
                // Already owned with available quantity — equip for free
                final short presetId = i;
                button.setAction(() -> equipOwnedPreset(presetId));
            } else if (canBuy && !isInstalled) {
                final short presetId = i;
                final int finalPrice = price;
                button.setAction(() -> purchasePreset(presetId, finalPrice));
            } else {
                button.setAction(NO_OP);
            }
            setItem(button, PRESET_ROW_START + i);
        }
    }

    private void purchasePreset(short presetId, int price) {
        UUID uuid = player.getUniqueId();
        PlayerCar activeCar = GarageManager.getActiveCar(uuid);
        if (activeCar == null) return;

        // Check if already purchased (shouldn't happen via GUI, but safety check)
        if (GarageManager.hasPurchased(uuid, selectedCategory, presetId)) {
            equipOwnedPreset(presetId);
            return;
        }

        // Spend coins
        if (price > 0 && RallyCoinManager.isEnabled()) {
            if (!RallyCoinManager.spendCoins(uuid, price, "Shop: " + selectedCategory + " → " + GarageManager.getPresetName(selectedCategory, presetId))) {
                Text.send(player, me.makkuusen.timing.system.theme.messages.Error.NOT_ENOUGH_COINS);
                return;
            }
        }

        // Record purchase in persistent inventory
        GarageManager.recordPurchase(uuid, selectedCategory, presetId);

        // Apply upgrade to active car
        boolean success = GarageManager.upgradeComponent(uuid, activeCar.getId(), selectedCategory, presetId);
        if (success) {
            // Play purchase sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5F, 1.5F);
            Text.send(player, me.makkuusen.timing.system.theme.messages.Success.GARAGE_COMPONENT_PURCHASED,
                    "%preset%", GarageManager.getPresetName(selectedCategory, presetId),
                    "%component%", selectedCategory,
                    "%cost%", RallyCoinManager.format(price));
        }

        // Refresh GUI preserving selected category
        new ShopGui(tPlayer, selectedCategory).show(player);
    }

    private void equipOwnedPreset(short presetId) {
        UUID uuid = player.getUniqueId();
        PlayerCar activeCar = GarageManager.getActiveCar(uuid);
        if (activeCar == null) return;

        // Apply upgrade to active car (no coin cost — already purchased)
        boolean success = GarageManager.upgradeComponent(uuid, activeCar.getId(), selectedCategory, presetId);
        if (success) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.MASTER, 0.3F, 1.2F);
            Text.send(player, me.makkuusen.timing.system.theme.messages.Success.GARAGE_COMPONENT_PURCHASED,
                    "%preset%", GarageManager.getPresetName(selectedCategory, presetId),
                    "%component%", selectedCategory,
                    "%cost%", RallyCoinManager.format(0));
        }

        // Refresh GUI preserving selected category
        new ShopGui(tPlayer, selectedCategory).show(player);
    }

    // ─── NAVIGATION ROW (slots 45-53) ───

    private void setNavigationRow() {
        // Fill with glass
        for (int slot = NAV_ROW_START; slot <= 53; slot++) {
            setItem(GuiCommon.getBorderGlassButton(), slot);
        }

        // Slot 45: Back/Return button
        GuiButton returnButton = new GuiButton(new ItemBuilder(Material.ARROW).setName(Text.get(player, Gui.RETURN)).build());
        returnButton.setAction(() -> {
            PlaySound.buttonClick(tPlayer);
            player.closeInventory();
        });
        setItem(returnButton, 45);

        // Slot 49: Balance display
        UUID uuid = player.getUniqueId();
        int balance = RallyCoinManager.isEnabled() ? RallyCoinManager.getBalance(uuid) : 0;
        ItemStack balanceItem = new ItemBuilder(Material.GOLD_INGOT).setName(
                Text.get(player, Gui.SHOP_BALANCE, "%balance%", RallyCoinManager.format(balance))
        ).build();
        GuiButton balanceButton = new GuiButton(balanceItem);
        balanceButton.setAction(NO_OP);
        setItem(balanceButton, 49);

        // Slot 50: Level display
        int level = LevelManager.isEnabled() ? LevelManager.getLevel(uuid) : 0;
        ItemStack levelItem = new ItemBuilder(Material.EXPERIENCE_BOTTLE).setName(
                Text.get(player, Gui.SHOP_LEVEL, "%level%", String.valueOf(level))
        ).build();
        GuiButton levelButton = new GuiButton(levelItem);
        levelButton.setAction(NO_OP);
        setItem(levelButton, 50);
    }
}
