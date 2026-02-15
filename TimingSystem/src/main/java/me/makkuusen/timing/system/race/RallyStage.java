package me.makkuusen.timing.system.race;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.track.Track;

/**
 * Represents one stage in a multi-stage rally event.
 */
@Getter
@Setter
public class RallyStage {

    private int stageIndex;
    private Track track;
    private String name;

    public RallyStage(int stageIndex, Track track, String name) {
        this.stageIndex = stageIndex;
        this.track = track;
        this.name = name;
    }
}
