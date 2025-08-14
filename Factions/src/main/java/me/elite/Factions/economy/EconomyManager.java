package me.elite.Factions.economy;

import me.elite.Factions.FactionsPlugin;
import net.ess3.api.IEssentials;
import net.ess3.api.IUser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
    private final FactionsPlugin plugin;
    private IEssentials essentials;
    private boolean useEssentials = false;

    // Internal economy storage (if EssentialsX not available)
    private final Map<UUID, Double> playerBalances = new HashMap<>();
    private static final double STARTING_BALANCE = 10000.0;

    // Number formatter for currency display
    private final NumberFormat currencyFormatter;

    public EconomyManager(FactionsPlugin plugin) {
        this.plugin = plugin;

        // Initialize currency formatter with comma separators
        currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);

        // Alternative: Use DecimalFormat for more control
        // DecimalFormat df = new DecimalFormat("#,##0.00");
        // currencyFormatter = df;

        initializeEconomy();
    }

    private void initializeEconomy() {
        // Check for EssentialsX
        Plugin essentialsPlugin = Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentialsPlugin != null && essentialsPlugin.isEnabled()) {
            try {
                essentials = (IEssentials) essentialsPlugin;
                useEssentials = true;
                plugin.getLogger().info("EconomyManager: Using EssentialsX for economy");
            } catch (Exception e) {
                plugin.getLogger().warning("EconomyManager: Failed to hook into EssentialsX, using internal economy");
                useEssentials = false;
            }
        } else {
            plugin.getLogger().info("EconomyManager: EssentialsX not found, using internal economy");
            useEssentials = false;
        }
    }

    /**
     * Get a player's balance
     */
    public double getBalance(OfflinePlayer player) {
        if (useEssentials) {
            try {
                IUser user = essentials.getUser(player.getUniqueId());
                return user.getMoney().doubleValue();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to get balance from EssentialsX for " + player.getName());
                return 0.0;
            }
        } else {
            return playerBalances.getOrDefault(player.getUniqueId(), STARTING_BALANCE);
        }
    }

    /**
     * Set a player's balance
     */
    public boolean setBalance(OfflinePlayer player, double amount) {
        if (amount < 0) return false;

        if (useEssentials) {
            try {
                IUser user = essentials.getUser(player.getUniqueId());
                user.setMoney(BigDecimal.valueOf(amount));
                return true;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to set balance in EssentialsX for " + player.getName());
                return false;
            }
        } else {
            playerBalances.put(player.getUniqueId(), amount);
            return true;
        }
    }

    /**
     * Add money to a player's balance
     */
    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) return false;

        double currentBalance = getBalance(player);
        return setBalance(player, currentBalance + amount);
    }

    /**
     * Remove money from a player's balance
     */
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) return false;

        double currentBalance = getBalance(player);
        if (currentBalance < amount) return false;

        return setBalance(player, currentBalance - amount);
    }

    /**
     * Check if a player has at least the specified amount
     */
    public boolean hasBalance(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    /**
     * Format currency for display with proper comma separators
     */
    public String format(double amount) {
        return currencyFormatter.format(amount);
    }

    /**
     * Format currency for display without the currency symbol (just numbers with commas)
     */
    public String formatNumber(double amount) {
        NumberFormat numberFormatter = NumberFormat.getNumberInstance(Locale.US);
        numberFormatter.setMinimumFractionDigits(2);
        numberFormatter.setMaximumFractionDigits(2);
        return numberFormatter.format(amount);
    }

    /**
     * Format currency with custom symbol
     */
    public String formatWithSymbol(double amount, String symbol) {
        return symbol + formatNumber(amount);
    }

    /**
     * Initialize a new player with starting balance (internal economy only)
     */
    public void initializePlayer(UUID playerUUID) {
        if (!useEssentials && !playerBalances.containsKey(playerUUID)) {
            playerBalances.put(playerUUID, STARTING_BALANCE);
        }
    }

    /**
     * Get all player balances for saving (internal economy only)
     */
    public Map<UUID, Double> getAllBalances() {
        return new HashMap<>(playerBalances);
    }

    /**
     * Load player balances from saved data (internal economy only)
     */
    public void loadBalances(Map<UUID, Double> balances) {
        if (!useEssentials) {
            playerBalances.clear();
            playerBalances.putAll(balances);
        }
    }

    /**
     * Check if using EssentialsX
     */
    public boolean isUsingEssentials() {
        return useEssentials;
    }
}