package dev.o7moon.openboatutils.physics;

public enum DrivetrainType {
    RWD(0),   // Rear-wheel drive: agile, oversteer-prone, classic rally feel
    FWD(1),   // Front-wheel drive: understeer-prone, traction advantage uphill
    AWD(2);   // All-wheel drive: best traction, modern WRC standard

    public final int id;

    DrivetrainType(int id) {
        this.id = id;
    }

    public static DrivetrainType fromId(int id) {
        for (DrivetrainType type : values()) {
            if (type.id == id) return type;
        }
        return AWD;
    }

    // How much of the drive force goes to the front axle (0.0 = pure RWD, 1.0 = pure FWD)
    public float getFrontDriveRatio() {
        return switch (this) {
            case RWD -> 0.0f;
            case FWD -> 1.0f;
            case AWD -> 0.5f;
        };
    }
}
