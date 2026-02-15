package me.makkuusen.timing.system.race;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.track.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a multi-stage rally event with ordered stages.
 */
@Getter
@Setter
public class RallyEvent {

    private int id;
    private String name;
    private UUID creatorUuid;
    private List<RallyStage> stages;
    private RallyEventState state;
    private int currentStageIndex;

    public RallyEvent(int id, String name, UUID creatorUuid) {
        this.id = id;
        this.name = name;
        this.creatorUuid = creatorUuid;
        this.stages = new ArrayList<>();
        this.state = RallyEventState.SETUP;
        this.currentStageIndex = 0;
    }

    public void addStage(Track track) {
        int index = stages.size();
        String stageName = "SS" + (index + 1);
        stages.add(new RallyStage(index, track, stageName));
    }

    public boolean removeStage(int index) {
        if (index < 0 || index >= stages.size()) {
            return false;
        }
        stages.remove(index);
        // Re-index remaining stages
        for (int i = 0; i < stages.size(); i++) {
            stages.get(i).setStageIndex(i);
            stages.get(i).setName("SS" + (i + 1));
        }
        return true;
    }

    public RallyStage getStage(int index) {
        if (index < 0 || index >= stages.size()) {
            return null;
        }
        return stages.get(index);
    }

    public int getTotalStages() {
        return stages.size();
    }

    public RallyStage getCurrentStage() {
        return getStage(currentStageIndex);
    }

    public boolean hasNextStage() {
        return currentStageIndex + 1 < stages.size();
    }

    public void advanceStage() {
        currentStageIndex++;
    }
}
