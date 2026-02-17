package me.makkuusen.timing.system.team;

/**
 * Roles within a team for team racing.
 * PILOT drives the car, MECHANIC performs pit stop tasks.
 */
public enum TeamRole {
    PILOT,
    MECHANIC;

    /**
     * Parse a role from string (case-insensitive).
     * @param value the string to parse
     * @return the TeamRole, or null if invalid
     */
    public static TeamRole fromString(String value) {
        if (value == null) return null;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
