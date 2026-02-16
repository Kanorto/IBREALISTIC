package dev.kanorto.ibrealistic.client;

import dev.kanorto.ibrealistic.IBRealistic;
import dev.kanorto.ibrealistic.physics.FourWheelPhysicsEngine;
import dev.kanorto.ibrealistic.physics.SurfaceProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Client-side particle effects for vehicle driving:
 * - Surface-dependent tire particles (dust, dirt, gravel, water spray)
 * - Tire smoke on handbrake/lock on asphalt
 * - Speed-based particle intensity
 */
public class VehicleParticleRenderer {

    // ─── PARTICLE RATES ───
    private static final int SURFACE_PARTICLE_INTERVAL = 2;
    private static final int DRIFT_SMOKE_INTERVAL = 1;
    private static final int WATER_SPRAY_INTERVAL = 3;

    // ─── SPEED THRESHOLDS ───
    private static final float MIN_SPEED_FOR_PARTICLES = 0.05f;
    private static final float MIN_SPEED_FOR_SPRAY = 0.15f;
    private static final float HIGH_SPEED_THRESHOLD = 0.35f;

    // ─── DRIFT DETECTION ───
    private static final float DRIFT_YAW_RATE_THRESHOLD = 0.3f;

    // ─── STATE ───
    private static int tickCounter = 0;
    private static final Random rand = new Random();

    /**
     * Called each client tick to spawn driving-related particles.
     */
    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Entity vehicle = client.player.getVehicle();
        if (vehicle == null) return;

        FourWheelPhysicsEngine physics = IBRealistic.fourWheelPhysics;
        if (!physics.isEnabled()) return;

        tickCounter++;
        Vec3d pos = vehicle.getPos();
        float speed = (float) vehicle.getVelocity().horizontalLength();

        if (speed < MIN_SPEED_FOR_PARTICLES) return;

        SurfaceProperties surface = physics.getCurrentSurface();
        boolean isDrifting = Math.abs(physics.getYawRate()) > DRIFT_YAW_RATE_THRESHOLD;
        boolean isHandbrake = IBRealistic.visualHandbrake;
        float yaw = vehicle.getYaw();

        // ─── SURFACE PARTICLES ───
        if (tickCounter % SURFACE_PARTICLE_INTERVAL == 0) {
            spawnSurfaceParticles(client, pos, yaw, speed, surface, isDrifting);
        }

        // ─── HANDBRAKE / DRIFT SMOKE (asphalt only) ───
        if ((isHandbrake || isDrifting) && isAsphalt(surface) && tickCounter % DRIFT_SMOKE_INTERVAL == 0) {
            spawnTireSmoke(client, pos, yaw, speed);
        }

