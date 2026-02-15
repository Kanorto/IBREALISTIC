package me.makkuusen.timing.system.race;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.track.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a reconnaissance (recce) session for track exploration.
 * No timing, reduced speed, allows creating pace notes.
 */
@Getter
@Setter
public class RecceSession {

    private final UUID playerUuid;
    private final Track track;
    /** Pace notes created by the player during this recce session. */
    private final List<String> paceNotes = new ArrayList<>();
    /** Whether this session is active. */
    private boolean active;
    /** Speed limit multiplier for recce mode (0.0-1.0, default 0.5 = 50% max speed). */
    private float speedMultiplier;

    public RecceSession(UUID playerUuid, Track track) {
        this.playerUuid = playerUuid;
        this.track = track;
        this.active = true;
        this.speedMultiplier = 0.5f;
    }

    public void addPaceNote(String note) {
        paceNotes.add(note);
    }

    public boolean isPlayer(UUID uuid) {
        return playerUuid.equals(uuid);
    }
}
