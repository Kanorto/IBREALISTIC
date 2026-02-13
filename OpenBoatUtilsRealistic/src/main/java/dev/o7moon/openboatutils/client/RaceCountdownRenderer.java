package dev.o7moon.openboatutils.client;

import dev.o7moon.openboatutils.OpenBoatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RotationAxis;

/**
 * Client-side countdown renderer for synchronized race start.
 * Renders a colored block 2 blocks in front of the player with particles.
 * Colors: red (5-4), yellow (3-2-1), green (GO).
 * All clients see GO at the same wall-clock instant.
 */
public class RaceCountdownRenderer {

    private static final float BLOCK_SCALE = 0.5f;
    private static final float BLOCK_DISTANCE = 2.0f;
    private static final float BLOCK_HEIGHT_OFFSET = 1.5f;

    private static int lastTickedSecond = -1;
    private static boolean goTriggered = false;
    private static long goDisplayEndMs = 0;
    private static final long GO_DISPLAY_DURATION_MS = 1500;

    /**
     * Called each client tick to update countdown state and play sounds.
     */
    public static void tick() {
        if (!OpenBoatUtils.countdownActive) {
            if (goTriggered && System.currentTimeMillis() > goDisplayEndMs) {
                goTriggered = false;
                lastTickedSecond = -1;
            }
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int remaining = OpenBoatUtils.getCountdownRemaining();

        if (remaining <= 0 && !goTriggered) {
            // GO!
            goTriggered = true;
            goDisplayEndMs = System.currentTimeMillis() + GO_DISPLAY_DURATION_MS;
            OpenBoatUtils.countdownActive = false;
            lastTickedSecond = 0;

            // Play GO sound
            client.player.playSound(
                    net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                    1.0f, 2.0f
            );
            return;
        }

        // Play tick sound for each new second
        if (remaining > 0 && remaining != lastTickedSecond && remaining <= OpenBoatUtils.countdownSeconds) {
            lastTickedSecond = remaining;
            client.player.playSound(
                    net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(),
                    1.0f, 1.0f
            );
        }
    }

    /**
     * Called from WorldRenderEvents to render the countdown block and particles.
     */
    public static void render(MatrixStack matrices, Camera camera, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        int displayNumber = getDisplayNumber();
        if (displayNumber < 0) return;

        // Get block state based on countdown phase
        BlockState blockState = getCountdownBlock(displayNumber);

        // Calculate position: 1 block in front of player's face
        Vec3d playerPos = client.player.getCameraPosVec(tickDelta);
        Vec3d lookDir = client.player.getRotationVec(tickDelta);
        Vec3d blockPos = playerPos.add(lookDir.multiply(BLOCK_DISTANCE));
        blockPos = blockPos.add(0, BLOCK_HEIGHT_OFFSET - 1.5, 0);

        // Camera offset
        Vec3d camPos = camera.getPos();
        double dx = blockPos.x - camPos.x;
        double dy = blockPos.y - camPos.y;
        double dz = blockPos.z - camPos.z;

        matrices.push();
        matrices.translate(dx, dy, dz);

        // Rotate to face the player
        float yaw = client.player.getYaw(tickDelta);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));

        // Scale
        matrices.scale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE);
        matrices.translate(-0.5, -0.5, -0.5);

        // Render the block
        BlockRenderManager blockRenderer = client.getBlockRenderManager();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        blockRenderer.renderBlockAsEntity(blockState, matrices, immediate, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        immediate.draw();

        matrices.pop();

        // Spawn particles around the block
        spawnCountdownParticles(client, blockPos, displayNumber);
    }

    /**
     * Gets the current number to display, or -1 if nothing to show.
     */
    private static int getDisplayNumber() {
        if (goTriggered && System.currentTimeMillis() <= goDisplayEndMs) {
            return 0; // 0 means GO
        }
        if (!OpenBoatUtils.countdownActive) return -1;

        int remaining = OpenBoatUtils.getCountdownRemaining();
        if (remaining > OpenBoatUtils.countdownSeconds) return -1;
        return remaining;
    }

    /**
     * Returns the colored block based on countdown phase.
     * 5-4: Red (RED_CONCRETE)
     * 3-2-1: Yellow (YELLOW_CONCRETE)
     * GO (0): Green (LIME_CONCRETE)
     */
    private static BlockState getCountdownBlock(int remaining) {
        if (remaining == 0) {
            return Blocks.LIME_CONCRETE.getDefaultState();
        } else if (remaining >= 4) {
            return Blocks.RED_CONCRETE.getDefaultState();
        } else {
            return Blocks.YELLOW_CONCRETE.getDefaultState();
        }
    }

    /**
     * Spawns particles around the countdown block position.
     */
    private static void spawnCountdownParticles(MinecraftClient client, Vec3d pos, int remaining) {
        if (client.world == null) return;

        // Only spawn particles every few ticks to avoid overload
        if (client.world.getTime() % 2 != 0) return;

        double spread = 0.4;
        for (int i = 0; i < 3; i++) {
            double ox = (Math.random() - 0.5) * spread;
            double oy = (Math.random() - 0.5) * spread;
            double oz = (Math.random() - 0.5) * spread;

            if (remaining == 0) {
                // Green sparkles for GO
                client.world.addParticle(ParticleTypes.HAPPY_VILLAGER,
                        pos.x + ox, pos.y + oy, pos.z + oz,
                        0, 0.02, 0);
            } else if (remaining >= 4) {
                // Red flame for countdown
                client.world.addParticle(ParticleTypes.FLAME,
                        pos.x + ox, pos.y + oy, pos.z + oz,
                        0, 0.01, 0);
            } else {
                // Yellow dust for countdown
                client.world.addParticle(ParticleTypes.END_ROD,
                        pos.x + ox, pos.y + oy, pos.z + oz,
                        0, 0.01, 0);
            }
        }
    }

    /**
     * Resets renderer state (called when disconnecting).
     */
    public static void reset() {
        lastTickedSecond = -1;
        goTriggered = false;
        goDisplayEndMs = 0;
    }
}
