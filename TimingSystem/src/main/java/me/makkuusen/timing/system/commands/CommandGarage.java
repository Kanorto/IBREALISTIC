package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.economy.GarageManager;
import me.makkuusen.timing.system.economy.LevelManager;
import me.makkuusen.timing.system.economy.PlayerCar;
import me.makkuusen.timing.system.economy.RallyCoinManager;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.theme.messages.Success;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

@CommandAlias("garage")
public class CommandGarage extends BaseCommand {

    @Default
    @CommandPermission("%permissiongarage_use")
    public static void onDefault(Player player) {
        if (!GarageManager.isEnabled()) {
            Text.send(player, Error.GARAGE_NOT_ENABLED);
            return;
        }

        UUID uuid = player.getUniqueId();
        List<PlayerCar> cars = GarageManager.getCars(uuid);

        Text.send(player, Info.GARAGE_TITLE);

        if (cars.isEmpty()) {
            Text.send(player, Info.GARAGE_EMPTY);
            return;
        }

        for (PlayerCar car : cars) {
            if (car.isActive()) {
                Text.send(player, Info.GARAGE_CAR_ACTIVE, "%name%", car.getName());
            } else {
                Text.send(player, Info.GARAGE_CAR_ENTRY,
                        "%id%", String.valueOf(car.getId()),
                        "%name%", car.getName(),
                        "%components%", car.getComponentSummary());
            }
        }
    }

    @Subcommand("create")
    @CommandCompletion("<name>")
    @CommandPermission("%permissiongarage_use")
    public static void onCreate(Player player, String name) {
        if (!GarageManager.isEnabled()) {
            Text.send(player, Error.GARAGE_NOT_ENABLED);
            return;
        }

        PlayerCar car = GarageManager.createCar(player.getUniqueId(), name);
        if (car == null) {
            Text.send(player, Error.GARAGE_FULL, "%max%", "5");
            return;
        }

        Text.send(player, Success.GARAGE_CAR_CREATED, "%name%", name);
    }

    @Subcommand("select")
    @CommandCompletion("<carId>")
    @CommandPermission("%permissiongarage_use")
    public static void onSelect(Player player, int carId) {
        if (!GarageManager.isEnabled()) {
            Text.send(player, Error.GARAGE_NOT_ENABLED);
            return;
        }

        boolean success = GarageManager.selectCar(player.getUniqueId(), carId);
        if (!success) {
            Text.send(player, Error.GARAGE_CAR_NOT_FOUND);
            return;
        }

        PlayerCar car = GarageManager.getActiveCar(player.getUniqueId());
        String carName = car != null ? car.getName() : String.valueOf(carId);
        Text.send(player, Success.GARAGE_CAR_SELECTED, "%name%", carName);
    }

    @Subcommand("delete")
    @CommandCompletion("<carId>")
    @CommandPermission("%permissiongarage_use")
    public static void onDelete(Player player, int carId) {
        if (!GarageManager.isEnabled()) {
            Text.send(player, Error.GARAGE_NOT_ENABLED);
            return;
        }

        // Find car name before deleting
        List<PlayerCar> cars = GarageManager.getCars(player.getUniqueId());
        String carName = cars.stream()
                .filter(c -> c.getId() == carId)
                .map(PlayerCar::getName)
                .findFirst()
                .orElse(String.valueOf(carId));

        boolean success = GarageManager.deleteCar(player.getUniqueId(), carId);
        if (!success) {
            Text.send(player, Error.GARAGE_CAR_NOT_FOUND);
            return;
        }

        Text.send(player, Success.GARAGE_CAR_DELETED, "%name%", carName);
    }

