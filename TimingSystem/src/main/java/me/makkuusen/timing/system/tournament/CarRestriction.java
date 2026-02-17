package me.makkuusen.timing.system.tournament;

/**
 * Restrictions on which car types are allowed in a tournament.
 */
public enum CarRestriction {
    ALL("All Cars"),
    SYSTEM_ONLY("System Cars Only"),
    CUSTOM_ONLY("Custom Cars Only");

    private final String displayName;

    CarRestriction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
