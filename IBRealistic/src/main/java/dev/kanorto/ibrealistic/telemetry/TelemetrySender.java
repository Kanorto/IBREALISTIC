package dev.kanorto.ibrealistic.telemetry;

import dev.kanorto.ibrealistic.IBRealistic;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

import java.io.IOException;

/**
 * Sends recorded telemetry data to the server in chunks.
 * <p>
 * Protocol (client → server on ibrealistic:settings):
 * - Packet 80: TELEMETRY_START — header + totalChunks + totalBytes
 * - Packet 81: TELEMETRY_CHUNK — chunkIndex + chunkData[]
 * - Packet 82: TELEMETRY_END — checksum
 * <p>
 * Server responds (server → client on ibrealistic:settings):
 * - Packet 83: TELEMETRY_ACK — status (0=OK, 1=RETRY, 2=REJECT)
 * - Packet 84: TELEMETRY_RESULT — validationStatus + reason
 */
public class TelemetrySender {

    // ─── PACKET IDS ───
    public static final short PACKET_TELEMETRY_START = 80;
    public static final short PACKET_TELEMETRY_CHUNK = 81;
    public static final short PACKET_TELEMETRY_END = 82;

    // ─── CHUNK SIZE ───
    /** Maximum chunk size in bytes (must be ≤ 32KB Minecraft plugin message limit) */
    private static final int CHUNK_SIZE = 16384; // 16 KB

    /** Delay between chunks to avoid flooding (ms) */
    private static final int CHUNK_DELAY_MS = 50;

    /**
     * Send compressed telemetry data to the server in chunks.
     * Should be called from a background thread.
     *
     * @param recorder the completed recorder with data to send
     */
    public static void sendToServer(TelemetryRecorder recorder) throws IOException {
        byte[] compressed = recorder.toCompressedBytes();
        TelemetryHeader header = recorder.getHeader();

        int totalChunks = (int) Math.ceil((double) compressed.length / CHUNK_SIZE);

        IBRealistic.LOG.info("Sending telemetry: {} bytes, {} chunks", compressed.length, totalChunks);

        // 1. Send TELEMETRY_START
        PacketByteBuf startPacket = PacketByteBufs.create();
        startPacket.writeShort(PACKET_TELEMETRY_START);
        startPacket.writeInt(totalChunks);
        startPacket.writeInt(compressed.length);
        startPacket.writeInt(header.totalTicks);
        startPacket.writeInt(header.trackId);
        startPacket.writeLong(header.finishTimeMs);
        IBRealistic.sendPacketC2S(startPacket);

        // 2. Send TELEMETRY_CHUNK for each chunk
        for (int i = 0; i < totalChunks; i++) {
            int offset = i * CHUNK_SIZE;
            int length = Math.min(CHUNK_SIZE, compressed.length - offset);

            PacketByteBuf chunkPacket = PacketByteBufs.create();
            chunkPacket.writeShort(PACKET_TELEMETRY_CHUNK);
            chunkPacket.writeInt(i);
            chunkPacket.writeInt(length);
            chunkPacket.writeBytes(compressed, offset, length);
            IBRealistic.sendPacketC2S(chunkPacket);

            // Small delay between chunks to avoid flooding
            if (i < totalChunks - 1) {
                try {
                    Thread.sleep(CHUNK_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        // 3. Send TELEMETRY_END
        PacketByteBuf endPacket = PacketByteBufs.create();
        endPacket.writeShort(PACKET_TELEMETRY_END);
        endPacket.writeLong(header.checksum);
        IBRealistic.sendPacketC2S(endPacket);

        IBRealistic.LOG.info("Telemetry sent to server ({} chunks)", totalChunks);
    }
}
