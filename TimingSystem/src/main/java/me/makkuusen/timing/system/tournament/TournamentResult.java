package me.makkuusen.timing.system.tournament;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A player's result in a tournament.
 */
@Getter
@Setter
public class TournamentResult {

    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose private int id;
    @Expose private int tournamentId;
    @Expose private UUID playerUuid;
    @Expose private Map<Integer, Long> trackTimes = new HashMap<>();
    @Expose private long totalTimeMs;
    @Expose private int position;
    @Expose private boolean rewardClaimed;

    public TournamentResult() {}

    // ─── SERIALIZATION ───

    public String trackTimesToJson() {
        return GSON.toJson(trackTimes);
    }

    public static Map<Integer, Long> trackTimesFromJson(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        Type type = new TypeToken<Map<Integer, Long>>(){}.getType();
        return GSON.fromJson(json, type);
    }

    // ─── HELPERS ───

    public void updateTrackTime(int trackId, long timeMs) {
        Long existing = trackTimes.get(trackId);
        if (existing == null || timeMs < existing) {
            trackTimes.put(trackId, timeMs);
        }
        recalculateTotal();
    }

    public void recalculateTotal() {
        totalTimeMs = trackTimes.values().stream().mapToLong(Long::longValue).sum();
    }

    public boolean hasCompletedTrack(int trackId) {
        return trackTimes.containsKey(trackId);
    }

    public int getCompletedTrackCount() {
        return trackTimes.size();
    }
}
