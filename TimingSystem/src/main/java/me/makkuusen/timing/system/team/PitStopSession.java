package me.makkuusen.timing.system.team;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks the state of a single pit stop during a team race.
 * Mechanics interact with hotbar items to complete tasks.
 */
@Getter
@Setter
public class PitStopSession {

    // ─── CONFIGURATION DEFAULTS ───
    public static final int DEFAULT_TIRE_CLICKS = 4;
    public static final int DEFAULT_REFUEL_TICKS = 5 * 20; // 5 seconds at 20 ticks/second
    public static final int DEFAULT_REPAIR_CLICKS = 6;
    public static final int DEFAULT_MIN_PITSTOP_SECONDS = 8;
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;
    public static final int DEFAULT_PENALTY_PER_EXTRA_SECOND = 2;
    public static final float BODY_DAMAGE_REPAIR_PER_CLICK = 0.15f;

    // ─── STATE ───
    private final UUID pilotUuid;
    private final int teamId;
    private final Instant startTime;

    /** Number of tire clicks completed (need DEFAULT_TIRE_CLICKS total) */
    private int tireClicksDone = 0;
    /** Refueling progress in ticks (need DEFAULT_REFUEL_TICKS total) */
    private int refuelTicksDone = 0;
    /** Number of repair clicks completed (need DEFAULT_REPAIR_CLICKS total) */
    private int repairClicksDone = 0;

    /** Whether all tasks are complete */
    private boolean allTasksComplete = false;
    /** Whether the pit stop has been released (mechanic pressed "done") */
    private boolean released = false;

    // ─── CONFIGURABLE LIMITS ───
    private int tireClicksRequired;
    private int refuelTicksRequired;
    private int repairClicksRequired;
    private int minPitstopSeconds;
    private int timeoutSeconds;
    private int penaltyPerExtraSecond;

    public PitStopSession(UUID pilotUuid, int teamId) {
        this.pilotUuid = pilotUuid;
        this.teamId = teamId;
        this.startTime = Instant.now();
        this.tireClicksRequired = DEFAULT_TIRE_CLICKS;
        this.refuelTicksRequired = DEFAULT_REFUEL_TICKS;
        this.repairClicksRequired = DEFAULT_REPAIR_CLICKS;
        this.minPitstopSeconds = DEFAULT_MIN_PITSTOP_SECONDS;
        this.timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        this.penaltyPerExtraSecond = DEFAULT_PENALTY_PER_EXTRA_SECOND;
    }

    /**
     * Record a tire change click.
     * @return true if all tires are now changed
     */
    public boolean clickTire() {
        if (tireClicksDone >= tireClicksRequired) return true;
        tireClicksDone++;
        updateAllTasksComplete();
        return tireClicksDone >= tireClicksRequired;
    }

    /**
     * Record refueling progress (called each tick while refueling).
     * @return true if refueling is complete
     */
    public boolean tickRefuel() {
        if (refuelTicksDone >= refuelTicksRequired) return true;
        refuelTicksDone++;
        updateAllTasksComplete();
        return refuelTicksDone >= refuelTicksRequired;
    }

    /**
     * Record a body repair click.
     * @return true if all repairs are done
     */
    public boolean clickRepair() {
        if (repairClicksDone >= repairClicksRequired) return true;
        repairClicksDone++;
        updateAllTasksComplete();
        return repairClicksDone >= repairClicksRequired;
    }

    /**
     * Check if tires are fully changed.
     */
    public boolean isTiresComplete() {
        return tireClicksDone >= tireClicksRequired;
    }

    /**
     * Check if refueling is complete.
     */
    public boolean isRefuelComplete() {
        return refuelTicksDone >= refuelTicksRequired;
    }

    /**
     * Check if repairs are complete.
     */
    public boolean isRepairComplete() {
        return repairClicksDone >= repairClicksRequired;
    }

    private void updateAllTasksComplete() {
        allTasksComplete = isTiresComplete() && isRefuelComplete() && isRepairComplete();
    }

    /**
     * Get elapsed time in milliseconds since pit stop started.
     */
    public long getElapsedMs() {
        return java.time.Duration.between(startTime, Instant.now()).toMillis();
    }

    /**
     * Get elapsed time in seconds since pit stop started.
     */
    public int getElapsedSeconds() {
        return (int) (getElapsedMs() / 1000);
    }

    /**
     * Calculate penalty milliseconds for exceeding minimum time.
     * Penalty only applies if the pit stop takes longer than minPitstopSeconds.
     */
    public long calculatePenaltyMs() {
        int elapsed = getElapsedSeconds();
        if (elapsed <= minPitstopSeconds) return 0;
        int extraSeconds = elapsed - minPitstopSeconds;
        return (long) extraSeconds * penaltyPerExtraSecond * 1000L;
    }

    /**
     * Get refuel progress as a fraction (0.0 to 1.0).
     */
    public float getRefuelProgress() {
        if (refuelTicksRequired <= 0) return 1.0f;
        return Math.min(1.0f, (float) refuelTicksDone / refuelTicksRequired);
    }

    /**
     * Get overall pit stop progress as a fraction (0.0 to 1.0).
     */
    public float getOverallProgress() {
        float tireProgress = tireClicksRequired > 0
                ? (float) tireClicksDone / tireClicksRequired : 1.0f;
        float refuelProg = getRefuelProgress();
        float repairProgress = repairClicksRequired > 0
                ? (float) repairClicksDone / repairClicksRequired : 1.0f;
        return (tireProgress + refuelProg + repairProgress) / 3.0f;
    }
}
