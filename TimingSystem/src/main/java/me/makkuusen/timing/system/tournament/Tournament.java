package me.makkuusen.timing.system.tournament;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object representing a tournament.
 */
@Getter
@Setter
public class Tournament {

    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose private int id;
    @Expose private String name;
    @Expose private TournamentType type;
    @Expose private TournamentState state;
    @Expose private long startTimestamp;
    @Expose private long endTimestamp;
    @Expose private List<Integer> trackIds = new ArrayList<>();
    @Expose private CarRestriction carRestriction = CarRestriction.ALL;
    @Expose private int seasonId;
    @Expose private int minPlayers = 3;
    @Expose private int minDifficulty = 1;
    @Expose private int maxDifficulty = 5;
    @Expose private Map<Integer, TournamentReward> rewards = new HashMap<>();

    public Tournament() {}

    // ─── SERIALIZATION ───

    public String trackIdsToJson() {
        return GSON.toJson(trackIds);
    }

    public static List<Integer> trackIdsFromJson(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        Type type = new TypeToken<List<Integer>>(){}.getType();
        return GSON.fromJson(json, type);
    }

    public String rewardsToJson() {
        return GSON.toJson(rewards);
    }

    public static Map<Integer, TournamentReward> rewardsFromJson(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        Type type = new TypeToken<Map<Integer, TournamentReward>>(){}.getType();
        return GSON.fromJson(json, type);
    }

    // ─── HELPERS ───

    public boolean isActive() {
        return state == TournamentState.ACTIVE || state == TournamentState.QUALIFYING;
    }

    public boolean isFinished() {
        return state == TournamentState.FINISHED || state == TournamentState.ARCHIVED;
    }

    public boolean isCancelled() {
        return state == TournamentState.CANCELLED;
    }

    public long getRemainingMs() {
        return Math.max(0, endTimestamp - System.currentTimeMillis());
    }

    public TournamentReward getRewardForPosition(int position) {
        return rewards.get(position);
    }

    /**
     * Default reward configuration for standard tournaments.
     */
    public static Map<Integer, TournamentReward> defaultRewards() {
        Map<Integer, TournamentReward> rewards = new HashMap<>();
        rewards.put(1, new TournamentReward(500, 1000));
        rewards.put(2, new TournamentReward(300, 600));
        rewards.put(3, new TournamentReward(200, 400));
        rewards.put(0, new TournamentReward(50, 100)); // participation
        return rewards;
    }
}