    @Subcommand("upgrade")
    @CommandCompletion("<carId> tire|suspension|engine|body|steering|brake|weight|type <preset>")
    @CommandPermission("%permissiongarage_use")
    public static void onUpgrade(Player player, int carId, String component, String preset) {
        if (!GarageManager.isEnabled()) {
            Text.send(player, Error.GARAGE_NOT_ENABLED);
            return;
        }

        UUID uuid = player.getUniqueId();

        // Validate component
        String[] validNames = GarageManager.getNamesForComponent(component);
        if (validNames == null) {
            Text.send(player, Error.GARAGE_INVALID_COMPONENT);
            return;
        }

        // Resolve preset ID
        short presetId = GarageManager.resolvePresetId(component, preset);
        if (presetId < 0) {
            Text.send(player, Error.GARAGE_INVALID_PRESET,
                    "%preset%", preset,
                    "%component%", component);
            return;
        }

        // Find the car first
        List<PlayerCar> cars = GarageManager.getCars(uuid);
        PlayerCar car = cars.stream()
                .filter(c -> c.getId() == carId)
                .findFirst()
                .orElse(null);

        if (car == null) {
            Text.send(player, Error.GARAGE_CAR_NOT_FOUND);
            return;
        }

        // Check if preset is already installed
        short currentPreset = GarageManager.getCurrentPreset(car, component);
        if (currentPreset == presetId) {
            Text.send(player, Error.GARAGE_ALREADY_INSTALLED);
            return;
        }

        // Check level requirement
        int requiredLevel = GarageManager.getPresetLevel(component, presetId);
        if (requiredLevel > 0 && LevelManager.isEnabled()) {
            int playerLevel = LevelManager.getLevel(uuid);
            if (playerLevel < requiredLevel) {
                Text.send(player, Error.GARAGE_INSUFFICIENT_LEVEL,
                        "%level%", String.valueOf(requiredLevel));
                return;
            }
        }

        // Check cost
        int cost = GarageManager.getPresetPrice(component, presetId);
        boolean needsPayment = cost > 0 && RallyCoinManager.isEnabled();
        if (needsPayment) {
            int balance = RallyCoinManager.getBalance(uuid);
            if (balance < cost) {
                Text.send(player, Error.GARAGE_INSUFFICIENT_COINS,
                        "%cost%", RallyCoinManager.format(cost),
                        "%balance%", RallyCoinManager.format(balance));
                return;
            }
        }

        // Apply upgrade
        boolean success = GarageManager.upgradeComponent(uuid, carId, component, presetId);
        if (!success) {
            Text.send(player, Error.GARAGE_CAR_NOT_FOUND);
            return;
        }

        // Spend coins only after upgrade succeeds
        if (needsPayment) {
            RallyCoinManager.spendCoins(uuid, cost, "Garage upgrade: " + component + " → " + preset);
            Text.send(player, Success.GARAGE_COMPONENT_PURCHASED,
                    "%preset%", preset.toUpperCase(),
                    "%component%", component,
                    "%cost%", RallyCoinManager.format(cost));
        }

        Text.send(player, Success.GARAGE_COMPONENT_UPGRADED,
                "%component%", component,
                "%preset%", preset.toUpperCase(),
                "%name%", car.getName());
    }

    @Subcommand("info")
    @CommandCompletion("<carId>")
    @CommandPermission("%permissiongarage_use")
    public static void onInfo(Player player, int carId) {
        if (!GarageManager.isEnabled()) {
            Text.send(player, Error.GARAGE_NOT_ENABLED);
            return;
        }

        List<PlayerCar> cars = GarageManager.getCars(player.getUniqueId());
        PlayerCar car = cars.stream()
                .filter(c -> c.getId() == carId)
                .findFirst()
                .orElse(null);

        if (car == null) {
            Text.send(player, Error.GARAGE_CAR_NOT_FOUND);
            return;
        }

        Text.send(player, Info.GARAGE_TITLE);
        Text.send(player, Info.GARAGE_CAR_ACTIVE, "%name%", car.getName());
        Text.send(player, Info.GARAGE_CAR_INFO, "%component%", "Vehicle Type", "%preset%", GarageManager.getPresetName("type", car.getVehicleType()));
        Text.send(player, Info.GARAGE_CAR_INFO, "%component%", "Tires", "%preset%", GarageManager.getPresetName("tire", car.getTirePreset()));
        Text.send(player, Info.GARAGE_CAR_INFO, "%component%", "Suspension", "%preset%", GarageManager.getPresetName("suspension", car.getSuspensionPreset()));
        Text.send(player, Info.GARAGE_CAR_INFO, "%component%", "Engine", "%preset%", GarageManager.getPresetName("engine", car.getEnginePreset()));
        Text.send(player, Info.GARAGE_CAR_INFO, "%component%", "Body", "%preset%", GarageManager.getPresetName("body", car.getBodyPreset()));
        Text.send(player, Info.GARAGE_CAR_INFO, "%component%", "Steering", "%preset%", GarageManager.getPresetName("steering", car.getSteeringPreset()));
        Text.send(player, Info.GARAGE_CAR_INFO, "%component%", "Brakes", "%preset%", GarageManager.getPresetName("brake", car.getBrakePreset()));
        Text.send(player, Info.GARAGE_CAR_INFO, "%component%", "Weight Dist.", "%preset%", GarageManager.getPresetName("weight", car.getWeightDistributionPreset()));
    }
}
