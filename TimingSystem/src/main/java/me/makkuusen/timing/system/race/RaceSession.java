package me.makkuusen.timing.system.race;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.UUID;

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

    public RaceSession(UUID playerUuid, Track track, RaceType raceType, String carType) {
        this.playerUuid = playerUuid;
        this.track = track;
        this.raceType = raceType;
        this.carType = carType;
        this.state = RaceState.WAITING;
        this.falseStartCount = 0;
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
}
