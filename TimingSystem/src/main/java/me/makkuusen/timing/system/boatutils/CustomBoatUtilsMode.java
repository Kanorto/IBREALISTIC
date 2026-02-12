package me.makkuusen.timing.system.boatutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Hover;
import me.makkuusen.timing.system.theme.messages.Warning;
import me.makkuusen.timing.system.tplayer.TPlayer;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Getter
@Setter
public class CustomBoatUtilsMode {

    private static final short PACKET_ID_RESET = 0;
    private static final short PACKET_ID_SET_STEP_HEIGHT = 1;
    private static final short PACKET_ID_SET_DEFAULT_SLIPPERINESS = 2;
    private static final short PACKET_ID_SET_BLOCKS_SLIPPERINESS = 3;
    private static final short PACKET_ID_SET_BOAT_FALL_DAMAGE = 4;
    private static final short PACKET_ID_SET_BOAT_WATER_ELEVATION = 5;
    private static final short PACKET_ID_SET_BOAT_AIR_CONTROL = 6;
    private static final short PACKET_ID_SET_BOAT_JUMP_FORCE = 7;
    private static final short PACKET_ID_SET_GRAVITY = 9;
    private static final short PACKET_ID_SET_YAW_ACCELERATION = 10;
    private static final short PACKET_ID_SET_FORWARD_ACCELERATION = 11;
    private static final short PACKET_ID_SET_BACKWARD_ACCELERATION = 12;
    private static final short PACKET_ID_SET_TURNING_FORWARD_ACCELERATION = 13;
    private static final short PACKET_ID_ALLOW_ACCELERATION_STACKING = 14;
    private static final short PACKET_ID_SET_UNDERWATER_CONTROL = 16;
    private static final short PACKET_ID_SET_SURFACE_WATER_CONTROL = 17;
    private static final short PACKET_ID_SET_COYOTE_TIME = 19;
    private static final short PACKET_ID_SET_WATER_JUMPING = 20;
    private static final short PACKET_ID_SET_SWIM_FORCE = 21;
    private static final short PACKET_ID_SET_PER_BLOCK_SETTING = 26;
    private static final short PACKET_ID_SET_AIR_STEPPING = 28;
    private static final short PACKET_ID_SET_REALISTIC_PHYSICS = 33;
    private static final short PACKET_ID_SET_VEHICLE_TYPE = 34;
    private static final short PACKET_ID_SET_VEHICLE_MASS = 35;
    private static final short PACKET_ID_SET_VEHICLE_WHEELBASE = 36;
    private static final short PACKET_ID_SET_VEHICLE_CG_HEIGHT = 37;
    private static final short PACKET_ID_SET_VEHICLE_TRACK_WIDTH = 38;
    private static final short PACKET_ID_SET_VEHICLE_MAX_STEERING = 39;
    private static final short PACKET_ID_SET_VEHICLE_STEERING_SPEED = 40;
    private static final short PACKET_ID_SET_VEHICLE_BRAKING_FORCE = 41;
    private static final short PACKET_ID_SET_VEHICLE_ENGINE_FORCE = 42;
    private static final short PACKET_ID_SET_VEHICLE_DRAG = 43;
    private static final short PACKET_ID_SET_VEHICLE_BRAKE_BIAS = 44;
    private static final short PACKET_ID_SET_VEHICLE_SUBSTEPS = 45;
    private static final short PACKET_ID_SET_VEHICLE_FRONT_WEIGHT_BIAS = 46;
    private static final short PACKET_ID_SET_BLOCK_SURFACE_TYPE = 47;
    private static final short PACKET_ID_SET_VEHICLE_DRIVETRAIN = 48;
    private static final short PACKET_ID_SET_DEFAULT_SURFACE_TYPE = 49;
    private static final short PACKET_ID_SET_VEHICLE_SPEED_STEERING_FACTOR = 50;
    private static final short PACKET_ID_SET_VEHICLE_ENGINE_BRAKING = 51;
    private static final short PACKET_ID_SET_VEHICLE_ROLL_STIFFNESS_RATIO = 52;
    private static final short PACKET_ID_SET_AWD_FRONT_SPLIT = 53;
    private static final short PACKET_ID_SET_FRONT_DIFFERENTIAL = 54;
    private static final short PACKET_ID_SET_REAR_DIFFERENTIAL = 55;
    private static final short PACKET_ID_SET_LSD_LOCKING_COEFF = 56;
    private static final short PACKET_ID_SET_DOWNFORCE_COEFFICIENT = 57;
    private static final short PACKET_ID_SET_DOWNFORCE_FRONT_BIAS = 58;
    private static final short PACKET_ID_SET_WEATHER_CONDITION = 59;
    private static final short PACKET_ID_SET_STEERING_RETURN_RATE = 60;
    private static final short PACKET_ID_REALISTIC_SERVER_INFO = 61;

