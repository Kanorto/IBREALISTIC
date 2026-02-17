package me.makkuusen.timing.system.team;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Represents a member of a team with a specific role.
 */
@Getter
@Setter
public class TeamMember {

    private final UUID uuid;
    private TeamRole role;
    private final long joinedAt;

    public TeamMember(UUID uuid, TeamRole role, long joinedAt) {
        this.uuid = uuid;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    /**
     * Check if this member is the pilot.
     */
    public boolean isPilot() {
        return role == TeamRole.PILOT;
    }

    /**
     * Check if this member is a mechanic.
     */
    public boolean isMechanic() {
        return role == TeamRole.MECHANIC;
    }
}
