package me.makkuusen.timing.system.tournament;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;

/**
 * A competitive season spanning multiple tournaments.
 */
@Getter
@Setter
public class Season {

    @Expose private int id;
    @Expose private String name;
    @Expose private long startTimestamp;
    @Expose private long endTimestamp;
    @Expose private String state = "ACTIVE";

    public Season() {}

    public boolean isActive() {
        return "ACTIVE".equals(state);
    }

    public boolean isFinished() {
        return "FINISHED".equals(state);
    }

    public long getRemainingMs() {
        return Math.max(0, endTimestamp - System.currentTimeMillis());
    }
}
