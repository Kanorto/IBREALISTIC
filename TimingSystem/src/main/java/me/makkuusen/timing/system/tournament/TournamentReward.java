package me.makkuusen.timing.system.tournament;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;

/**
 * Reward configuration for a tournament position.
 */
@Getter
@Setter
public class TournamentReward {
    @Expose private int coins;
    @Expose private int xp;
    @Expose private String titleReward;

    public TournamentReward() {}

    public TournamentReward(int coins, int xp) {
        this.coins = coins;
        this.xp = xp;
        this.titleReward = null;
    }

    public TournamentReward(int coins, int xp, String titleReward) {
        this.coins = coins;
        this.xp = xp;
        this.titleReward = titleReward;
    }

    public boolean hasTitleReward() {
        return titleReward != null && !titleReward.isEmpty();
    }
}
