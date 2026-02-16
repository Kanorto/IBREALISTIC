package dev.kanorto.ibrealistic.client;

import dev.kanorto.ibrealistic.IBRealistic;
import dev.kanorto.ibrealistic.physics.DamageState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Client-side particle effects for vehicle damage:
 * - Smoke when body damage > 0.5
 * - Sparks on collision (velocity drop detection)
 * - Steam/shimmer when engine overheating
 */
public class DamageParticleRenderer {

    // ─── PARTICLE RATES ───
    private static final int SMOKE_INTERVAL_TICKS = 3;
    private static final int OVERHEAT_INTERVAL_TICKS = 5;

    // ─── COLLISION DETECTION ───
    private static final float SPARK_SPEED_LOSS_THRESHOLD = 0.25f;
    private static final int SPARKS_PER_COLLISION = 8;

    // ─── STATE ───
    private static int tickCounter = 0;
    private static float prevSpeed = 0f;
    private static final Random rand = new Random();

    /**
     * Called each client tick to spawn damage-related particles.
     */
    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Entity vehicle = client.player.getVehicle();
        if (vehicle == null) return;

        DamageState state = IBRealistic.damageState;
        if (!state.isDamageEnabled()) return;

        tickCounter++;
        Vec3d pos = vehicle.getPos();
        float currentSpeed = (float) vehicle.getVelocity().horizontalLength();

        // ─── COLLISION SPARKS ───
        if (prevSpeed > 0.1f) {
            float speedLoss = (prevSpeed - currentSpeed) / prevSpeed;
            if (speedLoss > SPARK_SPEED_LOSS_THRESHOLD && state.getBodyDamage() > 0.1f) {
                spawnSparks(client, pos);
            }
        }
        prevSpeed = currentSpeed;

        // ─── DAMAGE SMOKE ───
        if (state.shouldEmitSmoke() && tickCounter % SMOKE_INTERVAL_TICKS == 0) {
            spawnSmoke(client, pos, state.getBodyDamage());
        }

        // ─── ENGINE OVERHEAT SHIMMER ───
        if (state.isEngineOverheating() && tickCounter % OVERHEAT_INTERVAL_TICKS == 0) {
            spawnOverheatParticles(client, pos);
        }
    }

    private static void spawnSparks(MinecraftClient client, Vec3d pos) {
        for (int i = 0; i < SPARKS_PER_COLLISION; i++) {
            double ox = (rand.nextDouble() - 0.5) * 1.2;
            double oy = rand.nextDouble() * 0.3;
            double oz = (rand.nextDouble() - 0.5) * 1.2;
            double vx = (rand.nextDouble() - 0.5) * 0.15;
            double vy = rand.nextDouble() * 0.1;
            double vz = (rand.nextDouble() - 0.5) * 0.15;
            client.world.addParticle(ParticleTypes.CRIT,
                    pos.x + ox, pos.y + oy, pos.z + oz,
                    vx, vy, vz);
        }
    }

    private static void spawnSmoke(MinecraftClient client, Vec3d pos, float damage) {
        int count = damage > 0.8f ? 3 : (damage > 0.6f ? 2 : 1);
        for (int i = 0; i < count; i++) {
            double ox = (rand.nextDouble() - 0.5) * 0.5;
            double oz = (rand.nextDouble() - 0.5) * 0.5;
            client.world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.x + ox, pos.y + 0.8, pos.z + oz,
                    0, 0.03, 0);
        }
    }

    private static void spawnOverheatParticles(MinecraftClient client, Vec3d pos) {
        double ox = (rand.nextDouble() - 0.5) * 0.3;
        double oz = (rand.nextDouble() - 0.5) * 0.3;
        client.world.addParticle(ParticleTypes.SMOKE,
                pos.x + ox, pos.y + 0.5, pos.z + oz,
                0, 0.02, 0);
    }

    public static void reset() {
        tickCounter = 0;
        prevSpeed = 0f;
    }
}
