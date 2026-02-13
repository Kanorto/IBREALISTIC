package dev.o7moon.openboatutils.physics;

/**
 * Differential types that control how torque is distributed between wheels on the same axle.
 */
public enum DifferentialType {
    OPEN(0),    // Torque goes to wheel with least resistance (realistic, prone to wheelspin)
    LOCKED(1),  // Both wheels on axle rotate at the same speed (maximum traction, understeer-prone)
    LSD(2);     // Limited Slip Differential: partial locking under torque difference

    public final int id;

    DifferentialType(int id) {
        this.id = id;
    }

    public static DifferentialType fromId(int id) {
        for (DifferentialType type : values()) {
            if (type.id == id) return type;
        }
        return OPEN;
    }
}
