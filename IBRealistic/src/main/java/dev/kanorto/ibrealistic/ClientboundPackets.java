package dev.kanorto.ibrealistic;

//? >=1.21 {
/*import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
*///?}
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;

import java.util.Arrays;

public enum ClientboundPackets {
    RESET,
    SET_STEP_HEIGHT,
    SET_DEFAULT_SLIPPERINESS,
    SET_BLOCKS_SLIPPERINESS,
    SET_BOAT_FALL_DAMAGE,
    SET_BOAT_WATER_ELEVATION,
    SET_AIR_CONTROL,
    SET_BOAT_JUMP_FORCE,
    SET_MODE,
    SET_GRAVITY,
    SET_YAW_ACCEL,
    SET_FORWARD_ACCEL,
    SET_BACKWARD_ACCEL,
    SET_TURN_ACCEL,
    ALLOW_ACCEL_STACKING,
    RESEND_VERSION,
    SET_UNDERWATER_CONTROL,
    SET_SURFACE_WATER_CONTROL,
    SET_EXCLUSIVE_MODE,
    SET_COYOTE_TIME,
    SET_WATER_JUMPING,
    SET_SWIM_FORCE,
    REMOVE_BLOCKS_SLIPPERINESS,
    CLEAR_SLIPPERINESS,
    MODE_SERIES,
    EXCLUSIVE_MODE_SERIES,
    SET_PER_BLOCK,
    SET_COLLISION_MODE,
    SET_STEP_WHILE_FALLING,
    SET_INTERPOLATION_COMPAT,
    SET_COLLISION_RESOLUTION,
    ADD_COLLISION_ENTITYTYPE_FILTER,
    CLEAR_COLLISION_ENTITYTYPE_FILTER,
    SET_REALISTIC_PHYSICS,
    SET_VEHICLE_TYPE,
    SET_VEHICLE_MASS,
    SET_VEHICLE_WHEELBASE,
    SET_VEHICLE_CG_HEIGHT,
    SET_VEHICLE_TRACK_WIDTH,
    SET_VEHICLE_MAX_STEERING,
    SET_VEHICLE_STEERING_SPEED,
    SET_VEHICLE_BRAKING_FORCE,
    SET_VEHICLE_ENGINE_FORCE,
    SET_VEHICLE_DRAG,
    SET_VEHICLE_BRAKE_BIAS,
    SET_VEHICLE_SUBSTEPS,
    SET_VEHICLE_FRONT_WEIGHT_BIAS,
    SET_BLOCK_SURFACE_TYPE,
    SET_VEHICLE_DRIVETRAIN,
    SET_DEFAULT_SURFACE_TYPE,
    SET_VEHICLE_SPEED_STEERING_FACTOR,
    SET_VEHICLE_ENGINE_BRAKING,
    SET_VEHICLE_ROLL_STIFFNESS_RATIO,
    SET_AWD_FRONT_SPLIT,
    SET_FRONT_DIFFERENTIAL,
    SET_REAR_DIFFERENTIAL,
    SET_LSD_LOCKING_COEFF,
    SET_DOWNFORCE_COEFFICIENT,
    SET_DOWNFORCE_FRONT_BIAS,
    SET_WEATHER_CONDITION,
    SET_STEERING_RETURN_RATE,
    REALISTIC_SERVER_INFO,
    SET_TIRE_PRESET,
    SET_SUSPENSION_PRESET,
    SET_ENGINE_PRESET,
    SET_BODY_PRESET,
    SET_STEERING_PRESET,
    SET_BRAKE_PRESET,
    SET_WEIGHT_DISTRIBUTION_PRESET,
    SET_RACE_COUNTDOWN;

    public static void registerCodecs() {
        //? >=1.21 {
        /*PayloadTypeRegistry.playS2C().register(IBRealistic.BytePayload.ID, IBRealistic.BytePayload.CODEC);
        *///?}
    }

    public static void registerHandlers(){
        //? <=1.20.4 {
        ClientPlayNetworking.registerGlobalReceiver(IBRealistic.settingsChannel, (client, handler, buf, responseSender) -> {
            handlePacket(buf);
        });
        //?}
        //? >=1.21 {
        /*ClientPlayNetworking.registerGlobalReceiver(IBRealistic.BytePayload.ID, ((payload, context) ->
                context.client().execute(() ->
                    handlePacket(new PacketByteBuf(Unpooled.wrappedBuffer(payload.data()))) )));
        *///?}
    }

