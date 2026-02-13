package dev.o7moon.openboatutils.physics;

/**
 * Weather conditions that affect surface grip globally.
 * Applied as a multiplier to surface friction coefficients.
 */
public enum WeatherCondition {
    CLEAR(0, 1.0f, 1.0f),
    RAIN(1, 0.70f, 1.3f),
    HEAVY_RAIN(2, 0.50f, 1.6f),
    SNOW(3, 0.40f, 1.5f),
    FOG(4, 0.95f, 1.1f);

    public final int id;
    /** Multiplier applied to surface muPeak and muSlide */
    public final float gripMultiplier;
    /** Multiplier applied to surface relaxationLength (higher = slower tire response) */
    public final float relaxationMultiplier;

    WeatherCondition(int id, float gripMultiplier, float relaxationMultiplier) {
        this.id = id;
        this.gripMultiplier = gripMultiplier;
        this.relaxationMultiplier = relaxationMultiplier;
    }

    public static WeatherCondition fromId(int id) {
        for (WeatherCondition condition : values()) {
            if (condition.id == id) return condition;
        }
        return CLEAR;
    }
}
