package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.ItemBuilder;
import me.makkuusen.timing.system.tournament.*;
import me.makkuusen.timing.system.tplayer.TPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GUI for viewing tournaments, ratings, and seasons.
 */
public class TournamentGui extends BaseGui {

    public TournamentGui(TPlayer tPlayer) {
        super(Component.text("🏆 Tournaments", NamedTextColor.GOLD, TextDecoration.BOLD), 6);

        // Fill border
        for (int i = 0; i < 54; i++) {
            setItem(GuiCommon.getBorderGlassButton(), i);
        }

        setActiveTournaments(tPlayer);
        setRatingSlot(tPlayer);
        setSeasonSlot(tPlayer);
        setNavigationRow(tPlayer);
    }

    // ─── ACTIVE TOURNAMENTS ───

    private void setActiveTournaments(TPlayer tPlayer) {
        Collection<Tournament> active = TournamentManager.getActiveTournaments();
        int[] slots = {10, 12, 14, 16}; // 4 tournament slots in row 2
        int index = 0;

        if (active.isEmpty()) {
            // Show "no active tournaments" placeholder
            ItemStack item = createInfoItem(
                Material.BARRIER,
                Component.text("No Active Tournaments", NamedTextColor.RED),
                List.of(
                    Component.text("No tournaments are currently", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("running. Check back later!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                )
            );
            GuiButton btn = new GuiButton(item);
            btn.setAction(() -> {});
            setItem(btn, 13);
            return;
        }

        for (Tournament t : active) {
            if (index >= slots.length) break;

            Material mat = switch (t.getType()) {
                case SPRINT -> Material.GOLDEN_APPLE;
                case RALLY -> Material.MAP;
                case ENDURANCE -> Material.CLOCK;
                case BRACKET -> Material.DIAMOND_SWORD;
            };

            NamedTextColor stateColor = switch (t.getState()) {
                case ACTIVE -> NamedTextColor.GREEN;
                case QUALIFYING -> NamedTextColor.AQUA;
                case SCHEDULED -> NamedTextColor.YELLOW;
                default -> NamedTextColor.GRAY;
            };

            String timeLeft = formatDuration(t.getRemainingMs());
            List<TournamentResult> results = TournamentManager.getResults(t.getId());

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Type: " + t.getType().getDisplayName(), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("State: " + t.getState().name(), stateColor).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Time Left: " + timeLeft, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Tracks: " + t.getTrackIds().size(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Participants: " + results.size(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Restriction: " + t.getCarRestriction().getDisplayName(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click for results", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));

            ItemStack item = createInfoItem(mat,
                Component.text(t.getName(), NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
                lore
            );
            GuiButton btn = new GuiButton(item);
            btn.setAction(() -> {
                if (tPlayer.getPlayer() != null) {
                    tPlayer.getPlayer().performCommand("tournament results " + t.getType().name());
                }
            });
            setItem(btn, slots[index]);
            index++;
        }

        // Also show recent finished tournaments
        List<Tournament> recent = TournamentManager.getRecentTournaments(4 - index);
        int[] recentSlots = {28, 30, 32, 34}; // row 4
        int recentIndex = 0;
        for (Tournament t : recent) {
            if (recentIndex >= recentSlots.length) break;
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Type: " + t.getType().getDisplayName(), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Status: " + t.getState().name(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click for results", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));

            ItemStack item = createInfoItem(Material.BOOK,
                Component.text(t.getName(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                lore
            );
            GuiButton btn = new GuiButton(item);
            btn.setAction(() -> {
                if (tPlayer.getPlayer() != null) {
                    tPlayer.getPlayer().performCommand("tournament results " + t.getType().name());
                }
            });
            setItem(btn, recentSlots[recentIndex]);
            recentIndex++;
        }
    }

    // ─── RATING ───

    private void setRatingSlot(TPlayer tPlayer) {
        PlayerRating rating = RatingManager.getOrCreateRating(tPlayer.getUniqueId());
        RatingRank rank = rating.getRank();

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(rank.getIcon() + " " + rank.getDisplayName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Rating: " + rating.getRating(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Peak: " + rating.getPeakRating(), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Games: " + rating.getGamesPlayed(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Deviation: ±" + rating.getDeviation(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

        // Rating progress bar
        int progress = Math.min(20, (rating.getRating() % 300) * 20 / 300);
        String bar = "█".repeat(progress) + "░".repeat(20 - progress);
        lore.add(Component.empty());
        lore.add(Component.text(bar, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Click for leaderboard", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));

        ItemStack item = createInfoItem(Material.EXPERIENCE_BOTTLE,
            Component.text("📊 Your Rating", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
            lore
        );
        GuiButton btn = new GuiButton(item);
        btn.setAction(() -> {
            if (tPlayer.getPlayer() != null) {
                tPlayer.getPlayer().performCommand("tournament rating");
            }
        });
        setItem(btn, 38);
    }

    // ─── SEASON ───

    private void setSeasonSlot(TPlayer tPlayer) {
        Season season = SeasonManager.getOrCreateCurrentSeason();
        SeasonPoints points = SeasonManager.getPlayerPoints(tPlayer.getUniqueId(), season.getId());

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Status: " + season.getState(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Time Left: " + formatDuration(season.getRemainingMs()), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Your Points: " + points.getTotalPoints() + " pts", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Tournaments: " + points.getTournamentsPlayed(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        if (points.getBestPosition() > 0) {
            lore.add(Component.text("Best Position: #" + points.getBestPosition(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Click for season details", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));

        ItemStack item = createInfoItem(Material.SUNFLOWER,
            Component.text("📅 " + season.getName(), NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
            lore
        );
        GuiButton btn = new GuiButton(item);
        btn.setAction(() -> {
            if (tPlayer.getPlayer() != null) {
                tPlayer.getPlayer().performCommand("tournament season");
            }
        });
        setItem(btn, 42);
    }

    // ─── NAVIGATION ───

    private void setNavigationRow(TPlayer tPlayer) {
        // Back button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
            .setName(Component.text("Back", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
            .build();
        GuiButton backBtn = new GuiButton(backItem);
        backBtn.setAction(() -> {
            if (tPlayer.getPlayer() != null) {
                tPlayer.getPlayer().closeInventory();
            }
        });
        setItem(backBtn, 45);

        // Refresh button
        ItemStack refreshItem = createInfoItem(Material.COMPASS,
            Component.text("Refresh", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
            List.of(Component.text("Click to refresh", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
        );
        GuiButton refreshBtn = new GuiButton(refreshItem);
        refreshBtn.setAction(() -> {
            if (tPlayer.getPlayer() != null) {
                new TournamentGui(tPlayer).show(tPlayer.getPlayer());
            }
        });
        setItem(refreshBtn, 53);
    }

    // ─── HELPERS ───

    private static ItemStack createInfoItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String formatDuration(long ms) {
        if (ms <= 0) return "Ended";
        long hours = ms / (1000 * 60 * 60);
        long days = hours / 24;
        hours = hours % 24;
        if (days > 0) return days + "d " + hours + "h";
        long minutes = (ms / (1000 * 60)) % 60;
        return hours + "h " + minutes + "m";
    }
}