    // Default values for realistic physics parameters
    private static final float DEFAULT_VEHICLE_MASS = 1190f;
    private static final float DEFAULT_VEHICLE_WHEELBASE = 2.53f;
    private static final float DEFAULT_VEHICLE_CG_HEIGHT = 0.45f;
    private static final float DEFAULT_VEHICLE_TRACK_WIDTH = 1.55f;
    private static final float DEFAULT_VEHICLE_MAX_STEERING = 0.50f;
    private static final float DEFAULT_VEHICLE_STEERING_SPEED = 5.0f;
    private static final float DEFAULT_VEHICLE_BRAKING_FORCE = 8000f;
    private static final float DEFAULT_VEHICLE_ENGINE_FORCE = 5500f;
    private static final float DEFAULT_VEHICLE_DRAG = 0.35f;
    private static final float DEFAULT_VEHICLE_BRAKE_BIAS = 0.65f;
    private static final int DEFAULT_VEHICLE_SUBSTEPS = 4;
    private static final float DEFAULT_VEHICLE_FRONT_WEIGHT_BIAS = 0.55f;
    private static final short DEFAULT_VEHICLE_DRIVETRAIN = 2; // AWD
    private static final String DEFAULT_DEFAULT_SURFACE = "ASPHALT_DRY";
    private static final float DEFAULT_VEHICLE_SPEED_STEERING_FACTOR = 0.004f;
    private static final float DEFAULT_VEHICLE_ENGINE_BRAKING = 800f;
    private static final float DEFAULT_VEHICLE_ROLL_STIFFNESS_RATIO = 0.55f;
    private static final float DEFAULT_AWD_FRONT_SPLIT = 0.5f;
    private static final short DEFAULT_FRONT_DIFFERENTIAL = 0; // OPEN
    private static final short DEFAULT_REAR_DIFFERENTIAL = 0; // OPEN
    private static final float DEFAULT_LSD_LOCKING_COEFF = 0.3f;
    private static final float DEFAULT_DOWNFORCE_COEFFICIENT = 0.5f;
    private static final float DEFAULT_DOWNFORCE_FRONT_BIAS = 0.4f;
    private static final short DEFAULT_WEATHER_CONDITION = 0; // CLEAR
    private static final float DEFAULT_STEERING_RETURN_RATE = 3.0f;

    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    @Expose
    private String name;
    @Expose
    private float stepHeight;
    @Expose
    private float defaultSlipperiness;
    @Expose
    private Map<String, Float> blocksSlipperiness = new HashMap<>();
    @Expose
    private boolean boatFallDamage;
    @Expose
    private boolean boatWaterElevation;
    @Expose
    private boolean boatAirControl;
    @Expose
    private float boatJumpForce;
    @Expose
    private boolean airStepping;
    @Expose
    private double gravity;
    @Expose
    private float yawAcceleration;
    @Expose
    private float forwardAcceleration;
    @Expose
    private float backwardAcceleration;
    @Expose
    private float turningForwardAcceleration;
    @Expose
    private boolean allowAccelerationStacking;
    @Expose
    private boolean underwaterControl;
    @Expose
    private boolean surfaceWaterControl;
    @Expose
    private int coyoteTime;
    @Expose
    private boolean waterJumping;
    @Expose
    private float swimForce;

    @Expose
    private Map<String, PerBlockSetting> perBlockSettings = new HashMap<>();

    // ── Realistic Physics Fields ──
    @Expose
    private boolean realisticPhysics;
    @Expose
    private short vehicleType = -1; // -1 = not set, 0-4 = WRC/GROUP_B/CLASSIC/LIGHTWEIGHT/TRUCK
    @Expose
    private float vehicleMass = DEFAULT_VEHICLE_MASS;
    @Expose
    private float vehicleWheelbase = DEFAULT_VEHICLE_WHEELBASE;
    @Expose
    private float vehicleCgHeight = DEFAULT_VEHICLE_CG_HEIGHT;
    @Expose
    private float vehicleTrackWidth = DEFAULT_VEHICLE_TRACK_WIDTH;
    @Expose
    private float vehicleMaxSteering = DEFAULT_VEHICLE_MAX_STEERING;
    @Expose
    private float vehicleSteeringSpeed = DEFAULT_VEHICLE_STEERING_SPEED;
    @Expose
    private float vehicleBrakingForce = DEFAULT_VEHICLE_BRAKING_FORCE;
    @Expose
    private float vehicleEngineForce = DEFAULT_VEHICLE_ENGINE_FORCE;
    @Expose
    private float vehicleDrag = DEFAULT_VEHICLE_DRAG;
    @Expose
    private float vehicleBrakeBias = DEFAULT_VEHICLE_BRAKE_BIAS;
    @Expose
    private int vehicleSubsteps = DEFAULT_VEHICLE_SUBSTEPS;
    @Expose
    private float vehicleFrontWeightBias = DEFAULT_VEHICLE_FRONT_WEIGHT_BIAS;
    @Expose
    private Map<String, String> blockSurfaceTypes = new HashMap<>();
    @Expose
    private short vehicleDrivetrain = DEFAULT_VEHICLE_DRIVETRAIN;
    @Expose
    private String defaultSurfaceType = DEFAULT_DEFAULT_SURFACE;
    @Expose
    private float vehicleSpeedSteeringFactor = DEFAULT_VEHICLE_SPEED_STEERING_FACTOR;
    @Expose
    private float vehicleEngineBraking = DEFAULT_VEHICLE_ENGINE_BRAKING;
    @Expose
    private float vehicleRollStiffnessRatio = DEFAULT_VEHICLE_ROLL_STIFFNESS_RATIO;
    @Expose
    private float awdFrontSplit = DEFAULT_AWD_FRONT_SPLIT;
    @Expose
    private short frontDifferential = DEFAULT_FRONT_DIFFERENTIAL;
    @Expose
    private short rearDifferential = DEFAULT_REAR_DIFFERENTIAL;
    @Expose
    private float lsdLockingCoeff = DEFAULT_LSD_LOCKING_COEFF;
    @Expose
    private float downforceCoefficient = DEFAULT_DOWNFORCE_COEFFICIENT;
    @Expose
    private float downforceFrontBias = DEFAULT_DOWNFORCE_FRONT_BIAS;
    @Expose
    private short weatherCondition = DEFAULT_WEATHER_CONDITION;
    @Expose
    private float steeringReturnRate = DEFAULT_STEERING_RETURN_RATE;

    public CustomBoatUtilsMode() {
        resetToVanilla();
    }

