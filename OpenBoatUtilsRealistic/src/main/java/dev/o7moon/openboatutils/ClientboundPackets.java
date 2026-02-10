package dev.o7moon.openboatutils;

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
    SET_STEERING_RETURN_RATE;

    public static void registerCodecs() {
        //? >=1.21 {
        /*PayloadTypeRegistry.playS2C().register(OpenBoatUtils.BytePayload.ID, OpenBoatUtils.BytePayload.CODEC);
        *///?}
    }

    public static void registerHandlers(){
        //? <=1.20.4 {
        ClientPlayNetworking.registerGlobalReceiver(OpenBoatUtils.settingsChannel, (client, handler, buf, responseSender) -> {
            handlePacket(buf);
        });
        //?}
        //? >=1.21 {
        /*ClientPlayNetworking.registerGlobalReceiver(OpenBoatUtils.BytePayload.ID, ((payload, context) ->
                context.client().execute(() ->
                    handlePacket(new PacketByteBuf(Unpooled.wrappedBuffer(payload.data()))) )));
        *///?}
    }

    public static void handlePacket(PacketByteBuf buf) {
        try {
            short packetID = buf.readShort();
            switch (packetID) {
                case 0:
                    OpenBoatUtils.resetSettings();
                    return;
                case 1:
                    float stepSize = buf.readFloat();
                    OpenBoatUtils.setStepSize(stepSize);
                    return;
                case 2:
                    float slipperiness = buf.readFloat();
                    OpenBoatUtils.setAllBlocksSlipperiness(slipperiness);
                    return;
                case 3:
                    slipperiness = buf.readFloat();
                    String blocks = buf.readString();
                    String[] blocksArray = blocks.split(",");
                    OpenBoatUtils.setBlocksSlipperiness(Arrays.asList(blocksArray), slipperiness);
                    return;
                case 4:
                    boolean fallDamage = buf.readBoolean();
                    OpenBoatUtils.setFallDamage(fallDamage);
                    return;
                case 5:
                    boolean waterElevation = buf.readBoolean();
                    OpenBoatUtils.setWaterElevation(waterElevation);
                    return;
                case 6:
                    boolean airControl = buf.readBoolean();
                    OpenBoatUtils.setAirControl(airControl);
                    return;
                case 7:
                    float jumpForce = buf.readFloat();
                    OpenBoatUtils.setJumpForce(jumpForce);
                    return;
                case 8:
                    short mode = buf.readShort();
                    Modes.setMode(Modes.values()[mode]);
                    return;
                case 9:
                    double gravity = buf.readDouble();
                    OpenBoatUtils.setGravityForce(gravity);
                    return;
                case 10:
                    float accel = buf.readFloat();
                    OpenBoatUtils.setYawAcceleration(accel);
                    return;
                case 11:
                    accel = buf.readFloat();
                    OpenBoatUtils.setForwardsAcceleration(accel);
                    return;
                case 12:
                    accel = buf.readFloat();
                    OpenBoatUtils.setBackwardsAcceleration(accel);
                    return;
                case 13:
                    accel = buf.readFloat();
                    OpenBoatUtils.setTurningForwardsAcceleration(accel);
                    return;
                case 14:
                    boolean allowed = buf.readBoolean();
                    OpenBoatUtils.setAllowAccelStacking(allowed);
                    return;
                case 15:
                    OpenBoatUtils.sendVersionPacket();
                    return;
                case 16:
                    boolean enabled = buf.readBoolean();
                    OpenBoatUtils.setUnderwaterControl(enabled);
                    return;
                case 17:
                    enabled = buf.readBoolean();
                    OpenBoatUtils.setSurfaceWaterControl(enabled);
                    return;
                case 18:
                    mode = buf.readShort();
                    OpenBoatUtils.resetSettings();
                    Modes.setMode(Modes.values()[mode]);
                    return;
                case 19:
                    int time = buf.readInt();
                    OpenBoatUtils.setCoyoteTime(time);
                    return;
                case 20:
                    enabled = buf.readBoolean();
                    OpenBoatUtils.setWaterJumping(enabled);
                    return;
                case 21:
                    float force = buf.readFloat();
                    OpenBoatUtils.setSwimForce(force);
                    return;
                case 22:
                    blocks = buf.readString();
                    blocksArray = blocks.split(",");
                    OpenBoatUtils.removeBlocksSlipperiness(Arrays.asList(blocksArray));
                    return;
                case 23:
                    OpenBoatUtils.clearSlipperinessMap();
                    return;
                case 24:
                    short amount = buf.readShort();
                    for (int i = 0; i < amount; i++) {
                        mode = buf.readShort();
                        Modes.setMode(Modes.values()[mode]);
                    }
                    return;
                case 25:
                    OpenBoatUtils.resetSettings();
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
                    OpenBoatUtils.setBlocksSetting(OpenBoatUtils.PerBlockSettingType.values()[setting], Arrays.asList(blocksArray), value);
                    return;
                case 27:
                    short cmode = buf.readShort();
                    OpenBoatUtils.setCollisionMode(CollisionMode.values()[cmode]);
                    return;
                case 28:
                    enabled = buf.readBoolean();
                    OpenBoatUtils.setCanStepWhileFalling(enabled);
                    return;
                case 29:
                    enabled = buf.readBoolean();
                    OpenBoatUtils.setInterpolationCompat(enabled);
                    return;
                case 30:
                    byte collisionResolution = buf.readByte();
                    OpenBoatUtils.setCollisionResolution(collisionResolution);
                    return;
                case 31:
                    String entitytypes = buf.readString();
                    OpenBoatUtils.addToCollisionFilter(entitytypes);
                    return;
                case 32:
                    OpenBoatUtils.clearCollisionFilter();
                    return;
                case 33:
                    enabled = buf.readBoolean();
                    OpenBoatUtils.setRealisticPhysicsEnabled(enabled);
                    return;
                case 34:
                    short vehicleType = buf.readShort();
                    dev.o7moon.openboatutils.physics.VehicleType[] types = dev.o7moon.openboatutils.physics.VehicleType.values();
                    if (vehicleType >= 0 && vehicleType < types.length) {
                        OpenBoatUtils.setVehicleType(types[vehicleType]);
                    }
                    return;
                case 35:
                    OpenBoatUtils.setVehicleMass(buf.readFloat());
                    return;
                case 36:
                    OpenBoatUtils.setVehicleWheelbase(buf.readFloat());
                    return;
                case 37:
                    OpenBoatUtils.setVehicleCgHeight(buf.readFloat());
                    return;
                case 38:
                    OpenBoatUtils.setVehicleTrackWidth(buf.readFloat());
                    return;
                case 39:
                    OpenBoatUtils.setVehicleMaxSteering(buf.readFloat());
                    return;
                case 40:
                    OpenBoatUtils.setVehicleSteeringSpeed(buf.readFloat());
                    return;
                case 41:
                    OpenBoatUtils.setVehicleBrakingForce(buf.readFloat());
                    return;
                case 42:
                    OpenBoatUtils.setVehicleEngineForce(buf.readFloat());
                    return;
                case 43:
                    OpenBoatUtils.setVehicleDragCoefficient(buf.readFloat());
                    return;
                case 44:
                    OpenBoatUtils.setVehicleBrakeBias(buf.readFloat());
                    return;
                case 45:
                    OpenBoatUtils.setVehicleSubsteps(buf.readInt());
                    return;
                case 46:
                    OpenBoatUtils.setVehicleFrontWeightBias(buf.readFloat());
                    return;
                case 47:
                    String blockId = buf.readString();
                    String surfaceType = buf.readString();
                    OpenBoatUtils.setBlockSurfaceType(blockId, surfaceType);
                    return;
                case 48:
                    short drivetrainId = buf.readShort();
                    OpenBoatUtils.setVehicleDrivetrain(drivetrainId);
                    return;
                case 49:
                    String defaultSurfaceName = buf.readString();
                    OpenBoatUtils.setDefaultSurfaceType(defaultSurfaceName);
                    return;
                case 50:
                    OpenBoatUtils.setVehicleSpeedSteeringFactor(buf.readFloat());
                    return;
                case 51:
                    OpenBoatUtils.setVehicleEngineBraking(buf.readFloat());
                    return;
                case 52:
                    OpenBoatUtils.setVehicleRollStiffnessRatio(buf.readFloat());
                    return;
                case 53:
                    OpenBoatUtils.setAwdFrontSplit(buf.readFloat());
                    return;
                case 54:
                    OpenBoatUtils.setFrontDifferential(buf.readShort());
                    return;
                case 55:
                    OpenBoatUtils.setRearDifferential(buf.readShort());
                    return;
                case 56:
                    OpenBoatUtils.setLsdLockingCoeff(buf.readFloat());
                    return;
                case 57:
                    OpenBoatUtils.setDownforceCoefficient(buf.readFloat());
                    return;
                case 58:
                    OpenBoatUtils.setDownforceFrontBias(buf.readFloat());
                    return;
                case 59:
                    OpenBoatUtils.setWeatherCondition(buf.readShort());
                    return;
                case 60:
                    OpenBoatUtils.setSteeringReturnRate(buf.readFloat());
                    return;
            }
        } catch (Exception E) {
            OpenBoatUtils.LOG.error("Error when handling clientbound openboatutils packet: ");
            for (StackTraceElement e : E.getStackTrace()){
                OpenBoatUtils.LOG.error(e.toString());
            }
        }
    }
}
