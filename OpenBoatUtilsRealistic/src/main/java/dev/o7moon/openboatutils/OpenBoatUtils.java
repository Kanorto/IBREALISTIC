package dev.o7moon.openboatutils;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LilyPadBlock;
//? >=1.21.3 {
/*import net.minecraft.entity.vehicle.AbstractBoatEntity;
*///?}
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.PacketByteBuf;
//? >=1.21 {
/*import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
*///?}
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.o7moon.openboatutils.physics.DifferentialType;
import dev.o7moon.openboatutils.physics.FourWheelPhysicsEngine;
import dev.o7moon.openboatutils.physics.SurfaceProperties;
import dev.o7moon.openboatutils.physics.VehicleConfig;
import dev.o7moon.openboatutils.physics.VehicleType;
import dev.o7moon.openboatutils.physics.WeatherCondition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OpenBoatUtils implements ModInitializer {

    @Override
    public void onInitialize() {
        ClientboundPackets.registerCodecs();
        ServerboundPackets.registerCodecs();

        ServerboundPackets.registerHandlers();

        SingleplayerCommands.registerCommands();
    }

    public static void resetAll(){
        // reset the default context
        resetSettings();

        // reset additional non-context state
        interpolationCompat = false;
        collisionResolution = 1;
    }

    public static final Logger LOG = LoggerFactory.getLogger("OpenBoatUtils");

    public static final int VERSION = 20;

    public static final Identifier settingsChannel = Identifier.of("openboatutils","settings");

    public static boolean enabled = false;
    public static boolean fallDamage = true;
    public static boolean waterElevation = false;
    public static boolean airControl = false;
    public static float defaultSlipperiness = 0.6f;
    public static float jumpForce = 0f;
    public static float stepSize = 0f;
    public static double gravityForce = -0.03999999910593033;// funny rounding
    public static float yawAcceleration = 1.0f;
    public static float forwardsAcceleration = 0.04f;
    public static float backwardsAcceleration = 0.005f;
    public static float turningForwardsAcceleration = 0.005f;
    public static boolean allowAccelStacking = false;
    public static boolean underwaterControl = false;
    public static boolean surfaceWaterControl = false;
    public static int coyoteTime = 0;
    public static int coyoteTimer = 0;// timer decrements per tick, is reset to time when grounded
    public static boolean waterJumping = false;
    public static float swimForce = 0.0f;
    public static CollisionMode collision = CollisionMode.VANILLA;
    public static boolean canStepWhileFalling = false; // Setting to true fixes "boatutils jank"

    // Realistic physics engine (four-wheel model, replaces old bicycle model)
    public static FourWheelPhysicsEngine fourWheelPhysics = new FourWheelPhysicsEngine();

    public static HashMap<String, Float> vanillaSlipperinessMap;

    public static HashMap<String, Float> slipperinessMap;
    public static ArrayList<String> collision_filter;

    // non-context settings, don't reset with the rest but reset when joining a server (could persist on proxies)
    // there is a separate reset packet that includes these, but the original ones are for resetting the
    // active context rather than the entire state of the mod.
    // (08/26/25) there is actually not a separate reset packet at the moment. TODO

    public static boolean interpolationCompat = false;

    public static byte collisionResolution = 1;// How many times the move() function is called per tick on a boat, higher values result in more smaller steps to reduce the distance to a diagonal wall before getting bumped by it. 1 is same as vanilla, higher values might cause performance issues.

    public enum PerBlockSettingType {
        jumpForce,
        forwardsAccel,
        backwardsAccel,
        yawAccel,
        turnForwardsAccel,
    }

    public static HashMap<Integer, HashMap<String, Float>> perBlockSettings;

    public static HashMap<String, Float> getVanillaSlipperinessMap() {
        if (vanillaSlipperinessMap == null) {
            vanillaSlipperinessMap = new HashMap<>();
            for (Block b : Registries.BLOCK.stream().toList()) {
                if (b.getSlipperiness() != 0.6f){
                    vanillaSlipperinessMap.put(Registries.BLOCK.getId(b).toString(), b.getSlipperiness());
                }
            }
        }
        return vanillaSlipperinessMap;
    }

    public static boolean settingHasPerBlock(PerBlockSettingType setting) {
        return perBlockSettings != null && perBlockSettings.containsKey(setting.ordinal());
    }

    public static float getPerBlockForBlock(PerBlockSettingType setting, String blockid){
        return settingHasPerBlock(setting) && perBlockSettings.get(setting.ordinal()).containsKey(blockid) ? perBlockSettings.get(setting.ordinal()).get(blockid): defaultPerBlock(setting);
    }

    // i LOVE how intellij formats this
    //? >=1.21.3 {
    /*public static float getNearbySetting(AbstractBoatEntity instance, PerBlockSettingType setting) {
        *///?}
    //? <=1.21 {
        
    public static float getNearbySetting(BoatEntity instance, PerBlockSettingType setting) {
        //?}
        Box box = instance.getBoundingBox();
        Box box2 = new Box(box.minX, box.minY - 0.001, box.minZ, box.maxX, box.minY, box.maxZ);
        int i = MathHelper.floor(box2.minX) - 1;
        int j = MathHelper.ceil(box2.maxX) + 1;
        int k = MathHelper.floor(box2.minY) - 1;
        int l = MathHelper.ceil(box2.maxY) + 1;
        int m = MathHelper.floor(box2.minZ) - 1;
        int n = MathHelper.ceil(box2.maxZ) + 1;
        VoxelShape voxelShape = VoxelShapes.cuboid(box2);
        float f = 0.0f;
        int o = 0;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int p = i; p < j; ++p) {
            for (int q = m; q < n; ++q) {
                int r = (p == i || p == j - 1 ? 1 : 0) + (q == m || q == n - 1 ? 1 : 0);
                if (r == 2) continue;
                for (int s = k; s < l; ++s) {
                    if (r > 0 && (s == k || s == l - 1)) continue;
                    mutable.set(p, s, q);
                    BlockState blockState = instance.getWorld().getBlockState(mutable);
                    if (blockState.getBlock() instanceof LilyPadBlock || !VoxelShapes.matchesAnywhere(blockState.getCollisionShape(instance.getWorld(), mutable).offset(p, s, q), voxelShape, BooleanBiFunction.AND)) continue;
                    f += getPerBlockForBlock(setting, Registries.BLOCK.getId(blockState.getBlock()).toString());
                    ++o;
                }
            }
        }
        if (o == 0) return getPerBlockForBlock(setting, "minecraft:air");
        return f / (float)o;
    }

    public static float defaultPerBlock(PerBlockSettingType setting) {
        switch (setting) {
            case yawAccel -> {return yawAcceleration;}
            case jumpForce -> {return jumpForce;}
            case forwardsAccel -> {return forwardsAcceleration;}
            case backwardsAccel -> {return backwardsAcceleration;}
            case turnForwardsAccel -> {return turningForwardsAcceleration;}
        };
        return 0;// unreachable but java compiler hates me (personally)
    }

    public static HashMap<String, Float> getSlipperinessMap() {
        if (slipperinessMap == null) {
            slipperinessMap = new HashMap<>(getVanillaSlipperinessMap());
        }
        return slipperinessMap;
    }

    public static void resetSettings(){
        enabled = false;
        stepSize = 0f;
        fallDamage = true;
        waterElevation = false;
        defaultSlipperiness = 0.6f;
        airControl = false;
        jumpForce = 0f;
        gravityForce = -0.03999999910593033;
        yawAcceleration = 1.0f;
        forwardsAcceleration = 0.04f;
        backwardsAcceleration = 0.005f;
        turningForwardsAcceleration = 0.005f;
        allowAccelStacking = false;
        underwaterControl = false;
        surfaceWaterControl = false;
        coyoteTime = 0;
        waterJumping = false;
        swimForce = 0.0f;
        slipperinessMap = new HashMap<>(getVanillaSlipperinessMap());
        perBlockSettings = new HashMap<>();
        collision = CollisionMode.VANILLA;
        collision_filter = new ArrayList<>();
        canStepWhileFalling = false;
        fourWheelPhysics = new FourWheelPhysicsEngine();
        SurfaceProperties.resetBlockSurfaceMap();
        visualRollAngle = 0f;
        visualSteeringAngle = 0f;
    }

    public static void setStepSize(float stepsize){
        enabled = true;
        stepSize = stepsize;
    }

    public static void setBlocksSlipperiness(List<String> blocks, float slipperiness){
        enabled = true;
        for (String block : blocks) {
            setBlockSlipperiness(block, slipperiness);
        }
    }

    public static void setAllBlocksSlipperiness(float slipperiness){
        enabled = true;
        defaultSlipperiness = slipperiness;
    }

    static void setBlockSlipperiness(String block, float slipperiness){
        getSlipperinessMap().put(block, slipperiness);
    }

    public static float getBlockSlipperiness(String block){
        if (getSlipperinessMap().containsKey(block)) return getSlipperinessMap().get(block);
        return defaultSlipperiness;
    }

    public static float getStepSize() {
        return stepSize;
    }

    public static void setFallDamage(boolean newValue) {
        enabled = true;
        fallDamage = newValue;
    }

    public static void setWaterElevation(boolean newValue) {
        enabled = true;
        waterElevation = newValue;
    }

    public static void setAirControl(boolean newValue) {
        enabled = true;
        airControl = newValue;
    }

    public static void setJumpForce(float newValue) {
        enabled = true;
        jumpForce = newValue;
    }

    public static void sendVersionPacket(){
        PacketByteBuf packet = PacketByteBufs.create();
        packet.writeShort(ServerboundPackets.VERSION.ordinal());
        packet.writeInt(VERSION);
        packet.writeBoolean(true); // realistic mod identifier
        sendPacketC2S(packet);
    }

    //? >=1.21 {
    /*public record BytePayload(ByteBuf data) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, BytePayload> CODEC = CustomPayload.codecOf(BytePayload::write, BytePayload::new);
        public static final Id<BytePayload> ID = new Id<>(settingsChannel);

        public BytePayload(PacketByteBuf buf) {
            this(buf.copy());
            buf.readerIndex(buf.writerIndex());// so mc doesn't complain we haven't read all the bytes
        }

        void write(PacketByteBuf buf) {
            buf.writeBytes(data);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    *///?}

    public static void sendPacketC2S(PacketByteBuf packet){
        //? <=1.20.4 {
        assert settingsChannel != null;
        ClientPlayNetworking.send(settingsChannel, packet);
        //?} else {
        /*BytePayload payload = new BytePayload(packet);
        ClientPlayNetworking.send(payload);
        *///?}
    }

    public static void sendPacketS2C(ServerPlayerEntity player, PacketByteBuf packet){
        //? <=1.20.4 {
        assert settingsChannel != null;
        ServerPlayNetworking.send(player, settingsChannel, packet);
        //?} else {
        /*BytePayload payload = new BytePayload(packet);
        ServerPlayNetworking.send(player, payload);
        *///?}
    }

    public static void setGravityForce(double g){
        enabled = true;
        gravityForce = g;
    }

    public static void setYawAcceleration(float accel){
        enabled = true;
        yawAcceleration = accel;
    }

    public static void setForwardsAcceleration(float accel){
        enabled = true;
        forwardsAcceleration = accel;
    }

    public static void setBackwardsAcceleration(float accel){
        enabled = true;
        backwardsAcceleration = accel;
    }

    public static void setTurningForwardsAcceleration(float accel){
        enabled = true;
        turningForwardsAcceleration = accel;
    }

    public static void setAllowAccelStacking(boolean value) {
        enabled = true;
        allowAccelStacking = value;
    }

    public static void setUnderwaterControl(boolean value) {
        enabled = true;
        underwaterControl = value;
    }

    public static void setSurfaceWaterControl(boolean value) {
        enabled = true;
        surfaceWaterControl = value;
    }

    public static void setCoyoteTime(int t) {
        enabled = true;
        coyoteTime = t;
    }

    public static void setWaterJumping(boolean value) {
        enabled = true;
        waterJumping = value;
    }
    public static void setSwimForce(float value) {
        enabled = true;
        swimForce = value;
    }
    public static void breakSlimePlease() {
        enabled = true;
        if (getSlipperinessMap().containsKey("minecraft:slime_block")) {
            getSlipperinessMap().remove("minecraft:slime_block");
        }
    }
    public static void removeBlockSlipperiness(String block) {
        enabled = true;
        if (getSlipperinessMap().containsKey(block)) {
            getSlipperinessMap().remove(block);
        }
    }
    public static void removeBlocksSlipperiness(List<String> blocks) {
        enabled = true;
        for (String block : blocks) {
            removeBlockSlipperiness(block);
        }
    }
    public static void clearSlipperinessMap() {
        enabled = true;
        slipperinessMap = new HashMap<>();
    }

    //? >=1.21.3 {
    /*public static float GetJumpForce(AbstractBoatEntity boat) {
        *///?}
    //? <=1.21 {
    public static float GetJumpForce(BoatEntity boat) {
     
    //?}
        if (!settingHasPerBlock(PerBlockSettingType.jumpForce)) return jumpForce;
        else return getNearbySetting(boat, PerBlockSettingType.jumpForce);
    }

    //? >=1.21.3 {
    /*public static float GetYawAccel(AbstractBoatEntity boat) {
        *///?}
    //? <=1.21 {
    public static float GetYawAccel(BoatEntity boat) {
    //?}
        if (!settingHasPerBlock(PerBlockSettingType.yawAccel)) return yawAcceleration;
        else return getNearbySetting(boat, PerBlockSettingType.yawAccel);
    }

    //? >=1.21.3 {
    /*public static float GetForwardAccel(AbstractBoatEntity boat) {
        *///?}
    //? <=1.21 {
    public static float GetForwardAccel(BoatEntity boat) {
    //?}
        if (!settingHasPerBlock(PerBlockSettingType.forwardsAccel)) return forwardsAcceleration;
        else return getNearbySetting(boat, PerBlockSettingType.forwardsAccel);
    }

    //? >=1.21.3 {
    /*public static float GetBackwardAccel(AbstractBoatEntity boat) {
    *///?}
    //? <=1.21 {
    public static float GetBackwardAccel(BoatEntity boat) {
    //?}
        if (!settingHasPerBlock(PerBlockSettingType.backwardsAccel)) return backwardsAcceleration;
        else return getNearbySetting(boat, PerBlockSettingType.backwardsAccel);
    }

    //? >=1.21.3 {
    /*public static float GetTurnForwardAccel(AbstractBoatEntity boat) {
    *///?}
    //? <=1.21 {
    public static float GetTurnForwardAccel(BoatEntity boat) {
    //?}
        if (!settingHasPerBlock(PerBlockSettingType.turnForwardsAccel)) return turningForwardsAcceleration;
        else return getNearbySetting(boat, PerBlockSettingType.turnForwardsAccel);
    }

    public static void setBlocksSetting(PerBlockSettingType setting, List<String> blocks, float value) {
        enabled = true;
        if (!settingHasPerBlock(setting)) perBlockSettings.put(setting.ordinal(), new HashMap<>());
        HashMap<String, Float> map = perBlockSettings.get(setting.ordinal());
        for (String block : blocks) {
            map.put(block, value);
        }
    }
    public static void setBlockSetting(PerBlockSettingType setting, String block, float value) {
        ArrayList<String> blocks = new ArrayList<>();
        blocks.add(block);
        setBlocksSetting(setting, blocks, value);
    }

    public static void setCollisionMode(CollisionMode mode) {
        enabled = true;
        collision = mode;
    }

    public static CollisionMode getCollisionMode() {
        return collision;
    }

    public static boolean canStepWhileFalling() {
        return canStepWhileFalling;
    }

    public static void setCanStepWhileFalling(boolean canStepWhileFalling) {
        enabled = true;
        OpenBoatUtils.canStepWhileFalling = canStepWhileFalling;
    }

    // doesn't deal with .enabled because its a non-context setting that is for the general runtime of obu
    // and not a specific client boat
    public static void setInterpolationCompat(boolean interpolationCompat) {
        OpenBoatUtils.interpolationCompat = interpolationCompat;
    }

    public static void setCollisionResolution(byte collisionResolution) {
        OpenBoatUtils.enabled = true;
        OpenBoatUtils.collisionResolution = collisionResolution;
    }

    public static void clearCollisionFilter() {
        // is the vanilla state, no need to turn on .enabled
        OpenBoatUtils.collision_filter = new ArrayList<>();
    }

    public static void addToCollisionFilter(String s) {
        for (String etype : s.split(",")) {
            enabled = true;
            OpenBoatUtils.collision_filter.add(etype);
        }
    }

    public static boolean entityIsInCollisionFilter(Entity entity) {
        return OpenBoatUtils.collision_filter.contains(Registries.ENTITY_TYPE.getId(entity.getType()).toString());
    }

    // ─── REALISTIC PHYSICS METHODS ───

    public static void setRealisticPhysicsEnabled(boolean value) {
        enabled = true;
        fourWheelPhysics.setEnabled(value);
    }

    public static void setVehicleType(VehicleType type) {
        enabled = true;
        VehicleConfig config = type.toConfig();
        fourWheelPhysics.setConfig(config);
        fourWheelPhysics.setEnabled(true);
    }

    public static void setVehicleConfig(VehicleConfig config) {
        enabled = true;
        fourWheelPhysics.setConfig(config);
        fourWheelPhysics.setEnabled(true);
    }

    public static void setVehicleMass(float mass) {
        enabled = true;
        fourWheelPhysics.getConfig().mass = mass;
    }

    public static void setVehicleWheelbase(float wheelbase) {
        enabled = true;
        fourWheelPhysics.getConfig().wheelbase = wheelbase;
    }

    public static void setVehicleCgHeight(float cgHeight) {
        enabled = true;
        fourWheelPhysics.getConfig().cgHeight = cgHeight;
    }

    public static void setVehicleTrackWidth(float trackWidth) {
        enabled = true;
        fourWheelPhysics.getConfig().trackWidth = trackWidth;
    }

    public static void setVehicleMaxSteering(float maxSteering) {
        enabled = true;
        fourWheelPhysics.getConfig().maxSteeringAngle = maxSteering;
    }

    public static void setVehicleSteeringSpeed(float steeringSpeed) {
        enabled = true;
        fourWheelPhysics.getConfig().steeringSpeed = steeringSpeed;
    }

    public static void setVehicleBrakingForce(float brakingForce) {
        enabled = true;
        fourWheelPhysics.getConfig().brakingForce = brakingForce;
    }

    public static void setVehicleEngineForce(float engineForce) {
        enabled = true;
        fourWheelPhysics.getConfig().engineForce = engineForce;
    }

    public static void setVehicleDragCoefficient(float drag) {
        enabled = true;
        fourWheelPhysics.getConfig().dragCoefficient = drag;
    }

    public static void setVehicleBrakeBias(float brakeBias) {
        enabled = true;
        fourWheelPhysics.getConfig().brakeBias = brakeBias;
    }

    public static void setVehicleSubsteps(int substeps) {
        enabled = true;
        int clamped = Math.max(1, Math.min(10, substeps));
        fourWheelPhysics.getConfig().substeps = clamped;
    }

    public static void setVehicleFrontWeightBias(float bias) {
        enabled = true;
        fourWheelPhysics.getConfig().frontWeightBias = bias;
    }

    public static void setBlockSurfaceType(String blockId, String surfaceType) {
        enabled = true;
        SurfaceProperties surface = SurfaceProperties.getSurfaceByName(surfaceType);
        SurfaceProperties.setBlockSurface(blockId, surface);
    }

    public static void setVehicleDrivetrain(short drivetrainId) {
        enabled = true;
        dev.o7moon.openboatutils.physics.DrivetrainType dt = dev.o7moon.openboatutils.physics.DrivetrainType.fromId(drivetrainId);
        fourWheelPhysics.getConfig().drivetrain = dt;
    }

    public static void setDefaultSurfaceType(String surfaceName) {
        enabled = true;
        SurfaceProperties.setDefaultSurfaceByName(surfaceName);
    }

    public static void setVehicleSpeedSteeringFactor(float factor) {
        enabled = true;
        fourWheelPhysics.getConfig().speedSteeringFactor = factor;
    }

    public static void setVehicleEngineBraking(float braking) {
        enabled = true;
        fourWheelPhysics.getConfig().engineBraking = braking;
    }

    public static void setVehicleRollStiffnessRatio(float ratio) {
        enabled = true;
        fourWheelPhysics.getConfig().rollStiffnessRatioFront = ratio;
    }

    public static void resetRealisticPhysics() {
        fourWheelPhysics = new FourWheelPhysicsEngine();
        SurfaceProperties.resetBlockSurfaceMap();
    }

    // ─── NEW FOUR-WHEEL SPECIFIC METHODS ───

    public static void setAwdFrontSplit(float split) {
        enabled = true;
        fourWheelPhysics.getConfig().awdFrontSplit = Math.max(0.0f, Math.min(1.0f, split));
    }

    public static void setFrontDifferential(short diffId) {
        enabled = true;
        fourWheelPhysics.getConfig().frontDifferential = DifferentialType.fromId(diffId);
    }

    public static void setRearDifferential(short diffId) {
        enabled = true;
        fourWheelPhysics.getConfig().rearDifferential = DifferentialType.fromId(diffId);
    }

    public static void setLsdLockingCoeff(float coeff) {
        enabled = true;
        fourWheelPhysics.getConfig().lsdLockingCoeff = Math.max(0.0f, Math.min(1.0f, coeff));
    }

    public static void setDownforceCoefficient(float coeff) {
        enabled = true;
        fourWheelPhysics.getConfig().downforceCoefficient = Math.max(0.0f, coeff);
    }

    public static void setDownforceFrontBias(float bias) {
        enabled = true;
        fourWheelPhysics.getConfig().downforceFrontBias = Math.max(0.0f, Math.min(1.0f, bias));
    }

    public static void setWeatherCondition(short weatherId) {
        enabled = true;
        fourWheelPhysics.setWeather(WeatherCondition.fromId(weatherId));
    }

    public static void setSteeringReturnRate(float rate) {
        enabled = true;
        fourWheelPhysics.getConfig().steeringReturnRate = Math.max(0.0f, rate);
    }

    // ─── VISUAL STATE (for renderer access) ───
    /** Current visual roll angle in degrees (set each tick by BoatMixin, read by render thread) */
    public static volatile float visualRollAngle = 0f;
    /** Current visual steering angle in radians (set each tick by BoatMixin, read by render thread) */
    public static volatile float visualSteeringAngle = 0f;
}
