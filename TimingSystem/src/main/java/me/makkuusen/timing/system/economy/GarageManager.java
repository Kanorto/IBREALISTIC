package me.makkuusen.timing.system.economy;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.boatutils.CustomBoatUtilsMode;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages player car garages — creation, upgrades, selection, and retrieval.
 * All preset names, prices, and level requirements are read from config.yml
 * under the {@code garage:} section. Changes take effect on plugin reload.
 */
public class GarageManager {

    // ─── COMPONENT KEYS (used in config paths and DB lookups) ───
    private static final String[] COMPONENT_KEYS = {
            "tire", "suspension", "engine", "body", "steering", "brake", "weight", "type"
    };

    // ─── HARDCODED FALLBACK DEFAULTS (used only when config is missing) ───
    private static final String[][] DEFAULT_NAMES = {
            {"STANDARD", "SOFT", "MEDIUM", "HARD", "RAIN", "ICE_SPIKES", "RALLY_GRAVEL"},
            {"COMFORT", "SPORT", "RALLY", "STIFF"},
            {"STOCK", "SPORT", "RALLY", "TURBO", "MONSTER"},
            {"STANDARD", "LIGHTWEIGHT", "AERO", "RALLY_SPEC", "HEAVY_DUTY"},
            {"STANDARD", "QUICK", "PROGRESSIVE", "DRIFT"},
            {"STANDARD", "SPORT", "RACING", "ENDURANCE"},
            {"BALANCED", "FRONT_BIASED", "REAR_BIASED", "MID_ENGINE"},
            {"WRC_CAR", "GROUP_B", "CLASSIC_RALLY", "LIGHTWEIGHT", "TRUCK"}
    };
    private static final int[][] DEFAULT_PRICES = {
            {0, 500, 300, 600, 1000, 1500, 1200},
            {0, 600, 1000, 1500},
            {0, 800, 1200, 2000, 3000},
            {0, 700, 1200, 1800, 900},
            {0, 500, 800, 1200},
            {0, 500, 1000, 700},
            {0, 400, 400, 800},
            {0, 1500, 1000, 800, 2000}
    };
    private static final int[][] DEFAULT_LEVELS = {
            {0, 3, 2, 5, 8, 12, 10},
            {0, 4, 8, 12},
            {0, 5, 10, 15, 20},
            {0, 5, 10, 15, 8},
            {0, 4, 8, 12},
            {0, 4, 10, 6},
            {0, 3, 3, 8},
            {0, 10, 8, 5, 15}
    };

    // ─── CONFIG HELPERS ───

    private static ConfigurationSection getGarageSection() {
        return TimingSystem.getPlugin().getConfig().getConfigurationSection("garage");
    }

    private static ConfigurationSection getPresetsSection(String component) {
        ConfigurationSection garage = getGarageSection();
        if (garage == null) return null;
        ConfigurationSection presets = garage.getConfigurationSection("presets");
        if (presets == null) return null;
        return presets.getConfigurationSection(resolveConfigKey(component));
    }

    /** Resolves user-facing component aliases to the config key */
    private static String resolveConfigKey(String component) {
        return switch (component.toLowerCase()) {
            case "tire", "tires" -> "tire";
            case "suspension" -> "suspension";
            case "engine" -> "engine";
            case "body" -> "body";
            case "steering" -> "steering";
            case "brake", "brakes" -> "brake";
            case "weight", "weightdistribution" -> "weight";
            case "vehicletype", "type" -> "type";
            default -> component.toLowerCase();
        };
    }

    /** Returns the fallback index for a component key in the defaults arrays */
    private static int fallbackIndex(String configKey) {
        for (int i = 0; i < COMPONENT_KEYS.length; i++) {
            if (COMPONENT_KEYS[i].equals(configKey)) return i;
        }
        return -1;
    }