    public static void handlePacket(PacketByteBuf buf) {
        try {
            short packetID = buf.readShort();
            switch (packetID) {
                case 0:
                    // RESET packet: only reset IBRealistic-specific state.
                    // In multiplayer, OBU resets its own state via openboatutils:settings channel.
                    // In singleplayer, packets 0-32 are also processed by cases below (which
                    // set OBU fields directly), so resetSettings() is not needed here.
                    IBRealistic.resetRealisticState();
                    return;
                case 1:
                    float stepSize = buf.readFloat();
                    IBRealistic.setStepSize(stepSize);
                    return;
                case 2:
                    float slipperiness = buf.readFloat();
                    IBRealistic.setAllBlocksSlipperiness(slipperiness);
                    return;
                case 3:
                    slipperiness = buf.readFloat();
                    String blocks = buf.readString();
                    String[] blocksArray = blocks.split(",");
                    IBRealistic.setBlocksSlipperiness(Arrays.asList(blocksArray), slipperiness);
                    return;
                case 4:
                    boolean fallDamage = buf.readBoolean();
                    IBRealistic.setFallDamage(fallDamage);
                    return;
                case 5:
                    boolean waterElevation = buf.readBoolean();
                    IBRealistic.setWaterElevation(waterElevation);
                    return;
                case 6:
                    boolean airControl = buf.readBoolean();
                    IBRealistic.setAirControl(airControl);
                    return;
                case 7:
                    float jumpForce = buf.readFloat();
                    IBRealistic.setJumpForce(jumpForce);
                    return;
                case 8:
                    short mode = buf.readShort();
                    Modes.setMode(Modes.values()[mode]);
                    return;
                case 9:
                    double gravity = buf.readDouble();
                    IBRealistic.setGravityForce(gravity);
                    return;
                case 10:
                    float accel = buf.readFloat();
                    IBRealistic.setYawAcceleration(accel);
                    return;
                case 11:
                    accel = buf.readFloat();
                    IBRealistic.setForwardsAcceleration(accel);
                    return;
                case 12:
                    accel = buf.readFloat();
                    IBRealistic.setBackwardsAcceleration(accel);
                    return;
                case 13:
                    accel = buf.readFloat();
                    IBRealistic.setTurningForwardsAcceleration(accel);
                    return;
                case 14:
                    boolean allowed = buf.readBoolean();
                    IBRealistic.setAllowAccelStacking(allowed);
                    return;
                case 15:
                    IBRealistic.sendVersionPacket();
                    return;
                case 16:
                    boolean enabled = buf.readBoolean();
                    IBRealistic.setUnderwaterControl(enabled);
                    return;
                case 17:
                    enabled = buf.readBoolean();
                    IBRealistic.setSurfaceWaterControl(enabled);
                    return;
                case 18:
                    mode = buf.readShort();
                    IBRealistic.resetSettings();
                    Modes.setMode(Modes.values()[mode]);
                    return;
                case 19:
                    int time = buf.readInt();
                    IBRealistic.setCoyoteTime(time);
                    return;
                case 20:
                    enabled = buf.readBoolean();
                    IBRealistic.setWaterJumping(enabled);
                    return;
                case 21:
                    float force = buf.readFloat();
                    IBRealistic.setSwimForce(force);
                    return;
                case 22:
                    blocks = buf.readString();
                    blocksArray = blocks.split(",");
                    IBRealistic.removeBlocksSlipperiness(Arrays.asList(blocksArray));
                    return;
                case 23:
                    IBRealistic.clearSlipperinessMap();
                    return;
                case 24:
                    short amount = buf.readShort();
                    for (int i = 0; i < amount; i++) {
                        mode = buf.readShort();
                        Modes.setMode(Modes.values()[mode]);
                    }
                    return;
                case 25:
                    IBRealistic.resetSettings();
                    amount = buf.readShort();
                    for (int i = 0; i < amount; i++) {
                        mode = buf.readShort();
                        Modes.setMode(Modes.values()[mode]);
                    }
                    return;
                case 26:
                    short setting = buf.readShort();
                    float value = buf.readFloat();
                    blocks = buf.readString();
                    blocksArray = blocks.split(",");
                    IBRealistic.setBlocksSetting(IBRealistic.PerBlockSettingType.values()[setting], Arrays.asList(blocksArray), value);
                    return;
                case 27:
                    short cmode = buf.readShort();
                    IBRealistic.setCollisionMode(CollisionMode.values()[cmode]);
                    return;
                case 28:
                    enabled = buf.readBoolean();
                    IBRealistic.setCanStepWhileFalling(enabled);
                    return;
                case 29:
                    enabled = buf.readBoolean();
                    IBRealistic.setInterpolationCompat(enabled);
                    return;
                case 30:
                    byte collisionResolution = buf.readByte();
                    IBRealistic.setCollisionResolution(collisionResolution);
                    return;
                case 31:
                    String entitytypes = buf.readString();
                    IBRealistic.addToCollisionFilter(entitytypes);
                    return;
                case 32:
                    IBRealistic.clearCollisionFilter();
                    return;
                case 33:
                    enabled = buf.readBoolean();
                    IBRealistic.setRealisticPhysicsEnabled(enabled);
                    return;
                case 34:
                    short vehicleType = buf.readShort();
                    dev.kanorto.ibrealistic.physics.VehicleType[] types = dev.kanorto.ibrealistic.physics.VehicleType.values();
                    if (vehicleType >= 0 && vehicleType < types.length) {
                        IBRealistic.setVehicleType(types[vehicleType]);
                    }
                    return;
                case 35:
                    IBRealistic.setVehicleMass(buf.readFloat());
                    return;
                case 36:
                    IBRealistic.setVehicleWheelbase(buf.readFloat());
                    return;
                case 37:
                    IBRealistic.setVehicleCgHeight(buf.readFloat());
                    return;
                case 38:
                    IBRealistic.setVehicleTrackWidth(buf.readFloat());
                    return;
                case 39:
                    IBRealistic.setVehicleMaxSteering(buf.readFloat());
                    return;
                case 40:
                    IBRealistic.setVehicleSteeringSpeed(buf.readFloat());
                    return;
                case 41:
                    IBRealistic.setVehicleBrakingForce(buf.readFloat());
                    return;
                case 42:
                    IBRealistic.setVehicleEngineForce(buf.readFloat());
                    return;
                case 43:
                    IBRealistic.setVehicleDragCoefficient(buf.readFloat());
                    return;
                case 44:
                    IBRealistic.setVehicleBrakeBias(buf.readFloat());
                    return;
                case 45:
                    IBRealistic.setVehicleSubsteps(buf.readInt());
                    return;
                case 46:
                    IBRealistic.setVehicleFrontWeightBias(buf.readFloat());
                    return;
                case 47:
                    String blockId = buf.readString();
                    String surfaceType = buf.readString();
                    IBRealistic.setBlockSurfaceType(blockId, surfaceType);
                    return;
                case 48:
                    short drivetrainId = buf.readShort();
                    IBRealistic.setVehicleDrivetrain(drivetrainId);
                    return;
                case 49:
                    String defaultSurfaceName = buf.readString();
                    IBRealistic.setDefaultSurfaceType(defaultSurfaceName);
                    return;
                case 50:
                    IBRealistic.setVehicleSpeedSteeringFactor(buf.readFloat());
                    return;
                case 51:
                    IBRealistic.setVehicleEngineBraking(buf.readFloat());
                    return;
                case 52:
                    IBRealistic.setVehicleRollStiffnessRatio(buf.readFloat());
                    return;
                case 53:
                    IBRealistic.setAwdFrontSplit(buf.readFloat());
                    return;
                case 54:
                    IBRealistic.setFrontDifferential(buf.readShort());
                    return;
                case 55:
                    IBRealistic.setRearDifferential(buf.readShort());
                    return;
                case 56:
                    IBRealistic.setLsdLockingCoeff(buf.readFloat());
                    return;
                case 57:
                    IBRealistic.setDownforceCoefficient(buf.readFloat());
                    return;
                case 58:
                    IBRealistic.setDownforceFrontBias(buf.readFloat());
                    return;
                case 59:
                    IBRealistic.setWeatherCondition(buf.readShort());
                    return;
                case 60:
                    IBRealistic.setSteeringReturnRate(buf.readFloat());
                    return;
                case 61:
                    String serverVersion = buf.readString();
                    int serverFeatures = buf.readInt();
                    String serverName = buf.readString();
                    IBRealistic.serverRealisticVersion = serverVersion;
                    IBRealistic.serverFeatures = serverFeatures;
                    IBRealistic.serverName = serverName;
                    IBRealistic.sendRealisticClientInfoPacket();
                    IBRealistic.LOG.info("Server realistic info: version=" + serverVersion
                            + " features=" + serverFeatures + " name=" + serverName);
                    return;
                case 62:
                    IBRealistic.setTirePreset(buf.readShort());
                    return;
                case 63:
                    IBRealistic.setSuspensionPreset(buf.readShort());
                    return;
                case 64:
                    IBRealistic.setEnginePreset(buf.readShort());
                    return;
                case 65:
                    IBRealistic.setBodyPreset(buf.readShort());
                    return;
                case 66:
                    IBRealistic.setSteeringPreset(buf.readShort());
                    return;
                case 67:
                    IBRealistic.setBrakePreset(buf.readShort());
                    return;
                case 68:
                    IBRealistic.setWeightDistributionPreset(buf.readShort());
                    return;
                case 69:
                    long goTimeMs = buf.readLong();
                    int countdownSeconds = buf.readInt();
                    IBRealistic.setRaceCountdown(goTimeMs, countdownSeconds);
                    return;
            }
        } catch (Exception E) {
            IBRealistic.LOG.error("Error when handling clientbound ibrealistic packet: ");
            for (StackTraceElement e : E.getStackTrace()){
                IBRealistic.LOG.error(e.toString());
            }
        }
    }
}
