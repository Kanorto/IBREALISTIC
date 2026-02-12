package dev.o7moon.openboatutils.physics;

public enum VehicleType {
    WRC_CAR(1190f, 2.53f, 0.45f, 1.55f, 0.55f, 0.50f, 2.5f, 8000f, 5500f, 0.35f, 0.015f, 0.65f, DrivetrainType.AWD),
    GROUP_B(1100f, 2.40f, 0.50f, 1.50f, 0.45f, 0.48f, 2.2f, 7500f, 6000f, 0.32f, 0.014f, 0.60f, DrivetrainType.RWD),
    CLASSIC_RALLY(1000f, 2.45f, 0.55f, 1.45f, 0.50f, 0.45f, 1.8f, 6000f, 4000f, 0.38f, 0.018f, 0.65f, DrivetrainType.RWD),
    LIGHTWEIGHT(800f, 2.30f, 0.42f, 1.40f, 0.60f, 0.55f, 3.0f, 5500f, 3000f, 0.30f, 0.012f, 0.70f, DrivetrainType.FWD),
    TRUCK(2000f, 3.20f, 0.90f, 1.80f, 0.50f, 0.35f, 1.5f, 10000f, 8000f, 0.45f, 0.025f, 0.60f, DrivetrainType.AWD);

    public final float mass;
    public final float wheelbase;
    public final float cgHeight;
    public final float trackWidth;
    public final float frontWeightBias;
    public final float maxSteeringAngle;
    public final float steeringSpeed;
    public final float brakingForce;
    public final float engineForce;
    public final float dragCoefficient;
    public final float rollingResistance;
    public final float brakeBias;
    public final DrivetrainType drivetrain;

    VehicleType(float mass, float wheelbase, float cgHeight, float trackWidth,
                float frontWeightBias, float maxSteeringAngle, float steeringSpeed,
                float brakingForce, float engineForce, float dragCoefficient,
                float rollingResistance, float brakeBias, DrivetrainType drivetrain) {
        this.mass = mass;
        this.wheelbase = wheelbase;
        this.cgHeight = cgHeight;
        this.trackWidth = trackWidth;
        this.frontWeightBias = frontWeightBias;
        this.maxSteeringAngle = maxSteeringAngle;
        this.steeringSpeed = steeringSpeed;
        this.brakingForce = brakingForce;
        this.engineForce = engineForce;
        this.dragCoefficient = dragCoefficient;
        this.rollingResistance = rollingResistance;
        this.brakeBias = brakeBias;
        this.drivetrain = drivetrain;
    }

    public VehicleConfig toConfig() {
        VehicleConfig config = new VehicleConfig();
        config.mass = this.mass;
        config.wheelbase = this.wheelbase;
        config.cgHeight = this.cgHeight;
        config.trackWidth = this.trackWidth;
        config.frontWeightBias = this.frontWeightBias;
        config.maxSteeringAngle = this.maxSteeringAngle;
        config.steeringSpeed = this.steeringSpeed;
        config.brakingForce = this.brakingForce;
        config.engineForce = this.engineForce;
        config.dragCoefficient = this.dragCoefficient;
        config.rollingResistance = this.rollingResistance;
        config.brakeBias = this.brakeBias;
        config.drivetrain = this.drivetrain;

        // ─── PER-VEHICLE DIFFERENTIAL AND TUNING ───
        switch (this) {
            case WRC_CAR:
                // Modern WRC: LSD front/rear, active center diff, slight rear bias
                config.frontDifferential = DifferentialType.LSD;
                config.rearDifferential = DifferentialType.LSD;
                config.lsdLockingCoeff = 0.4f;
                config.awdFrontSplit = 0.45f;
                config.engineBraking = 900f;
                config.downforceCoefficient = 0.6f;
                config.downforceFrontBias = 0.45f;
                break;
            case GROUP_B:
                // RWD beast: locked rear diff for maximum traction, high engine braking
                config.frontDifferential = DifferentialType.OPEN;
                config.rearDifferential = DifferentialType.LSD;
                config.lsdLockingCoeff = 0.6f;
                config.engineBraking = 1200f;
                config.downforceCoefficient = 0.4f;
                config.downforceFrontBias = 0.35f;
                break;
            case CLASSIC_RALLY:
                // Old school: open diffs, softer engine braking
                config.frontDifferential = DifferentialType.OPEN;
                config.rearDifferential = DifferentialType.OPEN;
                config.lsdLockingCoeff = 0.0f;
                config.engineBraking = 600f;
                config.downforceCoefficient = 0.2f;
                config.downforceFrontBias = 0.5f;
                break;
            case LIGHTWEIGHT:
                // FWD hatchback: LSD front for traction, no rear drive
                config.frontDifferential = DifferentialType.LSD;
                config.rearDifferential = DifferentialType.OPEN;
                config.lsdLockingCoeff = 0.35f;
                config.engineBraking = 500f;
                config.downforceCoefficient = 0.3f;
                config.downforceFrontBias = 0.5f;
                break;
            case TRUCK:
                // Heavy AWD: locked diffs for off-road, high engine braking
                config.frontDifferential = DifferentialType.LOCKED;
                config.rearDifferential = DifferentialType.LOCKED;
                config.lsdLockingCoeff = 1.0f;
                config.awdFrontSplit = 0.5f;
                config.engineBraking = 1500f;
                config.downforceCoefficient = 0.3f;
                config.downforceFrontBias = 0.5f;
                break;
        }

        return config;
    }
}
