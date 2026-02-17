package me.makkuusen.timing.system.team;

/**
 * Specific pit stop tasks that can be assigned to team members.
 * Each mechanic can have one or more tasks assigned by the team leader.
 */
public enum PitTask {
    /** Change front-left tire — mechanic must stand near FL wheel */
    TIRES_FL,
    /** Change front-right tire — mechanic must stand near FR wheel */
    TIRES_FR,
    /** Change rear-left tire — mechanic must stand near RL wheel */
    TIRES_RL,
    /** Change rear-right tire — mechanic must stand near RR wheel */
    TIRES_RR,
    /** Refuel the car — mechanic must stand near the rear */
    REFUEL,
    /** Repair body damage — mechanic can stand anywhere near the car */
    REPAIR;

    /**
     * Parse a pit task from string (case-insensitive).
     * @param value the string to parse
     * @return the PitTask, or null if invalid
     */
    public static PitTask fromString(String value) {
        if (value == null) return null;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Check if this task is a tire-related task.
     */
    public boolean isTireTask() {
        return this == TIRES_FL || this == TIRES_FR || this == TIRES_RL || this == TIRES_RR;
    }

    /**
     * Get the tire index (0=FL, 1=FR, 2=RL, 3=RR) or -1 if not a tire task.
     */
    public int getTireIndex() {
        return switch (this) {
            case TIRES_FL -> 0;
            case TIRES_FR -> 1;
            case TIRES_RL -> 2;
            case TIRES_RR -> 3;
            default -> -1;
        };
    }

    /**
     * Get a user-friendly display name.
     */
    public String getDisplayName() {
        return switch (this) {
            case TIRES_FL -> "Front-Left Tire";
            case TIRES_FR -> "Front-Right Tire";
            case TIRES_RL -> "Rear-Left Tire";
            case TIRES_RR -> "Rear-Right Tire";
            case REFUEL -> "Refuel";
            case REPAIR -> "Body Repair";
        };
    }
}
