package me.makkuusen.timing.system.race;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks a participant's progress through a multi-stage rally event.
 */
@Getter
@Setter
public class RallyParticipant {

    private static final long SUPER_RALLY_PENALTY_MS = 5 * 60 * 1000L;

    private final UUID playerUuid;
    private final int rallyId;
    private final Map<Integer, Long> stageTimes;
    private int currentStageIndex;
    private boolean retired;
    private long totalPenaltyMs;

    public RallyParticipant(UUID playerUuid, int rallyId) {
        this.playerUuid = playerUuid;
        this.rallyId = rallyId;
        this.stageTimes = new HashMap<>();
        this.currentStageIndex = 0;
        this.retired = false;
        this.totalPenaltyMs = 0;
    }

    /**
     * Returns the fixed super rally penalty (5 minutes) applied when a
     * participant retires from a stage instead of finishing normally.
     */
    public long getSuperRallyPenaltyMs() {
        return SUPER_RALLY_PENALTY_MS;
    }

    /**
     * Returns the total time: sum of all stage times plus accumulated penalties.
     */
    public long getTotalTimeMs() {
        long stageTotal = 0;
        for (long time : stageTimes.values()) {
            stageTotal += time;
        }
        return stageTotal + totalPenaltyMs;
    }

    public void recordStageTime(int stageIndex, long timeMs) {
        stageTimes.put(stageIndex, timeMs);
    }

    public int getCompletedStages() {
        return stageTimes.size();
    }
}
