package dev.o7moon.openboatutils.physics;

/**
 * Body/chassis presets that affect vehicle mass and aerodynamics.
 * Each preset modifies mass, downforce coefficient, and drag coefficient.
 */
public enum BodyPreset {
    // name, massMultiplier, downforceMultiplier, dragMultiplier, requiredLevel, price
    STANDARD(0, 1.0f, 1.0f, 1.0f, 0, 0),
    LIGHTWEIGHT(1, 0.85f, 0.90f, 0.95f, 5, 700),
    AERO(2, 1.05f, 1.50f, 1.10f, 10, 1200),
    RALLY_SPEC(3, 0.92f, 1.20f, 1.00f, 15, 1800),
    HEAVY_DUTY(4, 1.20f, 0.80f, 1.15f, 8, 900);

    public final int id;
    /** Multiplier applied to vehicle mass */
    public final float massMultiplier;
    /** Multiplier applied to downforce coefficient */
    public final float downforceMultiplier;
    /** Multiplier applied to aerodynamic drag */
    public final float dragMultiplier;
    /** Minimum player level required to purchase */
    public final int requiredLevel;
    /** Price in Rally Coins */
    public final int price;

    BodyPreset(int id, float massMultiplier, float downforceMultiplier,
               float dragMultiplier, int requiredLevel, int price) {
        this.id = id;
        this.massMultiplier = massMultiplier;
        this.downforceMultiplier = downforceMultiplier;
        this.dragMultiplier = dragMultiplier;
        this.requiredLevel = requiredLevel;
        this.price = price;
    }

    public static BodyPreset fromId(int id) {
        for (BodyPreset preset : values()) {
            if (preset.id == id) return preset;
        }
        return STANDARD;
    }
}