    /**
     * Gets the number of presets available for a component.
     * Reads from config, falls back to hardcoded defaults.
     */
    public static int getPresetCount(String component) {
        ConfigurationSection section = getPresetsSection(component);
        if (section != null) return section.getKeys(false).size();
        String[] fallback = getNamesForComponent(component);
        return fallback != null ? fallback.length : 0;
    }

    // ─── DATABASE OPERATIONS ───

    public static List<PlayerCar> getCars(UUID uuid) {
        List<PlayerCar> cars = new ArrayList<>();
        try {
            List<DbRow> rows = DB.getResults("SELECT * FROM ts_player_garage WHERE uuid = ? ORDER BY id", uuid.toString());
            for (DbRow row : rows) {
                PlayerCar car = new PlayerCar();
                car.setId(row.getInt("id"));
                car.setOwnerUuid(row.getString("uuid"));
                car.setName(row.getString("name"));
                car.setVehicleType((short)(int) row.getInt("vehicle_type"));
                car.setTirePreset((short)(int) row.getInt("tire_preset"));
                car.setSuspensionPreset((short)(int) row.getInt("suspension_preset"));
                car.setEnginePreset((short)(int) row.getInt("engine_preset"));
                car.setBodyPreset((short)(int) row.getInt("body_preset"));
                car.setSteeringPreset((short)(int) row.getInt("steering_preset"));
                car.setBrakePreset((short)(int) row.getInt("brake_preset"));
                car.setWeightDistributionPreset((short)(int) row.getInt("weight_distribution_preset"));
                car.setActive(row.getInt("active") == 1);
                cars.add(car);
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get cars for " + uuid, e);
        }
        return cars;
    }

    public static PlayerCar getActiveCar(UUID uuid) {
        try {
            DbRow row = DB.getFirstRow("SELECT * FROM ts_player_garage WHERE uuid = ? AND active = 1", uuid.toString());
            if (row == null) return null;
            PlayerCar car = new PlayerCar();
            car.setId(row.getInt("id"));
            car.setOwnerUuid(row.getString("uuid"));
            car.setName(row.getString("name"));
            car.setVehicleType((short)(int) row.getInt("vehicle_type"));
            car.setTirePreset((short)(int) row.getInt("tire_preset"));
            car.setSuspensionPreset((short)(int) row.getInt("suspension_preset"));
            car.setEnginePreset((short)(int) row.getInt("engine_preset"));
            car.setBodyPreset((short)(int) row.getInt("body_preset"));
            car.setSteeringPreset((short)(int) row.getInt("steering_preset"));
            car.setBrakePreset((short)(int) row.getInt("brake_preset"));
            car.setWeightDistributionPreset((short)(int) row.getInt("weight_distribution_preset"));
            car.setActive(true);
            return car;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get active car for " + uuid, e);
            return null;
        }
    }

    public static PlayerCar createCar(UUID uuid, String name) {
        List<PlayerCar> existing = getCars(uuid);
        if (existing.size() >= getMaxCars(uuid)) {
            return null;
        }

        try {
            boolean isFirst = existing.isEmpty();
            long id = DB.executeInsert(
                    "INSERT INTO ts_player_garage (uuid, name, vehicle_type, tire_preset, suspension_preset, engine_preset, body_preset, steering_preset, brake_preset, weight_distribution_preset, active) VALUES (?, ?, 0, 0, 0, 0, 0, 0, 0, 0, ?)",
                    uuid.toString(), name, isFirst ? 1 : 0);
            PlayerCar car = new PlayerCar(uuid.toString(), name);
            car.setId((int) id);
            car.setActive(isFirst);
            return car;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to create car for " + uuid, e);
            return null;
        }
    }

    public static boolean selectCar(UUID uuid, int carId) {
        try {
            DB.executeUpdate("UPDATE ts_player_garage SET active = 0 WHERE uuid = ?", uuid.toString());
            int rows = DB.executeUpdate("UPDATE ts_player_garage SET active = 1 WHERE uuid = ? AND id = ?", uuid.toString(), carId);
            return rows > 0;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to select car for " + uuid, e);
            return false;
        }
    }

    public static boolean deleteCar(UUID uuid, int carId) {
        try {
            int rows = DB.executeUpdate("DELETE FROM ts_player_garage WHERE uuid = ? AND id = ?", uuid.toString(), carId);
            return rows > 0;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to delete car for " + uuid, e);
            return false;
        }
    }

    public static boolean upgradeComponent(UUID uuid, int carId, String component, short presetId) {
        String column = getColumnForComponent(component);
        if (column == null) return false;

        try {
            int rows = DB.executeUpdate(
                    "UPDATE ts_player_garage SET " + column + " = ? WHERE uuid = ? AND id = ?",
                    (int) presetId, uuid.toString(), carId);
            return rows > 0;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to upgrade component for " + uuid, e);
            return false;
        }
    }

    public static void applyCarToMode(PlayerCar car, CustomBoatUtilsMode mode) {
        mode.setRealisticPhysics(true);
        mode.setVehicleType(car.getVehicleType());
        mode.setTirePreset(car.getTirePreset());
        mode.setSuspensionPreset(car.getSuspensionPreset());
        mode.setEnginePreset(car.getEnginePreset());
        mode.setBodyPreset(car.getBodyPreset());
        mode.setSteeringPreset(car.getSteeringPreset());
        mode.setBrakePreset(car.getBrakePreset());
        mode.setWeightDistributionPreset(car.getWeightDistributionPreset());
    }

    // ─── CONFIG-DRIVEN PRESET LOOKUPS ───

    public static short getCurrentPreset(PlayerCar car, String component) {
        return switch (component.toLowerCase()) {
            case "tire", "tires" -> car.getTirePreset();
            case "suspension" -> car.getSuspensionPreset();
            case "engine" -> car.getEnginePreset();
            case "body" -> car.getBodyPreset();
            case "steering" -> car.getSteeringPreset();
            case "brake", "brakes" -> car.getBrakePreset();
            case "weight", "weightdistribution" -> car.getWeightDistributionPreset();
            case "vehicletype", "type" -> car.getVehicleType();
            default -> -1;
        };
    }

    /**
     * Gets preset name from config, with hardcoded fallback.
     */
    public static String getPresetName(String component, short id) {
        ConfigurationSection section = getPresetsSection(component);
        if (section != null) {
            ConfigurationSection preset = section.getConfigurationSection(String.valueOf(id));
            if (preset != null) return preset.getString("name", "UNKNOWN");
        }
        // Fallback to hardcoded defaults
        String[] names = getNamesForComponent(component);
        if (names == null || id < 0 || id >= names.length) return "UNKNOWN";
        return names[id];
    }

    /**
     * Gets preset price from config, with hardcoded fallback.
     */
    public static int getPresetPrice(String component, short id) {
        ConfigurationSection section = getPresetsSection(component);
        if (section != null) {
            ConfigurationSection preset = section.getConfigurationSection(String.valueOf(id));
            if (preset != null) return preset.getInt("price", -1);
        }
        // Fallback to hardcoded defaults
        int[] prices = getPricesForComponent(component);
        if (prices == null || id < 0 || id >= prices.length) return -1;
        return prices[id];
    }

    /**
     * Gets preset level requirement from config, with hardcoded fallback.
     */
    public static int getPresetLevel(String component, short id) {
        ConfigurationSection section = getPresetsSection(component);
        if (section != null) {
            ConfigurationSection preset = section.getConfigurationSection(String.valueOf(id));
            if (preset != null) return preset.getInt("level", -1);
        }
        // Fallback to hardcoded defaults
        int[] levels = getLevelsForComponent(component);
        if (levels == null || id < 0 || id >= levels.length) return -1;
        return levels[id];
    }

    public static short resolvePresetId(String component, String presetName) {
        String upper = presetName.toUpperCase();
        // Try config first
        ConfigurationSection section = getPresetsSection(component);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection preset = section.getConfigurationSection(key);
                if (preset != null && upper.equals(preset.getString("name", "").toUpperCase())) {
                    try {
                        return Short.parseShort(key);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        // Fallback to hardcoded
        String[] names = getNamesForComponent(component);
        if (names == null) return -1;
        for (short i = 0; i < names.length; i++) {
            if (names[i].equals(upper)) return i;
        }
        return -1;
    }

    /**
     * Gets all preset names for a component. Reads from config, falls back to hardcoded.
     */
    public static String[] getNamesForComponent(String component) {
        ConfigurationSection section = getPresetsSection(component);
        if (section != null) {
            List<String> keys = new ArrayList<>(section.getKeys(false));
            keys.sort((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)));
            String[] names = new String[keys.size()];
            for (int i = 0; i < keys.size(); i++) {
                ConfigurationSection preset = section.getConfigurationSection(keys.get(i));
                names[i] = preset != null ? preset.getString("name", "UNKNOWN") : "UNKNOWN";
            }
            return names;
        }
        // Fallback
        String configKey = resolveConfigKey(component);
        int idx = fallbackIndex(configKey);
        return idx >= 0 ? DEFAULT_NAMES[idx] : null;
    }

    private static int[] getPricesForComponent(String component) {
        ConfigurationSection section = getPresetsSection(component);
        if (section != null) {
            List<String> keys = new ArrayList<>(section.getKeys(false));
            keys.sort((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)));
            int[] prices = new int[keys.size()];
            for (int i = 0; i < keys.size(); i++) {
                ConfigurationSection preset = section.getConfigurationSection(keys.get(i));
                prices[i] = preset != null ? preset.getInt("price", 0) : 0;
            }
            return prices;
        }
        String configKey = resolveConfigKey(component);
        int idx = fallbackIndex(configKey);
        return idx >= 0 ? DEFAULT_PRICES[idx] : null;
    }

    private static int[] getLevelsForComponent(String component) {
        ConfigurationSection section = getPresetsSection(component);
        if (section != null) {
            List<String> keys = new ArrayList<>(section.getKeys(false));
            keys.sort((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)));
            int[] levels = new int[keys.size()];
            for (int i = 0; i < keys.size(); i++) {
                ConfigurationSection preset = section.getConfigurationSection(keys.get(i));
                levels[i] = preset != null ? preset.getInt("level", 0) : 0;
            }
            return levels;
        }
        String configKey = resolveConfigKey(component);
        int idx = fallbackIndex(configKey);
        return idx >= 0 ? DEFAULT_LEVELS[idx] : null;
    }

