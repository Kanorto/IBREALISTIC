package me.makkuusen.timing.system.boatutils;

import me.makkuusen.timing.system.TimingSystem;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Sends notifications to a Discord webhook for anti-cheat alerts.
 */
public class DiscordWebhookManager {

    private static String webhookUrl;

    public static void initialize() {
        webhookUrl = TimingSystem.getPlugin().getConfig().getString(
                "anticheat.discord_webhook_url", "");
    }

    /**
     * Check if webhook is configured.
     */
    public static boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isEmpty()
                && webhookUrl.startsWith("https://discord.com/api/webhooks/");
    }

    /**
     * Send a suspicious activity alert to Discord.
     */
    public static void sendSuspicionAlert(String playerName, String reason, String details) {
        if (!isEnabled()) return;
        String content = "⚠️ **Anti-Cheat Suspicion** — `" + escapeJson(playerName) + "`\n"
                + "**Reason:** " + escapeJson(reason) + "\n"
                + "**Details:** " + escapeJson(details);
        sendAsync(buildPayload(content, 0xFFA500)); // Orange
    }

    /**
     * Send a ban alert to Discord.
     */
    public static void sendBanAlert(String playerName, String reason, String details, int banMinutes) {
        if (!isEnabled()) return;
        String content = "🔨 **Temporary Ban** — `" + escapeJson(playerName)
                + "` (" + banMinutes + " min)\n"
                + "**Reason:** " + escapeJson(reason) + "\n"
                + "**Details:** " + escapeJson(details);
        sendAsync(buildPayload(content, 0xFF0000)); // Red
    }

    /**
     * Send a world record anomaly alert to Discord.
     */
    public static void sendRecordAnomalyAlert(String playerName, String trackName,
                                               String newTime, String previousRecord,
                                               double marginSeconds, int totalParticipants) {
        if (!isEnabled()) return;
        String content = "🏁 **Record Anomaly** — `" + escapeJson(playerName) + "`\n"
                + "**Track:** " + escapeJson(trackName) + "\n"
                + "**New Time:** " + escapeJson(newTime) + " (Previous: "
                + escapeJson(previousRecord) + ")\n"
                + "**Margin:** " + String.format("%.1f", marginSeconds)
                + "s faster | **Participants:** " + totalParticipants;
        sendAsync(buildPayload(content, 0xFF4500)); // Red-Orange
    }

    // ─── INTERNALS ───

    private static String buildPayload(String content, int color) {
        return "{\"embeds\":[{\"description\":\""
                + escapeJson(content)
                + "\",\"color\":" + color + "}]}";
    }

    private static void sendAsync(String jsonPayload) {
        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    TimingSystem.getPlugin().getLogger().log(Level.WARNING,
                            "Discord webhook returned " + responseCode);
                }
                conn.disconnect();
            } catch (Exception e) {
                TimingSystem.getPlugin().getLogger().log(Level.WARNING,
                        "Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> {} // skip
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
