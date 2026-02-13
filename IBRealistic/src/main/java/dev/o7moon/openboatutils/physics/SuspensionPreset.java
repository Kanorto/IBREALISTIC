package dev.o7moon.openboatutils.physics;

/**
 * Suspension presets that affect weight transfer characteristics and ride height.
 * Each preset modifies roll stiffness, center of gravity height, and yaw rate damping.
 */
public enum SuspensionPreset {
    // name, rollStiffnessRatio, cgHeightMultiplier, yawRateDampingMultiplier, requiredLevel, price
    COMFORT(0, 0.55f, 1.00f, 0.990f, 0, 0),
    SPORT(1, 0.55f, 0.95f, 0.995f, 4, 600),
    RALLY(2, 0.58f, 0.85f, 0.997f, 8, 1000),
    STIFF(3, 0.65f, 0.80f, 0.998f, 12, 1500);

    public final int id;
    /** Front roll stiffness ratio (higher = more front grip in corners) */
    public final float rollStiffnessRatio;
    /** Multiplier applied to vehicle cgHeight (lower = lower CG = less weight transfer) */
    public final float cgHeightMultiplier;
    /** Yaw rate damping factor (higher = more stable, less responsive) */
    public final float yawRateDampingMultiplier;
    /** Minimum player level required to purchase */
    public final int requiredLevel;
    /** Price in Rally Coins */
    public final int price;

    SuspensionPreset(int id, float rollStiffnessRatio, float cgHeightMultiplier,
                     float yawRateDampingMultiplier, int requiredLevel, int price) {
        this.id = id;
        this.rollStiffnessRatio = rollStiffnessRatio;
        this.cgHeightMultiplier = cgHeightMultiplier;
        this.yawRateDampingMultiplier = yawRateDampingMultiplier;
        this.requiredLevel = requiredLevel;
        this.price = price;
    }

    public static SuspensionPreset fromId(int id) {
        for (SuspensionPreset preset : values()) {
            if (preset.id == id) return preset;
        }
        return COMFORT;
    }
}
