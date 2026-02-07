package dev.o7moon.openboatutils.physics;

/**
 * Simplified Fiala/Brush tire model with combined slip and friction circle.
 * Computes lateral and longitudinal forces based on slip angle, slip ratio,
 * vertical load, and surface properties.
 */
public class TireModel {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float MIN_SPEED = 0.5f;

    /**
     * Compute slip angle for an axle.
     *
     * @param vy       lateral velocity (m/s)
     * @param vx       longitudinal velocity (m/s)
     * @param yawRate  yaw rate (rad/s)
     * @param axleDist distance from CG to axle (positive = forward for front)
     * @param steer    steering angle (rad), 0 for rear axle
     * @return slip angle in radians
     */
    public static float computeSlipAngle(float vy, float vx, float yawRate, float axleDist, float steer) {
        float vxAbs = Math.max(Math.abs(vx), MIN_SPEED);
        float vyAxle = vy + yawRate * axleDist;
        return (float) Math.atan2(vyAxle, vxAbs) - steer;
    }

    /**
     * Compute lateral tire force using Fiala/Brush model.
     * Handles linear region, transition, and saturation with progressive falloff.
     *
     * @param slipAngle slip angle (rad)
     * @param fz        vertical load (N)
     * @param surface   surface properties
     * @return lateral force (N), negative = force opposing slip
     */
    public static float computeLateralForce(float slipAngle, float fz, SurfaceProperties surface) {
        if (fz <= 0f) return 0f;

        float mu = surface.muPeak;
        float cAlpha = surface.corneringStiffness;
        float tanAlpha = (float) Math.tan(slipAngle);
        float absTanAlpha = Math.abs(tanAlpha);

        // Slide angle threshold (where tire saturates)
        float alphaSlide = (float) Math.atan(3.0f * mu * fz / cAlpha);
        float absAlpha = Math.abs(slipAngle);

        float fy;
        if (absAlpha < alphaSlide) {
            // Fiala brush model: cubic polynomial
            float term1 = -cAlpha * tanAlpha;
            float term2 = (cAlpha * cAlpha / (3.0f * mu * fz)) * absTanAlpha * tanAlpha;
            float term3 = -(cAlpha * cAlpha * cAlpha / (27.0f * mu * mu * fz * fz)) * tanAlpha * tanAlpha * tanAlpha;
            fy = term1 + term2 + term3;
        } else {
            // Full sliding: force = mu * Fz, with progressive falloff past peak
            float peakAngleRad = surface.peakSlipAngleDeg * DEG_TO_RAD;
            float overPeak = Math.max(0f, absAlpha - peakAngleRad);
            float falloffMu = mu - (mu - surface.muSlide) * Math.min(1.0f, overPeak * surface.slipAngleFalloff);
            fy = -falloffMu * fz * Math.signum(slipAngle);
        }

        return fy;
    }

    /**
     * Compute longitudinal tire force (traction/braking).
     *
     * @param driveForce requested drive force (N)
     * @param brakeForce requested brake force (N)
     * @param fz         vertical load (N)
     * @param surface    surface properties
     * @param vx         longitudinal velocity for direction
     * @return longitudinal force (N)
     */
    public static float computeLongitudinalForce(float driveForce, float brakeForce,
                                                  float fz, SurfaceProperties surface, float vx) {
        if (fz <= 0f) return 0f;

        float maxFx = surface.muPeak * fz;

        // Net longitudinal demand
        float fx = driveForce;
        if (brakeForce > 0f) {
            fx -= brakeForce * Math.signum(vx);
        }

        // Clamp to available grip
        if (Math.abs(fx) > maxFx) {
            fx = maxFx * Math.signum(fx);
        }

        return fx;
    }

    /**
     * Apply friction circle/ellipse constraint.
     * Limits combined lateral and longitudinal forces to available grip.
     *
     * @param fx       longitudinal force (N)
     * @param fy       lateral force (N)
     * @param fz       vertical load (N)
     * @param surface  surface properties
     * @return float[2] = {fx_limited, fy_limited}
     */
    public static float[] applyFrictionCircle(float fx, float fy, float fz, SurfaceProperties surface) {
        float maxForce = surface.muPeak * fz;
        if (maxForce <= 0f) return new float[]{0f, 0f};

        float totalForce = (float) Math.sqrt(fx * fx + fy * fy);

        if (totalForce > maxForce) {
            float scale = maxForce / totalForce;
            fx *= scale;
            fy *= scale;
        }

        return new float[]{fx, fy};
    }

    /**
     * Apply load sensitivity: effective mu decreases with higher vertical load.
     * mu_eff = mu_base * (1 - sensitivity * (Fz/Fz_nominal - 1))
     *
     * @param fz         actual vertical load (N)
     * @param fzNominal  nominal (static) vertical load (N)
     * @param surface    surface properties
     * @return effective mu
     */
    public static float computeEffectiveMu(float fz, float fzNominal, SurfaceProperties surface) {
        if (fzNominal <= 0f) return surface.muPeak;
        float ratio = fz / fzNominal;
        float muEff = surface.muPeak * (1.0f - surface.loadSensitivity * (ratio - 1.0f));
        return Math.max(0.01f, muEff);
    }

    /**
     * Apply relaxation length filter (first-order lag) to tire force.
     * Smooths force transitions for realistic feel.
     *
     * @param currentForce  current applied force
     * @param targetForce   steady-state target force
     * @param speed         vehicle speed (m/s)
     * @param dt            time step (s)
     * @param relaxLength   relaxation length (m)
     * @return new force value after relaxation
     */
    public static float applyRelaxation(float currentForce, float targetForce,
                                         float speed, float dt, float relaxLength) {
        if (relaxLength <= 0f) return targetForce;
        float alpha = Math.min(1.0f, Math.abs(speed) * dt / relaxLength);
        return currentForce + (targetForce - currentForce) * alpha;
    }
}
