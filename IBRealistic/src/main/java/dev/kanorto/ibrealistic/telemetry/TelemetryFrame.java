package dev.kanorto.ibrealistic.telemetry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * One tick of telemetry data captured during a race.
 * Approximately 80 bytes per frame when serialized.
 */
public class TelemetryFrame {

    // ─── TIMING ───
    public int tick;

    // ─── POSITION ───
    public float posX;
    public float posY;
    public float posZ;

    // ─── VELOCITY (vehicle frame) ───
    public float vx;
    public float vy;

    // ─── HEADING ───
    public float yawAngle;
    public float yawRate;

    // ─── CONTROLS ───
    public float steeringAngle;
    public float throttleInput;
    public float brakeInput;
    public boolean handbrake;

    // ─── STATE ───
    public boolean airborne;
    public byte surfaceType;

    // ─── WHEEL SLIP ANGLES ───
    public float slipAngleFL;
    public float slipAngleFR;
    public float slipAngleRL;
    public float slipAngleRR;

    // ─── DERIVED ───
    public float speedKmh;
    public float gForceLateral;
    public float gForceLongitudinal;

    // ─── SERIALIZATION ───

    /**
     * Serialize this frame to a binary stream.
     */
    public void writeTo(DataOutputStream dos) throws IOException {
        dos.writeInt(tick);
        dos.writeFloat(posX);
        dos.writeFloat(posY);
        dos.writeFloat(posZ);
        dos.writeFloat(vx);
        dos.writeFloat(vy);
        dos.writeFloat(yawAngle);
        dos.writeFloat(yawRate);
        dos.writeFloat(steeringAngle);
        dos.writeFloat(throttleInput);
        dos.writeFloat(brakeInput);

        // Pack two booleans into one byte
        byte flags = 0;
        if (handbrake) flags |= 0x01;
        if (airborne) flags |= 0x02;
        dos.writeByte(flags);

        dos.writeByte(surfaceType);
        dos.writeFloat(slipAngleFL);
        dos.writeFloat(slipAngleFR);
        dos.writeFloat(slipAngleRL);
        dos.writeFloat(slipAngleRR);
        dos.writeFloat(speedKmh);
        dos.writeFloat(gForceLateral);
        dos.writeFloat(gForceLongitudinal);
    }

    /**
     * Deserialize a frame from a binary stream.
     */
    public static TelemetryFrame readFrom(DataInputStream dis) throws IOException {
        TelemetryFrame frame = new TelemetryFrame();
        frame.tick = dis.readInt();
        frame.posX = dis.readFloat();
        frame.posY = dis.readFloat();
        frame.posZ = dis.readFloat();
        frame.vx = dis.readFloat();
        frame.vy = dis.readFloat();
        frame.yawAngle = dis.readFloat();
        frame.yawRate = dis.readFloat();
        frame.steeringAngle = dis.readFloat();
        frame.throttleInput = dis.readFloat();
        frame.brakeInput = dis.readFloat();

        byte flags = dis.readByte();
        frame.handbrake = (flags & 0x01) != 0;
        frame.airborne = (flags & 0x02) != 0;

        frame.surfaceType = dis.readByte();
        frame.slipAngleFL = dis.readFloat();
        frame.slipAngleFR = dis.readFloat();
        frame.slipAngleRL = dis.readFloat();
        frame.slipAngleRR = dis.readFloat();
        frame.speedKmh = dis.readFloat();
        frame.gForceLateral = dis.readFloat();
        frame.gForceLongitudinal = dis.readFloat();
        return frame;
    }

    // ─── UTILITY ───

    /**
     * Round a float to 3 decimal places to reduce file size and noise.
     */
    public static float roundTo3(float value) {
        return Math.round(value * 1000f) / 1000f;
    }
}
