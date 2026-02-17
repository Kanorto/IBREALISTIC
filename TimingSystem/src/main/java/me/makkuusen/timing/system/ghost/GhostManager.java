package me.makkuusen.timing.system.ghost;

import me.makkuusen.timing.system.TimingSystem;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Manages ghost recordings on the server side.
 * <p>
 * Storage layout: plugins/TimingSystem/ghosts/{trackId}/{uuid}_pb.ghost
 * File format: GZIP compressed — 4-byte totalTicks + 8-byte finishTimeMs + N × GhostFrame
 */
public class GhostManager {

    private static final String GHOST_DIR = "ghosts";
    private static final String PB_SUFFIX = "_pb.ghost";

    /** Maximum ghost ticks (20 min at 20 tps) */
    private static final int MAX_GHOST_TICKS = 24000;

    // ─── CACHE ───
    /** Cache of loaded ghost data: trackId → (uuid → frames) */
    private static final Map<Integer, Map<UUID, CachedGhost>> ghostCache = new ConcurrentHashMap<>();

    public static class CachedGhost {
        public final List<GhostFrame> frames;
        public final long finishTimeMs;
        public CachedGhost(List<GhostFrame> frames, long finishTimeMs) {
            this.frames = frames;
            this.finishTimeMs = finishTimeMs;
        }
    }

    // ─── SAVE ───

