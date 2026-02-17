package me.makkuusen.timing.system.ghost;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Converts decompressed telemetry data into ghost frames.
 * <p>
 * Telemetry stream layout: [Header] + N × [Frame (82 bytes)]
 * Header: formatVersion(4) + playerUUID(UTF) + playerName(UTF) + trackId(4)
 *         + raceType(1) + carType(1) + vehicleType(1) + weatherCondition(1)
 *         + damageEnabled(1) + startTimestamp(8) + totalTicks(4) + finishTimeMs(8) + checksum(8)
 * Frame (82 bytes):
 *   tick(4) posX(4) posY(4) posZ(4) vx(4) vy(4) yawAngle(4) yawRate(4)
 *   steeringAngle(4) throttle(4) brake(4) flags(1) surface(1)
 *   pitch(4) roll(4) slipFL(4) slipFR(4) slipRL(4) slipRR(4)
 *   speedKmh(4) gLat(4) gLong(4)
 */
public class TelemetryToGhostConverter {

    private static final int TELEMETRY_BYTES_PER_FRAME = 82;

    /**
     * Convert compressed telemetry data (as stored on server) into ghost frames.
     * This decompresses the data, skips the header, and extracts ghost frames.
     *
     * @param compressedData the GZIP compressed telemetry data from the client
     * @return list of ghost frames
     */
    public static List<GhostFrame> convertFromCompressed(byte[] compressedData) throws IOException {
        byte[] decompressed;
        try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(compressedData));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            decompressed = out.toByteArray();
        }
        return convertFromDecompressed(decompressed);
    }

    /**
     * Convert decompressed telemetry stream into ghost frames.
     * The stream contains a variable-length header followed by 82-byte frames.
     */
    public static List<GhostFrame> convertFromDecompressed(byte[] decompressedTelemetry) throws IOException {
        List<GhostFrame> frames = new ArrayList<>();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(decompressedTelemetry));

        // ─── SKIP HEADER ───
        dis.readInt();   // formatVersion
        dis.readUTF();   // playerUUID
        dis.readUTF();   // playerName
        dis.readInt();   // trackId
        dis.readByte();  // raceType
        dis.readByte();  // carType
        dis.readByte();  // vehicleType
        dis.readByte();  // weatherCondition
        dis.readBoolean(); // damageEnabled
        dis.readLong();  // startTimestamp
        dis.readInt();   // totalTicks
        dis.readLong();  // finishTimeMs
        dis.readLong();  // checksum

        // ─── READ FRAMES ───
        while (dis.available() >= TELEMETRY_BYTES_PER_FRAME) {
            GhostFrame ghost = new GhostFrame();
            dis.readInt();    // tick — skip
            ghost.posX = dis.readFloat();
            ghost.posY = dis.readFloat();
            ghost.posZ = dis.readFloat();
            dis.readFloat();  // vx — skip
            dis.readFloat();  // vy — skip
            ghost.yawAngle = dis.readFloat();
            dis.readFloat();  // yawRate — skip
            ghost.steeringAngle = dis.readFloat();
            dis.readFloat();  // throttle — skip
            dis.readFloat();  // brake — skip
            dis.readByte();   // flags — skip
            dis.readByte();   // surface — skip
            dis.readFloat();  // pitch — skip
            dis.readFloat();  // roll — skip
            dis.readFloat();  // slipFL — skip
            dis.readFloat();  // slipFR — skip
            dis.readFloat();  // slipRL — skip
            dis.readFloat();  // slipRR — skip
            ghost.speedKmh = dis.readFloat();
            dis.readFloat();  // gLat — skip
            dis.readFloat();  // gLong — skip

            frames.add(ghost);
        }

        return frames;
    }
}
