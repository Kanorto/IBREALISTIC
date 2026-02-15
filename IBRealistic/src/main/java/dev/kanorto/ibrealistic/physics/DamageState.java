package dev.kanorto.ibrealistic.physics;

/**
 * Holds the damage and wear state for a vehicle.
 * Updated by server via packets — client applies effects to physics.
 * Thread-safe: volatile fields for cross-thread reads (render/game tick).
 */
public class DamageState {

    // ─── TIRE WEAR ───
    /** Per-wheel tire wear: 0.0 = new, 1.0 = fully worn. Order: FL, FR, RL, RR */
    private volatile float[] tireWear = new float[4];

    // ─── ENGINE TEMPERATURE ───
    /** Normalized engine temperature: 0.0 = cold, 1.0 = overheating */
    private volatile float engineTemp = 0f;

    // ─── BODY DAMAGE ───
    /** Body damage level: 0.0 = pristine, 1.0 = totalled */
    private volatile float bodyDamage = 0f;

    // ─── SERVICE ZONE ───
    /** Whether the vehicle is currently in a service/repair zone */
    private volatile boolean inServiceZone = false;
    /** Repair progress: 0.0 = not started, 1.0 = complete */
    private volatile float repairProgress = 0f;

    // ─── SYSTEM STATE ───
    /** Whether the damage system is enabled (server controls this) */
    private volatile boolean damageEnabled = false;

    // ─── DAMAGE EFFECTS CONFIGURATION ───
    /** Maximum tire grip loss at full wear (0.3 = 30% loss) */
    private static final float MAX_TIRE_GRIP_LOSS = 0.3f;
    /** Maximum engine power loss at full overheat (0.5 = 50% loss) */
    private static final float MAX_ENGINE_POWER_LOSS = 0.5f;

    // ─── DAMAGE EFFECT THRESHOLDS (body damage) ───
    private static final float DAMAGE_THRESHOLD_MINOR = 0.3f;
    private static final float DAMAGE_THRESHOLD_MODERATE = 0.6f;
    private static final float DAMAGE_THRESHOLD_SEVERE = 0.8f;

    /** Steering reduction at moderate damage (10%) */
    private static final float MODERATE_STEERING_PENALTY = 0.10f;
    /** Engine force reduction at moderate-severe damage (20%) */
    private static final float SEVERE_ENGINE_PENALTY = 0.20f;
    /** Speed limit multiplier at critical damage (50%) */
    private static final float CRITICAL_SPEED_LIMIT = 0.50f;

    // ─── GETTERS ───

    public float[] getTireWear() {
        return tireWear;
    }

    public float getTireWear(int wheelIndex) {
        if (wheelIndex < 0 || wheelIndex >= 4) return 0f;
        return tireWear[wheelIndex];
    }

    public float getAverageTireWear() {
        float[] w = tireWear;
        return (w[0] + w[1] + w[2] + w[3]) / 4f;
    }

    public float getEngineTemp() {
        return engineTemp;
    }

    public float getBodyDamage() {
        return bodyDamage;
    }

    public boolean isInServiceZone() {
        return inServiceZone;
    }

    public float getRepairProgress() {
        return repairProgress;
    }

    public boolean isDamageEnabled() {
        return damageEnabled;
    }

    // ─── SETTERS (called from packet handlers) ───

    public void setTireWear(float fl, float fr, float rl, float rr) {
        tireWear = new float[]{
                clamp(fl), clamp(fr), clamp(rl), clamp(rr)
        };
    }

    public void setEngineTemp(float temp) {
        this.engineTemp = clamp(temp);
    }

    public void setBodyDamage(float damage) {
        this.bodyDamage = clamp(damage);
    }

    public void setInServiceZone(boolean inZone) {
        this.inServiceZone = inZone;
    }

    public void setRepairProgress(float progress) {
        this.repairProgress = clamp(progress);
    }

    public void setDamageEnabled(boolean enabled) {
        this.damageEnabled = enabled;
    }

    // ─── DAMAGE EFFECTS (applied to physics) ───

    /**
     * Returns the effective friction multiplier for a wheel based on tire wear.
     * At full wear (1.0), friction is reduced by MAX_TIRE_GRIP_LOSS.
     * Formula: muEffective = mu × (1.0 - MAX_TIRE_GRIP_LOSS × tireWear)
     */
    public float getTireGripMultiplier(int wheelIndex) {
        if (!damageEnabled || wheelIndex < 0 || wheelIndex >= 4) return 1.0f;
        return 1.0f - MAX_TIRE_GRIP_LOSS * tireWear[wheelIndex];
    }

    /**
     * Returns the effective engine force multiplier based on engine temperature and body damage.
     * Engine overheat: up to 50% power loss at full temp.
     * Body damage above 0.6: additional 20% engine penalty.
     */
    public float getEngineForceMultiplier() {
        if (!damageEnabled) return 1.0f;
        float tempPenalty = MAX_ENGINE_POWER_LOSS * engineTemp;
        float dmgPenalty = bodyDamage >= DAMAGE_THRESHOLD_MODERATE ? SEVERE_ENGINE_PENALTY : 0f;
        return Math.max(0.1f, 1.0f - tempPenalty - dmgPenalty);
    }

    /**
     * Returns the effective max steering angle multiplier based on body damage.
     * Moderate damage (0.3-0.6): 10% steering reduction.
     */
    public float getSteeringMultiplier() {
        if (!damageEnabled) return 1.0f;
        if (bodyDamage >= DAMAGE_THRESHOLD_MINOR && bodyDamage < DAMAGE_THRESHOLD_SEVERE) {
            return 1.0f - MODERATE_STEERING_PENALTY;
        }
        if (bodyDamage >= DAMAGE_THRESHOLD_SEVERE) {
            return 1.0f - MODERATE_STEERING_PENALTY * 1.5f;
        }
        return 1.0f;
    }

    /**
     * Returns the maximum speed multiplier based on body damage.
     * Critical damage (>0.8): speed limited to 50%.
     */
    public float getMaxSpeedMultiplier() {
        if (!damageEnabled) return 1.0f;
        if (bodyDamage >= DAMAGE_THRESHOLD_SEVERE) {
            return CRITICAL_SPEED_LIMIT;
        }
        return 1.0f;
    }

    /**
     * Whether the vehicle should emit damage particles (smoke).
     */
    public boolean shouldEmitSmoke() {
        return damageEnabled && bodyDamage > 0.5f;
    }

    /**
     * Whether the engine is overheating (for visual/audio effects).
     */
    public boolean isEngineOverheating() {
        return damageEnabled && engineTemp > 0.8f;
    }

    // ─── RESET ───

    public void reset() {
        tireWear = new float[4];
        engineTemp = 0f;
        bodyDamage = 0f;
        inServiceZone = false;
        repairProgress = 0f;
        damageEnabled = false;
    }

    // ─── UTILITY ───

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