    public void resetToVanilla() {
        stepHeight = 0f;
        defaultSlipperiness = 0.6f;
        blocksSlipperiness.clear();
        boatFallDamage = true;
        boatWaterElevation = false;
        boatAirControl = false;
        boatJumpForce = 0f;
        airStepping = false;
        gravity = -0.03999999910593033;
        yawAcceleration = 1.0f;
        forwardAcceleration = 0.04f;
        backwardAcceleration = 0.005f;
        turningForwardAcceleration = 0.005f;
        allowAccelerationStacking = false;
        underwaterControl = false;
        surfaceWaterControl = false;
        coyoteTime = 0;
        waterJumping = false;
        swimForce = 0f;
        perBlockSettings.clear();
        realisticPhysics = false;
        vehicleType = -1;
        vehicleMass = DEFAULT_VEHICLE_MASS;
        vehicleWheelbase = DEFAULT_VEHICLE_WHEELBASE;
        vehicleCgHeight = DEFAULT_VEHICLE_CG_HEIGHT;
        vehicleTrackWidth = DEFAULT_VEHICLE_TRACK_WIDTH;
        vehicleMaxSteering = DEFAULT_VEHICLE_MAX_STEERING;
        vehicleSteeringSpeed = DEFAULT_VEHICLE_STEERING_SPEED;
        vehicleBrakingForce = DEFAULT_VEHICLE_BRAKING_FORCE;
        vehicleEngineForce = DEFAULT_VEHICLE_ENGINE_FORCE;
        vehicleDrag = DEFAULT_VEHICLE_DRAG;
        vehicleBrakeBias = DEFAULT_VEHICLE_BRAKE_BIAS;
        vehicleSubsteps = DEFAULT_VEHICLE_SUBSTEPS;
        vehicleFrontWeightBias = DEFAULT_VEHICLE_FRONT_WEIGHT_BIAS;
        blockSurfaceTypes.clear();
        vehicleDrivetrain = DEFAULT_VEHICLE_DRIVETRAIN;
        defaultSurfaceType = DEFAULT_DEFAULT_SURFACE;
        vehicleSpeedSteeringFactor = DEFAULT_VEHICLE_SPEED_STEERING_FACTOR;
        vehicleEngineBraking = DEFAULT_VEHICLE_ENGINE_BRAKING;
        vehicleRollStiffnessRatio = DEFAULT_VEHICLE_ROLL_STIFFNESS_RATIO;
        awdFrontSplit = DEFAULT_AWD_FRONT_SPLIT;
        frontDifferential = DEFAULT_FRONT_DIFFERENTIAL;
        rearDifferential = DEFAULT_REAR_DIFFERENTIAL;
        lsdLockingCoeff = DEFAULT_LSD_LOCKING_COEFF;
        downforceCoefficient = DEFAULT_DOWNFORCE_COEFFICIENT;
        downforceFrontBias = DEFAULT_DOWNFORCE_FRONT_BIAS;
        weatherCondition = DEFAULT_WEATHER_CONDITION;
        steeringReturnRate = DEFAULT_STEERING_RETURN_RATE;
    }

    public Map<String, String> getBlockSurfaceTypes() {
        return new HashMap<>(blockSurfaceTypes);
    }

    public void addBlockSurfaceType(String blockId, String surfaceType) {
        blockSurfaceTypes.put(blockId, surfaceType);
    }

    public void clearBlockSurfaceTypes() {
        blockSurfaceTypes.clear();
    }

    public boolean applyToPlayer(Player player) {
        if (playerHasCorrectVersion(player)) {
            finallyApplyToPlayer(player);
            return true;
        } else {
            TPlayer tPlayer = TimingSystemAPI.getTPlayer(player.getUniqueId());
            var boatUtilsWarning = tPlayer.getTheme().warning(">> ").append(Text.get(player, Warning.TRACK_REQUIRES_NEWER_BOAT_UTILS)).append(tPlayer.getTheme().warning(" <<"))
                .hoverEvent(HoverEvent.showText(Text.get(player, Hover.CLICK_TO_OPEN)))
                .clickEvent(ClickEvent.openUrl("https://modrinth.com/mod/openboatutils"));
            player.sendMessage(boatUtilsWarning);
            return false;
        }

    }

