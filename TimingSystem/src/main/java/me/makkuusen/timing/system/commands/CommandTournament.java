package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.tournament.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.gui.TournamentGui;

@CommandAlias("tournament")
public class CommandTournament extends BaseCommand {

    @Default
    @CommandPermission("timingsystem.tournament.view")
    @Description("Show active tournaments")
    public static void onDefault(Player player) {
        var tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        new TournamentGui(tPlayer).show(player);
    }

    @Subcommand("info")
    @CommandPermission("timingsystem.tournament.view")
    @Description("Show active tournaments info")
    public static void onInfo(Player player) {
        Collection<Tournament> active = TournamentManager.getActiveTournaments();
        if (active.isEmpty()) {
            player.sendMessage(Component.text("No active tournaments.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("🏆 Active Tournaments:", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (Tournament t : active) {
            long remaining = t.getRemainingMs();
            String timeLeft = formatDuration(remaining);
            player.sendMessage(
                Component.text("  " + t.getType().getDisplayName() + ": ", NamedTextColor.YELLOW)
                    .append(Component.text(t.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" [" + t.getState().name() + "]", NamedTextColor.GRAY))
                    .append(Component.text(" — " + timeLeft + " remaining", NamedTextColor.AQUA))
            );
            player.sendMessage(
                Component.text("    Tracks: " + t.getTrackIds().size() + " | Restriction: " + t.getCarRestriction().getDisplayName(), NamedTextColor.GRAY)
            );
        }
    }

    @Subcommand("results")
    @CommandPermission("timingsystem.tournament.view")
    @Description("Show tournament results")
    public static void onResults(Player player, @Optional String typeName) {
        Tournament t = resolveTournament(player, typeName);
        if (t == null) return;

        List<TournamentResult> results = TournamentManager.getResults(t.getId());
        if (results.isEmpty()) {
            player.sendMessage(Component.text("No results yet for \"" + t.getName() + "\".", NamedTextColor.GRAY));
            return;
        }

        results.sort(Comparator.comparingLong(TournamentResult::getTotalTimeMs));
        player.sendMessage(Component.text("🏆 Results: " + t.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));

        String[] medals = {"🥇", "🥈", "🥉"};
        int showCount = Math.min(10, results.size());
        for (int i = 0; i < showCount; i++) {
            TournamentResult r = results.get(i);
            String name = getPlayerName(r.getPlayerUuid());
            String medal = i < 3 ? medals[i] + " " : "";
            NamedTextColor color = i == 0 ? NamedTextColor.GOLD : i == 1 ? NamedTextColor.GRAY : i == 2 ? NamedTextColor.RED : NamedTextColor.WHITE;
            player.sendMessage(
                Component.text("  " + medal + "#" + (i + 1) + " " + name, color)
                    .append(Component.text(" — " + formatTime(r.getTotalTimeMs()), NamedTextColor.AQUA))
                    .append(Component.text(" (" + r.getCompletedTrackCount() + " tracks)", NamedTextColor.GRAY))
            );
        }
    }

    @Subcommand("top")
    @CommandPermission("timingsystem.tournament.view")
    @Description("Show top tournament participants")
    public static void onTop(Player player) {
        // Show from current season
        Season season = SeasonManager.getOrCreateCurrentSeason();
        List<SeasonPoints> top = SeasonManager.getTopSeasonPoints(season.getId(), 10);

        if (top.isEmpty()) {
            player.sendMessage(Component.text("No season data yet.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("🏆 Season " + season.getName() + " — Top Players:", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (int i = 0; i < top.size(); i++) {
            SeasonPoints sp = top.get(i);
            String name = getPlayerName(sp.getUuid());
            player.sendMessage(
                Component.text("  #" + (i + 1) + " " + name, NamedTextColor.YELLOW)
                    .append(Component.text(" — " + sp.getTotalPoints() + " pts", NamedTextColor.GREEN))
                    .append(Component.text(" (" + sp.getTournamentsPlayed() + " tournaments, best #" + sp.getBestPosition() + ")", NamedTextColor.GRAY))
            );
        }
    }

    @Subcommand("history")
    @CommandPermission("timingsystem.tournament.view")
    @Description("Show tournament history")
    public static void onHistory(Player player) {
        List<Tournament> history = TournamentManager.getRecentTournaments(10);
        if (history.isEmpty()) {
            player.sendMessage(Component.text("No finished tournaments yet.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("🏆 Tournament History:", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (Tournament t : history) {
            player.sendMessage(
                Component.text("  " + t.getName(), NamedTextColor.WHITE)
                    .append(Component.text(" [" + t.getType().getDisplayName() + "]", NamedTextColor.YELLOW))
                    .append(Component.text(" — " + t.getState().name(), NamedTextColor.GRAY))
            );
        }
    }

    @Subcommand("rating")
    @CommandPermission("timingsystem.tournament.view")
    @Description("Show ELO rating")
    public static void onRating(Player player) {
        // Show own rating
        PlayerRating own = RatingManager.getOrCreateRating(player.getUniqueId());
        RatingRank rank = own.getRank();
        player.sendMessage(
            Component.text("📊 Your Rating: ", NamedTextColor.GOLD)
                .append(Component.text(rank.getIcon() + " " + own.getRating(), NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" (" + rank.getDisplayName() + ")", NamedTextColor.GRAY))
        );
        player.sendMessage(
            Component.text("   Peak: " + own.getPeakRating() + " | Games: " + own.getGamesPlayed() + " | Deviation: ±" + own.getDeviation(), NamedTextColor.GRAY)
        );

        // Show top 5
        List<PlayerRating> topRatings = RatingManager.getTopRatings(5);
        if (!topRatings.isEmpty()) {
            player.sendMessage(Component.text("\n📊 Top Rated Players:", NamedTextColor.GOLD, TextDecoration.BOLD));
            for (int i = 0; i < topRatings.size(); i++) {
                PlayerRating pr = topRatings.get(i);
                String name = getPlayerName(pr.getUuid());
                RatingRank r = pr.getRank();
                player.sendMessage(
                    Component.text("  #" + (i + 1) + " " + name, NamedTextColor.YELLOW)
                        .append(Component.text(" — " + r.getIcon() + " " + pr.getRating(), NamedTextColor.GREEN))
                        .append(Component.text(" (peak: " + pr.getPeakRating() + ")", NamedTextColor.GRAY))
                );
            }
        }
    }

    @Subcommand("season")
    @CommandPermission("timingsystem.tournament.view")
    @Description("Show current season info")
    public static void onSeason(Player player) {
        Season season = SeasonManager.getOrCreateCurrentSeason();
        String timeLeft = formatDuration(season.getRemainingMs());

        player.sendMessage(Component.text("📅 " + season.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("  Status: " + season.getState() + " | Time remaining: " + timeLeft, NamedTextColor.GRAY));

        // Show player's season stats
        SeasonPoints sp = SeasonManager.getPlayerPoints(player.getUniqueId(), season.getId());
        player.sendMessage(
            Component.text("  Your Points: ", NamedTextColor.YELLOW)
                .append(Component.text(sp.getTotalPoints() + " pts", NamedTextColor.GREEN))
                .append(Component.text(" (" + sp.getTournamentsPlayed() + " tournaments)", NamedTextColor.GRAY))
        );
    }

    @Subcommand("bracket")
    @CommandPermission("timingsystem.tournament.view")
    @Description("Show bracket info")
    public static void onBracket(Player player) {
        Tournament t = TournamentManager.getActiveTournament(TournamentType.BRACKET);
        if (t == null) {
            player.sendMessage(Component.text("No active bracket tournament.", NamedTextColor.GRAY));
            return;
        }

        List<BracketMatch> matches = TournamentManager.getBracketMatches(t.getId());
        if (matches.isEmpty()) {
            player.sendMessage(Component.text("Bracket not yet generated. Qualification in progress.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("🏆 Bracket: " + t.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));

        // Group by round
        Map<Integer, List<BracketMatch>> byRound = matches.stream()
            .collect(Collectors.groupingBy(BracketMatch::getRoundNumber));

        for (Map.Entry<Integer, List<BracketMatch>> entry : new TreeMap<>(byRound).entrySet()) {
            player.sendMessage(Component.text("  Round " + entry.getKey() + ":", NamedTextColor.YELLOW));
            for (BracketMatch m : entry.getValue()) {
                String p1 = m.getPlayer1Uuid() != null ? getPlayerName(m.getPlayer1Uuid()) : "TBD";
                String p2 = m.getPlayer2Uuid() != null ? getPlayerName(m.getPlayer2Uuid()) : "TBD";
                String bracket = m.isUpperBracket() ? "Upper" : "Lower";
                NamedTextColor stateColor = m.isCompleted() ? NamedTextColor.GREEN : m.isActive() ? NamedTextColor.AQUA : NamedTextColor.GRAY;
                player.sendMessage(
                    Component.text("    [" + bracket + "] ", NamedTextColor.GRAY)
                        .append(Component.text(p1 + " vs " + p2, stateColor))
                        .append(Component.text(" [" + m.getState() + "]", NamedTextColor.DARK_GRAY))
                );
            }
        }
    }

    // ─── ADMIN SUBCOMMANDS ───

    @Subcommand("admin create")
    @CommandPermission("timingsystem.tournament.admin")
    @Description("Create a tournament")
    public static void onAdminCreate(Player player, String typeName, String name, int durationDays, @Optional String trackIdList) {
        TournamentType type;
        try {
            type = TournamentType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid type. Use: SPRINT, RALLY, ENDURANCE, BRACKET", NamedTextColor.RED));
            return;
        }

        if (TournamentManager.getActiveTournament(type) != null) {
            player.sendMessage(Component.text("A tournament of type " + type.getDisplayName() + " is already active!", NamedTextColor.RED));
            return;
        }

        List<Integer> trackIds = new ArrayList<>();
        if (trackIdList != null && !trackIdList.isEmpty()) {
            for (String s : trackIdList.split(",")) {
                try {
                    trackIds.add(Integer.parseInt(s.trim()));
                } catch (NumberFormatException ignored) {}
                }
        }

        if (trackIds.size() < type.getMinTracks()) {
            player.sendMessage(Component.text("Need at least " + type.getMinTracks() + " track(s) for " + type.getDisplayName() + " tournament.", NamedTextColor.RED));
            return;
        }

        long now = System.currentTimeMillis();
        long end = now + (long) durationDays * 24 * 60 * 60 * 1000;

        Tournament t = TournamentManager.createTournament(name, type, trackIds, now, end, CarRestriction.ALL, 1, 5);
        if (t != null) {
            player.sendMessage(
                Component.text("✅ Tournament \"" + t.getName() + "\" created! ", NamedTextColor.GREEN)
                    .append(Component.text("Type: " + type.getDisplayName() + ", Duration: " + durationDays + " days", NamedTextColor.GRAY))
            );
        } else {
            player.sendMessage(Component.text("Failed to create tournament. A tournament of this type may already exist.", NamedTextColor.RED));
        }
    }

    @Subcommand("admin cancel")
    @CommandPermission("timingsystem.tournament.admin")
    @Description("Cancel an active tournament")
    public static void onAdminCancel(Player player, String typeName) {
        TournamentType type;
        try {
            type = TournamentType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid type. Use: SPRINT, RALLY, ENDURANCE, BRACKET", NamedTextColor.RED));
            return;
        }

        Tournament t = TournamentManager.getActiveTournament(type);
        if (t == null) {
            player.sendMessage(Component.text("No active tournament of type " + type.getDisplayName() + ".", NamedTextColor.RED));
            return;
        }

        TournamentManager.cancelTournament(t, "Cancelled by admin " + player.getName());
        player.sendMessage(Component.text("✅ Tournament \"" + t.getName() + "\" cancelled.", NamedTextColor.GREEN));
    }

    @Subcommand("admin season start")
    @CommandPermission("timingsystem.tournament.admin")
    @Description("Start a new season")
    public static void onAdminSeasonStart(Player player) {
        Season newSeason = SeasonManager.endCurrentAndStartNew();
        if (newSeason != null) {
            player.sendMessage(Component.text("✅ New season \"" + newSeason.getName() + "\" started! Ratings soft-reset.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to start new season.", NamedTextColor.RED));
        }
    }

    @Subcommand("admin bracket generate")
    @CommandPermission("timingsystem.tournament.admin")
    @Description("Generate bracket from qualification")
    public static void onAdminBracketGenerate(Player player) {
        Tournament t = TournamentManager.getActiveTournament(TournamentType.BRACKET);
        if (t == null || t.getState() != TournamentState.QUALIFYING) {
            player.sendMessage(Component.text("No bracket tournament in qualification phase.", NamedTextColor.RED));
            return;
        }

        TournamentManager.generateBracket(t);
        player.sendMessage(Component.text("✅ Bracket generated! Matches are now active.", NamedTextColor.GREEN));
    }

    @Subcommand("admin finish")
    @CommandPermission("timingsystem.tournament.admin")
    @Description("Force-finish a tournament")
    public static void onAdminFinish(Player player, String typeName) {
        TournamentType type;
        try {
            type = TournamentType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid type. Use: SPRINT, RALLY, ENDURANCE, BRACKET", NamedTextColor.RED));
            return;
        }

        Tournament t = TournamentManager.getActiveTournament(type);
        if (t == null) {
            player.sendMessage(Component.text("No active tournament of type " + type.getDisplayName() + ".", NamedTextColor.RED));
            return;
        }

        TournamentManager.calculateAndFinish(t);
        player.sendMessage(Component.text("✅ Tournament \"" + t.getName() + "\" force-finished.", NamedTextColor.GREEN));
    }

    // ─── HELPERS ───

    private static Tournament resolveTournament(Player player, String typeName) {
        if (typeName != null && !typeName.isEmpty()) {
            try {
                TournamentType type = TournamentType.valueOf(typeName.toUpperCase());
                Tournament t = TournamentManager.getActiveTournament(type);
                if (t == null) {
                    // Try recent finished
                    List<Tournament> recent = TournamentManager.getRecentTournaments(1);
                    if (!recent.isEmpty()) return recent.get(0);
                    player.sendMessage(Component.text("No tournament found.", NamedTextColor.RED));
                    return null;
                }
                return t;
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Invalid type.", NamedTextColor.RED));
                return null;
            }
        }
        // Default: first active or latest finished
        Collection<Tournament> active = TournamentManager.getActiveTournaments();
        if (!active.isEmpty()) return active.iterator().next();
        List<Tournament> recent = TournamentManager.getRecentTournaments(1);
        if (!recent.isEmpty()) return recent.get(0);
        player.sendMessage(Component.text("No tournament found.", NamedTextColor.RED));
        return null;
    }

    private static String getPlayerName(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    private static String formatTime(long ms) {
        if (ms <= 0) return "N/A";
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        if (hours > 0) return String.format("%d:%02d:%02d.%03d", hours, minutes % 60, seconds % 60, ms % 1000);
        if (minutes > 0) return String.format("%d:%02d.%03d", minutes, seconds % 60, ms % 1000);
        return String.format("%d.%03d", seconds, ms % 1000);
    }

    private static String formatDuration(long ms) {
        if (ms <= 0) return "ended";
        long hours = ms / (1000 * 60 * 60);
        long days = hours / 24;
        hours = hours % 24;
        if (days > 0) return days + "d " + hours + "h";
        long minutes = (ms / (1000 * 60)) % 60;
        return hours + "h " + minutes + "m";
    }
}
