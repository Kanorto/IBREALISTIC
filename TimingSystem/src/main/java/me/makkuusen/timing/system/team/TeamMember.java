package me.makkuusen.timing.system.team;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Represents a member of a team with a specific role and assigned pit tasks.
 */
@Getter
@Setter
public class TeamMember {

    private final UUID uuid;
    private TeamRole role;
    private final long joinedAt;
    /** Set of pit tasks assigned to this member by the team leader */
    private final Set<PitTask> assignedTasks = new HashSet<>();

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

    /**
     * Assign a pit task to this member.
     */
    public void assignTask(PitTask task) {
        assignedTasks.add(task);
    }

    /**
     * Remove a pit task from this member.
     */
    public void removeTask(PitTask task) {
        assignedTasks.remove(task);
    }

    /**
     * Check if this member has a specific task assigned.
     */
    public boolean hasTask(PitTask task) {
        return assignedTasks.contains(task);
    }

    /**
     * Get assigned tasks as a comma-separated string.
     */
    public String getTasksDisplay() {
        if (assignedTasks.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        for (PitTask task : assignedTasks) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(task.getDisplayName());
        }
        return sb.toString();
    }

    /**
     * Encode assigned tasks as a string for database storage.
     * Format: "TIRES_FL,REFUEL,REPAIR"
     */
    public String encodeTasksForDb() {
        if (assignedTasks.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (PitTask task : assignedTasks) {
            if (!sb.isEmpty()) sb.append(",");
            sb.append(task.name());
        }
        return sb.toString();
    }

    /**
     * Load assigned tasks from a database string.
     */
    public void loadTasksFromDb(String encoded) {
        assignedTasks.clear();
        if (encoded == null || encoded.isEmpty()) return;
        for (String s : encoded.split(",")) {
            PitTask task = PitTask.fromString(s.trim());
            if (task != null) {
                assignedTasks.add(task);
            }
        }
    }
}
