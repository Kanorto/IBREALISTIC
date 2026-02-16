package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.economy.GarageManager;
import me.makkuusen.timing.system.gui.ShopGui;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.tplayer.TPlayer;
import org.bukkit.entity.Player;

@CommandAlias("shop")
public class CommandShop extends BaseCommand {

    @Default
    @CommandPermission("%permissiongarage_use")
    @Description("Open the upgrade shop")
    public static void onDefault(Player player) {
        if (!GarageManager.isEnabled()) {
            Text.send(player, Error.GARAGE_NOT_ENABLED);
            return;
        }

        TPlayer tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        new ShopGui(tPlayer).show(player);
    }
}
