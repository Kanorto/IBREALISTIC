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
 * GUI for the player's garage.
 * Shows owned cars, their components, and actions (select, delete, open shop).
 */
public class GarageGui extends BaseGui {

    private static final int ROWS = 6;
    private static final int MAX_CAR_SLOTS = 5;
    private static final int COMPONENT_ROW_START = 9;
    private static final int STATS_ROW_START = 18;
    private static final int NAV_ROW_START = 45;
    private static final Runnable NO_OP = () -> {};

    // Component display order
    private static final String[] COMPONENT_KEYS = {"type", "tire", "engine", "body", "suspension", "steering", "brake", "weight"};
    private static final Material[] COMPONENT_MATERIALS = {
            Material.OAK_BOAT, Material.LEATHER_HORSE_ARMOR, Material.PISTON, Material.IRON_CHESTPLATE,
            Material.CHAIN, Material.COMPASS, Material.REDSTONE, Material.ANVIL
    };
    private static final Gui[] COMPONENT_LABELS = {
            Gui.SHOP_CATEGORY_VEHICLE_TYPE, Gui.SHOP_CATEGORY_TIRES, Gui.SHOP_CATEGORY_ENGINE, Gui.SHOP_CATEGORY_BODY,
            Gui.SHOP_CATEGORY_SUSPENSION, Gui.SHOP_CATEGORY_STEERING, Gui.SHOP_CATEGORY_BRAKES, Gui.SHOP_CATEGORY_WEIGHT
    };

    private final TPlayer tPlayer;
    private final Player player;
    private int selectedCarIndex;

    public GarageGui(TPlayer tPlayer) {
        this(tPlayer, 0);
    }

    public GarageGui(TPlayer tPlayer, int selectedCarIndex) {
        super(Text.getGuiComponent(tPlayer.getPlayer(), Gui.GARAGE_TITLE), ROWS);
        this.tPlayer = tPlayer;
        this.player = tPlayer.getPlayer();
        this.selectedCarIndex = Math.max(0, Math.min(selectedCarIndex, MAX_CAR_SLOTS - 1));
        update();
    }

    private void update() {
        fillBackground();
        setCarSlots();
        setComponentSlots();
        setNavigationRow();
    }

    private void fillBackground() {
        for (int slot = 5; slot <= 8; slot++) {
            setItem(GuiCommon.getBorderGlassButton(), slot);
        }
        for (int slot = COMPONENT_ROW_START + COMPONENT_KEYS.length; slot < STATS_ROW_START; slot++) {
            setItem(GuiCommon.getBorderGlassButton(), slot);
        }
        for (int slot = STATS_ROW_START; slot < NAV_ROW_START; slot++) {
            setItem(GuiCommon.getBorderGlassButton(), slot);
        }
    }

    // ─── CAR SLOTS (0-4) ───

    private void setCarSlots() {
        List<PlayerCar> cars = GarageManager.getCars(player.getUniqueId());

        for (int i = 0; i < MAX_CAR_SLOTS; i++) {
            if (i < cars.size()) {
                PlayerCar car = cars.get(i);
                setOccupiedCarSlot(i, car, i == selectedCarIndex);
            } else {
                setEmptyCarSlot(i);
            }
        }
    }

    private void setOccupiedCarSlot(int slotIndex, PlayerCar car, boolean isSelected) {
        Material mat = car.isActive() ? Material.GOLDEN_HORSE_ARMOR : Material.IRON_HORSE_ARMOR;
        NamedTextColor color = car.isActive() ? NamedTextColor.GOLD : NamedTextColor.WHITE;

        Component name = Component.text(car.getName(), color)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, isSelected);

