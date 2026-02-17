package me.makkuusen.timing.system.ghost;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.boatutils.CustomBoatUtilsMode;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

/**
 * Sends ghost data from server to client via fragmented packets.
 * <p>
 * Protocol (server → client):
 * - Packet 85: GHOST_DATA_START — ghostIndex, totalChunks, totalBytes, totalTicks, finishTimeMs, displayMode
 * - Packet 86: GHOST_DATA_CHUNK — ghostIndex, chunkIndex, chunkData[]
 * - Packet 87: GHOST_DATA_END — ghostIndex
 */
public class GhostSender {

    // ─── PACKET IDS ───
    public static final short PACKET_ID_GHOST_DATA_START = 85;
    public static final short PACKET_ID_GHOST_DATA_CHUNK = 86;
    public static final short PACKET_ID_GHOST_DATA_END = 87;

    /** Chunk size for ghost data transfer */
    private static final int CHUNK_SIZE = 16384;

    /**
     * Send ghost data to a player.
     * Should be called from an async thread.
     *
     * @param player      target player
     * @param frames      ghost frames to send
     * @param finishTimeMs ghost finish time
     * @param ghostIndex  index of this ghost (0 for PB, 1+ for competition ghosts)
     * @param displayMode the display mode (LINE=1, BOAT=2, COMPETITION=3)
     */
    public static void sendGhost(Player player, List<GhostFrame> frames, long finishTimeMs,
                                 int ghostIndex, GhostDisplayMode displayMode) {
        try {
            // Compress ghost frames
            byte[] compressed = compressFrames(frames);

            int totalChunks = (int) Math.ceil((double) compressed.length / CHUNK_SIZE);

            // 1. Send GHOST_DATA_START
            sendStart(player, ghostIndex, totalChunks, compressed.length,
                    frames.size(), finishTimeMs, displayMode);

            // 2. Send chunks with small delays
            for (int i = 0; i < totalChunks; i++) {
                int offset = i * CHUNK_SIZE;
                int length = Math.min(CHUNK_SIZE, compressed.length - offset);
                byte[] chunkData = new byte[length];
                System.arraycopy(compressed, offset, chunkData, 0, length);
                sendChunk(player, ghostIndex, i, chunkData);

                if (i < totalChunks - 1) {
                    Thread.sleep(20); // Small delay to avoid flooding
                }
            }

            // 3. Send GHOST_DATA_END
            sendEnd(player, ghostIndex);

            TimingSystem.getPlugin().getLogger().info(
                    "Sent ghost to " + player.getName() + ": index=" + ghostIndex
                            + " ticks=" + frames.size() + " bytes=" + compressed.length);
        } catch (Exception e) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING,
                    "Failed to send ghost to " + player.getName(), e);
        }
    }

    private static byte[] compressFrames(List<GhostFrame> frames) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(gzip)) {
            for (GhostFrame frame : frames) {
                frame.writeTo(dos);
            }
        }
        return baos.toByteArray();
    }

    private static void sendStart(Player player, int ghostIndex, int totalChunks,
                                  int totalBytes, int totalTicks, long finishTimeMs,
                                  GhostDisplayMode displayMode) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteStream);
        out.writeShort(PACKET_ID_GHOST_DATA_START);
        out.writeByte(ghostIndex);
        out.writeInt(totalChunks);
        out.writeInt(totalBytes);
        out.writeInt(totalTicks);
        out.writeLong(finishTimeMs);
        out.writeByte(displayMode.getId());
        out.flush();
        player.sendPluginMessage(TimingSystem.getPlugin(),
                CustomBoatUtilsMode.CHANNEL_IBREALISTIC, byteStream.toByteArray());
    }

    private static void sendChunk(Player player, int ghostIndex, int chunkIndex,
                                  byte[] chunkData) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteStream);
        out.writeShort(PACKET_ID_GHOST_DATA_CHUNK);
        out.writeByte(ghostIndex);
        out.writeInt(chunkIndex);
        out.writeInt(chunkData.length);
        out.write(chunkData);
        out.flush();
        player.sendPluginMessage(TimingSystem.getPlugin(),
                CustomBoatUtilsMode.CHANNEL_IBREALISTIC, byteStream.toByteArray());
    }

    private static void sendEnd(Player player, int ghostIndex) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteStream);
        out.writeShort(PACKET_ID_GHOST_DATA_END);
        out.writeByte(ghostIndex);
        out.flush();
        player.sendPluginMessage(TimingSystem.getPlugin(),
                CustomBoatUtilsMode.CHANNEL_IBREALISTIC, byteStream.toByteArray());
    }
}
