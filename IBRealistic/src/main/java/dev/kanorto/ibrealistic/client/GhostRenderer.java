package dev.kanorto.ibrealistic.client;

import dev.kanorto.ibrealistic.ghost.GhostDataManager;
import dev.kanorto.ibrealistic.ghost.GhostDisplayMode;
import dev.kanorto.ibrealistic.ghost.GhostFrame;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Map;

/**
 * Renders ghost entities on the client.
 * Supports LINE (trail), BOAT (semi-transparent boat shape), and COMPETITION modes.
 * <p>
 * Ghost color scheme:
 * - Personal best (index 0): Blue (0.2, 0.5, 1.0)
 * - Competition ghosts (index 1+): colors cycle through a palette
 */
public class GhostRenderer {

    // ─── COLORS ───
    private static final float[][] GHOST_COLORS = {
            {0.2f, 0.5f, 1.0f},    // Blue — personal best
            {1.0f, 0.3f, 0.3f},    // Red
            {0.3f, 1.0f, 0.3f},    // Green
            {1.0f, 0.8f, 0.2f},    // Yellow
            {0.8f, 0.3f, 1.0f},    // Purple
            {0.3f, 1.0f, 0.8f},    // Cyan
    };

    /** Ghost transparency */
    private static final float GHOST_ALPHA = 0.4f;

    /** Line trail: number of past ticks to draw */
    private static final int LINE_TRAIL_LENGTH = 60; // 3 seconds

    /** Line trail height offset above ground */
    private static final float LINE_HEIGHT_OFFSET = 0.15f;

    // ─── RENDER ───

    /**
     * Render all active ghosts.
     * Called from WorldRenderEvents.LAST.
     */
    public static void render(MatrixStack matrices, Camera camera, float tickDelta) {
        if (!GhostDataManager.isPlaying() || !GhostDataManager.hasGhosts()) {
            return;
        }

        int currentTick = GhostDataManager.getCurrentTick();
        Vec3d cameraPos = camera.getPos();

        for (Map.Entry<Integer, GhostDataManager.GhostPlayback> entry :
                GhostDataManager.getActiveGhosts().entrySet()) {
            int ghostIndex = entry.getKey();
            GhostDataManager.GhostPlayback ghost = entry.getValue();

            if (currentTick >= ghost.frames.size()) continue;

            float[] color = getGhostColor(ghostIndex);
            GhostDisplayMode mode = ghost.displayMode;

            if (mode == GhostDisplayMode.LINE) {
                renderLine(matrices, cameraPos, ghost, currentTick, color);
            } else if (mode == GhostDisplayMode.BOAT || mode == GhostDisplayMode.COMPETITION) {
                renderBoat(matrices, cameraPos, ghost, currentTick, tickDelta, color);
            }
        }
    }

    /**
     * Render ghost as a line trail on the ground.
     */
    private static void renderLine(MatrixStack matrices, Vec3d cameraPos,
                                   GhostDataManager.GhostPlayback ghost, int currentTick,
                                   float[] color) {
        int startTick = Math.max(0, currentTick - LINE_TRAIL_LENGTH);
        if (startTick >= currentTick) return;

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        Tessellator tessellator = Tessellator.getInstance();
        //? <=1.20.4 {
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        //?} else {
        /*BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        *///?}

        for (int t = startTick; t <= currentTick && t < ghost.frames.size(); t++) {
            GhostFrame frame = ghost.frames.get(t);
            float alpha = GHOST_ALPHA * ((float) (t - startTick) / (currentTick - startTick + 1));
            //? <=1.20.4 {
            buffer.vertex(frame.posX, frame.posY + LINE_HEIGHT_OFFSET, frame.posZ)
                    .color(color[0], color[1], color[2], alpha)
                    .next();
            //?} else {
            /*buffer.vertex(frame.posX, frame.posY + LINE_HEIGHT_OFFSET, frame.posZ)
                    .color(color[0], color[1], color[2], alpha);
            *///?}
        }

        //? <=1.20.4 {
        tessellator.draw();
        //?} else {
        /*BufferRenderer.drawWithGlobalProgram(buffer.end());
        *///?}
        matrices.pop();
    }

    /**
     * Render ghost as a semi-transparent boat marker (diamond shape).
     */
    private static void renderBoat(MatrixStack matrices, Vec3d cameraPos,
                                   GhostDataManager.GhostPlayback ghost, int currentTick,
                                   float tickDelta, float[] color) {
        GhostFrame frame = ghost.getInterpolatedFrame(currentTick, tickDelta);
        if (frame == null) return;

        float ghostX = frame.posX;
        float ghostY = frame.posY;
        float ghostZ = frame.posZ;
        float yaw = frame.yawAngle;

        matrices.push();
        matrices.translate(ghostX - cameraPos.x, ghostY - cameraPos.y + 0.5, ghostZ - cameraPos.z);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotation(-yaw));

        // Draw a diamond-shaped marker
        Tessellator tessellator = Tessellator.getInstance();
        //? <=1.20.4 {
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        //?} else {
        /*BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        *///?}

        float hw = 0.4f; // half-width
        float hl = 0.8f; // half-length
        float hh = 0.3f; // half-height

