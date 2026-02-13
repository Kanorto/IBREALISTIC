package dev.o7moon.openboatutils.physics;

/**
 * Brake system presets that modify braking force and front/rear brake balance.
 * Each preset defines braking force multiplier and brake bias distribution.
 */
public enum BrakePreset {
    // name, brakingForceMultiplier, brakeBias, requiredLevel, price
    STANDARD(0, 1.0f, 0.65f, 0, 0),
    SPORT(1, 1.15f, 0.60f, 4, 500),
    RACING(2, 1.30f, 0.55f, 10, 1000),
    ENDURANCE(3, 1.10f, 0.70f, 6, 700);

    public final int id;
    /** Multiplier applied to base braking force */
    public final float brakingForceMultiplier;
    /** Brake bias (0.0 = all rear, 1.0 = all front) */
    public final float brakeBias;
    /** Minimum player level required to purchase */
    public final int requiredLevel;
    /** Price in Rally Coins */
    public final int price;

    BrakePreset(int id, float brakingForceMultiplier, float brakeBias,
                int requiredLevel, int price) {
        this.id = id;
        this.brakingForceMultiplier = brakingForceMultiplier;
        this.brakeBias = brakeBias;
        this.requiredLevel = requiredLevel;
        this.price = price;
    }

    public static BrakePreset fromId(int id) {
        for (BrakePreset preset : values()) {
            if (preset.id == id) return preset;
        }
        return STANDARD;
    }
}
