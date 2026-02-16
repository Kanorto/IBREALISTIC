package dev.kanorto.ibrealistic.client;

import dev.kanorto.ibrealistic.IBRealistic;
import dev.kanorto.ibrealistic.physics.FourWheelPhysicsEngine;
import dev.kanorto.ibrealistic.physics.SurfaceProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvents;

/**
 * Client-side vehicle sound effects:
 * - Engine hum (pitch varies with speed)
 * - Tire screech on drift/handbrake
 * - Gravel/loose surface crunch
 * - Impact sound on collision
 */
public class VehicleSoundRenderer {

    // ─── INTERVALS ───
    private static final int ENGINE_INTERVAL = 8;
    private static final int SCREECH_INTERVAL = 6;
    private static final int SURFACE_SOUND_INTERVAL = 10;
    private static final int COLLISION_COOLDOWN = 20;

    // ─── THRESHOLDS ───
    private static final float MIN_SPEED_FOR_ENGINE = 0.03f;
    private static final float MIN_SPEED_FOR_SURFACE = 0.08f;
    private static final float DRIFT_YAW_RATE_THRESHOLD = 0.35f;
    private static final float COLLISION_SPEED_LOSS = 0.3f;

    // ─── STATE ───
    private static int tickCounter = 0;
    private static float prevSpeed = 0f;
    private static int collisionCooldown = 0;

    /**
     * Called each client tick to play driving sounds.
     */
    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Entity vehicle = client.player.getVehicle();
        if (vehicle == null) return;

        FourWheelPhysicsEngine physics = IBRealistic.fourWheelPhysics;
        if (!physics.isEnabled()) return;

        tickCounter++;
        if (collisionCooldown > 0) collisionCooldown--;

        float speed = (float) vehicle.getVelocity().horizontalLength();
        SurfaceProperties surface = physics.getCurrentSurface();
        boolean isDrifting = Math.abs(physics.getYawRate()) > DRIFT_YAW_RATE_THRESHOLD;
        boolean isHandbrake = IBRealistic.visualHandbrake;

        // ─── COLLISION IMPACT ───
        if (prevSpeed > 0.15f && collisionCooldown == 0) {
            float speedLoss = (prevSpeed - speed) / prevSpeed;
            if (speedLoss > COLLISION_SPEED_LOSS) {
                playCollisionSound(client, speedLoss);
                collisionCooldown = COLLISION_COOLDOWN;
            }
        }
        prevSpeed = speed;

        if (speed < MIN_SPEED_FOR_ENGINE) return;

        // ─── ENGINE HUM ───
        if (tickCounter % ENGINE_INTERVAL == 0) {
            playEngineSound(client, speed);
        }

        // ─── TIRE SCREECH (drift/handbrake on asphalt) ───
        if ((isDrifting || isHandbrake) && isAsphalt(surface) && tickCounter % SCREECH_INTERVAL == 0) {
            playTireScreech(client, speed);
        }

        // ─── LOOSE SURFACE CRUNCH ───
        if (isLooseSurface(surface) && speed > MIN_SPEED_FOR_SURFACE && tickCounter % SURFACE_SOUND_INTERVAL == 0) {
            playSurfaceCrunch(client, speed, surface);
        }
    }

    private static void playEngineSound(MinecraftClient client, float speed) {
        float pitch = 0.5f + Math.min(speed * 3.0f, 1.5f);
        float volume = 0.15f + Math.min(speed * 0.5f, 0.35f);
        client.world.playSound(client.player, client.player.getBlockPos(),
                SoundEvents.ENTITY_MINECART_RIDING, net.minecraft.sound.SoundCategory.MASTER, volume, pitch);
    }

    private static void playTireScreech(MinecraftClient client, float speed) {
        float volume = 0.2f + Math.min(speed * 0.4f, 0.3f);
        float pitch = 0.6f + Math.min(speed * 1.5f, 0.8f);
        client.world.playSound(client.player, client.player.getBlockPos(),
                SoundEvents.BLOCK_GRINDSTONE_USE, net.minecraft.sound.SoundCategory.MASTER, volume, pitch);
    }

    private static void playSurfaceCrunch(MinecraftClient client, float speed, SurfaceProperties surface) {
        float volume = 0.1f + Math.min(speed * 0.3f, 0.2f);
        if (surface == SurfaceProperties.GRAVEL || surface == SurfaceProperties.SAND) {
            client.world.playSound(client.player, client.player.getBlockPos(),
                    SoundEvents.BLOCK_GRAVEL_STEP, net.minecraft.sound.SoundCategory.MASTER, volume, 0.8f);
        } else if (surface == SurfaceProperties.SNOW) {
            client.world.playSound(client.player, client.player.getBlockPos(),
                    SoundEvents.BLOCK_SNOW_STEP, net.minecraft.sound.SoundCategory.MASTER, volume, 0.9f);
        } else if (surface == SurfaceProperties.DIRT || surface == SurfaceProperties.MUD) {
            client.world.playSound(client.player, client.player.getBlockPos(),
                    SoundEvents.BLOCK_ROOTED_DIRT_STEP, net.minecraft.sound.SoundCategory.MASTER, volume, 0.7f);
        }
    }

    private static void playCollisionSound(MinecraftClient client, float impactForce) {
        float volume = 0.3f + Math.min(impactForce, 0.7f);
        float pitch = 0.5f + (1.0f - impactForce) * 0.5f;
        client.world.playSound(client.player, client.player.getBlockPos(),
                SoundEvents.ENTITY_IRON_GOLEM_HURT, net.minecraft.sound.SoundCategory.MASTER, volume, pitch);
    }

    // ─── HELPERS ───

    private static boolean isAsphalt(SurfaceProperties surface) {
        return surface == SurfaceProperties.ASPHALT_DRY || surface == SurfaceProperties.ASPHALT_WET;
    }

    private static boolean isLooseSurface(SurfaceProperties surface) {
        return surface == SurfaceProperties.GRAVEL || surface == SurfaceProperties.DIRT
                || surface == SurfaceProperties.MUD || surface == SurfaceProperties.SNOW
                || surface == SurfaceProperties.SAND;
    }

    public static void reset() {
        tickCounter = 0;
        prevSpeed = 0f;
        collisionCooldown = 0;
    }
}
