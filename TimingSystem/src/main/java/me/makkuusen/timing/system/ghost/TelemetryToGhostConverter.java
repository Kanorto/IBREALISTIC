package me.makkuusen.timing.system.ghost;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts decompressed telemetry data into ghost frames.
 * <p>
 * Telemetry frame layout (82 bytes):
 * tick(4) posX(4) posY(4) posZ(4) vx(4) vy(4) yawAngle(4) yawRate(4)
 * steeringAngle(4) throttle(4) brake(4) flags(1) surface(1)
 * pitch(4) roll(4) slipFL(4) slipFR(4) slipRL(4) slipRR(4)
 * speedKmh(4) gLat(4) gLong(4)
 */
public class TelemetryToGhostConverter {

    private static final int TELEMETRY_BYTES_PER_FRAME = 82;

    /**
     * Convert raw decompressed telemetry bytes into ghost frames.
     * Only extracts position, yaw, steering and speed.
     */
    public static List<GhostFrame> convert(byte[] decompressedTelemetry) throws IOException {
        List<GhostFrame> frames = new ArrayList<>();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(decompressedTelemetry));

        while (dis.available() >= TELEMETRY_BYTES_PER_FRAME) {
            GhostFrame ghost = new GhostFrame();
            dis.readInt(); // tick — skip
            ghost.posX = dis.readFloat();
            ghost.posY = dis.readFloat();
            ghost.posZ = dis.readFloat();
            dis.readFloat(); // vx — skip
            dis.readFloat(); // vy — skip
            ghost.yawAngle = dis.readFloat();
            dis.readFloat(); // yawRate — skip
            ghost.steeringAngle = dis.readFloat();
            dis.readFloat(); // throttle — skip
            dis.readFloat(); // brake — skip
            dis.readByte();  // flags — skip
            dis.readByte();  // surface — skip
            dis.readFloat(); // pitch — skip
            dis.readFloat(); // roll — skip
            dis.readFloat(); // slipFL — skip
            dis.readFloat(); // slipFR — skip
            dis.readFloat(); // slipRL — skip
            dis.readFloat(); // slipRR — skip
            ghost.speedKmh = dis.readFloat();
            dis.readFloat(); // gLat — skip
            dis.readFloat(); // gLong — skip

            frames.add(ghost);
        }

        return frames;
    }
}
