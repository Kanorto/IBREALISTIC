package dev.o7moon.openboatutils.physics;

public class TireModel {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float MIN_SPEED = 1.0f;

    public static float computeSlipAngle(float vy, float vx, float yawRate, float axleDist, float steer) {
        float vxAbs = Math.max(Math.abs(vx), MIN_SPEED);
        float vyAxle = vy + yawRate * axleDist;
        return (float) Math.atan2(vyAxle, vxAbs) - steer;
    }

    /**
     * Computes lateral force using the Fiala/Brush tire model.
     * Reads friction parameters from the provided SurfaceProperties.
     */
    public static float computeLateralForce(float slipAngle, float fz, SurfaceProperties surface) {
        return computeLateralForce(slipAngle, fz, surface.muPeak, surface.muSlide, surface.corneringStiffness,
                surface.peakSlipAngleDeg, surface.slipAngleFalloff);
    }

    /**
     * Computes lateral force using the Fiala/Brush tire model with explicit friction parameters.
     * Thread-safe: does not read or modify any shared mutable state.
     * Use this overload when per-axle mu values differ from the base surface (e.g. after load sensitivity).
     */
    public static float computeLateralForce(float slipAngle, float fz, float muPeak, float muSlide,
                                             float corneringStiffness, float peakSlipAngleDeg, float slipAngleFalloff) {
        if (fz <= 0f) return 0f;

        float mu = muPeak;
        float cAlpha = corneringStiffness;

        if (cAlpha <= 0.01f || mu <= 0.01f) return 0f;

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
            float peakAngleRad = peakSlipAngleDeg * DEG_TO_RAD;
            float overPeak = Math.max(0f, absAlpha - peakAngleRad);
            float falloffMu = mu - (mu - muSlide) * Math.min(1.0f, overPeak * slipAngleFalloff);
            fy = -falloffMu * fz * Math.signum(slipAngle);
        }

        return fy;
    }

    /**
     * Computes longitudinal force. Reads muPeak from the provided SurfaceProperties.
     */
    public static float computeLongitudinalForce(float driveForce, float brakeForce,
                                                  float fz, SurfaceProperties surface, float vx) {
        return computeLongitudinalForce(driveForce, brakeForce, fz, surface.muPeak, vx);
    }

    /**
     * Computes longitudinal force with explicit muPeak parameter.
     * Thread-safe: does not read or modify any shared mutable state.
     */
    public static float computeLongitudinalForce(float driveForce, float brakeForce,
                                                  float fz, float muPeak, float vx) {
        if (fz <= 0f) return 0f;

        float maxFx = muPeak * fz;

        // Net longitudinal demand
        float fx = driveForce;
        if (brakeForce > 0f) {
            // Use smooth velocity ratio instead of signum to prevent oscillation near zero
            float brakeDir = vx / Math.max(Math.abs(vx), 0.5f);
            fx -= brakeForce * brakeDir;
        }

        // Clamp to available grip
        if (Math.abs(fx) > maxFx) {
            fx = maxFx * Math.signum(fx);
        }

        return fx;
    }

    public static final class FrictionCircleResult {
        public float fx;
        public float fy;
    }

    public static FrictionCircleResult applyFrictionCircle(float fx, float fy, float fz, float muPeak,
                                                            FrictionCircleResult result) {
        float maxForce = muPeak * fz;
        if (maxForce <= 0f) {
            result.fx = 0f;
            result.fy = 0f;
            return result;
        }

        float totalForce = (float) Math.sqrt(fx * fx + fy * fy);

        if (totalForce > maxForce) {
            float scale = maxForce / totalForce;
            fx *= scale;
            fy *= scale;
        }

        result.fx = fx;
        result.fy = fy;
        return result;
    }

    public static float computeEffectiveMu(float fz, float fzNominal, SurfaceProperties surface) {
        if (fzNominal <= 0f) return surface.muPeak;
        float ratio = fz / fzNominal;
        float muEff = surface.muPeak * (1.0f - surface.loadSensitivity * (ratio - 1.0f));
        return Math.max(0.01f, muEff);
    }

    public static float applyRelaxation(float currentForce, float targetForce,
                                         float speed, float dt, float relaxLength) {
        if (relaxLength <= 0f) return targetForce;
        float alpha = Math.min(1.0f, Math.abs(speed) * dt / relaxLength);
        return currentForce + (targetForce - currentForce) * alpha;
    }
}
