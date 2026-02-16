package me.makkuusen.timing.system.telemetry;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.boatutils.CustomBoatUtilsMode;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

/**
 * Handles receiving fragmented telemetry data from the client mod.
 * <p>
 * Protocol:
 * <ol>
 *   <li>Client sends TELEMETRY_START (packet 80): header, totalChunks, totalBytes</li>
 *   <li>Client sends N × TELEMETRY_CHUNK (packet 81): chunkIndex, chunkData[]</li>
 *   <li>Client sends TELEMETRY_END (packet 82): checksum</li>
 *   <li>Server responds with TELEMETRY_ACK (packet 83) and TELEMETRY_RESULT (packet 84)</li>
 * </ol>
 */
public class TelemetryReceiver {

    // ─── PACKET IDS (CLIENT → SERVER) ───

    public static final short PACKET_ID_TELEMETRY_START = 80;
    public static final short PACKET_ID_TELEMETRY_CHUNK = 81;
    public static final short PACKET_ID_TELEMETRY_END = 82;

    // ─── PACKET IDS (SERVER → CLIENT) ───

    public static final short PACKET_ID_TELEMETRY_ACK = 83;
    public static final short PACKET_ID_TELEMETRY_RESULT = 84;

    // ─── GHOST PACKET ID (CLIENT → SERVER) ───

    public static final short PACKET_ID_GHOST_REQUEST = 88;

    // ─── ACK STATUS CODES ───

    private static final byte ACK_OK = 0;
    private static final byte ACK_RETRY_CHUNK = 1;
    private static final byte ACK_REJECT = 2;

    // ─── RESULT STATUS CODES ───

    private static final byte RESULT_VALID = 0;
    private static final byte RESULT_INVALID = 1;

    // ─── CONFIGURATION ───

    /** Maximum time (ms) a transfer session can stay open before timeout */
    private static final long SESSION_TIMEOUT_MS = 30_000L;

    /** Maximum allowed total bytes for a single telemetry transfer */
    private static final int MAX_TELEMETRY_BYTES = 5 * 1024 * 1024; // 5 MB

    /** Maximum allowed chunks per transfer */
    private static final int MAX_CHUNKS = 1024;

    // ─── ACTIVE SESSIONS ───

    private static final Map<UUID, TelemetryTransferSession> activeSessions = new ConcurrentHashMap<>();

    // ─── TRANSFER SESSION ───

    /**
     * Represents an in-progress telemetry data transfer from a single player.
     */
    private static class TelemetryTransferSession {
        final UUID playerUuid;
        final int expectedChunks;
        final int expectedBytes;
        final int trackId;
        final int expectedTicks;
        final long startTime;
        final Map<Integer, byte[]> receivedChunks;

        TelemetryTransferSession(UUID playerUuid, int expectedChunks, int expectedBytes,
                                 int trackId, int expectedTicks) {
            this.playerUuid = playerUuid;
            this.expectedChunks = expectedChunks;
            this.expectedBytes = expectedBytes;
            this.trackId = trackId;
            this.expectedTicks = expectedTicks;
            this.startTime = System.currentTimeMillis();
            this.receivedChunks = new ConcurrentHashMap<>();
        }

        boolean isTimedOut() {
            return System.currentTimeMillis() - startTime > SESSION_TIMEOUT_MS;
        }

        boolean isComplete() {
            return receivedChunks.size() == expectedChunks;
        }
    }

    // ─── PACKET HANDLERS ───

