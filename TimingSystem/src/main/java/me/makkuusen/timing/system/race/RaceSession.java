package me.makkuusen.timing.system.race;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.track.Track;

import java.time.Instant;
import java.util.*;

/**
 * Represents a single race session for a player.
 * Used to track state within a solo or multiplayer race.
 */
@Getter
@Setter
public class RaceSession {

    private final UUID playerUuid;
    private final Track track;
    private final RaceType raceType;
    private final String carType; // "SYSTEM" or "CUSTOM"
    private RaceState state;
    private Instant startTime;
    private Instant endTime;
    private int falseStartCount;

    // ─── FALSE START ───
    /** Penalty seconds accumulated from false starts. */
    private int falseStartPenaltySeconds;

    // ─── TIME CONTROLS ───
    /** Penalty seconds accumulated from time controls (late/early arrival). */
    private int timeControlPenaltySeconds;
    /** Set of time control region IDs already passed by this session. */
    private final Set<Integer> passedTimeControls = new HashSet<>();

    // ─── COUNTDOWN POSITION ───
    /** Player location at the start of countdown, used for false start detection. */
    private org.bukkit.Location countdownLocation;

    public RaceSession(UUID playerUuid, Track track, RaceType raceType, String carType) {
        this.playerUuid = playerUuid;
        this.track = track;
        this.raceType = raceType;
        this.carType = carType;
        this.state = RaceState.WAITING;
        this.falseStartCount = 0;
        this.falseStartPenaltySeconds = 0;
        this.timeControlPenaltySeconds = 0;
    }

    /**
     * Returns the elapsed race time in milliseconds, or 0 if not started.
     */
    public long getElapsedMs() {
        if (startTime == null) return 0;
        Instant end = endTime != null ? endTime : TimingSystem.currentTime;
        return java.time.Duration.between(startTime, end).toMillis();
    }

    /**
     * Returns total penalty in milliseconds (false start + time control penalties).
     */
    public long getTotalPenaltyMs() {
        return (falseStartPenaltySeconds + timeControlPenaltySeconds) * 1000L;
    }

    /**
     * Returns the adjusted race time (elapsed + penalties) in milliseconds.
     */
    public long getAdjustedTimeMs() {
        return getElapsedMs() + getTotalPenaltyMs();
    }

    /**
     * Whether this session belongs to the given player.
     */
    public boolean isPlayer(UUID uuid) {
        return playerUuid.equals(uuid);
    }

    /**
     * Whether the race is still active (not finished or cancelled).
     */
    public boolean isActive() {
        return state == RaceState.WAITING || state == RaceState.COUNTDOWN || state == RaceState.RACING;
    }

    /**
     * Records a false start and returns the penalty applied.
     */
    public FalseStartResult recordFalseStart() {
        falseStartCount++;
        if (falseStartCount >= 3) {
            return FalseStartResult.DISQUALIFIED;
        } else if (falseStartCount == 2) {
            falseStartPenaltySeconds += 60;
            return FalseStartResult.RESTART;
        } else {
            falseStartPenaltySeconds += 10;
            return FalseStartResult.PENALTY;
        }
    }

    /**
     * Checks if this time control region has already been passed.
     */
    public boolean hasPassedTimeControl(int regionId) {
        return passedTimeControls.contains(regionId);
    }

    /**
     * Marks a time control as passed and adds any penalty.
     */
    public void recordTimeControlPass(int regionId, int penaltySeconds) {
        passedTimeControls.add(regionId);
        timeControlPenaltySeconds += penaltySeconds;
    }
}
