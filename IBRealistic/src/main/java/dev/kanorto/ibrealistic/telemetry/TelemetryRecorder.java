package dev.kanorto.ibrealistic.telemetry;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;

/**
 * Records telemetry data during a race session.
 * One instance per race; captures one frame per tick.
 */
public class TelemetryRecorder {

    // ─── LIMITS ───
    /** Maximum recording length: 20 minutes at 20 tps */
    public static final int MAX_TELEMETRY_TICKS = 24000;

    // ─── STATE ───
    private volatile List<TelemetryFrame> frames;
    private TelemetryHeader header;
    private volatile boolean recording = false;
    private volatile int currentTick = 0;

    public TelemetryRecorder() {
        this.frames = new ArrayList<>();
        this.header = new TelemetryHeader();
    }

    // ─── RECORDING LIFECYCLE ───

    /**
     * Begin a new telemetry recording session.
     */
    public void startRecording(String uuid, String name, int trackId,
                               byte raceType, byte carType, byte vehicleType,
                               byte weatherCondition, boolean damageEnabled) {
        reset();
        header.playerUUID = uuid;
        header.playerName = name;
        header.trackId = trackId;
        header.raceType = raceType;
        header.carType = carType;
        header.vehicleType = vehicleType;
        header.weatherCondition = weatherCondition;
        header.damageEnabled = damageEnabled;
        header.startTimestamp = System.currentTimeMillis();
        recording = true;
    }

    /**
     * Record one tick of telemetry data.
     * Silently drops frames beyond MAX_TELEMETRY_TICKS.
     */
    public synchronized void recordTick(float posX, float posY, float posZ,
                           float vx, float vy,
                           float yawAngle, float yawRate,
                           float steeringAngle,
                           float throttle, float brake,
                           boolean handbrake, boolean airborne,
                           byte surfaceType,
                           float pitch, float roll,
                           float slipFL, float slipFR,
                           float slipRL, float slipRR,
                           float speedKmh,
                           float gLat, float gLong) {
        if (!recording || currentTick >= MAX_TELEMETRY_TICKS) {
            return;
        }

        TelemetryFrame frame = new TelemetryFrame();
        frame.tick = currentTick;
        frame.posX = TelemetryFrame.roundTo3(posX);
        frame.posY = TelemetryFrame.roundTo3(posY);
        frame.posZ = TelemetryFrame.roundTo3(posZ);
        frame.vx = TelemetryFrame.roundTo3(vx);
        frame.vy = TelemetryFrame.roundTo3(vy);
        frame.yawAngle = TelemetryFrame.roundTo3(yawAngle);
        frame.yawRate = TelemetryFrame.roundTo3(yawRate);
        frame.steeringAngle = TelemetryFrame.roundTo3(steeringAngle);
        frame.throttleInput = TelemetryFrame.roundTo3(throttle);
        frame.brakeInput = TelemetryFrame.roundTo3(brake);
        frame.handbrake = handbrake;
        frame.airborne = airborne;
        frame.surfaceType = surfaceType;
        frame.pitch = TelemetryFrame.roundTo3(pitch);
        frame.roll = TelemetryFrame.roundTo3(roll);
        frame.slipAngleFL = TelemetryFrame.roundTo3(slipFL);
        frame.slipAngleFR = TelemetryFrame.roundTo3(slipFR);
        frame.slipAngleRL = TelemetryFrame.roundTo3(slipRL);
        frame.slipAngleRR = TelemetryFrame.roundTo3(slipRR);
        frame.speedKmh = TelemetryFrame.roundTo3(speedKmh);
        frame.gForceLateral = TelemetryFrame.roundTo3(gLat);
        frame.gForceLongitudinal = TelemetryFrame.roundTo3(gLong);

        frames.add(frame);
        currentTick++;
    }

    /**
     * Finalize the recording. Sets total ticks and computes checksum.
     *
     * @param finishTimeMs race finish time in ms, or 0 if not finished
     */
    public synchronized void stopRecording(long finishTimeMs) {
        if (!recording) {
            return;
        }
        recording = false;
        header.totalTicks = frames.size();
        header.finishTimeMs = finishTimeMs;
        header.checksum = computeChecksum();
    }

    // ─── ACCESSORS ───

    public boolean isRecording() {
        return recording;
    }

    public TelemetryHeader getHeader() {
        return header;
    }

    public List<TelemetryFrame> getFrames() {
        return frames;
    }

    // ─── COMPRESSION ───

    /**
     * Produce GZIP-compressed bytes containing the header followed by all frames.
     * Intended for network transfer. For file storage, use
     * {@link TelemetryFileManager#saveToFile} which writes the header uncompressed
     * for quick metadata access and only compresses frames.
     */
    public synchronized byte[] toCompressedBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(gzip)) {
            header.writeTo(dos);
            for (TelemetryFrame frame : frames) {
                frame.writeTo(dos);
            }
        }
        return baos.toByteArray();
    }

    // ─── CHECKSUM ───

    /**
     * Compute a CRC32 checksum over all frame data for integrity verification.
     */
    public long computeChecksum() {
        CRC32 crc = new CRC32();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            for (TelemetryFrame frame : frames) {
                frame.writeTo(dos);
            }
            dos.flush();
            crc.update(baos.toByteArray());
        } catch (IOException e) {
            // ByteArrayOutputStream never throws IOException
            return 0L;
        }
        return crc.getValue();
    }

    // ─── RESET ───

    /**
     * Clear all state so this recorder can be reused.
     */
    public synchronized void reset() {
        frames = new ArrayList<>();
        header = new TelemetryHeader();
        recording = false;
        currentTick = 0;
    }
}