    /**
     * Handles TELEMETRY_START packet (ID 80).
     * Starts a new transfer session for the player.
     *
     * @param player the sending player
     * @param in     data input positioned after the packet ID
     */
    public static void handleTelemetryStart(Player player, ByteArrayDataInput in) {
        try {
            int totalChunks = in.readInt();
            int totalBytes = in.readInt();
            int trackId = in.readInt();
            int expectedTicks = in.readInt();

            // Validate bounds
            if (totalChunks <= 0 || totalChunks > MAX_CHUNKS) {
                sendAck(player, ACK_REJECT);
                TimingSystem.getPlugin().getLogger().warning(
                        "Rejected telemetry from " + player.getName() + ": invalid chunk count " + totalChunks);
                return;
            }
            if (totalBytes <= 0 || totalBytes > MAX_TELEMETRY_BYTES) {
                sendAck(player, ACK_REJECT);
                TimingSystem.getPlugin().getLogger().warning(
                        "Rejected telemetry from " + player.getName() + ": invalid size " + totalBytes);
                return;
            }

            // Replace any existing session for this player
            UUID uuid = player.getUniqueId();
            activeSessions.put(uuid, new TelemetryTransferSession(
                    uuid, totalChunks, totalBytes, trackId, expectedTicks));

            sendAck(player, ACK_OK);
            TimingSystem.getPlugin().getLogger().info(
                    "Telemetry transfer started from " + player.getName()
                            + ": chunks=" + totalChunks + " bytes=" + totalBytes + " track=" + trackId);
        } catch (Exception e) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING,
                    "Failed to handle TELEMETRY_START from " + player.getName(), e);
            sendAck(player, ACK_REJECT);
        }
    }

    /**
     * Handles TELEMETRY_CHUNK packet (ID 81).
     * Stores a single chunk of telemetry data.
     *
     * @param player the sending player
     * @param in     data input positioned after the packet ID
     */
    public static void handleTelemetryChunk(Player player, ByteArrayDataInput in) {
        try {
            UUID uuid = player.getUniqueId();
            TelemetryTransferSession session = activeSessions.get(uuid);

            if (session == null) {
                sendAck(player, ACK_REJECT);
                return;
            }

            if (session.isTimedOut()) {
                activeSessions.remove(uuid);
                sendAck(player, ACK_REJECT);
                TimingSystem.getPlugin().getLogger().warning(
                        "Telemetry session timed out for " + player.getName());
                return;
            }

            int chunkIndex = in.readInt();
            int chunkLength = in.readInt();

            if (chunkIndex < 0 || chunkIndex >= session.expectedChunks) {
                sendAck(player, ACK_RETRY_CHUNK);
                return;
            }
            if (chunkLength <= 0 || chunkLength > MAX_TELEMETRY_BYTES) {
                sendAck(player, ACK_RETRY_CHUNK);
                return;
            }

            byte[] chunkData = new byte[chunkLength];
            in.readFully(chunkData);

            session.receivedChunks.put(chunkIndex, chunkData);
            sendAck(player, ACK_OK);
        } catch (Exception e) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING,
                    "Failed to handle TELEMETRY_CHUNK from " + player.getName(), e);
            sendAck(player, ACK_RETRY_CHUNK);
        }
    }

    /**
     * Handles TELEMETRY_END packet (ID 82).
     * Assembles all chunks, validates checksum, decompresses and validates data.
     *
     * @param player the sending player
     * @param in     data input positioned after the packet ID
     */
    public static void handleTelemetryEnd(Player player, ByteArrayDataInput in) {
        try {
            long clientChecksum = in.readLong();
            UUID uuid = player.getUniqueId();
            TelemetryTransferSession session = activeSessions.remove(uuid);

            if (session == null) {
                sendAck(player, ACK_REJECT);
                return;
            }

            if (!session.isComplete()) {
                TimingSystem.getPlugin().getLogger().warning(
                        "Incomplete telemetry from " + player.getName()
                                + ": received " + session.receivedChunks.size()
                                + "/" + session.expectedChunks + " chunks");
                sendAck(player, ACK_REJECT);
                sendResult(player, RESULT_INVALID, "Incomplete transfer");
                return;
            }

            // Assemble chunks in order
            byte[] assembled = assembleChunks(session);
            if (assembled.length != session.expectedBytes) {
                TimingSystem.getPlugin().getLogger().warning(
                        "Size mismatch for " + player.getName()
                                + ": expected=" + session.expectedBytes + " got=" + assembled.length);
                sendAck(player, ACK_REJECT);
                sendResult(player, RESULT_INVALID, "Size mismatch");
                return;
            }

            // Verify checksum
            long computedChecksum = computeChecksum(assembled);
            if (computedChecksum != clientChecksum) {
                TimingSystem.getPlugin().getLogger().warning(
                        "Checksum mismatch for " + player.getName()
                                + ": expected=" + clientChecksum + " got=" + computedChecksum);
                sendAck(player, ACK_REJECT);
                sendResult(player, RESULT_INVALID, "Checksum mismatch");
                return;
            }

            sendAck(player, ACK_OK);

            // Decompress and validate asynchronously
            final byte[] compressedData = assembled;
            final int trackId = session.trackId;
            final int expectedTicks = session.expectedTicks;

            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> {
                processAssembledData(player, uuid, trackId, expectedTicks, compressedData, clientChecksum);
            });

        } catch (Exception e) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING,
                    "Failed to handle TELEMETRY_END from " + player.getName(), e);
            sendAck(player, ACK_REJECT);
        }
    }

    // ─── CLEANUP ───

    /**
     * Removes transfer sessions that have exceeded the timeout.
     * Should be called periodically (e.g. every 30 seconds).
     */
    public static void cleanupTimedOut() {
        activeSessions.entrySet().removeIf(entry -> {
            if (entry.getValue().isTimedOut()) {
                TimingSystem.getPlugin().getLogger().info(
                        "Cleaned up timed-out telemetry session for " + entry.getKey());
                return true;
            }
            return false;
        });
    }

    // ─── PRIVATE HELPERS ───

    /**
     * Assembles received chunks into a single byte array in order.
     */
    private static byte[] assembleChunks(TelemetryTransferSession session) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(session.expectedBytes);
        for (int i = 0; i < session.expectedChunks; i++) {
            byte[] chunk = session.receivedChunks.get(i);
            if (chunk == null) {
                throw new IOException("Missing chunk " + i);
            }
            out.write(chunk);
        }
        return out.toByteArray();
    }

    /**
     * Decompresses GZIP data.
     */
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

    /**
     * Computes a simple checksum (CRC-style sum of all bytes).
     */
    private static long computeChecksum(byte[] data) {
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(data);
        return crc.getValue();
    }

    /**
     * Decompresses, validates and stores telemetry data.
     * Runs on an async thread.
     */
    private static void processAssembledData(Player player, UUID uuid, int trackId,
                                             int expectedTicks, byte[] compressedData, long checksum) {
        try {
            byte[] decompressed = decompress(compressedData);

            // Validate telemetry data
            // maxSpeedBlocks=0 disables speed checks when vehicle type is unknown
            TelemetryValidator.ValidationResult result = TelemetryValidator.validate(
                    decompressed, expectedTicks, 0);

            int totalTicks = decompressed.length / 17; // BYTES_PER_FRAME

            // Save file and metadata
            TelemetryStorage.saveToFile(uuid, trackId, compressedData, result);
            TelemetryStorage.saveMetadata(uuid, trackId, 0, totalTicks, 0L,
                    result.getStatus(), result.getReason(), checksum);

            // Send result back to client on the main thread
            byte statusCode = TelemetryValidator.STATUS_VALID.equals(result.getStatus())
                    || TelemetryValidator.STATUS_SUSPICIOUS.equals(result.getStatus())
                    ? RESULT_VALID : RESULT_INVALID;

            org.bukkit.Bukkit.getScheduler().runTask(TimingSystem.getPlugin(), () -> {
                sendResult(player, statusCode, result.getReason());
            });

            TimingSystem.getPlugin().getLogger().info(
                    "Processed telemetry for " + player.getName()
                            + ": track=" + trackId + " ticks=" + totalTicks
                            + " status=" + result.getStatus());
        } catch (IOException e) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING,
                    "Failed to process telemetry from " + player.getName(), e);
            org.bukkit.Bukkit.getScheduler().runTask(TimingSystem.getPlugin(), () -> {
                sendResult(player, RESULT_INVALID, "Decompression failed");
            });
        }
    }

    // ─── RESPONSE PACKETS ───

    /**
     * Sends TELEMETRY_ACK (packet 83) to the client.
     *
     * @param player the target player
     * @param status ACK_OK, ACK_RETRY_CHUNK, or ACK_REJECT
     */
    private static void sendAck(Player player, byte status) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(PACKET_ID_TELEMETRY_ACK);
            out.writeByte(status);
            player.sendPluginMessage(TimingSystem.getPlugin(),
                    CustomBoatUtilsMode.CHANNEL_IBREALISTIC, byteStream.toByteArray());
        } catch (IOException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE,
                    "Failed to send TELEMETRY_ACK to " + player.getName(), e);
        }
    }

    /**
     * Sends TELEMETRY_RESULT (packet 84) to the client.
     *
     * @param player the target player
     * @param status RESULT_VALID or RESULT_INVALID
     * @param reason human-readable reason (empty if valid)
     */
    private static void sendResult(Player player, byte status, String reason) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(PACKET_ID_TELEMETRY_RESULT);
            out.writeByte(status);
            byte[] reasonBytes = (reason != null ? reason : "").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            out.writeShort(reasonBytes.length);
            out.write(reasonBytes);
            player.sendPluginMessage(TimingSystem.getPlugin(),
                    CustomBoatUtilsMode.CHANNEL_IBREALISTIC, byteStream.toByteArray());
        } catch (IOException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE,
                    "Failed to send TELEMETRY_RESULT to " + player.getName(), e);
        }
    }

    // ─── GHOST REQUEST ───

    /**
     * Handles GHOST_REQUEST packet (ID 88).
     * Client requests ghost data for a given track.
     *
     * @param player the requesting player
     * @param in     data input positioned after the packet ID
     */
    public static void handleGhostRequest(Player player, ByteArrayDataInput in) {
        try {
            int trackId = in.readInt();
            int requestedMode = in.readByte() & 0xFF;

            UUID uuid = player.getUniqueId();
            var tPlayer = me.makkuusen.timing.system.database.TSDatabase.getPlayer(uuid);
            if (tPlayer == null) return;

            // Use player's configured mode (requestedMode=0 means use settings)
            me.makkuusen.timing.system.ghost.GhostDisplayMode displayMode;
            if (requestedMode == 0) {
                displayMode = tPlayer.getSettings().getGhostDisplayMode();
            } else {
                displayMode = me.makkuusen.timing.system.ghost.GhostDisplayMode.fromId(requestedMode);
            }

            if (displayMode == me.makkuusen.timing.system.ghost.GhostDisplayMode.OFF) {
                return; // Ghost display is off
            }

            // Send ghosts asynchronously
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> {
                if (displayMode == me.makkuusen.timing.system.ghost.GhostDisplayMode.COMPETITION) {
                    // Competition mode — send multiple random ghosts from leaderboard
                    int maxGhosts = tPlayer.getSettings().getGhostCount();
                    var ghosts = me.makkuusen.timing.system.ghost.GhostManager.loadCompetitionGhosts(
                            trackId, uuid, maxGhosts);
                    // Also send player's own PB as ghost index 0
                    var pb = me.makkuusen.timing.system.ghost.GhostManager.loadGhost(uuid, trackId);
                    if (pb != null) {
                        me.makkuusen.timing.system.ghost.GhostSender.sendGhost(
                                player, pb.frames, pb.finishTimeMs, 0, displayMode);
                    }
                    int idx = 1;
                    for (var ghost : ghosts) {
                        me.makkuusen.timing.system.ghost.GhostSender.sendGhost(
                                player, ghost.frames, ghost.finishTimeMs, idx, displayMode);
                        idx++;
                    }
                    TimingSystem.getPlugin().getLogger().info(
                            "Sent " + (idx) + " competition ghosts to " + player.getName()
                                    + " for track " + trackId);
                } else {
                    // LINE or BOAT mode — send only personal best
                    var pb = me.makkuusen.timing.system.ghost.GhostManager.loadGhost(uuid, trackId);
                    if (pb != null) {
                        me.makkuusen.timing.system.ghost.GhostSender.sendGhost(
                                player, pb.frames, pb.finishTimeMs, 0, displayMode);
                    }
                }
            });
        } catch (Exception e) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING,
                    "Failed to handle GHOST_REQUEST from " + player.getName(), e);
        }
    }
}
