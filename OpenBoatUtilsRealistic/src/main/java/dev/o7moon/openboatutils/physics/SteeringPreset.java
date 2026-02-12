package dev.o7moon.openboatutils.physics;

/**
 * Steering system presets that affect steering response and behavior.
 * Each preset modifies maximum steering angle, steering speed, return rate, and speed sensitivity.
 */
public enum SteeringPreset {
    // name, maxSteeringAngle, steeringSpeed, steeringReturnRate, speedSteeringFactor, requiredLevel, price
    STANDARD(0, 0.50f, 1.4f, 1.5f, 0.004f, 0, 0),
    QUICK(1, 0.55f, 1.8f, 2.0f, 0.005f, 4, 500),
    PROGRESSIVE(2, 0.45f, 1.2f, 1.8f, 0.006f, 8, 800),
    DRIFT(3, 0.65f, 2.0f, 1.0f, 0.003f, 12, 1200);

    public final int id;
    /** Maximum steering angle in radians */
    public final float maxSteeringAngle;
    /** Steering input speed (rad/s) */
    public final float steeringSpeed;
    /** Steering return rate when no input (rad/s) */
    public final float steeringReturnRate;
    /** Speed-dependent steering reduction factor */
    public final float speedSteeringFactor;
    /** Minimum player level required to purchase */
    public final int requiredLevel;
    /** Price in Rally Coins */
    public final int price;

    SteeringPreset(int id, float maxSteeringAngle, float steeringSpeed,
                   float steeringReturnRate, float speedSteeringFactor,
                   int requiredLevel, int price) {
        this.id = id;
        this.maxSteeringAngle = maxSteeringAngle;
        this.steeringSpeed = steeringSpeed;
        this.steeringReturnRate = steeringReturnRate;
        this.speedSteeringFactor = speedSteeringFactor;
        this.requiredLevel = requiredLevel;
        this.price = price;
    }

    public static SteeringPreset fromId(int id) {
        for (SteeringPreset preset : values()) {
            if (preset.id == id) return preset;
        }
        return STANDARD;
    }
}
