package dev.kanorto.ibrealistic.physics;

/**
 * Tire compound presets that modify grip characteristics.
 * Each preset applies multipliers to the base surface friction values
 * and adjusts tire response (relaxation length).
 */
public enum TirePreset {
    // name, gripMultiplier, slideMultiplier, relaxationMultiplier, loadSensitivityMod, requiredLevel, price
    STANDARD(0, 1.0f, 1.0f, 1.0f, 0.0f, 0, 0),
    SOFT(1, 1.15f, 1.10f, 0.85f, 0.05f, 3, 500),
    MEDIUM(2, 1.05f, 1.05f, 0.95f, 0.02f, 2, 300),
    HARD(3, 0.90f, 0.95f, 1.15f, -0.03f, 5, 600),
    RAIN(4, 1.20f, 1.15f, 0.90f, 0.04f, 8, 1000),
    ICE_SPIKES(5, 1.40f, 1.30f, 0.80f, 0.06f, 12, 1500),
    RALLY_GRAVEL(6, 1.10f, 1.20f, 1.10f, 0.03f, 10, 1200);

    public final int id;
    /** Multiplier applied to surface muPeak */
    public final float gripMultiplier;
    /** Multiplier applied to surface muSlide */
    public final float slideMultiplier;
    /** Multiplier applied to tire relaxation length (lower = faster response) */
    public final float relaxationMultiplier;
    /** Additive modifier to surface load sensitivity */
    public final float loadSensitivityMod;
    /** Minimum player level required to purchase */
    public final int requiredLevel;
    /** Price in Rally Coins */
    public final int price;

    TirePreset(int id, float gripMultiplier, float slideMultiplier,
               float relaxationMultiplier, float loadSensitivityMod,
               int requiredLevel, int price) {
        this.id = id;
        this.gripMultiplier = gripMultiplier;
        this.slideMultiplier = slideMultiplier;
        this.relaxationMultiplier = relaxationMultiplier;
        this.loadSensitivityMod = loadSensitivityMod;
        this.requiredLevel = requiredLevel;
        this.price = price;
    }

    public static TirePreset fromId(int id) {
        for (TirePreset preset : values()) {
            if (preset.id == id) return preset;
        }
        return STANDARD;
    }
}
