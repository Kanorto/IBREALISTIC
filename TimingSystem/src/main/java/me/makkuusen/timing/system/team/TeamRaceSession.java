package me.makkuusen.timing.system.team;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.track.Track;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a team race session. Tracks race time, pit stops, and penalties.
 */
@Getter
@Setter
public class TeamRaceSession {

    // ─── STATE ENUM ───
    public enum State {
        WAITING,
        COUNTDOWN,
        RACING,
        IN_PIT_STOP,
        FINISHED,
        CANCELLED
    }

    private final int teamId;
    private final UUID pilotUuid;
    private final Track track;
    private final int totalLaps;
    private final int requiredPitStops;
    private State state;
    private Instant startTime;
    private Instant endTime;
    private int currentLap;
    private int pitStopsCompleted;

    /** History of all pit stops in this race */
    private final List<PitStopResult> pitStopHistory = new ArrayList<>();

    /** Currently active pit stop session, or null */
    private PitStopSession activePitStop;

    /** Total pit time in milliseconds */
    private long totalPitTimeMs;

    /** Total penalty in milliseconds (from pit stop delays) */
    private long totalPenaltyMs;

    public TeamRaceSession(int teamId, UUID pilotUuid, Track track, int totalLaps, int requiredPitStops) {
        this.teamId = teamId;
        this.pilotUuid = pilotUuid;
        this.track = track;
        this.totalLaps = totalLaps;
        this.requiredPitStops = requiredPitStops;
        this.state = State.WAITING;
        this.currentLap = 0;
        this.pitStopsCompleted = 0;
        this.totalPitTimeMs = 0;
        this.totalPenaltyMs = 0;
    }

    /**
     * Returns elapsed race time in milliseconds.
     */
    public long getRaceTimeMs() {
        if (startTime == null) return 0;
        Instant end = endTime != null ? endTime : TimingSystem.currentTime;
        return Duration.between(startTime, end).toMillis();
    }

    /**
     * Returns total time = race time + pit time + penalties.
     */
    public long getTotalTimeMs() {
        return getRaceTimeMs() + totalPenaltyMs;
    }

    /**
     * Whether the race is still active.
     */
    public boolean isActive() {
        return state == State.WAITING || state == State.COUNTDOWN
                || state == State.RACING || state == State.IN_PIT_STOP;
    }

    /**
     * Whether more pit stops are required.
     */
    public boolean needsMorePitStops() {
        return pitStopsCompleted < requiredPitStops;
    }

    /**
     * Record a completed pit stop.
     */
    public void recordPitStop(long pitTimeMs, long penaltyMs) {
        pitStopsCompleted++;
        totalPitTimeMs += pitTimeMs;
        totalPenaltyMs += penaltyMs;
        pitStopHistory.add(new PitStopResult(pitTimeMs, penaltyMs));
    }

    /**
     * Increment lap counter.
     * @return true if this was the final lap
     */
    public boolean completeLap() {
        currentLap++;
        return currentLap >= totalLaps;
    }

    /**
     * Get average pit stop time in milliseconds.
     */
    public long getAveragePitTimeMs() {
        if (pitStopHistory.isEmpty()) return 0;
        long total = pitStopHistory.stream().mapToLong(PitStopResult::pitTimeMs).sum();
        return total / pitStopHistory.size();
    }

    /**
     * Get best (fastest) pit stop time in milliseconds.
     */
    public long getBestPitTimeMs() {
        return pitStopHistory.stream()
                .mapToLong(PitStopResult::pitTimeMs)
                .min()
                .orElse(0);
    }

    /**
     * Record for a single pit stop.
     */
    public record PitStopResult(long pitTimeMs, long penaltyMs) {}
}