        // ─── WATER SPRAY (wet surfaces) ───
        if (isWetSurface(surface) && speed > MIN_SPEED_FOR_SPRAY && tickCounter % WATER_SPRAY_INTERVAL == 0) {
            spawnWaterSpray(client, pos, yaw, speed);
        }
    }

    // ─── SURFACE PARTICLES ───

    private static void spawnSurfaceParticles(MinecraftClient client, Vec3d pos, float yaw,
                                               float speed, SurfaceProperties surface, boolean isDrifting) {
        int count = getParticleCount(speed, isDrifting);

        if (surface == SurfaceProperties.GRAVEL || surface == SurfaceProperties.SAND) {
            spawnGravelDust(client, pos, yaw, count);
        } else if (surface == SurfaceProperties.DIRT || surface == SurfaceProperties.MUD) {
            spawnDirtKick(client, pos, yaw, count);
        } else if (surface == SurfaceProperties.SNOW) {
            spawnSnowPuff(client, pos, yaw, count);
        }
        // Asphalt dry/wet — no ground particles (handled by drift smoke / water spray)
    }

    private static void spawnGravelDust(MinecraftClient client, Vec3d pos, float yaw, int count) {
        float radYaw = (float) Math.toRadians(yaw);
        for (int i = 0; i < count; i++) {
            double ox = (rand.nextDouble() - 0.5) * 1.0 - Math.sin(radYaw) * 0.5;
            double oz = (rand.nextDouble() - 0.5) * 1.0 + Math.cos(radYaw) * 0.5;
            double vx = -Math.sin(radYaw) * 0.02 + (rand.nextDouble() - 0.5) * 0.03;
            double vz = Math.cos(radYaw) * 0.02 + (rand.nextDouble() - 0.5) * 0.03;
            client.world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.x + ox, pos.y + 0.1, pos.z + oz,
                    vx, 0.02, vz);
        }
    }

    private static void spawnDirtKick(MinecraftClient client, Vec3d pos, float yaw, int count) {
        float radYaw = (float) Math.toRadians(yaw);
        for (int i = 0; i < count; i++) {
            double ox = (rand.nextDouble() - 0.5) * 0.8 - Math.sin(radYaw) * 0.4;
            double oz = (rand.nextDouble() - 0.5) * 0.8 + Math.cos(radYaw) * 0.4;
            double vy = rand.nextDouble() * 0.04 + 0.01;
            client.world.addParticle(ParticleTypes.MYCELIUM,
                    pos.x + ox, pos.y + 0.15, pos.z + oz,
                    0, vy, 0);
        }
    }

    private static void spawnSnowPuff(MinecraftClient client, Vec3d pos, float yaw, int count) {
        float radYaw = (float) Math.toRadians(yaw);
        for (int i = 0; i < count; i++) {
            double ox = (rand.nextDouble() - 0.5) * 1.0 - Math.sin(radYaw) * 0.3;
            double oz = (rand.nextDouble() - 0.5) * 1.0 + Math.cos(radYaw) * 0.3;
            client.world.addParticle(ParticleTypes.SNOWFLAKE,
                    pos.x + ox, pos.y + 0.2, pos.z + oz,
                    (rand.nextDouble() - 0.5) * 0.02, 0.03, (rand.nextDouble() - 0.5) * 0.02);
        }
    }

    // ─── HANDBRAKE / DRIFT SMOKE ───

    private static void spawnTireSmoke(MinecraftClient client, Vec3d pos, float yaw, float speed) {
        float radYaw = (float) Math.toRadians(yaw);
        int count = speed > HIGH_SPEED_THRESHOLD ? 4 : 2;

        for (int i = 0; i < count; i++) {
            // Spawn behind the vehicle at rear wheel positions
            double rearOffset = 0.6;
            double sideOffset = (i % 2 == 0 ? 0.4 : -0.4);
            double ox = -Math.sin(radYaw) * -rearOffset + Math.cos(radYaw) * sideOffset;
            double oz = Math.cos(radYaw) * -rearOffset + Math.sin(radYaw) * sideOffset;
            client.world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.x + ox, pos.y + 0.05, pos.z + oz,
                    (rand.nextDouble() - 0.5) * 0.01, 0.02, (rand.nextDouble() - 0.5) * 0.01);
        }
    }

    // ─── WATER SPRAY ───

    private static void spawnWaterSpray(MinecraftClient client, Vec3d pos, float yaw, float speed) {
        float radYaw = (float) Math.toRadians(yaw);
        int count = speed > HIGH_SPEED_THRESHOLD ? 4 : 2;

        for (int i = 0; i < count; i++) {
            double ox = (rand.nextDouble() - 0.5) * 0.8 - Math.sin(radYaw) * 0.5;
            double oz = (rand.nextDouble() - 0.5) * 0.8 + Math.cos(radYaw) * 0.5;
            double vy = 0.04 + rand.nextDouble() * 0.03;
            client.world.addParticle(ParticleTypes.SPLASH,
                    pos.x + ox, pos.y + 0.1, pos.z + oz,
                    0, vy, 0);
        }
    }

    // ─── HELPERS ───

    private static int getParticleCount(float speed, boolean isDrifting) {
        int base = speed > HIGH_SPEED_THRESHOLD ? 3 : (speed > MIN_SPEED_FOR_SPRAY ? 2 : 1);
        return isDrifting ? base + 2 : base;
    }

    private static boolean isAsphalt(SurfaceProperties surface) {
        return surface == SurfaceProperties.ASPHALT_DRY || surface == SurfaceProperties.ASPHALT_WET;
    }

    private static boolean isWetSurface(SurfaceProperties surface) {
        return surface == SurfaceProperties.ASPHALT_WET || surface == SurfaceProperties.MUD;
    }

    public static void reset() {
        tickCounter = 0;
    }
}
