package dev.kanorto.ibrealistic.physics;

public class VehicleConfig {
    public float mass = 1190f;
    public float wheelbase = 2.53f;
    public float cgHeight = 0.45f;
    public float trackWidth = 1.55f;
    public float frontWeightBias = 0.55f;
    public float maxSteeringAngle = 0.50f;
    public float steeringSpeed = 1.4f;
    public float brakingForce = 8000f;
    public float engineForce = 5500f;
    /** Engine power in watts — limits drive force at high speed via F = min(engineForce, enginePower / v) */
    public float enginePower = 85000f;
    public float dragCoefficient = 0.35f;
    public float rollingResistance = 0.015f;
    public float brakeBias = 0.65f;
    public float engineBraking = 800f;
    public int substeps = 4;
    public float speedSteeringFactor = 0.004f;
    public float rollStiffnessRatioFront = 0.55f;
    public DrivetrainType drivetrain = DrivetrainType.AWD;

    // ─── STEERING RETURN (SELF-ALIGNING TORQUE) ───
    /** Rate at which steering passively returns to center when no input (rad/s, 0 = disabled) */
    public float steeringReturnRate = 1.5f;

    // ─── FOUR-WHEEL MODEL PARAMETERS ───
    /** AWD front/rear torque split (0.0 = full rear, 1.0 = full front, 0.5 = 50/50) */
    public float awdFrontSplit = 0.5f;
    /** Differential type for front axle */
    public DifferentialType frontDifferential = DifferentialType.OPEN;
    /** Differential type for rear axle */
    public DifferentialType rearDifferential = DifferentialType.OPEN;
    /** LSD locking coefficient (0.0 = open, 1.0 = locked) — used when differential is LSD */
    public float lsdLockingCoeff = 0.3f;

    // ─── AERODYNAMICS ───
    /** Downforce coefficient (Cl * A) — generates vertical load proportional to v² */
    public float downforceCoefficient = 0.5f;
    /** Downforce front/rear distribution (0.0 = all rear, 1.0 = all front, 0.5 = 50/50) */
    public float downforceFrontBias = 0.4f;

    // ─── COMPONENT PRESETS ───
    public TirePreset tirePreset = TirePreset.STANDARD;
    public SuspensionPreset suspensionPreset = SuspensionPreset.COMFORT;
    public EnginePreset enginePreset = EnginePreset.STOCK;
    public BodyPreset bodyPreset = BodyPreset.STANDARD;
    public SteeringPreset steeringPreset = SteeringPreset.STANDARD;
    public BrakePreset brakePreset = BrakePreset.STANDARD;
    public WeightDistributionPreset weightDistributionPreset = WeightDistributionPreset.BALANCED;

    // ─── EFFECTIVE VALUES (computed from base + presets) ───

    /** Returns effective mass after body preset multiplier */
    public float getEffectiveMass() {
        return mass * bodyPreset.massMultiplier;
    }

    /** Returns effective engine force after engine preset multiplier */
    public float getEffectiveEngineForce() {
        return engineForce * enginePreset.engineForceMultiplier;
    }

    /** Returns effective engine power after engine preset multiplier */
    public float getEffectiveEnginePower() {
        return enginePower * enginePreset.enginePowerMultiplier;
    }

    /** Returns effective engine braking after engine preset multiplier */
    public float getEffectiveEngineBraking() {
        return engineBraking * enginePreset.engineBrakingMultiplier;
    }

    /** Returns effective drag coefficient after body + engine preset multipliers */
    public float getEffectiveDragCoefficient() {
        return dragCoefficient * bodyPreset.dragMultiplier * enginePreset.dragMultiplier;
    }

    /** Returns effective downforce coefficient after body preset multiplier */
    public float getEffectiveDownforceCoefficient() {
        return downforceCoefficient * bodyPreset.downforceMultiplier;
    }

    /** Returns effective braking force after brake preset multiplier */
    public float getEffectiveBrakingForce() {
        return brakingForce * brakePreset.brakingForceMultiplier;
    }

    /** Returns effective brake bias from brake preset */
    public float getEffectiveBrakeBias() {
        return brakePreset.brakeBias;
    }

    /** Returns effective CG height after suspension preset multiplier */
    public float getEffectiveCgHeight() {
        return cgHeight * suspensionPreset.cgHeightMultiplier;
    }

    /** Returns effective roll stiffness ratio from suspension preset */
    public float getEffectiveRollStiffnessRatio() {
        return suspensionPreset.rollStiffnessRatio;
    }

    /** Returns effective max steering angle from steering preset (if set) or base value */
    public float getEffectiveMaxSteeringAngle() {
        if (steeringPreset != SteeringPreset.STANDARD) {
            return steeringPreset.maxSteeringAngle;
        }
        return maxSteeringAngle;
    }

    /** Returns effective steering speed from steering preset (if set) or base value */
    public float getEffectiveSteeringSpeed() {
        if (steeringPreset != SteeringPreset.STANDARD) {
            return steeringPreset.steeringSpeed;
        }
        return steeringSpeed;
    }

    /** Returns effective steering return rate from steering preset (if set) or base value */
    public float getEffectiveSteeringReturnRate() {
        if (steeringPreset != SteeringPreset.STANDARD) {
            return steeringPreset.steeringReturnRate;
        }
        return steeringReturnRate;
    }

    /** Returns effective speed steering factor from steering preset (if set) or base value */
    public float getEffectiveSpeedSteeringFactor() {
        if (steeringPreset != SteeringPreset.STANDARD) {
            return steeringPreset.speedSteeringFactor;
        }
        return speedSteeringFactor;
    }

    /** Returns effective front weight bias from weight distribution preset (if set) or base value */
    public float getEffectiveFrontWeightBias() {
        if (weightDistributionPreset != WeightDistributionPreset.BALANCED) {
            return weightDistributionPreset.frontWeightBias;
        }
        return frontWeightBias;
    }

    public float getFrontAxleDistance() {
        return wheelbase * getEffectiveFrontWeightBias();
    }

    public float getRearAxleDistance() {
        return wheelbase * (1.0f - getEffectiveFrontWeightBias());
    }

    public float getStaticFrontLoad() {
        return getEffectiveMass() * 9.81f * getEffectiveFrontWeightBias();
    }

    public float getStaticRearLoad() {
        return getEffectiveMass() * 9.81f * (1.0f - getEffectiveFrontWeightBias());
    }

    /** Half track width — distance from vehicle centerline to each wheel */
    public float getHalfTrack() {
        return trackWidth * 0.5f;
    }

    public static VehicleConfig createDefault() {
        return VehicleType.WRC_CAR.toConfig();
    }
}
