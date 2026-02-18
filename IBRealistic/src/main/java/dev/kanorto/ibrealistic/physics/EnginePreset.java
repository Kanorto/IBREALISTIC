package dev.kanorto.ibrealistic.physics;

/**
 * Engine presets that modify power output and engine braking characteristics.
 * Each preset defines engine force, engine braking, and drag coefficient multipliers.
 */
public enum EnginePreset {
    // name, engineForceMultiplier, engineBrakingMultiplier, enginePowerMultiplier, dragMultiplier, requiredLevel, price
    STOCK(0, 1.0f, 1.0f, 1.0f, 1.0f, 0, 0),
    SPORT(1, 1.10f, 1.10f, 1.15f, 1.02f, 5, 800),
    RALLY(2, 1.15f, 1.20f, 1.30f, 1.05f, 10, 1200),
    TURBO(3, 1.20f, 1.05f, 1.50f, 1.10f, 15, 2000),
    MONSTER(4, 1.25f, 0.90f, 1.80f, 1.15f, 20, 3000);

    public final int id;
    /** Multiplier applied to base engine force */
    public final float engineForceMultiplier;
    /** Multiplier applied to engine braking force */
    public final float engineBrakingMultiplier;
    /** Multiplier applied to engine power (watts) — affects top speed */
    public final float enginePowerMultiplier;
    /** Multiplier applied to aerodynamic drag coefficient */
    public final float dragMultiplier;
    /** Minimum player level required to purchase */
    public final int requiredLevel;
    /** Price in Rally Coins */
    public final int price;

    EnginePreset(int id, float engineForceMultiplier, float engineBrakingMultiplier,
                 float enginePowerMultiplier, float dragMultiplier, int requiredLevel, int price) {
        this.id = id;
        this.engineForceMultiplier = engineForceMultiplier;
        this.engineBrakingMultiplier = engineBrakingMultiplier;
        this.enginePowerMultiplier = enginePowerMultiplier;
        this.dragMultiplier = dragMultiplier;
        this.requiredLevel = requiredLevel;
        this.price = price;
    }

    public static EnginePreset fromId(int id) {
        for (EnginePreset preset : values()) {
            if (preset.id == id) return preset;
        }
        return STOCK;
    }
}
