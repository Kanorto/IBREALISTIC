package dev.kanorto.ibrealistic.ghost;

import dev.kanorto.ibrealistic.IBRealistic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/**
 * Manages ghost data received from the server.
 * Handles receiving fragmented ghost data, decompression, and playback state.
 * <p>
 * All ghost data lives in memory — no local files.
 */
public class GhostDataManager {

    // ─── LIMITS ───

    /** Maximum number of concurrent ghost transfers */
    private static final int MAX_ACTIVE_TRANSFERS = 8;
    /** Maximum number of active ghosts in playback */
    private static final int MAX_ACTIVE_GHOSTS = 8;
    /** Maximum ghost data size (compressed bytes) */
    private static final int MAX_GHOST_BYTES = 2 * 1024 * 1024; // 2 MB
    /** Maximum chunks per ghost transfer */
    private static final int MAX_GHOST_CHUNKS = 512;

    // ─── TRANSFER STATE ───

    /** Active ghost transfers: ghostIndex → transfer session */
    private static final Map<Integer, GhostTransferSession> activeTransfers = new ConcurrentHashMap<>();

    /** Completed ghosts ready for playback: ghostIndex → ghost data */
    private static final Map<Integer, GhostPlayback> activeGhosts = new ConcurrentHashMap<>();

    // ─── PLAYBACK STATE ───

    /** Whether ghost playback is active (set to true when race starts) */
    private static volatile boolean playing = false;

    /** Current playback tick (incremented each game tick) */
    private static volatile int currentTick = 0;

    // ─── TRANSFER SESSION ───

    private static class GhostTransferSession {
        final int ghostIndex;
        final int expectedChunks;
        final int expectedBytes;
        final int totalTicks;
        final long finishTimeMs;
        final GhostDisplayMode displayMode;
        final Map<Integer, byte[]> receivedChunks = new ConcurrentHashMap<>();

        GhostTransferSession(int ghostIndex, int expectedChunks, int expectedBytes,
                             int totalTicks, long finishTimeMs, GhostDisplayMode displayMode) {
            this.ghostIndex = ghostIndex;
            this.expectedChunks = expectedChunks;
            this.expectedBytes = expectedBytes;
            this.totalTicks = totalTicks;
            this.finishTimeMs = finishTimeMs;
            this.displayMode = displayMode;
        }
    }

    // ─── PLAYBACK DATA ───

    /**
     * Represents a fully received ghost ready for playback.
     */
    public static class GhostPlayback {
        public final int ghostIndex;
        public final List<GhostFrame> frames;
        public final long finishTimeMs;
        public final GhostDisplayMode displayMode;

        GhostPlayback(int ghostIndex, List<GhostFrame> frames, long finishTimeMs,
                      GhostDisplayMode displayMode) {
            this.ghostIndex = ghostIndex;
            this.frames = frames;
            this.finishTimeMs = finishTimeMs;
            this.displayMode = displayMode;
        }

        public GhostFrame getFrame(int tick) {
            if (tick < 0 || tick >= frames.size()) return null;
            return frames.get(tick);
        }

        /**
         * Get interpolated frame between two ticks for smooth rendering.
         */
        public GhostFrame getInterpolatedFrame(int tick, float partialTick) {
            if (tick < 0 || tick >= frames.size()) return null;
            GhostFrame current = frames.get(tick);
            if (tick + 1 >= frames.size()) return current;
            GhostFrame next = frames.get(tick + 1);

            GhostFrame interp = new GhostFrame();
            interp.posX = lerp(current.posX, next.posX, partialTick);
            interp.posY = lerp(current.posY, next.posY, partialTick);
            interp.posZ = lerp(current.posZ, next.posZ, partialTick);
            interp.yawAngle = lerpAngle(current.yawAngle, next.yawAngle, partialTick);
            interp.steeringAngle = lerp(current.steeringAngle, next.steeringAngle, partialTick);
            interp.speedKmh = lerp(current.speedKmh, next.speedKmh, partialTick);
            return interp;
        }

        private static float lerp(float a, float b, float t) {
            return a + (b - a) * t;
        }

        private static float lerpAngle(float a, float b, float t) {
            float diff = b - a;
            // Normalize to [-PI, PI] in constant time
            diff = (float) (((diff + Math.PI) % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI) - Math.PI);
            return a + diff * t;
        }
    }

    // ─── PACKET HANDLERS ───

