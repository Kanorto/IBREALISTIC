package me.makkuusen.timing.system.team;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.economy.RallyCoinManager;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

/**
 * Manages spare parts required for pit stops.
 * Mechanics must have the correct spare parts in their inventory to perform tasks.
 * Parts are purchased from the shop.
 */
public class SparePartsManager {

    private static final String SPARE_PART_KEY = "spare_part";
    public static final String PART_TIRE_SET = "tire_set";
    public static final String PART_FUEL_CANISTER = "fuel_canister";
    public static final String PART_REPAIR_KIT = "repair_kit";

    // ─── COSTS (configurable) ───
    private static final int DEFAULT_TIRE_SET_COST = 200;
    private static final int DEFAULT_FUEL_CANISTER_COST = 100;
    private static final int DEFAULT_REPAIR_KIT_COST = 150;

    private static NamespacedKey sparePartKey;
    private static NamespacedKey compoundKey;

    public static void initialize() {
        sparePartKey = new NamespacedKey(TimingSystem.getPlugin(), SPARE_PART_KEY);
        compoundKey = new NamespacedKey(TimingSystem.getPlugin(), "tire_compound");
    }

    // ─── COSTS ───

    public static int getTireSetCost(TireCompound compound) {
        int base = TimingSystem.getPlugin().getConfig().getInt(
                "team_race.spare_parts.tire_set_cost", DEFAULT_TIRE_SET_COST);
        return base + compound.getCost();
    }

    public static int getFuelCanisterCost() {
        return TimingSystem.getPlugin().getConfig().getInt(
                "team_race.spare_parts.fuel_canister_cost", DEFAULT_FUEL_CANISTER_COST);
    }

    public static int getRepairKitCost() {
        return TimingSystem.getPlugin().getConfig().getInt(
                "team_race.spare_parts.repair_kit_cost", DEFAULT_REPAIR_KIT_COST);
    }

    // ─── PURCHASE ───

    public static boolean purchaseTireSet(Player player, TireCompound compound) {
        int cost = getTireSetCost(compound);
        if (!RallyCoinManager.spendCoins(player.getUniqueId(), cost,
                "Tire Set (" + compound.getDisplayName() + ")")) {
            Text.send(player, Error.NOT_ENOUGH_COINS);
            return false;
        }
        ItemStack item = createTireSetItem(compound);
        player.getInventory().addItem(item);
        Text.send(player, Success.SPARE_PARTS_PURCHASED);
        return true;
    }

    public static boolean purchaseFuelCanister(Player player) {
        int cost = getFuelCanisterCost();
        if (!RallyCoinManager.spendCoins(player.getUniqueId(), cost, "Fuel Canister")) {
            Text.send(player, Error.NOT_ENOUGH_COINS);
            return false;
        }
        ItemStack item = createFuelCanisterItem();
        player.getInventory().addItem(item);
        Text.send(player, Success.SPARE_PARTS_PURCHASED);
        return true;
    }

    public static boolean purchaseRepairKit(Player player) {
        int cost = getRepairKitCost();
        if (!RallyCoinManager.spendCoins(player.getUniqueId(), cost, "Repair Kit")) {
            Text.send(player, Error.NOT_ENOUGH_COINS);
            return false;
        }
        ItemStack item = createRepairKitItem();
        player.getInventory().addItem(item);
        Text.send(player, Success.SPARE_PARTS_PURCHASED);
        return true;
    }

    // ─── ITEM CREATION ───

