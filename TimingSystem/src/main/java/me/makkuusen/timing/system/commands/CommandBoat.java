package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.boatutils.BoatUtilsManager;
import me.makkuusen.timing.system.boatutils.BoatUtilsMode;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.sounds.PlaySound;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CommandAlias("boat|b")
public class CommandBoat extends BaseCommand {

    private static final long COOLDOWN_MS = 2000;
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    @Default
    @CommandPermission("%permissiontimingsystem_boat")
    public static void onBoat(Player player) {
        if (isPlayerInBoat(player)) {
            return;
        }

        if (!player.isOnGround()) {
            Text.send(player, Error.NOT_NOW);
            return;
        }

        if (isOnCooldown(player)) {
            Text.send(player, Error.NOT_NOW);
            return;
        }

        if (TimeTrialController.lastTimeTrialTrack.containsKey(player.getUniqueId())) {
            Track track = TimeTrialController.lastTimeTrialTrack.get(player.getUniqueId());
            ApiUtilities.spawnBoatAndAddPlayerWithBoatUtils(player, player.getLocation(), track, true);
            if (track.isBoatUtils()) {
                PlaySound.boatUtilsEffect(TSDatabase.getPlayer(player.getUniqueId()));
            }
            return;
        }
        ApiUtilities.spawnBoatAndAddPlayer(player, player.getLocation());
    }

    public static void clearCooldown(UUID uuid) {
        cooldowns.remove(uuid);
    }

    private static boolean isOnCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(uuid);
        if (lastUse != null && (now - lastUse) < COOLDOWN_MS) {
            return true;
        }
        cooldowns.put(uuid, now);
        return false;
    }

    private static boolean isPlayerInBoat(Player p) {
        return p.getVehicle() instanceof Boat boat;
    }

    @Subcommand("mode")
    @CommandCompletion("@boatUtilsMode")
    @CommandPermission("%permissiontimingsystem_boat_mode")
    public static void onBoatWithMode(Player player, BoatUtilsMode mode) {
        if (isPlayerInBoat(player)) {
            return;
        }

        if (!player.isOnGround()) {
            Text.send(player, Error.NOT_NOW);
            return;
        }

        ApiUtilities.spawnBoatAndAddPlayer(player, player.getLocation());
        BoatUtilsManager.sendBoatUtilsModePluginMessage(player, mode, null, false);
    }
}
