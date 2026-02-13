package me.makkuusen.timing.system.race;

/**
 * State machine for a race session.
 */
public enum RaceState {
    /** Waiting for players to join. */
    WAITING,
    /** Countdown in progress. */
    COUNTDOWN,
    /** Race is active. */
    RACING,
    /** Race completed. */
    FINISHED,
    /** Race cancelled. */
    CANCELLED
}
