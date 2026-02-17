package me.makkuusen.timing.system.team;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.boatutils.DamageWearManager;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.theme.messages.Warning;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages pit stop mechanics during team races.
 * When the pilot enters a SERVICEPARK region, mechanics receive a hotbar
 * with tools to perform tire changes, refueling, and body repairs.
 *
 * Hotbar layout:
 *   Slot 0 — 🔧 Tires (IRON_HOE): click to change tires (4 clicks total)
 *   Slot 1 — ⛽ Refuel (BLAZE_ROD): hold RMB to refuel (5 seconds)
 *   Slot 2 — 🔩 Repair (ANVIL): click to repair body (6 clicks total)
 *   Slot 3 — ✅ Done (LIME_DYE): release pilot when all tasks complete
 */
public class PitStopManager {

    // ─── ITEM IDENTIFICATION ───
    private static final String PITSTOP_ITEM_KEY = "pitstop_item";
    public static final String ITEM_TYPE_TIRES = "tires";
    public static final String ITEM_TYPE_REFUEL = "refuel";
    public static final String ITEM_TYPE_REPAIR = "repair";
    public static final String ITEM_TYPE_DONE = "done";

    // ─── WHEEL PROXIMITY ───
    /** Distance from vehicle center to wheel offset (blocks) */
    private static final double WHEEL_OFFSET_FORWARD = 0.6;
    private static final double WHEEL_OFFSET_SIDE = 0.5;
    /** Maximum distance from a wheel to change it (blocks) */
    private static final double WHEEL_PROXIMITY_RADIUS = 2.0;
    /** Maximum distance from vehicle rear for refueling (blocks) */
    private static final double REFUEL_PROXIMITY_RADIUS = 2.5;
    /** Pilot UUID → active PitStopSession */
    private static final Map<UUID, PitStopSession> activePitStops = new ConcurrentHashMap<>();

    /** Mechanic UUID → pilot UUID they are servicing */
    private static final Map<UUID, UUID> mechanicAssignments = new ConcurrentHashMap<>();

    /** Pilot UUID → BossBar for pit stop progress */
    private static final Map<UUID, BossBar> pitBossBars = new ConcurrentHashMap<>();

    /** Mechanic UUID → saved inventory contents */
    private static final Map<UUID, ItemStack[]> savedInventories = new ConcurrentHashMap<>();

    /** Pilot UUID → saved walk speed */
    private static final Map<UUID, Float> savedWalkSpeeds = new ConcurrentHashMap<>();

    /** Mechanic UUIDs currently holding RMB for refueling → tick task IDs */
    private static final Map<UUID, Integer> refuelTasks = new ConcurrentHashMap<>();

    private static NamespacedKey pitstopItemKey;

    // ─── INITIALIZATION ───

    public static void initialize() {
        pitstopItemKey = new NamespacedKey(TimingSystem.getPlugin(), PITSTOP_ITEM_KEY);
    }

    // ─── CONFIGURATION ───

    private static int getTireClicks() {
        return TimingSystem.getPlugin().getConfig().getInt("team_race.pitstop.tire_clicks",
                PitStopSession.DEFAULT_TIRE_CLICKS);
    }

    private static int getRefuelTimeSeconds() {
        return TimingSystem.getPlugin().getConfig().getInt("team_race.pitstop.refuel_time_seconds", 5);
    }

    private static int getRepairClicks() {
        return TimingSystem.getPlugin().getConfig().getInt("team_race.pitstop.repair_clicks",
                PitStopSession.DEFAULT_REPAIR_CLICKS);
    }

    private static int getMinPitstopSeconds() {
        return TimingSystem.getPlugin().getConfig().getInt("team_race.pitstop.min_pitstop_seconds",
                PitStopSession.DEFAULT_MIN_PITSTOP_SECONDS);
    }

    private static int getTimeoutSeconds() {
        return TimingSystem.getPlugin().getConfig().getInt("team_race.pitstop.timeout_seconds",
                PitStopSession.DEFAULT_TIMEOUT_SECONDS);
    }

