package dev.o7moon.openboatutils.physics;

public class VehicleConfig {
    public float mass = 1190f;
    public float wheelbase = 2.53f;
    public float cgHeight = 0.45f;
    public float trackWidth = 1.55f;
    public float frontWeightBias = 0.55f;
    public float maxSteeringAngle = 0.50f;
    public float steeringSpeed = 5.0f;
    public float brakingForce = 8000f;
    public float engineForce = 5500f;
    public float dragCoefficient = 0.35f;
    public float rollingResistance = 0.015f;
    public float brakeBias = 0.65f;
    public float engineBraking = 800f;
    public int substeps = 4;
    public float speedSteeringFactor = 0.004f;
    public float rollStiffnessRatioFront = 0.55f;
    public DrivetrainType drivetrain = DrivetrainType.AWD;

    public float getFrontAxleDistance() {
        return wheelbase * frontWeightBias;
    }

    public float getRearAxleDistance() {
        return wheelbase * (1.0f - frontWeightBias);
    }

    public float getStaticFrontLoad() {
        return mass * 9.81f * frontWeightBias;
    }

    public float getStaticRearLoad() {
        return mass * 9.81f * (1.0f - frontWeightBias);
    }

    public static VehicleConfig createDefault() {
        return VehicleType.WRC_CAR.toConfig();
    }
}
