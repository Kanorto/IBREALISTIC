package dev.o7moon.openboatutils.physics;

/**
 * Weight distribution presets that modify front/rear weight bias.
 * Affects handling balance between understeer and oversteer tendencies.
 */
public enum WeightDistributionPreset {
    // name, frontWeightBias, requiredLevel, price
    BALANCED(0, 0.50f, 0, 0),
    FRONT_BIASED(1, 0.58f, 3, 400),
    REAR_BIASED(2, 0.42f, 3, 400),
    MID_ENGINE(3, 0.45f, 8, 800);

    public final int id;
    /** Front weight bias (0.0 = all rear, 1.0 = all front) */
    public final float frontWeightBias;
    /** Minimum player level required to purchase */
    public final int requiredLevel;
    /** Price in Rally Coins */
    public final int price;

    WeightDistributionPreset(int id, float frontWeightBias, int requiredLevel, int price) {
        this.id = id;
        this.frontWeightBias = frontWeightBias;
        this.requiredLevel = requiredLevel;
        this.price = price;
    }

    public static WeightDistributionPreset fromId(int id) {
        for (WeightDistributionPreset preset : values()) {
            if (preset.id == id) return preset;
        }
        return BALANCED;
    }
}