    private static String getColumnForComponent(String component) {
        return switch (component.toLowerCase()) {
            case "tire", "tires" -> "tire_preset";
            case "suspension" -> "suspension_preset";
            case "engine" -> "engine_preset";
            case "body" -> "body_preset";
            case "steering" -> "steering_preset";
            case "brake", "brakes" -> "brake_preset";
            case "weight", "weightdistribution" -> "weight_distribution_preset";
            case "vehicletype", "type" -> "vehicle_type";
            default -> null;
        };
    }

    /**
     * Returns max cars from config, with hardcoded fallback.
     */
    public static int getMaxCars(UUID uuid) {
        ConfigurationSection garage = getGarageSection();
        if (garage != null) return garage.getInt("max_cars", 5);
        return 5;
    }

    /**
     * Returns cost of an extra car slot from config.
     */
    public static int getExtraSlotCost() {
        ConfigurationSection garage = getGarageSection();
        if (garage != null) return garage.getInt("extra_slot_cost", 2000);
        return 2000;
    }

    /**
     * Returns the minimum level required to create a car.
     */
    public static int getCarCreationLevel() {
        ConfigurationSection garage = getGarageSection();
        if (garage != null) return garage.getInt("car_creation_level", 0);
        return 0;
    }

    /**
     * Returns whether the garage system is enabled in config.
     */
    public static boolean isEnabled() {
        return TimingSystem.getPlugin().getConfig().getBoolean("economy.enabled", true);
    }

