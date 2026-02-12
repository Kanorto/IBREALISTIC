package me.makkuusen.timing.system.economy;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.boatutils.CustomBoatUtilsMode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages player car garages — creation, upgrades, selection, and retrieval.
 * Each player can own up to MAX_CARS cars (expandable with coins).
 */
public class GarageManager {

    private static final int DEFAULT_MAX_CARS = 5;
    private static final int EXTRA_SLOT_COST = 2000;

    // ─── PRESET NAMES (must match mod enum order) ───
    private static final String[] TIRE_NAMES = {"STANDARD", "SOFT", "MEDIUM", "HARD", "RAIN", "ICE_SPIKES", "RALLY_GRAVEL"};
    private static final String[] SUSPENSION_NAMES = {"COMFORT", "SPORT", "RALLY", "STIFF"};
    private static final String[] ENGINE_NAMES = {"STOCK", "SPORT", "RALLY", "TURBO", "MONSTER"};
    private static final String[] BODY_NAMES = {"STANDARD", "LIGHTWEIGHT", "AERO", "RALLY_SPEC", "HEAVY_DUTY"};
    private static final String[] STEERING_NAMES = {"STANDARD", "QUICK", "PROGRESSIVE", "DRIFT"};
    private static final String[] BRAKE_NAMES = {"STANDARD", "SPORT", "RACING", "ENDURANCE"};
    private static final String[] WEIGHT_DIST_NAMES = {"BALANCED", "FRONT_BIASED", "REAR_BIASED", "MID_ENGINE"};
    private static final String[] VEHICLE_TYPE_NAMES = {"WRC_CAR", "GROUP_B", "CLASSIC_RALLY", "LIGHTWEIGHT", "TRUCK"};

    // ─── PRESET PRICES (must match mod enum) ───
    private static final int[] TIRE_PRICES = {0, 500, 300, 600, 1000, 1500, 1200};
    private static final int[] SUSPENSION_PRICES = {0, 600, 1000, 1500};
    private static final int[] ENGINE_PRICES = {0, 800, 1200, 2000, 3000};
    private static final int[] BODY_PRICES = {0, 700, 1200, 1800, 900};
    private static final int[] STEERING_PRICES = {0, 500, 800, 1200};
    private static final int[] BRAKE_PRICES = {0, 500, 1000, 700};
    private static final int[] WEIGHT_DIST_PRICES = {0, 400, 400, 800};
    private static final int[] VEHICLE_TYPE_PRICES = {0, 1500, 1000, 800, 2000};

    // ─── PRESET REQUIRED LEVELS (must match mod enum) ───
    private static final int[] TIRE_LEVELS = {0, 3, 2, 5, 8, 12, 10};
    private static final int[] SUSPENSION_LEVELS = {0, 4, 8, 12};
    private static final int[] ENGINE_LEVELS = {0, 5, 10, 15, 20};
    private static final int[] BODY_LEVELS = {0, 5, 10, 15, 8};
    private static final int[] STEERING_LEVELS = {0, 4, 8, 12};
    private static final int[] BRAKE_LEVELS = {0, 4, 10, 6};
    private static final int[] WEIGHT_DIST_LEVELS = {0, 3, 3, 8};
    private static final int[] VEHICLE_TYPE_LEVELS = {0, 10, 8, 5, 15};

    /**
     * Gets all cars owned by a player.
     */
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

    /**
     * Gets the active car for a player, or null if none.
     */
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

    /**
     * Creates a new car for a player.
     * @return the created car, or null if the player has reached the car limit
     */
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

    /**
     * Sets a car as active, deactivating all others for that player.
     */
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

    /**
     * Deletes a car from the player's garage.
     */
    public static boolean deleteCar(UUID uuid, int carId) {
        try {
            int rows = DB.executeUpdate("DELETE FROM ts_player_garage WHERE uuid = ? AND id = ?", uuid.toString(), carId);
            return rows > 0;
        } catch (SQLException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to delete car for " + uuid, e);
            return false;
        }
    }

    /**
     * Upgrades a component preset on a car.
     * @return true if successful
     */
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

    /**
     * Applies a car's preset configuration to a CustomBoatUtilsMode.
     */
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

    // ─── PRESET NAME LOOKUPS ───

    public static String getPresetName(String component, short id) {
        String[] names = getNamesForComponent(component);
        if (names == null || id < 0 || id >= names.length) return "UNKNOWN";
        return names[id];
    }

    public static int getPresetPrice(String component, short id) {
        int[] prices = getPricesForComponent(component);
        if (prices == null || id < 0 || id >= prices.length) return -1;
        return prices[id];
    }

    public static int getPresetLevel(String component, short id) {
        int[] levels = getLevelsForComponent(component);
        if (levels == null || id < 0 || id >= levels.length) return -1;
        return levels[id];
    }

    public static short resolvePresetId(String component, String presetName) {
        String[] names = getNamesForComponent(component);
        if (names == null) return -1;
        String upper = presetName.toUpperCase();
        for (short i = 0; i < names.length; i++) {
            if (names[i].equals(upper)) return i;
        }
        return -1;
    }

    public static String[] getNamesForComponent(String component) {
        return switch (component.toLowerCase()) {
            case "tire", "tires" -> TIRE_NAMES;
            case "suspension" -> SUSPENSION_NAMES;
            case "engine" -> ENGINE_NAMES;
            case "body" -> BODY_NAMES;
            case "steering" -> STEERING_NAMES;
            case "brake", "brakes" -> BRAKE_NAMES;
            case "weight", "weightdistribution" -> WEIGHT_DIST_NAMES;
            case "type", "vehicletype" -> VEHICLE_TYPE_NAMES;
            default -> null;
        };
    }

    private static int[] getPricesForComponent(String component) {
        return switch (component.toLowerCase()) {
            case "tire", "tires" -> TIRE_PRICES;
            case "suspension" -> SUSPENSION_PRICES;
            case "engine" -> ENGINE_PRICES;
            case "body" -> BODY_PRICES;
            case "steering" -> STEERING_PRICES;
            case "brake", "brakes" -> BRAKE_PRICES;
            case "weight", "weightdistribution" -> WEIGHT_DIST_PRICES;
            case "type", "vehicletype" -> VEHICLE_TYPE_PRICES;
            default -> null;
        };
    }

    private static int[] getLevelsForComponent(String component) {
        return switch (component.toLowerCase()) {
            case "tire", "tires" -> TIRE_LEVELS;
            case "suspension" -> SUSPENSION_LEVELS;
            case "engine" -> ENGINE_LEVELS;
            case "body" -> BODY_LEVELS;
            case "steering" -> STEERING_LEVELS;
            case "brake", "brakes" -> BRAKE_LEVELS;
            case "weight", "weightdistribution" -> WEIGHT_DIST_LEVELS;
            case "type", "vehicletype" -> VEHICLE_TYPE_LEVELS;
            default -> null;
        };
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

    private static int getMaxCars(UUID uuid) {
        // Could be extended with purchased extra slots
        return DEFAULT_MAX_CARS;
    }

    /**
     * Returns whether the garage system is enabled in config.
     */
    public static boolean isEnabled() {
        return TimingSystem.getPlugin().getConfig().getBoolean("economy.enabled", true);
    }
}
