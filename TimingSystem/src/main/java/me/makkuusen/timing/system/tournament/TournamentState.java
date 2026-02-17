package me.makkuusen.timing.system.tournament;

/**
 * Lifecycle states of a tournament.
 */
public enum TournamentState {
    SCHEDULED,
    QUALIFYING,
    ACTIVE,
    CALCULATING,
    FINISHED,
    ARCHIVED,
    CANCELLED
}