        ItemStack item = new ItemBuilder(mat).setName(name).build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            if (car.isActive()) {
                lore.add(Text.get(player, Gui.GARAGE_CAR_ACTIVE_LABEL));
            }
            lore.add(Component.text(car.getComponentSummary(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            if (isSelected) {
                lore.add(Component.empty());
                lore.add(Text.get(player, Gui.GARAGE_SELECTED));
            }
            meta.lore(lore);
            if (isSelected) {
                meta.setEnchantmentGlintOverride(true);
            }
            item.setItemMeta(meta);
        }

        GuiButton button = new GuiButton(item);
        final int idx = slotIndex;
        button.setAction(() -> {
            PlaySound.buttonClick(tPlayer);
            new GarageGui(tPlayer, idx).show(player);
        });
        setItem(button, slotIndex);
    }

    private void setEmptyCarSlot(int slotIndex) {
        ItemStack item = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).setName(
                Text.get(player, Gui.GARAGE_EMPTY_SLOT)
        ).build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Text.get(player, Gui.GARAGE_CREATE_CAR));
            lore.add(Component.text("Slot #" + (slotIndex + 1), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        GuiButton button = new GuiButton(item);
        button.setAction(() -> {
            PlaySound.buttonClick(tPlayer);
            // Create a car with a default name
            String defaultName = "Car " + (slotIndex + 1);
            PlayerCar newCar = GarageManager.createCar(player.getUniqueId(), defaultName);
            if (newCar != null) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5F, 1.2F);
                Text.send(player, me.makkuusen.timing.system.theme.messages.Success.GARAGE_CAR_CREATED,
                        "%name%", defaultName);
                new GarageGui(tPlayer, slotIndex).show(player);
            } else {
                Text.send(player, me.makkuusen.timing.system.theme.messages.Error.GARAGE_FULL,
                        "%max%", String.valueOf(MAX_CAR_SLOTS));
            }
        });
        setItem(button, slotIndex);
    }

    // ─── COMPONENT SLOTS (9-16) ───

    private void setComponentSlots() {
        List<PlayerCar> cars = GarageManager.getCars(player.getUniqueId());
        PlayerCar selectedCar = (selectedCarIndex < cars.size()) ? cars.get(selectedCarIndex) : null;

        for (int i = 0; i < COMPONENT_KEYS.length; i++) {
            if (selectedCar != null) {
                setComponentButton(i, selectedCar);
            } else {
                setItem(GuiCommon.getBorderGlassButton(), COMPONENT_ROW_START + i);
            }
        }
    }

    private void setComponentButton(int componentIndex, PlayerCar car) {
        String compKey = COMPONENT_KEYS[componentIndex];
        Material mat = COMPONENT_MATERIALS[componentIndex];

        short currentPreset = GarageManager.getCurrentPreset(car, compKey);
        String presetName = GarageManager.getPresetName(compKey, currentPreset);

        Component name = Text.get(player, COMPONENT_LABELS[componentIndex]);

        ItemStack item = new ItemBuilder(mat).setName(name).build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(presetName, NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Text.get(player, Gui.GARAGE_OPEN_SHOP));
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        GuiButton button = new GuiButton(item);
        final String category = compKey;
        button.setAction(() -> {
            PlaySound.buttonClick(tPlayer);
            new ShopGui(tPlayer, category).show(player);
        });
        setItem(button, COMPONENT_ROW_START + componentIndex);
    }

    // ─── NAVIGATION ROW (slots 45-53) ───

    private void setNavigationRow() {
        for (int slot = NAV_ROW_START; slot <= 53; slot++) {
            setItem(GuiCommon.getBorderGlassButton(), slot);
        }

        // Slot 45: Return button
        GuiButton returnButton = new GuiButton(new ItemBuilder(Material.ARROW).setName(
                Text.get(player, Gui.RETURN)
        ).build());
        returnButton.setAction(() -> {
            PlaySound.buttonClick(tPlayer);
            player.closeInventory();
        });
        setItem(returnButton, 45);

        // Slot 48: Select active car
        List<PlayerCar> cars = GarageManager.getCars(player.getUniqueId());
        PlayerCar selectedCar = (selectedCarIndex < cars.size()) ? cars.get(selectedCarIndex) : null;

        if (selectedCar != null && !selectedCar.isActive()) {
            GuiButton selectButton = new GuiButton(new ItemBuilder(Material.EMERALD).setName(
                    Text.get(player, Gui.GARAGE_SELECT_CAR)
            ).build());
            selectButton.setAction(() -> {
                PlaySound.buttonClick(tPlayer);
                GarageManager.selectCar(player.getUniqueId(), selectedCar.getId());
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0.5F, 1.0F);
                Text.send(player, me.makkuusen.timing.system.theme.messages.Success.GARAGE_CAR_SELECTED,
                        "%name%", selectedCar.getName());
                new GarageGui(tPlayer, selectedCarIndex).show(player);
            });
            setItem(selectButton, 48);
        }

        // Slot 49: Delete car
        if (selectedCar != null) {
            GuiButton deleteButton = new GuiButton(new ItemBuilder(Material.BARRIER).setName(
                    Text.get(player, Gui.GARAGE_DELETE_CAR)
            ).build());
            deleteButton.setAction(() -> {
                PlaySound.buttonClick(tPlayer);
                GarageManager.deleteCar(player.getUniqueId(), selectedCar.getId());
                Text.send(player, me.makkuusen.timing.system.theme.messages.Success.GARAGE_CAR_DELETED,
                        "%name%", selectedCar.getName());
                new GarageGui(tPlayer, 0).show(player);
            });
            setItem(deleteButton, 49);
        }

        // Slot 52: Open shop
        GuiButton shopButton = new GuiButton(new ItemBuilder(Material.GOLD_INGOT).setName(
                Text.get(player, Gui.GARAGE_OPEN_SHOP)
        ).build());
        shopButton.setAction(() -> {
            PlaySound.buttonClick(tPlayer);
            new ShopGui(tPlayer).show(player);
        });
        setItem(shopButton, 52);

        // Slot 53: Balance display
        UUID uuid = player.getUniqueId();
        int balance = RallyCoinManager.isEnabled() ? RallyCoinManager.getBalance(uuid) : 0;
        ItemStack balanceItem = new ItemBuilder(Material.SUNFLOWER).setName(
                Text.get(player, Gui.SHOP_BALANCE, "%balance%", RallyCoinManager.format(balance))
        ).build();
        GuiButton balanceButton = new GuiButton(balanceItem);
        balanceButton.setAction(NO_OP);
        setItem(balanceButton, 53);
    }
}