    private void finallyApplyToPlayer(Player player) {
        resetPlayer(player);

        if (this.stepHeight != 0f)
            sendShortAndFloatPacket(player, PACKET_ID_SET_STEP_HEIGHT, this.stepHeight);
        if (this.defaultSlipperiness != 0.6f)
            sendShortAndFloatPacket(player, PACKET_ID_SET_DEFAULT_SLIPPERINESS, this.defaultSlipperiness);
        if (!this.boatFallDamage)
            sendShortAndBooleanPacket(player, PACKET_ID_SET_BOAT_FALL_DAMAGE, this.boatFallDamage);
        if (this.boatWaterElevation)
            sendShortAndBooleanPacket(player, PACKET_ID_SET_BOAT_WATER_ELEVATION, this.boatWaterElevation);
        if (this.boatAirControl)
            sendShortAndBooleanPacket(player, PACKET_ID_SET_BOAT_AIR_CONTROL, this.boatAirControl);
        if (this.boatJumpForce != 0f)
            sendShortAndFloatPacket(player, PACKET_ID_SET_BOAT_JUMP_FORCE, this.boatJumpForce);
        if (this.airStepping)
            sendShortAndBooleanPacket(player, PACKET_ID_SET_AIR_STEPPING, this.airStepping);
        if (this.gravity != -0.03999999910593033)
            sendShortAndDoublePacket(player, PACKET_ID_SET_GRAVITY, this.gravity);
        if (this.yawAcceleration != 1.0f)
            sendShortAndFloatPacket(player, PACKET_ID_SET_YAW_ACCELERATION, this.yawAcceleration);
        if (this.forwardAcceleration != 0.04f)
            sendShortAndFloatPacket(player, PACKET_ID_SET_FORWARD_ACCELERATION, this.forwardAcceleration);
        if (this.backwardAcceleration != 0.005f)
            sendShortAndFloatPacket(player, PACKET_ID_SET_BACKWARD_ACCELERATION, this.backwardAcceleration);
        if (this.turningForwardAcceleration != 0.005f)
            sendShortAndFloatPacket(player, PACKET_ID_SET_TURNING_FORWARD_ACCELERATION,
                    this.turningForwardAcceleration);
        if (this.allowAccelerationStacking)
            sendShortAndBooleanPacket(player, PACKET_ID_ALLOW_ACCELERATION_STACKING, this.allowAccelerationStacking);
        if (this.underwaterControl)
            sendShortAndBooleanPacket(player, PACKET_ID_SET_UNDERWATER_CONTROL, this.underwaterControl);
        if (this.surfaceWaterControl)
            sendShortAndBooleanPacket(player, PACKET_ID_SET_SURFACE_WATER_CONTROL, this.surfaceWaterControl);
        if (this.coyoteTime != 0)
            sendShortAndIntPacket(player, PACKET_ID_SET_COYOTE_TIME, this.coyoteTime);
        if (this.waterJumping)
            sendShortAndBooleanPacket(player, PACKET_ID_SET_WATER_JUMPING, this.waterJumping);
        if (this.swimForce != 0f)
            sendShortAndFloatPacket(player, PACKET_ID_SET_SWIM_FORCE, this.swimForce);

        // Block slipperiness settings
        for (Map.Entry<String, Float> entry : this.blocksSlipperiness.entrySet()) {
            sendShortAndFloatAndStringPacket(player, PACKET_ID_SET_BLOCKS_SLIPPERINESS, entry.getValue(),
                    entry.getKey());
        }

        // Per-block settings
        for (PerBlockSetting setting : this.perBlockSettings.values()) {
            sendShortAndShortAndFloatAndStringPacket(player, PACKET_ID_SET_PER_BLOCK_SETTING, setting.getType(),
                    setting.getAsFloat(), setting.getBlockId());
        }

        // Realistic physics settings
        if (this.realisticPhysics)
            sendShortAndBooleanPacket(player, PACKET_ID_SET_REALISTIC_PHYSICS, this.realisticPhysics);
        if (this.vehicleType >= 0)
            sendShortAndShortPacket(player, PACKET_ID_SET_VEHICLE_TYPE, this.vehicleType);
        if (this.vehicleMass != DEFAULT_VEHICLE_MASS)
            sendShortAndFloatPacket(player, PACKET_ID_SET_VEHICLE_MASS, this.vehicleMass);
        if (this.vehicleWheelbase != DEFAULT_VEHICLE_WHEELBASE)
            sendShortAndFloatPacket(player, PACKET_ID_SET_VEHICLE_WHEELBASE, this.vehicleWheelbase);
        if (this.vehicleCgHeight != DEFAULT_VEHICLE_CG_HEIGHT)
            sendShortAndFloatPacket(player, PACKET_ID_SET_VEHICLE_CG_HEIGHT, this.vehicleCgHeight);
        if (this.vehicleTrackWidth != DEFAULT_VEHICLE_TRACK_WIDTH)
            sendShortAndFloatPacket(player, PACKET_ID_SET_VEHICLE_TRACK_WIDTH, this.vehicleTrackWidth);
        if (this.vehicleMaxSteering != DEFAULT_VEHICLE_MAX_STEERING)
            sendShortAndFloatPacket(player, PACKET_ID_SET_VEHICLE_MAX_STEERING, this.vehicleMaxSteering);
        if (this.vehicleSteeringSpeed != DEFAULT_VEHICLE_STEERING_SPEED)
            sendShortAndFloatPacket(player, PACKET_ID_SET_VEHICLE_STEERING_SPEED, this.vehicleSteeringSpeed);
        if (this.vehicleBrakingForce != DEFAULT_VEHICLE_BRAKING_FORCE)
            sendShortAndFloatPacket(player, PACKET_ID_SET_VEHICLE_BRAKING_FORCE, this.vehicleBrakingForce);
        if (this.vehicleEngineForce != DEFAULT_VEHICLE_ENGINE_FORCE)
            sendShortAndFloatPacket(player, PACKET_ID_SET_VEHICLE_ENGINE_FORCE, this.vehicleEngineForce);
        if (this.vehicleDrag != DEFAULT_VEHICLE_DRAG)
            sendShortAndFloatPacket(player, PACKET_ID_SET_VEHICLE_DRAG, this.vehicleDrag);
        if (this.vehicleBrakeBias != DEFAULT_VEHICLE_BRAKE_BIAS)
            sendShortAndFloatPacket(player, PACKET_ID_SET_VEHICLE_BRAKE_BIAS, this.vehicleBrakeBias);
        if (this.vehicleSubsteps != DEFAULT_VEHICLE_SUBSTEPS)
            sendShortAndIntPacket(player, PACKET_ID_SET_VEHICLE_SUBSTEPS, this.vehicleSubsteps);
        if (this.vehicleFrontWeightBias != DEFAULT_VEHICLE_FRONT_WEIGHT_BIAS)
            sendShortAndFloatPacket(player, PACKET_ID_SET_VEHICLE_FRONT_WEIGHT_BIAS, this.vehicleFrontWeightBias);

        // Block surface type mappings
        for (Map.Entry<String, String> entry : this.blockSurfaceTypes.entrySet()) {
            sendShortAndTwoStringsPacket(player, PACKET_ID_SET_BLOCK_SURFACE_TYPE, entry.getKey(), entry.getValue());
        }

        // Drivetrain type
        if (this.vehicleDrivetrain != DEFAULT_VEHICLE_DRIVETRAIN)
            sendShortAndShortPacket(player, PACKET_ID_SET_VEHICLE_DRIVETRAIN, this.vehicleDrivetrain);

        // Default surface type for unmapped blocks
        if (!this.defaultSurfaceType.equals(DEFAULT_DEFAULT_SURFACE))
            sendShortAndStringPacket(player, PACKET_ID_SET_DEFAULT_SURFACE_TYPE, this.defaultSurfaceType);

        // Speed-dependent steering factor
        if (this.vehicleSpeedSteeringFactor != DEFAULT_VEHICLE_SPEED_STEERING_FACTOR)
            sendShortAndFloatPacket(player, PACKET_ID_SET_VEHICLE_SPEED_STEERING_FACTOR, this.vehicleSpeedSteeringFactor);

        // Engine braking force
        if (this.vehicleEngineBraking != DEFAULT_VEHICLE_ENGINE_BRAKING)
            sendShortAndFloatPacket(player, PACKET_ID_SET_VEHICLE_ENGINE_BRAKING, this.vehicleEngineBraking);

        // Roll stiffness ratio (front)
        if (this.vehicleRollStiffnessRatio != DEFAULT_VEHICLE_ROLL_STIFFNESS_RATIO)
            sendShortAndFloatPacket(player, PACKET_ID_SET_VEHICLE_ROLL_STIFFNESS_RATIO, this.vehicleRollStiffnessRatio);

        // AWD front split
        if (this.awdFrontSplit != DEFAULT_AWD_FRONT_SPLIT)
            sendShortAndFloatPacket(player, PACKET_ID_SET_AWD_FRONT_SPLIT, this.awdFrontSplit);

        // Front differential
        if (this.frontDifferential != DEFAULT_FRONT_DIFFERENTIAL)
            sendShortAndShortPacket(player, PACKET_ID_SET_FRONT_DIFFERENTIAL, this.frontDifferential);

        // Rear differential
        if (this.rearDifferential != DEFAULT_REAR_DIFFERENTIAL)
            sendShortAndShortPacket(player, PACKET_ID_SET_REAR_DIFFERENTIAL, this.rearDifferential);

        // LSD locking coefficient
        if (this.lsdLockingCoeff != DEFAULT_LSD_LOCKING_COEFF)
            sendShortAndFloatPacket(player, PACKET_ID_SET_LSD_LOCKING_COEFF, this.lsdLockingCoeff);

        // Downforce coefficient
        if (this.downforceCoefficient != DEFAULT_DOWNFORCE_COEFFICIENT)
            sendShortAndFloatPacket(player, PACKET_ID_SET_DOWNFORCE_COEFFICIENT, this.downforceCoefficient);

        // Downforce front bias
        if (this.downforceFrontBias != DEFAULT_DOWNFORCE_FRONT_BIAS)
            sendShortAndFloatPacket(player, PACKET_ID_SET_DOWNFORCE_FRONT_BIAS, this.downforceFrontBias);

        // Weather condition
        if (this.weatherCondition != DEFAULT_WEATHER_CONDITION)
            sendShortAndShortPacket(player, PACKET_ID_SET_WEATHER_CONDITION, this.weatherCondition);

        // Steering return rate (self-aligning torque)
        if (this.steeringReturnRate != DEFAULT_STEERING_RETURN_RATE)
            sendShortAndFloatPacket(player, PACKET_ID_SET_STEERING_RETURN_RATE, this.steeringReturnRate);
    }