    // ─── PERSISTENT PURCHASE INVENTORY ───

    public static boolean hasPurchased(UUID uuid, String component, short presetId) {
        if (presetId == 0) return true;
        int price = getPresetPrice(component, presetId);
        if (price == 0) return true;
        try {
            DbRow row = DB.getFirstRow(
                    "SELECT id FROM ts_player_purchases WHERE uuid = ? AND component = ? AND preset_id = ?",
                    uuid.toString(), component.toLowerCase(), (int) presetId);
            return row != null;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to check purchase for " + uuid, e);
            return false;
        }
    }

    public static void recordPurchase(UUID uuid, String component, short presetId) {
        if (presetId == 0) return;
        try {
            DB.executeInsert(
                    "INSERT OR IGNORE INTO ts_player_purchases (uuid, component, preset_id, purchased_at) VALUES (?, ?, ?, ?)",
                    uuid.toString(), component.toLowerCase(), (int) presetId, System.currentTimeMillis());
        } catch (SQLException e) {
            try {
                DB.executeInsert(
                        "INSERT IGNORE INTO ts_player_purchases (uuid, component, preset_id, purchased_at) VALUES (?, ?, ?, ?)",
                        uuid.toString(), component.toLowerCase(), (int) presetId, System.currentTimeMillis());
            } catch (SQLException ex) {
                TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to record purchase for " + uuid, ex);
            }
        }
    }

