package me.makkuusen.timing.system.team;

import lombok.Getter;

/**
 * F1-style tire compounds with grip and wear characteristics.
 * Affects performance based on weather conditions.
 */
@Getter
public enum TireCompound {
    SOFT(0, "Soft", 1.15f, 2.0f, 0.6f, 0.7f, 500),
    MEDIUM(1, "Medium", 1.0f, 1.0f, 0.85f, 0.8f, 300),
    HARD(2, "Hard", 0.90f, 0.6f, 1.0f, 0.85f, 400),
    WET(3, "Wet", 0.80f, 0.8f, 0.9f, 1.3f, 800),
    INTERMEDIATE(4, "Intermediate", 0.92f, 0.9f, 0.92f, 1.15f, 600);

    private final int id;
    private final String displayName;
    /** Grip multiplier on DRY surface */
    private final float dryGrip;
    /** Wear rate multiplier (higher = faster wear) */
    private final float wearRate;
    /** Durability — how many laps before severe degradation (relative, 1.0 = baseline) */
    private final float durability;
    /** Grip multiplier on WET surface (rain, heavy rain) */
    private final float wetGrip;
    /** Cost in rally coins */
    private final int cost;

    TireCompound(int id, String displayName, float dryGrip, float wearRate,
                 float durability, float wetGrip, int cost) {
        this.id = id;
        this.displayName = displayName;
        this.dryGrip = dryGrip;
        this.wearRate = wearRate;
        this.durability = durability;
        this.wetGrip = wetGrip;
        this.cost = cost;
    }

    /**
     * Get effective grip for a given weather condition.
     */
    public float getEffectiveGrip(boolean isWet) {
        return isWet ? wetGrip : dryGrip;
    }

    public static TireCompound fromId(int id) {
        for (TireCompound c : values()) {
            if (c.id == id) return c;
        }
        return MEDIUM;
    }

    public static TireCompound fromName(String name) {
        if (name == null) return null;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
