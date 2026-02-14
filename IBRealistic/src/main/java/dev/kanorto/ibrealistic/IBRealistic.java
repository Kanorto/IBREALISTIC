package dev.kanorto.ibrealistic;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
//? >=1.21 {
/*import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
*///?}
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.kanorto.ibrealistic.physics.DifferentialType;
import dev.kanorto.ibrealistic.physics.FourWheelPhysicsEngine;
import dev.kanorto.ibrealistic.physics.SurfaceProperties;
import dev.kanorto.ibrealistic.physics.VehicleConfig;
import dev.kanorto.ibrealistic.physics.VehicleType;
import dev.kanorto.ibrealistic.physics.WeatherCondition;
import dev.kanorto.ibrealistic.physics.TirePreset;
import dev.kanorto.ibrealistic.physics.SuspensionPreset;
import dev.kanorto.ibrealistic.physics.EnginePreset;
import dev.kanorto.ibrealistic.physics.BodyPreset;
import dev.kanorto.ibrealistic.physics.SteeringPreset;
import dev.kanorto.ibrealistic.physics.BrakePreset;
import dev.kanorto.ibrealistic.physics.WeightDistributionPreset;

import dev.o7moon.openboatutils.OpenBoatUtils;

/**
 * IBRealistic — addon to OpenBoatUtils (OBU) providing realistic four-wheel physics.
 * <p>
 * Architecture: OBU handles base boat settings (packets 0-32) on {@code openboatutils:settings}.
 * IBRealistic handles realistic physics settings (packets 33-69) on {@code ibrealistic:settings}.
 * This ensures compatibility: players with only OBU work on any OBU server,
 * and IBRealistic features activate only on servers that support them.
 */
public class IBRealistic implements ModInitializer {

    @Override
    public void onInitialize() {
        ClientboundPackets.registerCodecs();
        ServerboundPackets.registerCodecs();

        ServerboundPackets.registerHandlers();

        SingleplayerCommands.registerCommands();
    }

    /**
     * Full reset of all state — OBU-base fields (via OBU) + IBRealistic fields.
     * Used ONLY in singleplayer mode (where packets go through local channels).
     */
    public static void resetAll(){
        OpenBoatUtils.resetAll();
        resetRealisticState();
        resetServerInfo();
    }

    public static final Logger LOG = LoggerFactory.getLogger("IBRealistic");

    public static final int VERSION = 18;

    public static final Identifier settingsChannel = Identifier.of("ibrealistic","settings");

    // ─── BUILD HASH ───
    /** Dynamic integrity hash computed from the mod's own JAR file at runtime */
    public static final String BUILD_HASH = computeJarHash();

    /**
     * Computes SHA-256 hash of the mod's own JAR file at runtime.
     * This hash changes if anyone modifies the JAR, making it impossible to
     * copy a valid hash into a tampered build.
     */
    private static String computeJarHash() {
        try {
            var modContainer = FabricLoader.getInstance().getModContainer("ibrealistic");
            if (modContainer.isEmpty()) {
                LOG.warn("Cannot compute build hash: mod container not found");
                return "unknown";
            }
            var paths = modContainer.get().getOrigin().getPaths();
            if (paths.isEmpty()) {
                LOG.warn("Cannot compute build hash: no mod paths found");
                return "unknown";
            }
            java.nio.file.Path jarPath = paths.get(0);
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            try (java.io.InputStream is = java.nio.file.Files.newInputStream(jarPath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            LOG.warn("Failed to compute build hash: {}", e.getMessage());
            return "unknown";
        }
    }

    // ─── REALISTIC PHYSICS ENGINE ───
    /** Four-wheel physics engine — replaces old bicycle model */
    public static FourWheelPhysicsEngine fourWheelPhysics = new FourWheelPhysicsEngine();

    /** Debug HUD toggle for realistic physics diagnostics */
    public static volatile boolean realisticDebugHud = false;

    /**
     * Full reset of OBU settings + IBRealistic state.
     * Used in singleplayer mode where there's no separate OBU server channel.
     */
    public static void resetSettings(){
        OpenBoatUtils.resetSettings();
        resetRealisticState();
    }

    /**
     * Reset ONLY IBRealistic-specific state (physics engine, surfaces, visual state, countdown).
     * Called when IBRealistic receives RESET on ibrealistic:settings channel in multiplayer.
     * Does NOT touch OBU-base fields (enabled, stepSize, gravity, slipperiness, etc.).
     */
    public static void resetRealisticState() {
        fourWheelPhysics = new FourWheelPhysicsEngine();
        SurfaceProperties.resetBlockSurfaceMap();
        visualRollAngle = 0f;
        visualSteeringAngle = 0f;
        visualHandbrake = false;
        countdownActive = false;
        countdownGoTimeMs = 0;
        countdownSeconds = 0;
        realisticDebugHud = false;
    }

    // ─── VERSION / PACKET COMMUNICATION ───

    public static void sendVersionPacket(){
        PacketByteBuf packet = PacketByteBufs.create();
        packet.writeShort(ServerboundPackets.VERSION.ordinal());
        packet.writeInt(VERSION);
        packet.writeBoolean(true); // realistic mod identifier
        packet.writeString(BUILD_HASH); // build integrity hash
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

    // ─── REALISTIC PHYSICS METHODS ───

    public static void setRealisticPhysicsEnabled(boolean value) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.setEnabled(value);
    }

    public static void setVehicleType(VehicleType type) {
        OpenBoatUtils.enabled = true;
        VehicleConfig config = type.toConfig();
        fourWheelPhysics.setConfig(config);
        fourWheelPhysics.setEnabled(true);
    }

    public static void setVehicleConfig(VehicleConfig config) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.setConfig(config);
        fourWheelPhysics.setEnabled(true);
    }

    public static void setVehicleMass(float mass) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().mass = mass;
    }