    public static void resetPlayer(Player player) {
        sendPacket(player, PACKET_ID_RESET);
    }

    // <editor-fold desc="Packet Sending Helpers">
    private static void sendPacket(Player player, short packetId) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndBooleanPacket(Player player, short packetId, boolean value) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeBoolean(value);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndFloatPacket(Player player, short packetId, float value) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeFloat(value);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndDoublePacket(Player player, short packetId, double value) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeDouble(value);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndIntPacket(Player player, short packetId, int value) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeInt(value);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndFloatAndStringPacket(Player player, short packetId, float value,
            String stringValue) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeFloat(value);
            writeString(out, stringValue);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndShortAndFloatAndStringPacket(Player player, short packetId, short settingType,
            float value, String stringValue) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeShort(settingType);
            out.writeFloat(value);
            writeString(out, stringValue);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndShortPacket(Player player, short packetId, short value) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeShort(value);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndTwoStringsPacket(Player player, short packetId, String value1, String value2) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            writeString(out, value1);
            writeString(out, value2);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndStringPacket(Player player, short packetId, String value) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            writeString(out, value);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void logPacketError(Player player, short packetId, IOException e) {
        TimingSystem.getPlugin().getLogger().log(Level.SEVERE,
                "Failed to serialize and send packet " + packetId + " for player " + player.getName(), e);
    }

