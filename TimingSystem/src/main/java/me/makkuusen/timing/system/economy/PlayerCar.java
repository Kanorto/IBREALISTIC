package me.makkuusen.timing.system.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Represents a player's car configuration in their garage.
 * A car is a combination of component presets that together define
 * the vehicle's physics properties.
 */
@Getter
@Setter
public class PlayerCar {

    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Expose
    private int id;
    @Expose
    private String ownerUuid;
    @Expose
    private String name;
    @Expose
    private short vehicleType = 0; // WRC_CAR default
    @Expose
    private short tirePreset = 0; // STANDARD
    @Expose
    private short suspensionPreset = 0; // COMFORT
    @Expose
    private short enginePreset = 0; // STOCK
    @Expose
    private short bodyPreset = 0; // STANDARD
    @Expose
    private short steeringPreset = 0; // STANDARD
    @Expose
    private short brakePreset = 0; // STANDARD
    @Expose
    private short weightDistributionPreset = 0; // BALANCED
    @Expose
    private short exhaustPreset = 0; // STANDARD
    @Expose
    private short differentialPreset = 0; // STANDARD
    @Expose
    private short gearboxPreset = 0; // STANDARD
    @Expose
    private short turboPreset = 0; // NONE
    @Expose
    private short intercoolerPreset = 0; // STANDARD
    @Expose
    private boolean active = false;

    public PlayerCar() {}

    public PlayerCar(String ownerUuid, String name) {
        this.ownerUuid = ownerUuid;
        this.name = name;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static PlayerCar fromJson(String json) {
        return GSON.fromJson(json, PlayerCar.class);
    }

    /**
     * Returns a human-readable summary of the car's components.
     */
    public String getComponentSummary() {
        return String.format("VT:%d T:%d S:%d E:%d B:%d St:%d Br:%d W:%d Ex:%d D:%d G:%d Tu:%d I:%d",
                vehicleType, tirePreset, suspensionPreset, enginePreset,
                bodyPreset, steeringPreset, brakePreset, weightDistributionPreset,
                exhaustPreset, differentialPreset, gearboxPreset, turboPreset, intercoolerPreset);
    }
}