    private static int getPenaltyPerExtraSecond() {
        return TimingSystem.getPlugin().getConfig().getInt("team_race.pitstop.penalty_per_extra_second",
                PitStopSession.DEFAULT_PENALTY_PER_EXTRA_SECOND);
    }

    // ─── PIT STOP LIFECYCLE ───

    /**
     * Start a pit stop for a team race.
     * Called when the pilot enters a SERVICEPARK region during a team race.
     */
    public static boolean startPitStop(UUID pilotUuid, int teamId, Team team) {
        if (activePitStops.containsKey(pilotUuid)) return false;

        Player pilot = Bukkit.getPlayer(pilotUuid);
        if (pilot == null) return false;

        // Create session with configured limits
        PitStopSession session = new PitStopSession(pilotUuid, teamId);
        session.setTireClicksRequired(getTireClicks());
        session.setRefuelTicksRequired(getRefuelTimeSeconds() * 20);
        session.setRepairClicksRequired(getRepairClicks());
        session.setMinPitstopSeconds(getMinPitstopSeconds());
        session.setTimeoutSeconds(getTimeoutSeconds());
        session.setPenaltyPerExtraSecond(getPenaltyPerExtraSecond());

        activePitStops.put(pilotUuid, session);

        // Freeze pilot
        savedWalkSpeeds.putIfAbsent(pilotUuid, pilot.getWalkSpeed());
        pilot.setWalkSpeed(0f);

        // Create BossBar
        BossBar bar = BossBar.bossBar(
                buildPitStopTitle(session),
                0f,
                BossBar.Color.YELLOW,
                BossBar.Overlay.PROGRESS
        );
        pitBossBars.put(pilotUuid, bar);
        pilot.showBossBar(bar);

        Text.send(pilot, Info.PITSTOP_STARTED);

        // Give hotbar to online mechanics
        List<UUID> mechanicUuids = team.getMechanicUuids();
        for (UUID mechanicUuid : mechanicUuids) {
            Player mechanic = Bukkit.getPlayer(mechanicUuid);
            if (mechanic != null && mechanic.isOnline()) {
                assignMechanic(mechanic, pilotUuid, bar);
            }
        }

        // Schedule timeout
        int timeoutTicks = session.getTimeoutSeconds() * 20;
        Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> {
            if (activePitStops.containsKey(pilotUuid)) {
                forceEndPitStop(pilotUuid, true);
            }
        }, timeoutTicks);

        // Schedule 5-second warning
        int warningTicks = Math.max(0, (session.getTimeoutSeconds() - 5)) * 20;
        if (session.getTimeoutSeconds() > 5) {
            Bukkit.getScheduler().runTaskLater(TimingSystem.getPlugin(), () -> {
                if (!activePitStops.containsKey(pilotUuid)) return;
                Player p = Bukkit.getPlayer(pilotUuid);
                if (p != null) {
                    Text.send(p, Warning.PITSTOP_TIME_WARNING, "%remaining%", "5");
                }
                // Warn mechanics too
                for (Map.Entry<UUID, UUID> entry : mechanicAssignments.entrySet()) {
                    if (entry.getValue().equals(pilotUuid)) {
                        Player mech = Bukkit.getPlayer(entry.getKey());
                        if (mech != null) {
                            Text.send(mech, Warning.PITSTOP_TIME_WARNING, "%remaining%", "5");
                        }
                    }
                }
            }, warningTicks);
        }

