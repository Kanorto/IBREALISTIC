package dev.o7moon.openboatutils.physics;

public enum VehicleType {
    WRC_CAR(1190f, 2.53f, 0.45f, 1.55f, 0.55f, 0.50f, 5.0f, 8000f, 5500f, 0.35f, 0.015f, 0.65f, DrivetrainType.AWD),
    GROUP_B(1100f, 2.40f, 0.50f, 1.50f, 0.45f, 0.48f, 4.5f, 7500f, 6000f, 0.32f, 0.014f, 0.60f, DrivetrainType.RWD),
    CLASSIC_RALLY(1000f, 2.45f, 0.55f, 1.45f, 0.50f, 0.45f, 4.0f, 6000f, 4000f, 0.38f, 0.018f, 0.65f, DrivetrainType.RWD),
    LIGHTWEIGHT(800f, 2.30f, 0.42f, 1.40f, 0.60f, 0.55f, 6.0f, 5500f, 3000f, 0.30f, 0.012f, 0.70f, DrivetrainType.FWD),
    TRUCK(2000f, 3.20f, 0.90f, 1.80f, 0.50f, 0.35f, 3.0f, 10000f, 8000f, 0.45f, 0.025f, 0.60f, DrivetrainType.AWD);

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
        return config;
    }
}