    /**
     * Sends REALISTIC_SERVER_INFO packet to a player.
     * Contains the server's realistic version, supported feature flags, and server name.
     */
    public static void sendRealisticServerInfo(Player player, String realisticVersion, int featureFlags, String serverName) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(PACKET_ID_REALISTIC_SERVER_INFO);
            writeString(out, realisticVersion);
            out.writeInt(featureFlags);
            writeString(out, serverName);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, PACKET_ID_REALISTIC_SERVER_INFO, e);
        }
    }

    // </editor-fold>

    // Writes a String to a DataOutputStream in a format compatible with Minecraft's PacketByteBuf
    public static void writeString(DataOutputStream out, String stringValue) throws IOException {
        byte[] stringBytes = stringValue.getBytes(StandardCharsets.UTF_8);
        int length = stringBytes.length;

        while (true) {
            if ((length & ~SEGMENT_BITS) == 0) {
                out.writeByte(length);
                break;
            }
            out.writeByte((length & SEGMENT_BITS) | CONTINUE_BIT);
            length >>>= 7;
        }

        out.write(stringBytes);
    }

    // Applies settings from another mode to this mode
    public void applySettingsFrom(CustomBoatUtilsMode other) {
        if (other == null)
            return;

        if (other.stepHeight != 0f)
            this.stepHeight = other.stepHeight;
        if (other.defaultSlipperiness != 0.6f)
            this.defaultSlipperiness = other.defaultSlipperiness;
        this.blocksSlipperiness.putAll(other.blocksSlipperiness);
        this.boatFallDamage = other.boatFallDamage;
        this.boatWaterElevation = other.boatWaterElevation;
        this.boatAirControl = other.boatAirControl;
        if (other.boatJumpForce != 0f)
            this.boatJumpForce = other.boatJumpForce;
        if (other.gravity != -0.03999999910593033)
            this.gravity = other.gravity;
        if (other.yawAcceleration != 1.0f)
            this.yawAcceleration = other.yawAcceleration;
        if (other.forwardAcceleration != 0.04f)
            this.forwardAcceleration = other.forwardAcceleration;
        if (other.backwardAcceleration != 0.005f)
            this.backwardAcceleration = other.backwardAcceleration;
        if (other.turningForwardAcceleration != 0.005f)
            this.turningForwardAcceleration = other.turningForwardAcceleration;
        this.allowAccelerationStacking = other.allowAccelerationStacking;
        this.underwaterControl = other.underwaterControl;
        this.surfaceWaterControl = other.surfaceWaterControl;
        if (other.coyoteTime != 0)
            this.coyoteTime = other.coyoteTime;
        this.waterJumping = other.waterJumping;
        if (other.swimForce != 0f)
            this.swimForce = other.swimForce;
        this.perBlockSettings.putAll(other.perBlockSettings);
        this.airStepping = other.airStepping;
        this.realisticPhysics = other.realisticPhysics;
        if (other.vehicleType >= 0)
            this.vehicleType = other.vehicleType;
        if (other.vehicleMass != DEFAULT_VEHICLE_MASS)
            this.vehicleMass = other.vehicleMass;
        if (other.vehicleWheelbase != DEFAULT_VEHICLE_WHEELBASE)
            this.vehicleWheelbase = other.vehicleWheelbase;
        if (other.vehicleCgHeight != DEFAULT_VEHICLE_CG_HEIGHT)
            this.vehicleCgHeight = other.vehicleCgHeight;
        if (other.vehicleTrackWidth != DEFAULT_VEHICLE_TRACK_WIDTH)
            this.vehicleTrackWidth = other.vehicleTrackWidth;
        if (other.vehicleMaxSteering != DEFAULT_VEHICLE_MAX_STEERING)
            this.vehicleMaxSteering = other.vehicleMaxSteering;
        if (other.vehicleSteeringSpeed != DEFAULT_VEHICLE_STEERING_SPEED)
            this.vehicleSteeringSpeed = other.vehicleSteeringSpeed;
        if (other.vehicleBrakingForce != DEFAULT_VEHICLE_BRAKING_FORCE)
            this.vehicleBrakingForce = other.vehicleBrakingForce;
        if (other.vehicleEngineForce != DEFAULT_VEHICLE_ENGINE_FORCE)
            this.vehicleEngineForce = other.vehicleEngineForce;
        if (other.vehicleDrag != DEFAULT_VEHICLE_DRAG)
            this.vehicleDrag = other.vehicleDrag;
        if (other.vehicleBrakeBias != DEFAULT_VEHICLE_BRAKE_BIAS)
            this.vehicleBrakeBias = other.vehicleBrakeBias;
        if (other.vehicleSubsteps != DEFAULT_VEHICLE_SUBSTEPS)
            this.vehicleSubsteps = other.vehicleSubsteps;
        if (other.vehicleFrontWeightBias != DEFAULT_VEHICLE_FRONT_WEIGHT_BIAS)
            this.vehicleFrontWeightBias = other.vehicleFrontWeightBias;
        this.blockSurfaceTypes.putAll(other.blockSurfaceTypes);
        if (other.vehicleDrivetrain != DEFAULT_VEHICLE_DRIVETRAIN)
            this.vehicleDrivetrain = other.vehicleDrivetrain;
        if (!other.defaultSurfaceType.equals(DEFAULT_DEFAULT_SURFACE))
            this.defaultSurfaceType = other.defaultSurfaceType;
        if (other.vehicleSpeedSteeringFactor != DEFAULT_VEHICLE_SPEED_STEERING_FACTOR)
            this.vehicleSpeedSteeringFactor = other.vehicleSpeedSteeringFactor;
        if (other.vehicleEngineBraking != DEFAULT_VEHICLE_ENGINE_BRAKING)
            this.vehicleEngineBraking = other.vehicleEngineBraking;
        if (other.vehicleRollStiffnessRatio != DEFAULT_VEHICLE_ROLL_STIFFNESS_RATIO)
            this.vehicleRollStiffnessRatio = other.vehicleRollStiffnessRatio;
        if (other.awdFrontSplit != DEFAULT_AWD_FRONT_SPLIT)
            this.awdFrontSplit = other.awdFrontSplit;
        if (other.frontDifferential != DEFAULT_FRONT_DIFFERENTIAL)
            this.frontDifferential = other.frontDifferential;
        if (other.rearDifferential != DEFAULT_REAR_DIFFERENTIAL)
            this.rearDifferential = other.rearDifferential;
        if (other.lsdLockingCoeff != DEFAULT_LSD_LOCKING_COEFF)
            this.lsdLockingCoeff = other.lsdLockingCoeff;
        if (other.downforceCoefficient != DEFAULT_DOWNFORCE_COEFFICIENT)
            this.downforceCoefficient = other.downforceCoefficient;
        if (other.downforceFrontBias != DEFAULT_DOWNFORCE_FRONT_BIAS)
            this.downforceFrontBias = other.downforceFrontBias;
        if (other.weatherCondition != DEFAULT_WEATHER_CONDITION)
            this.weatherCondition = other.weatherCondition;
    }

    public void setBlocksSlipperiness(float slipperiness, String blockIds) {
        if (blockIds == null || blockIds.trim().isEmpty()) {
            return;
        }

        String[] blocks = blockIds.split(",");
        for (String blockId : blocks) {
            blockId = blockId.trim();
            if (!blockId.isEmpty()) {
                blocksSlipperiness.put(blockId, slipperiness);
            }
        }
    }

    public void clearBlocksSlipperiness(String blockIds) {
        if (blockIds == null || blockIds.trim().isEmpty()) {
            return;
        }

        String[] blocks = blockIds.split(",");
        for (String blockId : blocks) {
            blockId = blockId.trim();
            if (!blockId.isEmpty()) {
                blocksSlipperiness.remove(blockId);
            }
        }
    }

    public void clearAllSlipperiness() {
        blocksSlipperiness.clear();
    }

    public void setPerBlockSetting(short settingType, Object value, String blockIds) {
        if (blockIds == null || blockIds.trim().isEmpty()) {
            return;
        }

        String[] blocks = blockIds.split(",");
        for (String blockId : blocks) {
            blockId = blockId.trim();
            if (!blockId.isEmpty()) {
                String key = blockId + ":" + settingType;
                perBlockSettings.put(key, new PerBlockSetting(settingType, value, blockId));
            }
        }
    }

    public void clearPerBlockSettings(String blockIds) {
        if (blockIds == null || blockIds.trim().isEmpty()) {
            return;
        }

        String[] blocks = blockIds.split(",");
        for (String blockId : blocks) {
            String trimmedBlockId = blockId.trim();
            if (!trimmedBlockId.isEmpty()) {
                perBlockSettings.entrySet().removeIf(entry -> entry.getValue().getBlockId().equals(trimmedBlockId));
            }
        }
    }

    public void clearAllPerBlockSettings() {
        perBlockSettings.clear();
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static CustomBoatUtilsMode fromJson(String json) {
        return GSON.fromJson(json, CustomBoatUtilsMode.class);
    }

    public Map<String, List<NonDefaultSetting>> getNonDefaultSettings() {
        Map<String, List<NonDefaultSetting>> nonDefaultSettings = new HashMap<>();

        // --- Numeric Settings ---
        List<NonDefaultSetting> numericSettings = new ArrayList<>();
        if (this.stepHeight != 0f)
            numericSettings.add(new NonDefaultSetting("stepHeight", this.stepHeight, 0f));
        if (this.defaultSlipperiness != 0.6f)
            numericSettings.add(new NonDefaultSetting("defaultSlipperiness", this.defaultSlipperiness, 0.6f));
        if (this.boatJumpForce != 0f)
            numericSettings.add(new NonDefaultSetting("boatJumpForce", this.boatJumpForce, 0f));
        if (this.yawAcceleration != 1.0f)
            numericSettings.add(new NonDefaultSetting("yawAcceleration", this.yawAcceleration, 1.0f));
        if (this.forwardAcceleration != 0.04f)
            numericSettings.add(new NonDefaultSetting("forwardAcceleration", this.forwardAcceleration, 0.04f));
        if (this.backwardAcceleration != 0.005f)
            numericSettings.add(new NonDefaultSetting("backwardAcceleration", this.backwardAcceleration, 0.005f));
        if (this.turningForwardAcceleration != 0.005f)
            numericSettings.add(new NonDefaultSetting("turningForwardAcceleration", this.turningForwardAcceleration, 0.005f));
        if (this.swimForce != 0f)
            numericSettings.add(new NonDefaultSetting("swimForce", this.swimForce, 0f));
        if (this.gravity != -0.03999999910593033)
            numericSettings.add(new NonDefaultSetting("gravity", this.gravity, -0.03999999910593033));
        if (this.coyoteTime != 0)
            numericSettings.add(new NonDefaultSetting("coyoteTime", this.coyoteTime, 0));

        if (!numericSettings.isEmpty()) {
            nonDefaultSettings.put("Numeric Settings", numericSettings);
        }

        // --- Realistic Physics Settings ---
        List<NonDefaultSetting> realisticSettings = new ArrayList<>();
        if (this.realisticPhysics)
            realisticSettings.add(new NonDefaultSetting("realisticPhysics", this.realisticPhysics, false));
        if (this.vehicleType >= 0)
            realisticSettings.add(new NonDefaultSetting("vehicleType", this.vehicleType, (short) -1));
        if (this.vehicleMass != DEFAULT_VEHICLE_MASS)
            realisticSettings.add(new NonDefaultSetting("vehicleMass", this.vehicleMass, DEFAULT_VEHICLE_MASS));
        if (this.vehicleWheelbase != DEFAULT_VEHICLE_WHEELBASE)
            realisticSettings.add(new NonDefaultSetting("vehicleWheelbase", this.vehicleWheelbase, DEFAULT_VEHICLE_WHEELBASE));
        if (this.vehicleCgHeight != DEFAULT_VEHICLE_CG_HEIGHT)
            realisticSettings.add(new NonDefaultSetting("vehicleCgHeight", this.vehicleCgHeight, DEFAULT_VEHICLE_CG_HEIGHT));
        if (this.vehicleTrackWidth != DEFAULT_VEHICLE_TRACK_WIDTH)
            realisticSettings.add(new NonDefaultSetting("vehicleTrackWidth", this.vehicleTrackWidth, DEFAULT_VEHICLE_TRACK_WIDTH));
        if (this.vehicleMaxSteering != DEFAULT_VEHICLE_MAX_STEERING)
            realisticSettings.add(new NonDefaultSetting("vehicleMaxSteering", this.vehicleMaxSteering, DEFAULT_VEHICLE_MAX_STEERING));
        if (this.vehicleSteeringSpeed != DEFAULT_VEHICLE_STEERING_SPEED)
            realisticSettings.add(new NonDefaultSetting("vehicleSteeringSpeed", this.vehicleSteeringSpeed, DEFAULT_VEHICLE_STEERING_SPEED));
        if (this.vehicleBrakingForce != DEFAULT_VEHICLE_BRAKING_FORCE)
            realisticSettings.add(new NonDefaultSetting("vehicleBrakingForce", this.vehicleBrakingForce, DEFAULT_VEHICLE_BRAKING_FORCE));
        if (this.vehicleEngineForce != DEFAULT_VEHICLE_ENGINE_FORCE)
            realisticSettings.add(new NonDefaultSetting("vehicleEngineForce", this.vehicleEngineForce, DEFAULT_VEHICLE_ENGINE_FORCE));
        if (this.vehicleDrag != DEFAULT_VEHICLE_DRAG)
            realisticSettings.add(new NonDefaultSetting("vehicleDrag", this.vehicleDrag, DEFAULT_VEHICLE_DRAG));
        if (this.vehicleBrakeBias != DEFAULT_VEHICLE_BRAKE_BIAS)
            realisticSettings.add(new NonDefaultSetting("vehicleBrakeBias", this.vehicleBrakeBias, DEFAULT_VEHICLE_BRAKE_BIAS));
        if (this.vehicleSubsteps != DEFAULT_VEHICLE_SUBSTEPS)
            realisticSettings.add(new NonDefaultSetting("vehicleSubsteps", this.vehicleSubsteps, 4));
        if (this.vehicleFrontWeightBias != DEFAULT_VEHICLE_FRONT_WEIGHT_BIAS)
            realisticSettings.add(new NonDefaultSetting("vehicleFrontWeightBias", this.vehicleFrontWeightBias, DEFAULT_VEHICLE_FRONT_WEIGHT_BIAS));
        if (this.vehicleDrivetrain != DEFAULT_VEHICLE_DRIVETRAIN)
            realisticSettings.add(new NonDefaultSetting("vehicleDrivetrain", drivetrainName(this.vehicleDrivetrain), drivetrainName(DEFAULT_VEHICLE_DRIVETRAIN)));
        if (!this.defaultSurfaceType.equals(DEFAULT_DEFAULT_SURFACE))
            realisticSettings.add(new NonDefaultSetting("defaultSurfaceType", this.defaultSurfaceType, DEFAULT_DEFAULT_SURFACE));
        if (this.vehicleSpeedSteeringFactor != DEFAULT_VEHICLE_SPEED_STEERING_FACTOR)
            realisticSettings.add(new NonDefaultSetting("vehicleSpeedSteeringFactor", this.vehicleSpeedSteeringFactor, DEFAULT_VEHICLE_SPEED_STEERING_FACTOR));
        if (this.vehicleEngineBraking != DEFAULT_VEHICLE_ENGINE_BRAKING)
            realisticSettings.add(new NonDefaultSetting("vehicleEngineBraking", this.vehicleEngineBraking, DEFAULT_VEHICLE_ENGINE_BRAKING));
        if (this.vehicleRollStiffnessRatio != DEFAULT_VEHICLE_ROLL_STIFFNESS_RATIO)
            realisticSettings.add(new NonDefaultSetting("vehicleRollStiffnessRatio", this.vehicleRollStiffnessRatio, DEFAULT_VEHICLE_ROLL_STIFFNESS_RATIO));
        if (!this.blockSurfaceTypes.isEmpty())
            realisticSettings.add(new NonDefaultSetting("blockSurfaceTypes", this.blockSurfaceTypes.size() + " mapping(s)", "none"));
        if (this.awdFrontSplit != DEFAULT_AWD_FRONT_SPLIT)
            realisticSettings.add(new NonDefaultSetting("awdFrontSplit", this.awdFrontSplit, DEFAULT_AWD_FRONT_SPLIT));
        if (this.frontDifferential != DEFAULT_FRONT_DIFFERENTIAL)
            realisticSettings.add(new NonDefaultSetting("frontDifferential", differentialName(this.frontDifferential), differentialName(DEFAULT_FRONT_DIFFERENTIAL)));
        if (this.rearDifferential != DEFAULT_REAR_DIFFERENTIAL)
            realisticSettings.add(new NonDefaultSetting("rearDifferential", differentialName(this.rearDifferential), differentialName(DEFAULT_REAR_DIFFERENTIAL)));
        if (this.lsdLockingCoeff != DEFAULT_LSD_LOCKING_COEFF)
            realisticSettings.add(new NonDefaultSetting("lsdLockingCoeff", this.lsdLockingCoeff, DEFAULT_LSD_LOCKING_COEFF));
        if (this.downforceCoefficient != DEFAULT_DOWNFORCE_COEFFICIENT)
            realisticSettings.add(new NonDefaultSetting("downforceCoefficient", this.downforceCoefficient, DEFAULT_DOWNFORCE_COEFFICIENT));
        if (this.downforceFrontBias != DEFAULT_DOWNFORCE_FRONT_BIAS)
            realisticSettings.add(new NonDefaultSetting("downforceFrontBias", this.downforceFrontBias, DEFAULT_DOWNFORCE_FRONT_BIAS));
        if (this.weatherCondition != DEFAULT_WEATHER_CONDITION)
            realisticSettings.add(new NonDefaultSetting("weatherCondition", weatherName(this.weatherCondition), weatherName(DEFAULT_WEATHER_CONDITION)));
        if (this.steeringReturnRate != DEFAULT_STEERING_RETURN_RATE)
            realisticSettings.add(new NonDefaultSetting("steeringReturnRate", this.steeringReturnRate, DEFAULT_STEERING_RETURN_RATE));

        if (!realisticSettings.isEmpty()) {
            nonDefaultSettings.put("Realistic Physics", realisticSettings);
        }

        // --- Boolean Toggles ---
        List<NonDefaultSetting> booleanSettings = new ArrayList<>();
        if (!this.boatFallDamage)
            booleanSettings.add(new NonDefaultSetting("boatFallDamage", this.boatFallDamage, true));
        if (this.boatWaterElevation)
            booleanSettings.add(new NonDefaultSetting("boatWaterElevation", this.boatWaterElevation, false));
        if (this.boatAirControl)
            booleanSettings.add(new NonDefaultSetting("boatAirControl", this.boatAirControl, false));
        if (this.airStepping)
            booleanSettings.add(new NonDefaultSetting("airStepping", this.airStepping, false));
        if (this.allowAccelerationStacking)
            booleanSettings.add(new NonDefaultSetting("allowAccelerationStacking", this.allowAccelerationStacking, false));
        if (this.underwaterControl)
            booleanSettings.add(new NonDefaultSetting("underwaterControl", this.underwaterControl, false));
        if (this.surfaceWaterControl)
            booleanSettings.add(new NonDefaultSetting("surfaceWaterControl", this.surfaceWaterControl, false));
        if (this.waterJumping)
            booleanSettings.add(new NonDefaultSetting("waterJumping", this.waterJumping, false));

        if (!booleanSettings.isEmpty()) {
            nonDefaultSettings.put("Boolean Toggles", booleanSettings);
        }

        // --- Block Slipperiness ---
        if (this.blocksSlipperiness != null && !this.blocksSlipperiness.isEmpty()) {
            List<NonDefaultSetting> slipperinessList = new ArrayList<>();
            this.blocksSlipperiness.forEach(
                    (blockId, slipperiness) -> slipperinessList.add(
                            new NonDefaultSetting(blockId, slipperiness, null) // No simple default for a map entry
                    )
            );
            nonDefaultSettings.put("Block Slipperiness", slipperinessList);
        }

        // --- Per-Block Settings ---
        if (this.perBlockSettings != null && !this.perBlockSettings.isEmpty()) {
            Map<Short, List<NonDefaultSetting>> settingsByType = new HashMap<>();
            this.perBlockSettings.forEach((blockId, setting) -> {
                settingsByType
                        .computeIfAbsent(setting.getType(), k -> new ArrayList<>())
                        .add(new NonDefaultSetting(blockId, setting.getValue(), null)); // No simple default
            });

            settingsByType.forEach((type, settings) -> {
                nonDefaultSettings.put("Per-Block Settings (Type " + type + ")", settings);
            });
        }

        return nonDefaultSettings;
    }

    private int getVersionRequirementFromSettingName(String settingName) {
        switch (settingName) {
            case "placeholder" -> {
                return 1;
            }
            case "realisticPhysics", "vehicleType", "vehicleMass", "vehicleWheelbase",
                 "vehicleCgHeight", "vehicleTrackWidth", "vehicleMaxSteering",
                 "vehicleSteeringSpeed", "vehicleBrakingForce", "vehicleEngineForce",
                 "vehicleDrag", "vehicleBrakeBias", "vehicleSubsteps",
                 "vehicleFrontWeightBias", "vehicleDrivetrain", "defaultSurfaceType",
                 "blockSurfaceTypes", "vehicleSpeedSteeringFactor", "vehicleEngineBraking",
                 "vehicleRollStiffnessRatio" -> {
                return 19;
            }
            case "awdFrontSplit", "frontDifferential", "rearDifferential",
                 "lsdLockingCoeff", "downforceCoefficient", "downforceFrontBias",
                 "weatherCondition" -> {
                return 20;
            }
            default -> {
                return 11;
            }
        }
    }

    private static String drivetrainName(short id) {
        return switch (id) {
            case 0 -> "RWD";
            case 1 -> "FWD";
            case 2 -> "AWD";
            default -> "AWD";
        };
    }

    private static String differentialName(short id) {
        return switch (id) {
            case 0 -> "OPEN";
            case 1 -> "LOCKED";
            case 2 -> "LSD";
            default -> "OPEN";
        };
    }

    private static String weatherName(short id) {
        return switch (id) {
            case 0 -> "CLEAR";
            case 1 -> "RAIN";
            case 2 -> "HEAVY_RAIN";
            case 3 -> "SNOW";
            case 4 -> "FOG";
            default -> "CLEAR";
        };
    }

    public int getRequiredVersion() {
        Map<String, List<NonDefaultSetting>> nonDefaultSettings = this.getNonDefaultSettings();

        return nonDefaultSettings.values().stream()
                .flatMap(List::stream)
                .map(NonDefaultSetting::name)
                .mapToInt(this::getVersionRequirementFromSettingName)
                .max()
                .orElse(0);
    }

    public boolean playerHasCorrectVersion(Player player) {
        if (player == null) return false;
        TPlayer tplayer = TimingSystemAPI.getTPlayer(player.getUniqueId());
        if (!tplayer.hasBoatUtils()) return false;
        return (tplayer.getBoatUtilsVersion() >= getRequiredVersion());
    }
}