    public static ItemStack createTireSetItem(TireCompound compound) {
        ItemStack item = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("🛞 Tire Set — " + compound.getDisplayName(), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Compound: " + compound.getDisplayName(), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Dry Grip: " + Math.round(compound.getDryGrip() * 100) + "%", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Wet Grip: " + Math.round(compound.getWetGrip() * 100) + "%", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Wear Rate: " + Math.round(compound.getWearRate() * 100) + "%", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Use during pit stop to change tires", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(sparePartKey, PersistentDataType.STRING, PART_TIRE_SET);
        meta.getPersistentDataContainer().set(compoundKey, PersistentDataType.INTEGER, compound.getId());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createFuelCanisterItem() {
        ItemStack item = new ItemStack(Material.HONEY_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("⛽ Fuel Canister", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Use during pit stop to refuel", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(sparePartKey, PersistentDataType.STRING, PART_FUEL_CANISTER);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createRepairKitItem() {
        ItemStack item = new ItemStack(Material.IRON_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("🔩 Repair Kit", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Use during pit stop to repair body", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(sparePartKey, PersistentDataType.STRING, PART_REPAIR_KIT);
        item.setItemMeta(meta);
        return item;
    }

    // ─── VALIDATION ───

    /**
     * Check if a player has a tire set for the given compound in their inventory.
     * @return true if found (does NOT consume)
     */
    public static boolean hasTireSet(Player player, TireCompound compound) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            String partType = item.getItemMeta().getPersistentDataContainer()
                    .get(sparePartKey, PersistentDataType.STRING);
            if (!PART_TIRE_SET.equals(partType)) continue;
            Integer compId = item.getItemMeta().getPersistentDataContainer()
                    .get(compoundKey, PersistentDataType.INTEGER);
            if (compId != null && compId == compound.getId()) return true;
        }
        return false;
    }

    /** Check if player has any tire set. */
    public static boolean hasAnyTireSet(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSparePart(item, PART_TIRE_SET)) return true;
        }
        return false;
    }

    /** Get the compound of the first tire set found. */
    public static TireCompound getFirstTireSetCompound(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            String partType = item.getItemMeta().getPersistentDataContainer()
                    .get(sparePartKey, PersistentDataType.STRING);
            if (!PART_TIRE_SET.equals(partType)) continue;
            Integer compId = item.getItemMeta().getPersistentDataContainer()
                    .get(compoundKey, PersistentDataType.INTEGER);
            if (compId != null) return TireCompound.fromId(compId);
        }
        return null;
    }

    public static boolean hasFuelCanister(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSparePart(item, PART_FUEL_CANISTER)) return true;
        }
        return false;
    }

    public static boolean hasRepairKit(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSparePart(item, PART_REPAIR_KIT)) return true;
        }
        return false;
    }

    // ─── CONSUMPTION ───

    /** Consume one tire set of the given compound. Returns true if consumed. */
    public static boolean consumeTireSet(Player player, TireCompound compound) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || !item.hasItemMeta()) continue;
            String partType = item.getItemMeta().getPersistentDataContainer()
                    .get(sparePartKey, PersistentDataType.STRING);
            if (!PART_TIRE_SET.equals(partType)) continue;
            Integer compId = item.getItemMeta().getPersistentDataContainer()
                    .get(compoundKey, PersistentDataType.INTEGER);
            if (compId != null && compId == compound.getId()) {
                if (item.getAmount() <= 1) {
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - 1);
                }
                return true;
            }
        }
        return false;
    }

    /** Consume first available tire set. Returns compound used, or null. */
    public static TireCompound consumeAnyTireSet(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || !item.hasItemMeta()) continue;
            String partType = item.getItemMeta().getPersistentDataContainer()
                    .get(sparePartKey, PersistentDataType.STRING);
            if (!PART_TIRE_SET.equals(partType)) continue;
            Integer compId = item.getItemMeta().getPersistentDataContainer()
                    .get(compoundKey, PersistentDataType.INTEGER);
            if (compId != null) {
                if (item.getAmount() <= 1) {
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - 1);
                }
                return TireCompound.fromId(compId);
            }
        }
        return null;
    }

    public static boolean consumeFuelCanister(Player player) {
        return consumeSparePart(player, PART_FUEL_CANISTER);
    }

    public static boolean consumeRepairKit(Player player) {
        return consumeSparePart(player, PART_REPAIR_KIT);
    }

    // ─── HELPERS ───

    private static boolean isSparePart(ItemStack item, String expectedType) {
        if (item == null || !item.hasItemMeta()) return false;
        if (sparePartKey == null) return false;
        String type = item.getItemMeta().getPersistentDataContainer()
                .get(sparePartKey, PersistentDataType.STRING);
        return expectedType.equals(type);
    }

    private static boolean consumeSparePart(Player player, String partType) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isSparePart(item, partType)) {
                if (item.getAmount() <= 1) {
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - 1);
                }
                return true;
            }
        }
        return false;
    }
}
