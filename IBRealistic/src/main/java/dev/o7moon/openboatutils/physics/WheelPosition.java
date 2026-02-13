package dev.o7moon.openboatutils.physics;

/**
 * Represents the four wheel positions on a vehicle.
 */
public enum WheelPosition {
    FRONT_LEFT(0),
    FRONT_RIGHT(1),
    REAR_LEFT(2),
    REAR_RIGHT(3);

    public final int index;

    WheelPosition(int index) {
        this.index = index;
    }

    public boolean isFront() {
        return this == FRONT_LEFT || this == FRONT_RIGHT;
    }

    public boolean isLeft() {
        return this == FRONT_LEFT || this == REAR_LEFT;
    }
}
