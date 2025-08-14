package me.elite.Factions.economy;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.BankLog;
import me.elite.Factions.data.Faction;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BankLogsManager {
    private final FactionsPlugin plugin;
    private final Map<String, List<BankLog>> factionBankLogs = new ConcurrentHashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yyyy");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");

    // Configuration
    private static final int MAX_LOGS_PER_FACTION = 100; // Keep last 100 logs per faction

    public BankLogsManager(FactionsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Add a bank log entry for a faction
     */
    public void addBankLog(String factionName, UUID playerUUID, String playerName,
                           BankLog.BankLogType type, double amount, double oldBalance, double newBalance) {
        BankLog log = new BankLog(playerUUID, playerName, type, amount, oldBalance, newBalance);

        factionBankLogs.computeIfAbsent(factionName, k -> new ArrayList<>()).add(log);

        // Keep only the most recent logs to prevent memory issues
        List<BankLog> logs = factionBankLogs.get(factionName);
        if (logs.size() > MAX_LOGS_PER_FACTION) {
            // Remove oldest logs (from the beginning of the list)
            logs.subList(0, logs.size() - MAX_LOGS_PER_FACTION).clear();
        }

        plugin.getLogger().info("Added bank log for " + factionName + ": " + type.getDisplayName() +
                " " + plugin.getEconomyManager().format(amount) + " by " + playerName);
    }

    /**
     * Get bank logs for a faction, sorted by most recent first
     */
    public List<BankLog> getBankLogs(String factionName) {
        List<BankLog> logs = factionBankLogs.getOrDefault(factionName, new ArrayList<>());
        // Return a copy sorted by timestamp (most recent first)
        List<BankLog> sortedLogs = new ArrayList<>(logs);
        sortedLogs.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return sortedLogs;
    }

    /**
     * Get bank logs grouped by date, with most recent date first
     */
    public Map<String, List<BankLog>> getBankLogsGroupedByDate(String factionName) {
        List<BankLog> logs = getBankLogs(factionName);
        Map<String, List<BankLog>> groupedLogs = new LinkedHashMap<>();

        for (BankLog log : logs) {
            String date = dateFormat.format(new Date(log.timestamp));
            groupedLogs.computeIfAbsent(date, k -> new ArrayList<>()).add(log);
        }

        return groupedLogs;
    }

    /**
     * Format a timestamp to time string (e.g., "2:50 PM")
     */
    public String formatTime(long timestamp) {
        return timeFormat.format(new Date(timestamp));
    }

    /**
     * Format a timestamp to date string (e.g., "8/14/2025")
     */
    public String formatDate(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }

    /**
     * Clear all logs for a faction (used when faction is disbanded)
     */
    public void clearFactionLogs(String factionName) {
        factionBankLogs.remove(factionName);
        plugin.getLogger().info("Cleared bank logs for disbanded faction: " + factionName);
    }

    /**
     * Get all faction bank logs for saving
     */
    public Map<String, List<BankLog>> getAllFactionLogs() {
        return new HashMap<>(factionBankLogs);
    }

    /**
     * Load faction bank logs from saved data
     */
    public void loadFactionLogs(Map<String, List<BankLog>> savedLogs) {
        factionBankLogs.clear();
        if (savedLogs != null) {
            factionBankLogs.putAll(savedLogs);
            int totalLogs = savedLogs.values().stream().mapToInt(List::size).sum();
            plugin.getLogger().info("Loaded " + totalLogs + " bank logs for " + savedLogs.size() + " factions");
        }
    }

    /**
     * Get total number of logs across all factions (for statistics)
     */
    public int getTotalLogCount() {
        return factionBankLogs.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Get number of logs for a specific faction
     */
    public int getLogCount(String factionName) {
        return factionBankLogs.getOrDefault(factionName, new ArrayList<>()).size();
    }
}