        return true;
    }

    /**
     * Assign a mechanic to the active pit stop.
     */
    private static void assignMechanic(Player mechanic, UUID pilotUuid, BossBar bar) {
        mechanicAssignments.put(mechanic.getUniqueId(), pilotUuid);

        // Save current inventory
        savedInventories.put(mechanic.getUniqueId(), mechanic.getInventory().getContents().clone());

        // Clear and give pit stop hotbar
        mechanic.getInventory().clear();
        mechanic.getInventory().setItem(0, createPitStopItem(ITEM_TYPE_TIRES, mechanic));
        mechanic.getInventory().setItem(1, createPitStopItem(ITEM_TYPE_REFUEL, mechanic));
        mechanic.getInventory().setItem(2, createPitStopItem(ITEM_TYPE_REPAIR, mechanic));
        mechanic.getInventory().setItem(3, createPitStopItem(ITEM_TYPE_DONE, mechanic));

        // Show BossBar to mechanic too
        mechanic.showBossBar(bar);

        Text.send(mechanic, Info.PITSTOP_MECHANIC_ASSIGNED);
        mechanic.playSound(mechanic.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
    }

    /**
     * Handle a mechanic clicking a pit stop item.
     * Called from the event listener.
     */
    public static boolean handlePitStopClick(Player mechanic, String itemType) {
        UUID mechanicUuid = mechanic.getUniqueId();
        UUID pilotUuid = mechanicAssignments.get(mechanicUuid);
        if (pilotUuid == null) return false;

        PitStopSession session = activePitStops.get(pilotUuid);
        if (session == null) return false;

        switch (itemType) {
            case ITEM_TYPE_TIRES -> handleTireClick(mechanic, session, pilotUuid);
            case ITEM_TYPE_REPAIR -> handleRepairClick(mechanic, session, pilotUuid);
            case ITEM_TYPE_DONE -> handleDoneClick(mechanic, session, pilotUuid);
            default -> { return false; }
        }

        return true;
    }

    /**
     * Start refueling (called when mechanic holds RMB on refuel item).
     */
    public static boolean startRefueling(Player mechanic) {
        UUID mechanicUuid = mechanic.getUniqueId();
        UUID pilotUuid = mechanicAssignments.get(mechanicUuid);
        if (pilotUuid == null) return false;

        PitStopSession session = activePitStops.get(pilotUuid);
        if (session == null || session.isRefuelComplete()) return false;

        if (refuelTasks.containsKey(mechanicUuid)) return false;

        // Validate task assignment
        TeamMember member = getTeamMember(mechanicUuid, session.getTeamId());
        if (member != null && !member.getAssignedTasks().isEmpty() && !member.hasTask(PitTask.REFUEL)) {
            Text.send(mechanic, Warning.PITSTOP_NOT_ASSIGNED, "%task%", PitTask.REFUEL.getDisplayName());
            return false;
        }

        // Start a repeating task for refueling
        int taskId = Bukkit.getScheduler().runTaskTimer(TimingSystem.getPlugin(), () -> {
            PitStopSession s = activePitStops.get(pilotUuid);
            if (s == null || s.isRefuelComplete()) {
                stopRefueling(mechanicUuid);
                return;
            }
            boolean complete = s.tickRefuel();
            updateBossBar(pilotUuid, s);
            if (complete) {
                mechanic.playSound(mechanic.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                Text.send(mechanic, Success.PITSTOP_REFUEL_COMPLETE);
                stopRefueling(mechanicUuid);
            }
        }, 0L, 1L).getTaskId();

        refuelTasks.put(mechanicUuid, taskId);
        return true;
    }

    /**
     * Stop refueling (called when mechanic releases RMB).
     */
    public static void stopRefueling(UUID mechanicUuid) {
        Integer taskId = refuelTasks.remove(mechanicUuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    // ─── CLICK HANDLERS ───

    private static void handleTireClick(Player mechanic, PitStopSession session, UUID pilotUuid) {
        if (session.isTiresComplete()) {
            Text.send(mechanic, Info.PITSTOP_TIRES_ALREADY_DONE);
            return;
        }

        // Determine which wheel the mechanic is near based on vehicle position
        Player pilot = Bukkit.getPlayer(pilotUuid);
        if (pilot == null || pilot.getVehicle() == null) {
            Text.send(mechanic, Error.GENERIC);
            return;
        }

        int nearestWheel = findNearestWheel(mechanic, pilot.getVehicle());
        if (nearestWheel < 0) {
            Text.send(mechanic, Warning.PITSTOP_NOT_NEAR_WHEEL);
            return;
        }

        // Check if mechanic is assigned to this wheel
        PitTask tireTask = switch (nearestWheel) {
            case 0 -> PitTask.TIRES_FL;
            case 1 -> PitTask.TIRES_FR;
            case 2 -> PitTask.TIRES_RL;
            case 3 -> PitTask.TIRES_RR;
            default -> null;
        };

        if (tireTask != null) {
            // If mechanic has specific task assignments, validate
            TeamMember member = getTeamMember(mechanic.getUniqueId(), session.getTeamId());
            if (member != null && !member.getAssignedTasks().isEmpty() && !member.hasTask(tireTask)) {
                Text.send(mechanic, Warning.PITSTOP_NOT_ASSIGNED,
                        "%task%", tireTask.getDisplayName());
                return;
            }
        }

        if (session.isTireChanged(nearestWheel)) {
            String wheelName = getWheelName(nearestWheel);
            Text.send(mechanic, Info.PITSTOP_TIRES_ALREADY_DONE);
            return;
        }

        boolean allComplete = session.changeTire(nearestWheel);
        mechanic.playSound(mechanic.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);

        updateBossBar(pilotUuid, session);

        String wheelName = getWheelName(nearestWheel);
        if (allComplete) {
            mechanic.playSound(mechanic.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
            Text.send(mechanic, Success.PITSTOP_TIRES_COMPLETE);
        } else {
            Text.send(mechanic, Info.PITSTOP_TIRE_PROGRESS,
                    "%done%", String.valueOf(session.getTiresChangedCount()),
                    "%total%", "4");
        }
    }

    /**
     * Find the nearest wheel to the mechanic based on vehicle orientation.
     * @return wheel index (0=FL, 1=FR, 2=RL, 3=RR) or -1 if not close enough
     */
    private static int findNearestWheel(Player mechanic, org.bukkit.entity.Entity vehicle) {
        org.bukkit.Location vehicleLoc = vehicle.getLocation();
        double yawRad = Math.toRadians(-vehicleLoc.getYaw());

        // Calculate forward and right vectors
        double forwardX = Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double rightX = -forwardZ;
        double rightZ = forwardX;

        // Wheel world positions (4 wheels)
        double[][] wheelOffsets = {
                { WHEEL_OFFSET_FORWARD,  -WHEEL_OFFSET_SIDE},  // FL
                { WHEEL_OFFSET_FORWARD,   WHEEL_OFFSET_SIDE},  // FR
                {-WHEEL_OFFSET_FORWARD,  -WHEEL_OFFSET_SIDE},  // RL
                {-WHEEL_OFFSET_FORWARD,   WHEEL_OFFSET_SIDE},  // RR
        };

        double mechX = mechanic.getLocation().getX();
        double mechZ = mechanic.getLocation().getZ();
        double vehX = vehicleLoc.getX();
        double vehZ = vehicleLoc.getZ();

        int nearestWheel = -1;
        double nearestDistSq = WHEEL_PROXIMITY_RADIUS * WHEEL_PROXIMITY_RADIUS;

        for (int i = 0; i < 4; i++) {
            double wheelX = vehX + forwardX * wheelOffsets[i][0] + rightX * wheelOffsets[i][1];
            double wheelZ = vehZ + forwardZ * wheelOffsets[i][0] + rightZ * wheelOffsets[i][1];

            double dx = mechX - wheelX;
            double dz = mechZ - wheelZ;
            double distSq = dx * dx + dz * dz;

            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearestWheel = i;
            }
        }

        return nearestWheel;
    }

    /**
     * Get a human-readable wheel name.
     */
    private static String getWheelName(int index) {
        return switch (index) {
            case 0 -> "Front-Left";
            case 1 -> "Front-Right";
            case 2 -> "Rear-Left";
            case 3 -> "Rear-Right";
            default -> "Unknown";
        };
    }

    /**
     * Get TeamMember from team by UUID.
     */
    private static TeamMember getTeamMember(UUID mechanicUuid, int teamId) {
        java.util.Optional<Team> maybeTeam = TeamManager.getTeam(teamId);
        if (maybeTeam.isEmpty()) return null;
        return maybeTeam.get().getMember(mechanicUuid);
    }

    private static void handleRepairClick(Player mechanic, PitStopSession session, UUID pilotUuid) {
        if (session.isRepairComplete()) {
            Text.send(mechanic, Info.PITSTOP_REPAIR_ALREADY_DONE);
            return;
        }

        // Validate task assignment
        TeamMember member = getTeamMember(mechanic.getUniqueId(), session.getTeamId());
        if (member != null && !member.getAssignedTasks().isEmpty() && !member.hasTask(PitTask.REPAIR)) {
            Text.send(mechanic, Warning.PITSTOP_NOT_ASSIGNED, "%task%", PitTask.REPAIR.getDisplayName());
            return;
        }

        boolean complete = session.clickRepair();
        mechanic.playSound(mechanic.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.0f);

        updateBossBar(pilotUuid, session);

        if (complete) {
            mechanic.playSound(mechanic.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
            Text.send(mechanic, Success.PITSTOP_REPAIR_COMPLETE);
        } else {
            Text.send(mechanic, Info.PITSTOP_REPAIR_PROGRESS,
                    "%done%", String.valueOf(session.getRepairClicksDone()),
                    "%total%", String.valueOf(session.getRepairClicksRequired()));
        }
    }

    private static void handleDoneClick(Player mechanic, PitStopSession session, UUID pilotUuid) {
        if (!session.isAllTasksComplete()) {
            Text.send(mechanic, Warning.PITSTOP_NOT_READY);

            StringBuilder missing = new StringBuilder();
            if (!session.isTiresComplete()) missing.append("Tires ");
            if (!session.isRefuelComplete()) missing.append("Refuel ");
            if (!session.isRepairComplete()) missing.append("Repair ");
            Text.send(mechanic, Info.PITSTOP_TASKS_REMAINING,
                    "%tasks%", missing.toString().trim());
            return;
        }

        // Check minimum time
        int elapsed = session.getElapsedSeconds();
        if (elapsed < session.getMinPitstopSeconds()) {
            int remaining = session.getMinPitstopSeconds() - elapsed;
            Text.send(mechanic, Warning.PITSTOP_MIN_TIME,
                    "%remaining%", String.valueOf(remaining));
            return;
        }

        completePitStop(pilotUuid, session);
    }

    // ─── PIT STOP COMPLETION ───

    /**
     * Complete the pit stop normally (all tasks done, mechanic pressed Done).
     */
    private static void completePitStop(UUID pilotUuid, PitStopSession session) {
        long pitTimeMs = session.getElapsedMs();
        long penaltyMs = session.calculatePenaltyMs();
        session.setReleased(true);

        // Apply repairs to damage system
        DamageWearManager.setTireWear(pilotUuid, 0f);
        DamageWearManager.setEngineTemp(pilotUuid, 0.1f);
        float repairFraction = (float) session.getRepairClicksDone() / session.getRepairClicksRequired();
        float currentDamage = DamageWearManager.getBodyDamage(pilotUuid);
        DamageWearManager.setBodyDamage(pilotUuid,
                Math.max(0f, currentDamage - (repairFraction * PitStopSession.BODY_DAMAGE_REPAIR_PER_CLICK
                        * session.getRepairClicksRequired())));

        endPitStopCleanup(pilotUuid, pitTimeMs, penaltyMs, false);
    }

    /**
     * Force-end the pit stop (timeout or cancellation).
     */
    public static void forceEndPitStop(UUID pilotUuid, boolean timeout) {
        PitStopSession session = activePitStops.get(pilotUuid);
        if (session == null) return;

        long pitTimeMs = session.getElapsedMs();
        long penaltyMs = session.calculatePenaltyMs();

        // Apply partial repairs based on what was completed
        if (session.isTiresComplete()) {
            DamageWearManager.setTireWear(pilotUuid, 0f);
        }
        if (session.isRefuelComplete()) {
            DamageWearManager.setEngineTemp(pilotUuid, 0.1f);
        }
        if (session.getRepairClicksDone() > 0) {
            float repairFraction = (float) session.getRepairClicksDone() / session.getRepairClicksRequired();
            float currentDamage = DamageWearManager.getBodyDamage(pilotUuid);
            DamageWearManager.setBodyDamage(pilotUuid,
                    Math.max(0f, currentDamage - (repairFraction * PitStopSession.BODY_DAMAGE_REPAIR_PER_CLICK
                            * session.getRepairClicksRequired())));
        }

        endPitStopCleanup(pilotUuid, pitTimeMs, penaltyMs, timeout);
    }

    /**
     * Common cleanup for pit stop end.
     */
    private static void endPitStopCleanup(UUID pilotUuid, long pitTimeMs, long penaltyMs, boolean timeout) {
        PitStopSession session = activePitStops.remove(pilotUuid);
        if (session == null) return;

        // Unfreeze pilot
        Player pilot = Bukkit.getPlayer(pilotUuid);
        if (pilot != null) {
            Float savedSpeed = savedWalkSpeeds.remove(pilotUuid);
            pilot.setWalkSpeed(savedSpeed != null ? savedSpeed : 0.2f);

            // Hide BossBar
            BossBar bar = pitBossBars.remove(pilotUuid);
            if (bar != null) {
                pilot.hideBossBar(bar);
            }

            String timeFormatted = ApiUtilities.formatAsTime(pitTimeMs);
            if (timeout) {
                Text.send(pilot, Error.PITSTOP_TIMEOUT);
            } else {
                Text.send(pilot, Success.PITSTOP_COMPLETE, "%time%", timeFormatted);
            }

            if (penaltyMs > 0) {
                String penaltyFormatted = ApiUtilities.formatAsTime(penaltyMs);
                Text.send(pilot, Warning.PITSTOP_PENALTY, "%penalty%", penaltyFormatted);
            }

            pilot.playSound(pilot.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }

        // Restore mechanic inventories
        for (Map.Entry<UUID, UUID> entry : new HashMap<>(mechanicAssignments).entrySet()) {
            if (entry.getValue().equals(pilotUuid)) {
                UUID mechanicUuid = entry.getKey();
                mechanicAssignments.remove(mechanicUuid);
                stopRefueling(mechanicUuid);

                Player mechanic = Bukkit.getPlayer(mechanicUuid);
                if (mechanic != null) {
                    // Hide BossBar
                    BossBar bar = pitBossBars.get(pilotUuid);
                    if (bar != null) {
                        mechanic.hideBossBar(bar);
                    }

                    // Restore inventory
                    ItemStack[] saved = savedInventories.remove(mechanicUuid);
                    if (saved != null) {
                        mechanic.getInventory().setContents(saved);
                    }

                    String timeFormatted = ApiUtilities.formatAsTime(pitTimeMs);
                    Text.send(mechanic, Success.PITSTOP_MECHANIC_DONE, "%time%", timeFormatted);
                }
            }
        }

        // Update team race session
        TeamRaceManager.recordPitStop(pilotUuid, pitTimeMs, penaltyMs);
    }

    // ─── BOSSBAR ───

    private static void updateBossBar(UUID pilotUuid, PitStopSession session) {
        BossBar bar = pitBossBars.get(pilotUuid);
        if (bar == null) return;

        float progress = Math.min(1.0f, session.getOverallProgress());
        bar.progress(progress);
        bar.name(buildPitStopTitle(session));

        if (session.isAllTasksComplete()) {
            bar.color(BossBar.Color.GREEN);
        } else if (progress > 0.5f) {
            bar.color(BossBar.Color.YELLOW);
        } else {
            bar.color(BossBar.Color.RED);
        }
    }

    private static Component buildPitStopTitle(PitStopSession session) {
        // Build per-wheel tire status: FL/FR/RL/RR
        String fl = session.isTireChanged(0) ? "✅" : "⬜";
        String fr = session.isTireChanged(1) ? "✅" : "⬜";
        String rl = session.isTireChanged(2) ? "✅" : "⬜";
        String rr = session.isTireChanged(3) ? "✅" : "⬜";
        String tiresDisplay = fl + fr + rl + rr;

        String refuel = session.isRefuelComplete() ? "✅" :
                Math.round(session.getRefuelProgress() * 100) + "%";
        String repair = session.isRepairComplete() ? "✅" :
                session.getRepairClicksDone() + "/" + session.getRepairClicksRequired();
        int elapsed = session.getElapsedSeconds();

        return Component.text("🔧 PIT STOP │ ", NamedTextColor.GOLD)
                .append(Component.text("Tires: " + tiresDisplay, NamedTextColor.WHITE))
                .append(Component.text(" │ ", NamedTextColor.GRAY))
                .append(Component.text("Fuel: " + refuel, NamedTextColor.WHITE))
                .append(Component.text(" │ ", NamedTextColor.GRAY))
                .append(Component.text("Repair: " + repair, NamedTextColor.WHITE))
                .append(Component.text(" │ ", NamedTextColor.GRAY))
                .append(Component.text(elapsed + "s", NamedTextColor.AQUA));
    }

    // ─── HOTBAR ITEMS ───

    private static ItemStack createPitStopItem(String type, Player mechanic) {
        Material material;
        String displayName;
        List<Component> lore;

        switch (type) {
            case ITEM_TYPE_TIRES -> {
                material = Material.IRON_HOE;
                displayName = "🔧 Tires";
                lore = List.of(
                        Component.text("Click to change a tire", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("0/" + getTireClicks() + " tires changed", NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false)
                );
            }
            case ITEM_TYPE_REFUEL -> {
                material = Material.BLAZE_ROD;
                displayName = "⛽ Refuel";
                lore = List.of(
                        Component.text("Hold right-click to refuel", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("0% complete", NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false)
                );
            }
            case ITEM_TYPE_REPAIR -> {
                material = Material.ANVIL;
                displayName = "🔩 Repair Body";
                lore = List.of(
                        Component.text("Click to repair body damage", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("0/" + getRepairClicks() + " repairs done", NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false)
                );
            }
            case ITEM_TYPE_DONE -> {
                material = Material.LIME_DYE;
                displayName = "✅ Release Pilot";
                lore = List.of(
                        Component.text("Click when all tasks are complete", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("⚠ All tasks must be done first!", NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false)
                );
            }
            default -> {
                return new ItemStack(Material.BARRIER);
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName, NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(pitstopItemKey, PersistentDataType.STRING, type);
        item.setItemMeta(meta);
        return item;
    }

    // ─── QUERIES ───

    /**
     * Check if a pit stop item was clicked.
     * @return the item type, or null if not a pit stop item
     */
    public static String getPitStopItemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        if (pitstopItemKey == null) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(pitstopItemKey, PersistentDataType.STRING);
    }

    /**
     * Check if a player is a mechanic in an active pit stop.
     */
    public static boolean isMechanicInPitStop(UUID uuid) {
        return mechanicAssignments.containsKey(uuid);
    }

    /**
     * Check if a pilot is in an active pit stop.
     */
    public static boolean isPilotInPitStop(UUID uuid) {
        return activePitStops.containsKey(uuid);
    }

    /**
     * Get the active pit stop session for a pilot.
     */
    public static PitStopSession getActivePitStop(UUID pilotUuid) {
        return activePitStops.get(pilotUuid);
    }

    // ─── CLEANUP ───

    /**
     * Cancel all pit stops for a player (on disconnect or race cancel).
     */
    public static void cancelForPlayer(UUID uuid) {
        // Check if pilot
        if (activePitStops.containsKey(uuid)) {
            forceEndPitStop(uuid, false);
        }

        // Check if mechanic
        UUID pilotUuid = mechanicAssignments.remove(uuid);
        if (pilotUuid != null) {
            stopRefueling(uuid);
            ItemStack[] saved = savedInventories.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && saved != null) {
                player.getInventory().setContents(saved);
            }
        }
    }

    /**
     * Shutdown cleanup.
     */
    public static void shutdown() {
        for (UUID pilotUuid : new ArrayList<>(activePitStops.keySet())) {
            forceEndPitStop(pilotUuid, false);
        }
        activePitStops.clear();
        mechanicAssignments.clear();
        pitBossBars.clear();
        savedInventories.clear();
        savedWalkSpeeds.clear();
        for (Integer taskId : refuelTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        refuelTasks.clear();
    }
}
