package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.economy.GarageManager;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
import org.bukkit.entity.Player;

@CommandAlias("shop")
public class CommandShop extends BaseCommand {

    @Default
    @CommandCompletion("tire|suspension|engine|body|steering|brake|weight|type")
    @CommandPermission("%permissiongarage_use")
    @Description("Browse the upgrade shop")
    @Syntax("[component]")
    public static void onDefault(Player player, @Optional String component) {
        if (!GarageManager.isEnabled()) {
            Text.send(player, Error.GARAGE_NOT_ENABLED);
            return;
        }

        if (component == null || component.isEmpty()) {
            // Show all component categories
            Text.send(player, Info.GARAGE_SHOP_CATEGORIES);
            return;
        }

        String[] names = GarageManager.getNamesForComponent(component);
        if (names == null) {
            Text.send(player, Error.GARAGE_INVALID_COMPONENT);
            return;
        }

        Text.send(player, Info.GARAGE_SHOP_TITLE, "%component%", component.toUpperCase());

        for (short i = 0; i < names.length; i++) {
            int price = GarageManager.getPresetPrice(component, i);
            int level = GarageManager.getPresetLevel(component, i);
            Text.send(player, Info.GARAGE_SHOP_ENTRY,
                    "%name%", names[i],
                    "%cost%", String.valueOf(price),
                    "%level%", String.valueOf(level));
        }
    }
}
