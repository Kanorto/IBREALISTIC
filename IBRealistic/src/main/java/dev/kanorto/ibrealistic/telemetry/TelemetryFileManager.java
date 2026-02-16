package dev.kanorto.ibrealistic.telemetry;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Manages local telemetry file persistence.
 * Files are stored in .minecraft/ibrealistic/telemetry/{server_hash}/
 *
 * File format (.ibrt):
 *   4 bytes  - magic "IBRT"
 *   4 bytes  - format version (int)
 *   header   - TelemetryHeader (serialized)
 *   gzip     - TelemetryFrame[totalTicks] (compressed)
 *   8 bytes  - CRC32 checksum (long)
 */
public class TelemetryFileManager {

    // ─── CONSTANTS ───
    public static final String MAGIC = "IBRT";
    public static final int FORMAT_VERSION = 1;
    public static final int MAX_LOCAL_FILES = 500;
    public static final int RETENTION_DAYS = 30;

    private static final String TELEMETRY_ROOT = "ibrealistic/telemetry";
    private static final String FILE_EXTENSION = ".ibrt";
    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ─── DIRECTORY MANAGEMENT ───

    /**
     * Get (and create if needed) the telemetry directory for a given server.
     *
     * @param serverHash unique hash identifying the server (or "singleplayer")
     * @return path to the server-specific telemetry directory
     */
    public static Path getTelemetryDir(String serverHash) throws IOException {
        // Sanitize serverHash to prevent path traversal
        String sanitized = serverHash.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (sanitized.isEmpty()) sanitized = "default";
        Path gameDir = Paths.get(System.getProperty("user.dir", "."));
        Path dir = gameDir.resolve(TELEMETRY_ROOT).resolve(sanitized);
        Files.createDirectories(dir);
        return dir;
    }

    // ─── FILE SAVE ───

    /**
     * Save a completed recording to a .ibrt file on disk.
     *
     * @param recorder    the completed recorder with header and frames
     * @param serverHash  server identifier for directory selection
     */
    public static void saveToFile(TelemetryRecorder recorder, String serverHash) throws IOException {
        TelemetryHeader header = recorder.getHeader();
        List<TelemetryFrame> frames = recorder.getFrames();

        Path dir = getTelemetryDir(serverHash);
        boolean finished = header.finishTimeMs > 0;
        String fileName = generateFileName(header.trackId, finished);
        Path filePath = dir.resolve(fileName);

        try (OutputStream fos = Files.newOutputStream(filePath);
             DataOutputStream dos = new DataOutputStream(fos)) {

            // Magic bytes
            dos.writeBytes(MAGIC);

            // Format version
            dos.writeInt(FORMAT_VERSION);

            // Header
            header.writeTo(dos);

            // GZIP-compressed frames
            byte[] compressedFrames = compressFrames(frames);
            dos.writeInt(compressedFrames.length);
            dos.write(compressedFrames);

            // Trailing checksum
            dos.writeLong(header.checksum);
        }

        // Enforce file limits after saving
        enforceFileLimits(dir);
    }

    // ─── FILE LIST ───

    /**
     * List all .ibrt telemetry files for a server, sorted newest first.
     */
    public static List<Path> listLocalFiles(String serverHash) throws IOException {
        Path dir = getTelemetryDir(serverHash);
        List<Path> files = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*" + FILE_EXTENSION)) {
            for (Path entry : stream) {
                files.add(entry);
            }
        }

        files.sort(Comparator.comparing(Path::getFileName).reversed());
        return files;
    }

    // ─── CLEANUP ───

    /**
     * Delete telemetry files older than RETENTION_DAYS.
     */
    public static void cleanOldFiles(String serverHash) throws IOException {
        Path dir = getTelemetryDir(serverHash);
        long cutoffMillis = System.currentTimeMillis() - ((long) RETENTION_DAYS * 24 * 60 * 60 * 1000);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*" + FILE_EXTENSION)) {
            for (Path entry : stream) {
                BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                if (attrs.creationTime().toMillis() < cutoffMillis) {
                    Files.deleteIfExists(entry);
                }
            }
        }
    }

    // ─── FILE NAMING ───

    /**
     * Generate a descriptive file name for a telemetry recording.
     * Format: "20260216_153000_track42_finished.ibrt"
     *
     * @param trackId  track identifier (0 for singleplayer)
     * @param finished whether the race was completed
     */
    public static String generateFileName(int trackId, boolean finished) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        String timestamp = now.format(FILE_DATE_FORMAT);
        String trackPart = trackId > 0 ? "track" + trackId : "freeplay";
        String statusPart = finished ? "finished" : "dnf";
        return timestamp + "_" + trackPart + "_" + statusPart + FILE_EXTENSION;
    }

    // ─── INTERNAL HELPERS ───

    /**
     * GZIP-compress a list of frames into a byte array.
     */
    private static byte[] compressFrames(List<TelemetryFrame> frames) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(gzip)) {
            for (TelemetryFrame frame : frames) {
                frame.writeTo(dos);
            }
        }
        return baos.toByteArray();
    }

    /**
     * Enforce MAX_LOCAL_FILES limit by deleting the oldest files.
     */
    private static void enforceFileLimits(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*" + FILE_EXTENSION)) {
            for (Path entry : stream) {
                files.add(entry);
            }
        }

        if (files.size() <= MAX_LOCAL_FILES) {
            return;
        }

        // Sort by name ascending (oldest first due to timestamp prefix)
        files.sort(Comparator.comparing(Path::getFileName));

        int toDelete = files.size() - MAX_LOCAL_FILES;
        for (int i = 0; i < toDelete; i++) {
            Files.deleteIfExists(files.get(i));
        }
    }
}