    public static void setVehicleWheelbase(float wheelbase) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().wheelbase = wheelbase;
    }

    public static void setVehicleCgHeight(float cgHeight) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().cgHeight = cgHeight;
    }

    public static void setVehicleTrackWidth(float trackWidth) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().trackWidth = trackWidth;
    }

    public static void setVehicleMaxSteering(float maxSteering) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().maxSteeringAngle = maxSteering;
    }

    public static void setVehicleSteeringSpeed(float steeringSpeed) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().steeringSpeed = steeringSpeed;
    }

    public static void setVehicleBrakingForce(float brakingForce) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().brakingForce = brakingForce;
    }

    public static void setVehicleEngineForce(float engineForce) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().engineForce = engineForce;
    }

    public static void setVehicleDragCoefficient(float drag) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().dragCoefficient = drag;
    }

    public static void setVehicleBrakeBias(float brakeBias) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().brakeBias = brakeBias;
    }

    public static void setVehicleSubsteps(int substeps) {
        OpenBoatUtils.enabled = true;
        int clamped = Math.max(1, Math.min(10, substeps));
        fourWheelPhysics.getConfig().substeps = clamped;
    }

    public static void setVehicleFrontWeightBias(float bias) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().frontWeightBias = bias;
    }

    public static void setBlockSurfaceType(String blockId, String surfaceType) {
        OpenBoatUtils.enabled = true;
        SurfaceProperties surface = SurfaceProperties.getSurfaceByName(surfaceType);
        SurfaceProperties.setBlockSurface(blockId, surface);
    }

    public static void setVehicleDrivetrain(short drivetrainId) {
        OpenBoatUtils.enabled = true;
        dev.kanorto.ibrealistic.physics.DrivetrainType dt = dev.kanorto.ibrealistic.physics.DrivetrainType.fromId(drivetrainId);
        fourWheelPhysics.getConfig().drivetrain = dt;
    }

    public static void setDefaultSurfaceType(String surfaceName) {
        OpenBoatUtils.enabled = true;
        SurfaceProperties.setDefaultSurfaceByName(surfaceName);
    }

    public static void setVehicleSpeedSteeringFactor(float factor) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().speedSteeringFactor = factor;
    }

    public static void setVehicleEngineBraking(float braking) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().engineBraking = braking;
    }

    public static void setVehicleRollStiffnessRatio(float ratio) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().rollStiffnessRatioFront = ratio;
    }

    public static void resetRealisticPhysics() {
        fourWheelPhysics = new FourWheelPhysicsEngine();
        SurfaceProperties.resetBlockSurfaceMap();
    }

    // ─── NEW FOUR-WHEEL SPECIFIC METHODS ───

    public static void setAwdFrontSplit(float split) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().awdFrontSplit = Math.max(0.0f, Math.min(1.0f, split));
    }

    public static void setFrontDifferential(short diffId) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().frontDifferential = DifferentialType.fromId(diffId);
    }

    public static void setRearDifferential(short diffId) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().rearDifferential = DifferentialType.fromId(diffId);
    }

    public static void setLsdLockingCoeff(float coeff) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().lsdLockingCoeff = Math.max(0.0f, Math.min(1.0f, coeff));
    }

    public static void setDownforceCoefficient(float coeff) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().downforceCoefficient = Math.max(0.0f, coeff);
    }

    public static void setDownforceFrontBias(float bias) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().downforceFrontBias = Math.max(0.0f, Math.min(1.0f, bias));
    }

    public static void setWeatherCondition(short weatherId) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.setWeather(WeatherCondition.fromId(weatherId));
    }

    public static void setSteeringReturnRate(float rate) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().steeringReturnRate = Math.max(0.0f, rate);
    }

    // ─── COMPONENT PRESETS ───
    public static void setTirePreset(short presetId) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().tirePreset = TirePreset.fromId(presetId);
    }

    public static void setSuspensionPreset(short presetId) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().suspensionPreset = SuspensionPreset.fromId(presetId);
    }

    public static void setEnginePreset(short presetId) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().enginePreset = EnginePreset.fromId(presetId);
    }

    public static void setBodyPreset(short presetId) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().bodyPreset = BodyPreset.fromId(presetId);
    }

    public static void setSteeringPreset(short presetId) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().steeringPreset = SteeringPreset.fromId(presetId);
    }

    public static void setBrakePreset(short presetId) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().brakePreset = BrakePreset.fromId(presetId);
    }

    public static void setWeightDistributionPreset(short presetId) {
        OpenBoatUtils.enabled = true;
        fourWheelPhysics.getConfig().weightDistributionPreset = WeightDistributionPreset.fromId(presetId);
    }

    // ─── VISUAL STATE (for renderer access) ───
    /** Current visual roll angle in degrees (set each tick by BoatMixin, read by render thread) */
    public static volatile float visualRollAngle = 0f;
    /** Current visual steering angle in radians (set each tick by BoatMixin, read by render thread) */
    public static volatile float visualSteeringAngle = 0f;
    /** Whether the handbrake is currently engaged (set each tick by BoatMixin, read by render thread) */
    public static volatile boolean visualHandbrake = false;

    // ─── RACE COUNTDOWN STATE ───
    /** Absolute system time (ms) when GO should happen, or 0 if no countdown active */
    public static volatile long countdownGoTimeMs = 0;
    /** Number of countdown seconds (e.g. 5 for 5..4..3..2..1..GO) */
    public static volatile int countdownSeconds = 0;
    /** Whether countdown is currently active */
    public static volatile boolean countdownActive = false;

    /**
     * Sets up a client-side synchronized race countdown.
     * Called when the server sends SET_RACE_COUNTDOWN packet.
     *
     * @param goTimeMs absolute System.currentTimeMillis() when GO should happen
     * @param seconds number of countdown seconds
     */
    public static void setRaceCountdown(long goTimeMs, int seconds) {
        if (goTimeMs == 0) {
            // Cancel countdown
            countdownActive = false;
            countdownGoTimeMs = 0;
            countdownSeconds = 0;
        } else {
            countdownGoTimeMs = goTimeMs;
            countdownSeconds = seconds;
            countdownActive = true;
        }
    }

    /**
     * Gets the remaining seconds until GO, or -1 if no countdown active.
     * Returns 0 when it's time for GO.
     */
    public static int getCountdownRemaining() {
        if (!countdownActive) return -1;
        long now = System.currentTimeMillis();
        long remaining = countdownGoTimeMs - now;
        if (remaining <= 0) return 0;
        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * Checks if the countdown just hit GO (remaining <= 0 and still active).
     */
    public static boolean isCountdownGo() {
        if (!countdownActive) return false;
        return System.currentTimeMillis() >= countdownGoTimeMs;
    }

    // ─── SERVER VERSION INFO ───
    /** Realistic version reported by the connected server (null if not a realistic server) */
    public static volatile String serverRealisticVersion = null;
    /** Feature flags bitfield reported by the server */
    public static volatile int serverFeatures = 0;
    /** Server name reported by the server */
    public static volatile String serverName = null;

    /**
     * Returns the realistic version of this client mod, read from fabric.mod.json at runtime.
     * The version format is "{obu_version}-{realistic_version}_{mc_suffix}".
     * This method extracts the realistic_version part (e.g. "1.0.6" from "0.4.10-1.0.6_1.20.4").
     * <p>
     * Fallback behavior:
     * - If mod container not found: returns "unknown"
     * - If version has no dash: returns the full version string
     * - If version has no underscore after dash: returns everything after the dash
     */
    public static String getClientRealisticVersion() {
        String fullVersion = FabricLoader.getInstance()
                .getModContainer("ibrealistic")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        // Format: {obu_version}-{realistic_version}_{mc_suffix}
        int dashIdx = fullVersion.indexOf('-');
        if (dashIdx < 0) return fullVersion;
        String afterDash = fullVersion.substring(dashIdx + 1);
        int underscoreIdx = afterDash.indexOf('_');
        if (underscoreIdx < 0) return afterDash;
        return afterDash.substring(0, underscoreIdx);
    }

    /**
     * Sends REALISTIC_CLIENT_INFO packet to the server.
     * Called automatically after receiving REALISTIC_SERVER_INFO.
     */
    public static void sendRealisticClientInfoPacket() {
        PacketByteBuf packet = PacketByteBufs.create();
        packet.writeShort(ServerboundPackets.REALISTIC_CLIENT_INFO.ordinal());
        packet.writeString(getClientRealisticVersion());
        packet.writeInt(RealisticFeature.allClientFeatures());
        sendPacketC2S(packet);
    }

    /**
     * Resets server version info (called when disconnecting/reconnecting).
     */
    public static void resetServerInfo() {
        serverRealisticVersion = null;
        serverFeatures = 0;
        serverName = null;
    }
}