    /**
     * Handle GHOST_DATA_START packet (85).
     */
    public static void handleGhostStart(int ghostIndex, int totalChunks, int totalBytes,
                                        int totalTicks, long finishTimeMs, int displayModeId) {
        // Validate bounds to prevent memory exhaustion
        if (totalChunks <= 0 || totalChunks > MAX_GHOST_CHUNKS
                || totalBytes <= 0 || totalBytes > MAX_GHOST_BYTES
                || ghostIndex < 0 || ghostIndex > MAX_ACTIVE_GHOSTS) {
            IBRealistic.LOG.warn("Rejected ghost transfer: invalid parameters index={} chunks={} bytes={}",
                    ghostIndex, totalChunks, totalBytes);
            return;
        }
        if (activeTransfers.size() >= MAX_ACTIVE_TRANSFERS) {
            IBRealistic.LOG.warn("Rejected ghost transfer: too many active transfers");
            return;
        }
        GhostDisplayMode mode = GhostDisplayMode.fromId(displayModeId);
        activeTransfers.put(ghostIndex, new GhostTransferSession(
                ghostIndex, totalChunks, totalBytes, totalTicks, finishTimeMs, mode));
        IBRealistic.LOG.info("Ghost transfer started: index={} chunks={} ticks={} time={}ms mode={}",
                ghostIndex, totalChunks, totalTicks, finishTimeMs, mode);
    }

    /**
     * Handle GHOST_DATA_CHUNK packet (86).
     */
    public static void handleGhostChunk(int ghostIndex, int chunkIndex, byte[] chunkData) {
        GhostTransferSession session = activeTransfers.get(ghostIndex);
        if (session == null) {
            IBRealistic.LOG.warn("Received ghost chunk for unknown transfer: index={}", ghostIndex);
            return;
        }
        session.receivedChunks.put(chunkIndex, chunkData);
    }

    /**
     * Handle GHOST_DATA_END packet (87). Assemble and decompress ghost data.
     */
    public static void handleGhostEnd(int ghostIndex) {
        GhostTransferSession session = activeTransfers.remove(ghostIndex);
        if (session == null) {
            IBRealistic.LOG.warn("Received ghost end for unknown transfer: index={}", ghostIndex);
            return;
        }

        // Assemble chunks
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(session.expectedBytes);
            for (int i = 0; i < session.expectedChunks; i++) {
                byte[] chunk = session.receivedChunks.get(i);
                if (chunk == null) {
                    IBRealistic.LOG.warn("Missing ghost chunk {} for index={}", i, ghostIndex);
                    return;
                }
                baos.write(chunk);
            }

            byte[] compressed = baos.toByteArray();

            // Decompress
            byte[] decompressed = decompress(compressed);

            // Parse frames
            List<GhostFrame> frames = parseFrames(decompressed);
            if (frames.isEmpty()) {
                IBRealistic.LOG.warn("No frames parsed from ghost data: index={}", ghostIndex);
                return;
            }

            // Store as playback-ready ghost
            activeGhosts.put(ghostIndex, new GhostPlayback(
                    ghostIndex, frames, session.finishTimeMs, session.displayMode));

            IBRealistic.LOG.info("Ghost ready: index={} frames={} time={}ms mode={}",
                    ghostIndex, frames.size(), session.finishTimeMs, session.displayMode);
        } catch (IOException e) {
            IBRealistic.LOG.error("Failed to decompress ghost data: {}", e.getMessage());
        }
    }

    // ─── PLAYBACK CONTROL ───

    /**
     * Start ghost playback (called when race countdown hits GO).
     */
    public static void startPlayback() {
        currentTick = 0;
        playing = true;
    }

    /**
     * Advance playback by one tick.
     */
    public static void tick() {
        if (!playing) return;
        currentTick++;

        // Stop if all ghosts have finished
        boolean anyAlive = false;
        for (GhostPlayback ghost : activeGhosts.values()) {
            if (currentTick < ghost.frames.size()) {
                anyAlive = true;
                break;
            }
        }
        if (!anyAlive && !activeGhosts.isEmpty()) {
            // Keep playing flag true — ghosts just stop at their last position
        }
    }

    /**
     * Stop ghost playback (called when race ends or player exits vehicle).
     */
    public static void stopPlayback() {
        playing = false;
        currentTick = 0;
    }

    // ─── ACCESSORS ───

    public static boolean isPlaying() {
        return playing;
    }

    public static int getCurrentTick() {
        return currentTick;
    }

    public static Map<Integer, GhostPlayback> getActiveGhosts() {
        return activeGhosts;
    }

    public static boolean hasGhosts() {
        return !activeGhosts.isEmpty();
    }

    // ─── RESET ───

    /**
     * Clear all ghost data and reset state.
     * Called on disconnect, track change, or when resetting realistic state.
     */
    public static void reset() {
        activeTransfers.clear();
        activeGhosts.clear();
        playing = false;
        currentTick = 0;
    }

    // ─── PRIVATE HELPERS ───

    private static byte[] decompress(byte[] compressed) throws IOException {
        try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        }
    }

    private static List<GhostFrame> parseFrames(byte[] data) throws IOException {
        List<GhostFrame> frames = new ArrayList<>();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
        while (dis.available() >= GhostFrame.BYTES_PER_FRAME) {
            frames.add(GhostFrame.readFrom(dis));
        }
        return frames;
    }
}
