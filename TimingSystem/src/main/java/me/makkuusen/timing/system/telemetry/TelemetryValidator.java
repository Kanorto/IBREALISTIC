package me.makkuusen.timing.system.telemetry;

import lombok.Getter;
import me.makkuusen.timing.system.TimingSystem;

import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Validates received telemetry data for integrity, physical plausibility,
 * and statistical patterns (e.g. bot detection).
 */
public class TelemetryValidator {

    // ─── CONFIGURATION CONSTANTS ───

    /** Maximum allowed distance (blocks) between consecutive ticks */
    private static final float MAX_TELEPORT_DISTANCE = 15.0f;

    /** Speed tolerance multiplier (20% over declared max) */
    private static final float SPEED_TOLERANCE = 1.2f;

    /** Acceleration tolerance multiplier (30% over physical limit) */
    private static final float ACCELERATION_TOLERANCE = 1.3f;

    /** Minimum ratio of recorded ticks to expected ticks */
    private static final float MIN_TICK_COVERAGE = 0.9f;

    /** Percentage of full-throttle ticks that triggers suspicious flag */
    private static final float SUSPICIOUS_FULL_THROTTLE_PERCENT = 0.95f;

    /** Minimum number of ticks for a valid telemetry recording */
    private static final int MIN_TICKS = 10;

    /** Bytes per telemetry frame: x(4) + z(4) + yaw(4) + speed(4) + throttle(1) = 17 */
    private static final int BYTES_PER_FRAME = 17;

    // ─── VALIDATION RESULT ───

    /**
     * Result of telemetry validation.
     */
    public static class ValidationResult {
        @Getter private final String status;
        @Getter private final String reason;

        public ValidationResult(String status, String reason) {
            this.status = status;
            this.reason = reason;
        }
    }

    /**
     * Validation status constants.
     */
    public static final String STATUS_VALID = "VALID";
    public static final String STATUS_INVALID = "INVALID";
    public static final String STATUS_SUSPICIOUS = "SUSPICIOUS";
    public static final String STATUS_PENDING = "PENDING";

    // ─── PUBLIC API ───

    /**
     * Validates decompressed telemetry data.
     *
     * @param decompressedData raw telemetry bytes (frames of BYTES_PER_FRAME each)
     * @param expectedTicks    number of ticks the race should have lasted
     * @param maxSpeedBlocks   maximum speed in blocks/tick for the vehicle type
     * @return validation result with status and reason
     */
    public static ValidationResult validate(byte[] decompressedData, int expectedTicks, double maxSpeedBlocks) {
        // ─── INTEGRITY CHECKS ───
        if (decompressedData == null || decompressedData.length == 0) {
            return new ValidationResult(STATUS_INVALID, "Empty telemetry data");
        }

        if (decompressedData.length % BYTES_PER_FRAME != 0) {
            return new ValidationResult(STATUS_INVALID,
                    "Data size " + decompressedData.length + " not a multiple of frame size " + BYTES_PER_FRAME);
        }

        int totalTicks = decompressedData.length / BYTES_PER_FRAME;
        if (totalTicks < MIN_TICKS) {
            return new ValidationResult(STATUS_INVALID, "Too few ticks: " + totalTicks);
        }

        // Tick coverage check: recorded ticks should be within ±10% of expected
        if (expectedTicks > 0) {
            float coverage = (float) totalTicks / expectedTicks;
            if (coverage < MIN_TICK_COVERAGE) {
                return new ValidationResult(STATUS_INVALID,
                        "Tick coverage too low: " + String.format("%.1f%%", coverage * 100)
                                + " (expected >=" + String.format("%.0f%%", MIN_TICK_COVERAGE * 100) + ")");
            }
        }

        // ─── PHYSICAL CHECKS ───
        try {
            return validatePhysical(decompressedData, totalTicks, maxSpeedBlocks);
        } catch (IOException e) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to parse telemetry frames", e);
            return new ValidationResult(STATUS_INVALID, "Failed to parse frames: " + e.getMessage());
        }
    }

    // ─── PRIVATE HELPERS ───

    private static ValidationResult validatePhysical(byte[] data, int totalTicks, double maxSpeedBlocks) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));

        float prevX = 0;
        float prevZ = 0;
        float prevSpeed = 0;
        int fullThrottleTicks = 0;
        float allowedMaxSpeed = (float) (maxSpeedBlocks * SPEED_TOLERANCE);

        for (int i = 0; i < totalTicks; i++) {
            float x = in.readFloat();
            float z = in.readFloat();
            float yaw = in.readFloat();
            float speed = in.readFloat();
            byte throttle = in.readByte();

            if (i > 0) {
                // Teleportation check
                float dx = x - prevX;
                float dz = z - prevZ;
                float dist = (float) Math.sqrt(dx * dx + dz * dz);
                if (dist > MAX_TELEPORT_DISTANCE) {
                    return new ValidationResult(STATUS_INVALID,
                            "Teleport detected at tick " + i + ": distance=" + String.format("%.2f", dist));
                }

                // Speed sanity check
                if (maxSpeedBlocks > 0 && speed > allowedMaxSpeed) {
                    return new ValidationResult(STATUS_INVALID,
                            "Speed exceeds limit at tick " + i + ": " + String.format("%.2f", speed)
                                    + " > " + String.format("%.2f", allowedMaxSpeed));
                }

                // Acceleration check (speed change per tick)
                float accelPerTick = Math.abs(speed - prevSpeed);
                float maxAccel = (float) (maxSpeedBlocks * ACCELERATION_TOLERANCE);
                if (maxSpeedBlocks > 0 && accelPerTick > maxAccel) {
                    return new ValidationResult(STATUS_INVALID,
                            "Excessive acceleration at tick " + i + ": " + String.format("%.2f", accelPerTick));
                }
            }

            // Track full-throttle usage for bot detection
            // throttle byte: 0 = no input, nonzero = throttle applied
            if (throttle != 0) {
                fullThrottleTicks++;
            }

            prevX = x;
            prevZ = z;
            prevSpeed = speed;
        }

        // ─── STATISTICAL CHECKS ───
        float fullThrottleRatio = (float) fullThrottleTicks / totalTicks;
        if (fullThrottleRatio > SUSPICIOUS_FULL_THROTTLE_PERCENT) {
            return new ValidationResult(STATUS_SUSPICIOUS,
                    "Full throttle " + String.format("%.1f%%", fullThrottleRatio * 100) + " of the time (potential bot)");
        }

        return new ValidationResult(STATUS_VALID, "");
    }
}
