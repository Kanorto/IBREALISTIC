package me.makkuusen.timing.system.tournament;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * A single match in a double-elimination bracket.
 */
@Getter
@Setter
public class BracketMatch {

    @Expose private int id;
    @Expose private int tournamentId;
    @Expose private int roundNumber;
    @Expose private int matchIndex;
    @Expose private boolean upperBracket;
    @Expose private UUID player1Uuid;
    @Expose private UUID player2Uuid;
    @Expose private UUID winnerUuid;
    @Expose private long player1TimeMs;
    @Expose private long player2TimeMs;
    @Expose private int trackId;
    @Expose private String state = "PENDING";

    public BracketMatch() {}

    // ─── STATES ───

    public boolean isPending() { return "PENDING".equals(state); }
    public boolean isActive() { return "ACTIVE".equals(state); }
    public boolean isCompleted() { return "COMPLETED".equals(state); }
    public boolean isBye() { return "BYE".equals(state); }

    // ─── HELPERS ───

    public boolean hasPlayer(UUID uuid) {
        return uuid.equals(player1Uuid) || uuid.equals(player2Uuid);
    }

    public UUID getOpponent(UUID uuid) {
        if (uuid.equals(player1Uuid)) return player2Uuid;
        if (uuid.equals(player2Uuid)) return player1Uuid;
        return null;
    }

    public boolean isFull() {
        return player1Uuid != null && player2Uuid != null;
    }

    /**
     * Determines the winner based on race times. Lower time wins.
     * Returns null if both times are 0 (not yet raced).
     */
    public UUID determineWinner() {
        if (player1TimeMs <= 0 && player2TimeMs <= 0) return null;
        if (player1TimeMs <= 0) return player2Uuid;
        if (player2TimeMs <= 0) return player1Uuid;
        return player1TimeMs <= player2TimeMs ? player1Uuid : player2Uuid;
    }

    public UUID determineLoser() {
        UUID winner = determineWinner();
        if (winner == null) return null;
        return winner.equals(player1Uuid) ? player2Uuid : player1Uuid;
    }
}
