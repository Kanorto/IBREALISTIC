package me.makkuusen.timing.system.telemetry;

import co.aikar.idb.DB;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Stores telemetry files on disk and metadata in the database.
 * <p>
 * File layout: plugins/TimingSystem/telemetry/{uuid}/{trackId}_{timestamp}.ibrt
 */
public class TelemetryStorage {

    // ─── CONSTANTS ───

    /** Maximum number of telemetry files kept per player */
    private static final int MAX_FILES_PER_PLAYER = 100;

    /** Default retention period in days for telemetry files */
    public static final int RETENTION_DAYS = 90;

    /** Subdirectory under the plugin data folder */
    private static final String TELEMETRY_DIR = "telemetry";

    /** File extension for telemetry recordings */
    private static final String FILE_EXTENSION = ".ibrt";

    // ─── FILE OPERATIONS ───

    /**
     * Saves compressed telemetry data to disk.
     *
     * @param playerUuid     player UUID
     * @param trackId        track identifier
     * @param compressedData GZIP-compressed telemetry bytes
     * @param result         validation result for this recording
     */
    public static void saveToFile(UUID playerUuid, int trackId, byte[] compressedData,
                                  TelemetryValidator.ValidationResult result) {
        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> {
            try {
                File playerDir = getPlayerDirectory(playerUuid);
                enforceFileLimit(playerDir);

                String fileName = trackId + "_" + ApiUtilities.getTimestamp() + FILE_EXTENSION;
                File outFile = new File(playerDir, fileName);

                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(compressedData);
                }

                TimingSystem.getPlugin().getLogger().info(
                        "Saved telemetry for " + playerUuid + " track=" + trackId
                                + " size=" + compressedData.length + "b status=" + result.getStatus());
            } catch (IOException e) {
                TimingSystem.getPlugin().getLogger().log(Level.SEVERE,
                        "Failed to save telemetry file for " + playerUuid, e);
            }
        });
    }

    // ─── DATABASE OPERATIONS ───

    /**
     * Saves telemetry metadata to the database.
     * Runs asynchronously to avoid blocking the main thread.
     */
    public static void saveMetadata(UUID playerUuid, int trackId, int raceResultId,
                                    int totalTicks, long finishTimeMs,
                                    String validationStatus, String validationReason,
                                    long checksum) {
        String filePath = TELEMETRY_DIR + "/" + playerUuid + "/"
                + trackId + "_" + ApiUtilities.getTimestamp() + FILE_EXTENSION;
        String createdAt = String.valueOf(ApiUtilities.getTimestamp());

        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> {
            try {
                DB.executeInsert(
                        "INSERT INTO `ts_telemetry_meta` "
                                + "(`uuid`, `race_result_id`, `track_id`, `file_path`, `total_ticks`, "
                                + "`finish_time_ms`, `validation_status`, `validation_reason`, `checksum`, `created_at`) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                        playerUuid.toString(),
                        raceResultId,
                        trackId,
                        filePath,
                        totalTicks,
                        finishTimeMs,
                        validationStatus,
                        validationReason,
                        checksum,
                        createdAt
                );
            } catch (Exception e) {
                TimingSystem.getPlugin().getLogger().log(Level.SEVERE,
                        "Failed to save telemetry metadata for " + playerUuid, e);
            }
        });
    }

    // ─── CLEANUP ───

    /**
     * Deletes telemetry files and metadata older than the specified retention period.
     * Should be called periodically (e.g. on server startup or via a scheduled task).
     *
     * @param retentionDays number of days to keep telemetry data
     */
    public static void cleanOldFiles(int retentionDays) {
        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> {
            long cutoffTimestamp = ApiUtilities.getTimestamp() - ((long) retentionDays * 24 * 60 * 60);

            // Clean database entries
            try {
                DB.executeUpdate(
                        "DELETE FROM `ts_telemetry_meta` WHERE CAST(`created_at` AS INTEGER) < ?;",
                        cutoffTimestamp
                );
            } catch (Exception e) {
                TimingSystem.getPlugin().getLogger().log(Level.WARNING,
                        "Failed to clean old telemetry metadata", e);
            }

            // Clean files on disk
            File telemetryRoot = new File(TimingSystem.getPlugin().getDataFolder(), TELEMETRY_DIR);
            if (!telemetryRoot.exists()) {
                return;
            }

            File[] playerDirs = telemetryRoot.listFiles(File::isDirectory);
            if (playerDirs == null) {
                return;
            }

            int deletedCount = 0;
            for (File playerDir : playerDirs) {
                File[] files = playerDir.listFiles((dir, name) -> name.endsWith(FILE_EXTENSION));
                if (files == null) continue;

                for (File file : files) {
                    long fileAgeSeconds = ApiUtilities.getTimestamp() - (file.lastModified() / 1000L);
                    long retentionSeconds = (long) retentionDays * 24 * 60 * 60;
                    if (fileAgeSeconds > retentionSeconds) {
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                }

                // Remove empty player directories
                File[] remaining = playerDir.listFiles();
                if (remaining != null && remaining.length == 0) {
                    playerDir.delete();
                }
            }

            if (deletedCount > 0) {
                TimingSystem.getPlugin().getLogger().info(
                        "Cleaned " + deletedCount + " old telemetry files (retention=" + retentionDays + " days)");
            }
        });
    }

    // ─── PRIVATE HELPERS ───

    /**
     * Returns (and creates if necessary) the telemetry directory for a player.
     */
    private static File getPlayerDirectory(UUID playerUuid) throws IOException {
        File dir = new File(TimingSystem.getPlugin().getDataFolder(),
                TELEMETRY_DIR + File.separator + playerUuid);
        if (!dir.exists()) {
            Files.createDirectories(dir.toPath());
        }
        return dir;
    }

    /**
     * Enforces the per-player file limit by deleting the oldest files.
     */
    private static void enforceFileLimit(File playerDir) {
        File[] files = playerDir.listFiles((dir, name) -> name.endsWith(FILE_EXTENSION));
        if (files == null || files.length < MAX_FILES_PER_PLAYER) {
            return;
        }

        // Sort by last modified (oldest first)
        java.util.Arrays.sort(files, java.util.Comparator.comparingLong(File::lastModified));

        int toDelete = files.length - MAX_FILES_PER_PLAYER + 1; // +1 to make room for new file
        for (int i = 0; i < toDelete; i++) {
            if (files[i].delete()) {
                TimingSystem.getPlugin().getLogger().info(
                        "Deleted old telemetry file: " + files[i].getName());
            }
        }
    }
}