    /**
     * Save ghost data for a player's personal best on a track.
     * Called after a valid telemetry finish that is a new PB.
     */
    public static void saveGhost(UUID playerUuid, int trackId, List<GhostFrame> frames, long finishTimeMs) {
        if (trackId <= 0) return; // Validate trackId
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> {
            try {
                File dir = getTrackDirectory(trackId);
                File file = new File(dir, playerUuid.toString() + PB_SUFFIX);
                writeGhostFile(file, frames, finishTimeMs);

                // Update cache
                ghostCache.computeIfAbsent(trackId, k -> new ConcurrentHashMap<>())
                        .put(playerUuid, new CachedGhost(frames, finishTimeMs));

                TimingSystem.getPlugin().getLogger().info(
                        "Saved ghost for " + playerUuid + " track=" + trackId
                                + " ticks=" + frames.size() + " time=" + finishTimeMs + "ms");
            } catch (IOException e) {
                TimingSystem.getPlugin().getLogger().log(Level.SEVERE,
                        "Failed to save ghost for " + playerUuid + " track=" + trackId, e);
            }
        });
    }

    // ─── LOAD ───

    /**
     * Load a player's PB ghost for a track, with caching.
     * Returns null if no ghost exists.
     */
    public static CachedGhost loadGhost(UUID playerUuid, int trackId) {
        if (trackId <= 0) return null; // Validate trackId
        // Check cache first
        Map<UUID, CachedGhost> trackGhosts = ghostCache.get(trackId);
        if (trackGhosts != null) {
            CachedGhost cached = trackGhosts.get(playerUuid);
            if (cached != null) return cached;
        }

        // Load from file
        File file = new File(getTrackDirectoryPath(trackId), playerUuid.toString() + PB_SUFFIX);
        if (!file.exists()) return null;

        try {
            CachedGhost ghost = readGhostFile(file);
            ghostCache.computeIfAbsent(trackId, k -> new ConcurrentHashMap<>())
                    .put(playerUuid, ghost);
            return ghost;
        } catch (IOException e) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING,
                    "Failed to load ghost for " + playerUuid + " track=" + trackId, e);
            return null;
        }
    }

    /**
     * Load random ghosts from leaderboard for competition mode.
     * Selects ghosts spread across different finish times.
     *
     * @param trackId     track to load ghosts for
     * @param excludeUuid exclude this player (the requesting player)
     * @param maxGhosts   maximum number of ghosts to return
     * @return list of cached ghosts, sorted by finish time
     */
    public static List<CachedGhost> loadCompetitionGhosts(int trackId, UUID excludeUuid, int maxGhosts) {
        File dir = getTrackDirectoryPath(trackId);
        if (!dir.exists() || !dir.isDirectory()) return Collections.emptyList();

        File[] ghostFiles = dir.listFiles((d, name) -> name.endsWith(PB_SUFFIX));
        if (ghostFiles == null || ghostFiles.length == 0) return Collections.emptyList();

        // Determine max ghosts based on entries
        int entries = ghostFiles.length;
        int allowedMax;
        if (entries < 20) {
            return Collections.emptyList(); // Not enough entries for competition
        } else if (entries < 50) {
            allowedMax = 2;
        } else if (entries < 100) {
            allowedMax = Math.min(maxGhosts, 4);
        } else {
            allowedMax = Math.min(maxGhosts, 6);
        }

        // Load all ghosts (except the requesting player)
        List<CachedGhost> allGhosts = new ArrayList<>();
        for (File f : ghostFiles) {
            String name = f.getName();
            String uuidStr = name.substring(0, name.length() - PB_SUFFIX.length());
            try {
                UUID uuid = UUID.fromString(uuidStr);
                if (uuid.equals(excludeUuid)) continue;
                CachedGhost ghost = loadGhostFromFile(uuid, trackId, f);
                if (ghost != null) allGhosts.add(ghost);
            } catch (Exception e) {
                // Skip invalid files
            }
        }

        if (allGhosts.isEmpty()) return Collections.emptyList();

        // Sort by finish time
        allGhosts.sort(Comparator.comparingLong(g -> g.finishTimeMs));

        // Select spread-out ghosts across the time range
        return selectSpreadGhosts(allGhosts, allowedMax);
    }

    /**
     * Selects ghosts spread evenly across finish times.
     */
    private static List<CachedGhost> selectSpreadGhosts(List<CachedGhost> sorted, int count) {
        if (sorted.size() <= count) return new ArrayList<>(sorted);

        List<CachedGhost> selected = new ArrayList<>();
        float step = (float) sorted.size() / count;
        for (int i = 0; i < count; i++) {
            int index = Math.min((int) (i * step), sorted.size() - 1);
            selected.add(sorted.get(index));
        }
        return selected;
    }

    // ─── FILE I/O ───

    private static void writeGhostFile(File file, List<GhostFrame> frames, long finishTimeMs) throws IOException {
        int ticks = Math.min(frames.size(), MAX_GHOST_TICKS);
        try (FileOutputStream fos = new FileOutputStream(file);
             GZIPOutputStream gzip = new GZIPOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(gzip)) {
            dos.writeInt(ticks);
            dos.writeLong(finishTimeMs);
            for (int i = 0; i < ticks; i++) {
                frames.get(i).writeTo(dos);
            }
        }
    }

    private static CachedGhost readGhostFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             GZIPInputStream gzip = new GZIPInputStream(fis);
             DataInputStream dis = new DataInputStream(gzip)) {
            int totalTicks = dis.readInt();
            long finishTimeMs = dis.readLong();
            List<GhostFrame> frames = new ArrayList<>(totalTicks);
            for (int i = 0; i < totalTicks; i++) {
                frames.add(GhostFrame.readFrom(dis));
            }
            return new CachedGhost(frames, finishTimeMs);
        }
    }

    private static CachedGhost loadGhostFromFile(UUID uuid, int trackId, File file) {
        try {
            CachedGhost ghost = readGhostFile(file);
            ghostCache.computeIfAbsent(trackId, k -> new ConcurrentHashMap<>())
                    .put(uuid, ghost);
            return ghost;
        } catch (IOException e) {
            return null;
        }
    }

    // ─── DIRECTORY HELPERS ───

    private static File getTrackDirectory(int trackId) throws IOException {
        File dir = new File(TimingSystem.getPlugin().getDataFolder(),
                GHOST_DIR + File.separator + trackId);
        if (!dir.exists()) {
            Files.createDirectories(dir.toPath());
        }
        return dir;
    }

    private static File getTrackDirectoryPath(int trackId) {
        return new File(TimingSystem.getPlugin().getDataFolder(),
                GHOST_DIR + File.separator + trackId);
    }

    /**
     * Clear cached ghosts for a track. Called when track is modified.
     */
    public static void clearCache(int trackId) {
        ghostCache.remove(trackId);
    }

    /**
     * Clear all cached ghosts.
     */
    public static void clearAllCache() {
        ghostCache.clear();
    }
}