        // Top triangle strip forming a boat-like diamond shape
        //? <=1.20.4 {
        buffer.vertex(matrices.peek().getPositionMatrix(), 0, hh, -hl).color(color[0], color[1], color[2], GHOST_ALPHA).next();
        buffer.vertex(matrices.peek().getPositionMatrix(), -hw, 0, 0).color(color[0], color[1], color[2], GHOST_ALPHA).next();
        buffer.vertex(matrices.peek().getPositionMatrix(), hw, 0, 0).color(color[0], color[1], color[2], GHOST_ALPHA).next();
        buffer.vertex(matrices.peek().getPositionMatrix(), 0, hh, hl).color(color[0], color[1], color[2], GHOST_ALPHA).next();
        buffer.vertex(matrices.peek().getPositionMatrix(), -hw, 0, 0).color(color[0], color[1], color[2], GHOST_ALPHA).next();
        buffer.vertex(matrices.peek().getPositionMatrix(), 0, -hh, 0).color(color[0], color[1], color[2], GHOST_ALPHA * 0.6f).next();
        buffer.vertex(matrices.peek().getPositionMatrix(), hw, 0, 0).color(color[0], color[1], color[2], GHOST_ALPHA * 0.6f).next();
        //?} else {
        /*buffer.vertex(matrices.peek().getPositionMatrix(), 0, hh, -hl).color(color[0], color[1], color[2], GHOST_ALPHA);
        buffer.vertex(matrices.peek().getPositionMatrix(), -hw, 0, 0).color(color[0], color[1], color[2], GHOST_ALPHA);
        buffer.vertex(matrices.peek().getPositionMatrix(), hw, 0, 0).color(color[0], color[1], color[2], GHOST_ALPHA);
        buffer.vertex(matrices.peek().getPositionMatrix(), 0, hh, hl).color(color[0], color[1], color[2], GHOST_ALPHA);
        buffer.vertex(matrices.peek().getPositionMatrix(), -hw, 0, 0).color(color[0], color[1], color[2], GHOST_ALPHA);
        buffer.vertex(matrices.peek().getPositionMatrix(), 0, -hh, 0).color(color[0], color[1], color[2], GHOST_ALPHA * 0.6f);
        buffer.vertex(matrices.peek().getPositionMatrix(), hw, 0, 0).color(color[0], color[1], color[2], GHOST_ALPHA * 0.6f);
        *///?}

        //? <=1.20.4 {
        tessellator.draw();
        //?} else {
        /*BufferRenderer.drawWithGlobalProgram(buffer.end());
        *///?}
        matrices.pop();
    }

    /**
     * Render delta time overlay on HUD showing time difference to ghost.
     * Called from HudRenderCallback.
     */
    public static void renderOverlay(net.minecraft.client.gui.DrawContext drawContext) {
        if (!GhostDataManager.isPlaying() || !GhostDataManager.hasGhosts()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.getVehicle() == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int y = 10;

        for (Map.Entry<Integer, GhostDataManager.GhostPlayback> entry :
                GhostDataManager.getActiveGhosts().entrySet()) {
            GhostDataManager.GhostPlayback ghost = entry.getValue();
            int tick = GhostDataManager.getCurrentTick();
            if (tick >= ghost.frames.size()) continue;

            GhostFrame ghostFrame = ghost.getFrame(tick);
            if (ghostFrame == null) continue;

            // Calculate distance to ghost
            double dx = client.player.getVehicle().getX() - ghostFrame.posX;
            double dz = client.player.getVehicle().getZ() - ghostFrame.posZ;
            double dist = Math.sqrt(dx * dx + dz * dz);

            // Estimate delta time (based on distance and ghost speed)
            float ghostSpeed = Math.max(ghostFrame.speedKmh / 3.6f, 0.1f); // m/s
            float deltaSeconds = Math.min((float) (dist / ghostSpeed), 99.9f);

            // Determine if ahead or behind by comparing with ghost position's progress
            boolean playerAhead = isPlayerAheadOfGhost(client, ghost, tick);

            float[] color = getGhostColor(entry.getKey());
            int textColor;

            String deltaText;
            if (playerAhead) {
                deltaText = String.format("-%.1fs", deltaSeconds);
                textColor = 0xFF44FF44; // Green — ahead
            } else {
                deltaText = String.format("+%.1fs", deltaSeconds);
                textColor = 0xFFFF4444; // Red — behind
            }

            String speedText = String.format("Ghost: %.0f km/h", ghostFrame.speedKmh);

            int textWidth = client.textRenderer.getWidth(deltaText);
            drawContext.drawTextWithShadow(client.textRenderer, deltaText,
                    screenWidth / 2 - textWidth / 2, y, textColor);
            y += 12;
            int speedWidth = client.textRenderer.getWidth(speedText);
            drawContext.drawTextWithShadow(client.textRenderer, speedText,
                    screenWidth / 2 - speedWidth / 2, y, 0xFFCCCCCC);
            y += 16;
        }
    }

    /**
     * Simple heuristic to determine if the player is ahead of the ghost.
     * Compares player position to ghost's future vs past positions.
     */
    private static boolean isPlayerAheadOfGhost(MinecraftClient client,
                                                GhostDataManager.GhostPlayback ghost, int tick) {
        if (client.player == null || client.player.getVehicle() == null) return false;

        double playerX = client.player.getVehicle().getX();
        double playerZ = client.player.getVehicle().getZ();

        GhostFrame current = ghost.getFrame(tick);
        if (current == null) return false;

        // Compare distance to ghost's current position with a point 20 ticks ahead
        int futureTick = Math.min(tick + 20, ghost.frames.size() - 1);
        GhostFrame future = ghost.getFrame(futureTick);
        if (future == null) return false;

        double distToCurrent = distSq(playerX, playerZ, current.posX, current.posZ);
        double distToFuture = distSq(playerX, playerZ, future.posX, future.posZ);

        return distToFuture < distToCurrent;
    }

    private static double distSq(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return dx * dx + dz * dz;
    }

    private static float[] getGhostColor(int ghostIndex) {
        return GHOST_COLORS[ghostIndex % GHOST_COLORS.length];
    }

    // ─── RESET ───

    public static void reset() {
        // No persistent state to reset — GhostDataManager handles data
    }
}
