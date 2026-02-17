package me.makkuusen.timing.system.team;

import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.tplayer.TPlayer;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class Team implements Comparable<Team> {
    private final int id;
    private String name;
    private final List<TPlayer> players;
    private final long dateCreated;
    private final UUID creator;
    @Setter
    private int maxMembers = 4; // 1 pilot + 3 mechanics
    private boolean playersLoaded = false;

    // ─── ROLE TRACKING ───
    /** Map of player UUID to TeamMember with role info */
    private final Map<UUID, TeamMember> members = new HashMap<>();

    /**
     * Constructor for creating a Team from database data
     */
    public Team(DbRow data) {
        this.id = data.getInt("id");
        this.name = data.getString("name");
        this.dateCreated = data.getLong("dateCreated");
        this.creator = UUID.fromString(data.getString("creator"));
        this.players = new ArrayList<>();
        Integer maxMem = data.get("maxMembers");
        if (maxMem != null) {
            this.maxMembers = maxMem;
        }
    }

    /**
     * Constructor for creating a new Team
     */
    public Team(int id, String name, UUID creator, long dateCreated) {
        this.id = id;
        this.name = name;
        this.creator = creator;
        this.dateCreated = dateCreated;
        this.players = new ArrayList<>();
    }

    /**
     * Add a player to the team
     * @param player The player to add
     * @return true if player was added, false if already in team
     */
    public boolean addPlayer(TPlayer player) {
        if (hasPlayer(player)) {
            return false;
        }
        return players.add(player);
    }

    /**
     * Remove a player from the team
     * @param player The player to remove
     * @return true if player was removed, false if not in team
     */
    public boolean removePlayer(TPlayer player) {
        return players.remove(player);
    }

    /**
     * Check if a player is in the team
     * @param player The player to check
     * @return true if player is in team
     */
    public boolean hasPlayer(TPlayer player) {
        return players.contains(player);
    }

    /**
     * Mark that players have been loaded from database
     */
    public void setPlayersLoaded(boolean loaded) {
        this.playersLoaded = loaded;
    }

    /**
     * Check if players have been loaded from database
     * @return true if players are loaded
     */
    public boolean arePlayersLoaded() {
        return playersLoaded;
    }

    /**
     * Clear the players list and mark as not loaded
     */
    public void clearPlayers() {
        players.clear();
        members.clear();
        playersLoaded = false;
    }

    /**
     * Get the number of players in the team
     * @return player count
     */
    public int getPlayerCount() {
        return players.size();
    }

    /**
     * Check if the team is empty
     * @return true if no players in team
     */
    public boolean isEmpty() {
        return players.isEmpty();
    }

    /**
     * Get the display name of the team
     * @return team name
     */
    public String getDisplayName() {
        return name;
    }

    /**
     * Get the creator of the team as a TPlayer
     * @return TPlayer who created the team, or null if not found
     */
    public TPlayer getCreator() {
        return TSDatabase.getPlayer(creator);
    }

    /**
     * Set the team name
     * @param name new team name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Validate team name
     * @param name team name to validate
     * @return true if valid
     */
    public static boolean isValidTeamName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // Check length (max 32 characters)
        if (name.length() > 32) {
            return false;
        }
        
        // Check for special characters that could interfere with commands
        // Allow letters, numbers, spaces, hyphens, and underscores
        return name.matches("^[a-zA-Z0-9\\s\\-_]+$");
    }

    // ─── ROLE MANAGEMENT ───

    /**
     * Add a member with a specific role.
     */
    public void addMember(UUID uuid, TeamRole role, long joinedAt) {
        members.put(uuid, new TeamMember(uuid, role, joinedAt));
    }

    /**
     * Get the role of a player.
     * @return TeamRole or null if not a member
     */
    public TeamRole getMemberRole(UUID uuid) {
        TeamMember member = members.get(uuid);
        return member != null ? member.getRole() : null;
    }

    /**
     * Set the role of a member.
     */
    public void setMemberRole(UUID uuid, TeamRole role) {
        TeamMember member = members.get(uuid);
        if (member != null) {
            member.setRole(role);
        }
    }

    /**
     * Check if the team has at least one pilot.
     */
    public boolean hasPilot() {
        return members.values().stream().anyMatch(TeamMember::isPilot);
    }

    /**
     * Get the pilot UUID, or null if no pilot assigned.
     */
    public UUID getPilotUuid() {
        return members.values().stream()
                .filter(TeamMember::isPilot)
                .map(TeamMember::getUuid)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all mechanic UUIDs.
     */
    public List<UUID> getMechanicUuids() {
        return members.values().stream()
                .filter(TeamMember::isMechanic)
                .map(TeamMember::getUuid)
                .collect(Collectors.toList());
    }

    /**
     * Check if the team is full.
     */
    public boolean isFull() {
        return players.size() >= maxMembers;
    }

    @Override
    public int compareTo(Team other) {
        return this.name.compareToIgnoreCase(other.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Team team = (Team) obj;
        return id == team.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", playerCount=" + players.size() +
                '}';
    }
}