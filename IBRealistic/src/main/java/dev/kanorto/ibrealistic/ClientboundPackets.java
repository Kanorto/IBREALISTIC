package dev.kanorto.ibrealistic;

//? >=1.21 {
/*import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
*///?}
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;

public enum ClientboundPackets {
    // ─── OBU-BASE PACKETS (0-32) ───
    // These are handled by OpenBoatUtils on the openboatutils:settings channel.
    // Enum values kept to preserve ordinal=packetID mapping for singleplayer commands.
    RESET,                          // 0  - also handled here for resetting realistic state
    SET_STEP_HEIGHT,                // 1  - OBU
    SET_DEFAULT_SLIPPERINESS,       // 2  - OBU
    SET_BLOCKS_SLIPPERINESS,        // 3  - OBU
    SET_BOAT_FALL_DAMAGE,           // 4  - OBU
    SET_BOAT_WATER_ELEVATION,       // 5  - OBU
    SET_AIR_CONTROL,                // 6  - OBU
    SET_BOAT_JUMP_FORCE,            // 7  - OBU
    SET_MODE,                       // 8  - OBU
    SET_GRAVITY,                    // 9  - OBU
    SET_YAW_ACCEL,                  // 10 - OBU
    SET_FORWARD_ACCEL,              // 11 - OBU
    SET_BACKWARD_ACCEL,             // 12 - OBU
    SET_TURN_ACCEL,                 // 13 - OBU
    ALLOW_ACCEL_STACKING,           // 14 - OBU
    RESEND_VERSION,                 // 15 - OBU
    SET_UNDERWATER_CONTROL,         // 16 - OBU
    SET_SURFACE_WATER_CONTROL,      // 17 - OBU
    SET_EXCLUSIVE_MODE,             // 18 - OBU
    SET_COYOTE_TIME,                // 19 - OBU
    SET_WATER_JUMPING,              // 20 - OBU
    SET_SWIM_FORCE,                 // 21 - OBU
    REMOVE_BLOCKS_SLIPPERINESS,     // 22 - OBU
    CLEAR_SLIPPERINESS,             // 23 - OBU
    MODE_SERIES,                    // 24 - OBU
    EXCLUSIVE_MODE_SERIES,          // 25 - OBU
    SET_PER_BLOCK,                  // 26 - OBU
    SET_COLLISION_MODE,             // 27 - OBU
    SET_STEP_WHILE_FALLING,         // 28 - OBU
    SET_INTERPOLATION_COMPAT,       // 29 - OBU
    SET_COLLISION_RESOLUTION,       // 30 - OBU
    ADD_COLLISION_ENTITYTYPE_FILTER, // 31 - OBU
    CLEAR_COLLISION_ENTITYTYPE_FILTER, // 32 - OBU
    // ─── IBREALISTIC PACKETS (33-69) ───
    // These are handled by IBRealistic on the ibrealistic:settings channel.
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
    SET_RACE_COUNTDOWN,
    // ─── DAMAGE & WEAR PACKETS (70-76) ───
    SET_DAMAGE_ENABLED,          // 70 — enable/disable damage system
    SYNC_TIRE_WEAR,              // 71 — authoritative tire wear (4 floats)
    SYNC_ENGINE_TEMP,            // 72 — authoritative engine temperature
    SYNC_BODY_DAMAGE,            // 73 — authoritative body damage
    SET_SERVICE_ZONE,            // 74 — service zone state (bool + float)
    SET_DAMAGE_CONFIG,           // 75 — damage rates configuration
    DAMAGE_NOTIFICATION;         // 76 — server-sent damage notification

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
                    // RESET on ibrealistic:settings channel — reset only realistic state.
                    // OBU resets its own state via its handler on openboatutils:settings.
                    IBRealistic.resetRealisticState();
                    return;
                // Cases 1-32: mostly handled by OBU on openboatutils:settings.
                // Exceptions: SET_MODE (8), SET_EXCLUSIVE_MODE (18), MODE_SERIES (24),
                // EXCLUSIVE_MODE_SERIES (25) — these are handled here for singleplayer
                // because IBRealistic's Modes enum has realistic modes (25+) that
                // OBU's Modes enum doesn't know about.
                case 8: {
                    // SET_MODE — apply a single mode (supports OBU + realistic modes)
                    short mode = buf.readShort();
                    Modes[] allModes = Modes.values();
                    if (mode >= 0 && mode < allModes.length) {
                        Modes.setMode(allModes[mode]);
                    }
                    return;
                }
                case 18: {
                    // SET_EXCLUSIVE_MODE — reset all settings, then apply a single mode
                    short mode = buf.readShort();
                    IBRealistic.resetSettings();
                    Modes[] allModes = Modes.values();
                    if (mode >= 0 && mode < allModes.length) {
                        Modes.setMode(allModes[mode]);
                    }
                    return;
                }
                case 24: {
                    // MODE_SERIES — apply multiple modes in sequence
                    short count = buf.readShort();
                    Modes[] allModes = Modes.values();
                    for (int i = 0; i < count; i++) {
                        short mode = buf.readShort();
                        if (mode >= 0 && mode < allModes.length) {
                            Modes.setMode(allModes[mode]);
                        }
                    }
                    return;
                }
                case 25: {
                    // EXCLUSIVE_MODE_SERIES — reset all settings, then apply multiple modes
                    IBRealistic.resetSettings();
                    short count = buf.readShort();
                    Modes[] allModes = Modes.values();
                    for (int i = 0; i < count; i++) {
                        short mode = buf.readShort();
                        if (mode >= 0 && mode < allModes.length) {
                            Modes.setMode(allModes[mode]);
                        }
                    }
                    return;
                }
                case 1: case 2: case 3: case 4: case 5: case 6: case 7:
                case 9: case 10: case 11: case 12: case 13: case 14: case 15: case 16:
                case 17: case 19: case 20: case 21: case 22: case 23:
                case 26: case 27: case 28: case 29: case 30: case 31: case 32:
                    IBRealistic.LOG.warn("Received OBU-base packet {} on ibrealistic:settings channel. "
                            + "This packet should be sent via openboatutils:settings.", packetID);
                    return;
                case 33:
                    boolean enabled = buf.readBoolean();
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
                // ─── DAMAGE & WEAR PACKETS (70-76) ───
                case 70:
                    IBRealistic.setDamageEnabled(buf.readBoolean());
                    return;
                case 71:
                    IBRealistic.syncTireWear(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
                    return;
                case 72:
                    IBRealistic.syncEngineTemp(buf.readFloat());
                    return;
                case 73:
                    IBRealistic.syncBodyDamage(buf.readFloat());
                    return;
                case 74:
                    IBRealistic.setServiceZoneState(buf.readBoolean(), buf.readFloat());
                    return;
                case 75:
                    // SET_DAMAGE_CONFIG — reserved for future wear rate configuration
                    return;
                case 76: {
                    // DAMAGE_NOTIFICATION — server sends a notification message to show on HUD
                    String message = buf.readString(256);
                    int color = buf.readInt();
                    long duration = buf.readLong();
                    dev.kanorto.ibrealistic.client.HudNotificationRenderer.addNotification(message, color, duration);
                    return;
                }
            }
        } catch (Exception E) {
            IBRealistic.LOG.error("Error when handling clientbound ibrealistic packet: ");
            for (StackTraceElement e : E.getStackTrace()){
                IBRealistic.LOG.error(e.toString());
            }
        }
    }
}
