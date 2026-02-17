package dev.kanorto.ibrealistic.ghost;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * One tick of ghost data received from the server.
 * 24 bytes when serialized (6 floats × 4 bytes).
 */
public class GhostFrame {

    public float posX;
    public float posY;
    public float posZ;
    public float yawAngle;
    public float steeringAngle;
    public float speedKmh;

    public static GhostFrame readFrom(DataInputStream dis) throws IOException {
        GhostFrame f = new GhostFrame();
        f.posX = dis.readFloat();
        f.posY = dis.readFloat();
        f.posZ = dis.readFloat();
        f.yawAngle = dis.readFloat();
        f.steeringAngle = dis.readFloat();
        f.speedKmh = dis.readFloat();
        return f;
    }

    public static final int BYTES_PER_FRAME = 24;
}
