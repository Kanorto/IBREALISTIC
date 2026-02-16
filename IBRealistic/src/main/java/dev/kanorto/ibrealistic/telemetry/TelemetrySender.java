package dev.kanorto.ibrealistic.telemetry;

import dev.kanorto.ibrealistic.IBRealistic;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
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
    /** Maximum chunk size in bytes (16 KiB, well within 32KB Minecraft plugin message limit) */
    private static final int CHUNK_SIZE = 16384;

    /** Delay between chunks to avoid flooding (ms) */
    private static final int CHUNK_DELAY_MS = 50;

    /**
     * Send compressed telemetry data to the server in chunks.
     * Should be called from a background thread — packet sends are
     * scheduled on the render thread via MinecraftClient.execute().
     *
     * @param recorder the completed recorder with data to send
     */
    public static void sendToServer(TelemetryRecorder recorder) throws IOException {
        byte[] compressed = recorder.toCompressedBytes();
        TelemetryHeader header = recorder.getHeader();

        if (header.totalTicks <= 0) {
            IBRealistic.LOG.info("Skipping telemetry send: no ticks recorded");
            return;
        }

        int totalChunks = (int) Math.ceil((double) compressed.length / CHUNK_SIZE);

        IBRealistic.LOG.info("Sending telemetry: {} bytes, {} chunks", compressed.length, totalChunks);

        MinecraftClient client = MinecraftClient.getInstance();

        // 1. Send TELEMETRY_START
        client.execute(() -> {
            try {
                PacketByteBuf startPacket = PacketByteBufs.create();
                startPacket.writeShort(PACKET_TELEMETRY_START);
                startPacket.writeInt(totalChunks);
                startPacket.writeInt(compressed.length);
                startPacket.writeInt(header.totalTicks);
                startPacket.writeInt(header.trackId);
                startPacket.writeLong(header.finishTimeMs);
                IBRealistic.sendPacketC2S(startPacket);
            } catch (Exception e) {
                IBRealistic.LOG.error("Failed to send TELEMETRY_START: {}", e.getMessage());
            }
        });

        // 2. Send TELEMETRY_CHUNK for each chunk
        for (int i = 0; i < totalChunks; i++) {
            int offset = i * CHUNK_SIZE;
            int length = Math.min(CHUNK_SIZE, compressed.length - offset);
            final int chunkIndex = i;
            final byte[] chunkData = new byte[length];
            System.arraycopy(compressed, offset, chunkData, 0, length);

            client.execute(() -> {
                try {
                    PacketByteBuf chunkPacket = PacketByteBufs.create();
                    chunkPacket.writeShort(PACKET_TELEMETRY_CHUNK);
                    chunkPacket.writeInt(chunkIndex);
                    chunkPacket.writeInt(chunkData.length);
                    chunkPacket.writeBytes(chunkData);
                    IBRealistic.sendPacketC2S(chunkPacket);
                } catch (Exception e) {
                    IBRealistic.LOG.error("Failed to send chunk {}: {}", chunkIndex, e.getMessage());
                }
            });

            // Small delay between chunks to avoid flooding
            if (i < totalChunks - 1) {
                try {
                    Thread.sleep(CHUNK_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    IBRealistic.LOG.warn("Telemetry send interrupted at chunk {}/{}", i, totalChunks);
                    return;
                }
            }
        }

        // 3. Send TELEMETRY_END
        long checksum = header.checksum;
        client.execute(() -> {
            try {
                PacketByteBuf endPacket = PacketByteBufs.create();
                endPacket.writeShort(PACKET_TELEMETRY_END);
                endPacket.writeLong(checksum);
                IBRealistic.sendPacketC2S(endPacket);
            } catch (Exception e) {
                IBRealistic.LOG.error("Failed to send TELEMETRY_END: {}", e.getMessage());
            }
        });

        IBRealistic.LOG.info("Telemetry sent to server ({} chunks)", totalChunks);
    }
}
