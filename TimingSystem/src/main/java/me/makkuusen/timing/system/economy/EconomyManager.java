package me.makkuusen.timing.system.economy;

import me.makkuusen.timing.system.TimingSystem;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Wrapper around Vault Economy API.
 * Provides graceful fallback when Vault is not installed.
 */
public class EconomyManager {

    private static Economy vaultEconomy = null;
    private static boolean vaultAvailable = false;

    /**
     * Attempts to hook into Vault economy. Called during plugin enable.
     */
    public static void initialize() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            TimingSystem.getPlugin().getLogger().info("Vault not found. External economy disabled.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            TimingSystem.getPlugin().getLogger().warning("Vault found but no economy provider registered.");
            return;
        }
        vaultEconomy = rsp.getProvider();
        vaultAvailable = true;
        TimingSystem.getPlugin().getLogger().info("Vault economy hooked: " + vaultEconomy.getName());
    }

    public static boolean isVaultAvailable() {
        return vaultAvailable;
    }

    public static double getBalance(Player player) {
        if (!vaultAvailable) return 0;
        return vaultEconomy.getBalance(player);
    }

    public static boolean withdraw(Player player, double amount) {
        if (!vaultAvailable) return false;
        return vaultEconomy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public static boolean deposit(Player player, double amount) {
        if (!vaultAvailable) return false;
        return vaultEconomy.depositPlayer(player, amount).transactionSuccess();
    }

    public static String format(double amount) {
        if (!vaultAvailable) return String.valueOf(amount);
        return vaultEconomy.format(amount);
    }
}
