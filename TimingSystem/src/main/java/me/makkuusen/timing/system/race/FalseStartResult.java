package me.makkuusen.timing.system.race;

/**
 * Result of a false start detection.
 */
public enum FalseStartResult {
    /** First offense: +10 second penalty. */
    PENALTY,
    /** Second offense: +60 second penalty + automatic restart. */
    RESTART,
    /** Third offense: disqualification. */
    DISQUALIFIED
}