    public static List<Short> getPurchasedPresets(UUID uuid, String component) {
        List<Short> purchased = new ArrayList<>();
        purchased.add((short) 0);
        int[] prices = getPricesForComponent(component);
        if (prices != null) {
            for (short i = 1; i < prices.length; i++) {
                if (prices[i] == 0) purchased.add(i);
            }
        }
        try {
            List<DbRow> rows = DB.getResults(
                    "SELECT preset_id FROM ts_player_purchases WHERE uuid = ? AND component = ?",
                    uuid.toString(), component.toLowerCase());
            for (DbRow row : rows) {
                short id = (short)(int) row.getInt("preset_id");
                if (!purchased.contains(id)) {
                    purchased.add(id);
                }
            }
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to get purchases for " + uuid, e);
        }
        return purchased;
    }

    // ─── CONFIG AUTO-UPDATE ───

    /**
     * Ensures the garage section exists in the live config.
     * Called during plugin startup to merge defaults for any missing keys
     * without overwriting user customizations.
     */
    public static void ensureConfigDefaults() {
        var plugin = TimingSystem.getPlugin();
        var config = plugin.getConfig();
        boolean changed = false;

        // Top-level garage keys
        if (!config.contains("garage.max_cars")) {
            config.set("garage.max_cars", 5);
            changed = true;
        }
        if (!config.contains("garage.extra_slot_cost")) {
            config.set("garage.extra_slot_cost", 2000);
            changed = true;
        }
        if (!config.contains("garage.car_creation_level")) {
            config.set("garage.car_creation_level", 0);
            changed = true;
        }

        // Preset defaults per component
        for (int c = 0; c < COMPONENT_KEYS.length; c++) {
            String key = COMPONENT_KEYS[c];
            String[] names = DEFAULT_NAMES[c];
            int[] prices = DEFAULT_PRICES[c];
            int[] levels = DEFAULT_LEVELS[c];
            for (int i = 0; i < names.length; i++) {
                String basePath = "garage.presets." + key + "." + i;
                if (!config.contains(basePath + ".name")) {
                    config.set(basePath + ".name", names[i]);
                    config.set(basePath + ".price", prices[i]);
                    config.set(basePath + ".level", levels[i]);
                    changed = true;
                }
            }
        }

        if (changed) {
            plugin.saveConfig();
            plugin.getLogger().info("[Garage] Config auto-updated with missing garage defaults.");
        }
    }
}
