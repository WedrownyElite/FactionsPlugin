package me.elite.Factions.power;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Faction;
import me.elite.Factions.territory.ChunkCoord;
import me.elite.Factions.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PowerManager {
    private final FactionsPlugin plugin;
    private final Map<String, Faction> factions;
    private final Map<UUID, String> playerFactions;
    private final Map<String, Map<ChunkCoord, String>> worldClaims;

    public int getStartingPower() {
        return plugin.getConfigManager().getStartingPower();
    }

    public int getMaxPowerPerPlayer() {
        return plugin.getConfigManager().getMaxPowerPerPlayer();
    }

    public int getPowerPerChunk() {
        return plugin.getConfigManager().getPowerPerChunk();
    }

    public int getPowerRestorePerChunk() {
        return plugin.getConfigManager().getPowerRestorePerChunk();
    }

    public long getPowerRegenInterval() {
        return plugin.getConfigManager().getPowerRegenInterval();
    }

    public int getPowerRegenAmount() {
        return plugin.getConfigManager().getPowerRegenAmount();
    }

    /**
     * Get a human-readable string for the power regeneration interval
     */
    private String getPowerRegenIntervalString() {
        long intervalMs = getPowerRegenInterval();
        int amount = getPowerRegenAmount();

        double totalHours = intervalMs / (1000.0 * 60.0 * 60.0);

        // Handle common cases
        if (totalHours >= 24) {
            double days = totalHours / 24;
            if (days == Math.floor(days)) {
                long wholeDays = (long) days;
                return amount + " power per " + wholeDays + " " + (wholeDays == 1 ? "day" : "days") + " of playtime";
            }
        }

        if (totalHours >= 1) {
            if (totalHours == Math.floor(totalHours)) {
                long wholeHours = (long) totalHours;
                return amount + " power per " + wholeHours + " " + (wholeHours == 1 ? "hour" : "hours") + " of playtime";
            } else {
                return String.format("%d power per %.1f hours of playtime", amount, totalHours);
            }
        }

        // Handle minutes and seconds as before...
        long minutes = (long) (totalHours * 60);
        if (minutes >= 1) {
            return amount + " power per " + minutes + " " + (minutes == 1 ? "minute" : "minutes") + " of playtime";
        }

        long seconds = (long) (totalHours * 3600);
        return amount + " power per " + seconds + " " + (seconds == 1 ? "second" : "seconds") + " of playtime";
    }

    // Player power data: UUID -> PlayerPowerData
    private final Map<UUID, PlayerPowerData> playerPowerData = new HashMap<>();

    // Player session tracking for power regeneration
    private final Map<UUID, Long> playerLoginTimes = new HashMap<>();

    public PowerManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
        this.worldClaims = plugin.getWorldClaims();

        // Start power regeneration task (runs every 5 minutes)
        startPowerRegenTask();
    }

    /**
     * Initialize power data for a new player
     */
    public void initializePlayerPower(UUID playerUUID) {
        if (!playerPowerData.containsKey(playerUUID)) {
            playerPowerData.put(playerUUID, new PlayerPowerData(getStartingPower(), getMaxPowerPerPlayer()));
        }
    }

    /**
     * Get a player's current available power
     */
    public int getPlayerPower(UUID playerUUID) {
        PlayerPowerData data = playerPowerData.get(playerUUID);
        return data != null ? data.getCurrentPower() : getStartingPower();
    }

    /**
     * Get a player's maximum power
     */
    public int getPlayerMaxPower(UUID playerUUID) {
        PlayerPowerData data = playerPowerData.get(playerUUID);
        return data != null ? data.getMaxPower() : getMaxPowerPerPlayer();
    }

    /**
     * Get total faction power (sum of all members' power)
     */
    public int getFactionPower(String factionName) {
        Faction faction = factions.get(factionName);
        if (faction == null) return 0;

        int totalPower = 0;
        for (UUID memberUUID : faction.members.keySet()) {
            totalPower += getPlayerPower(memberUUID);
        }
        return totalPower;
    }

    /**
     * Get faction's maximum possible power (sum of all members' max power)
     */
    public int getFactionMaxPower(String factionName) {
        Faction faction = factions.get(factionName);
        if (faction == null) return 0;

        int totalMaxPower = 0;
        for (UUID memberUUID : faction.members.keySet()) {
            totalMaxPower += getPlayerMaxPower(memberUUID);
        }
        return totalMaxPower;
    }

    /**
     * Check if faction has enough power to claim a chunk
     */
    public boolean canFactionClaim(String factionName) {
        return getFactionPower(factionName) >= getPowerPerChunk();
    }
    /**
     * Consume power when claiming a chunk - deduct from leader first, then others
     * NO ongoing maintenance tracking
     */
    public boolean consumePowerForClaim(String factionName) {
        if (!canFactionClaim(factionName)) {
            return false;
        }

        Faction faction = factions.get(factionName);
        if (faction == null) return false;

        int powerNeeded = getPowerPerChunk();

        // Try faction leader first
        UUID leaderUUID = faction.owner;
        PlayerPowerData leaderData = playerPowerData.get(leaderUUID);
        if (leaderData != null && leaderData.getCurrentPower() >= powerNeeded) {
            leaderData.removePower(powerNeeded);
            return true;
        }

        // If leader doesn't have enough, try other members
        for (UUID memberUUID : faction.members.keySet()) {
            if (memberUUID.equals(leaderUUID)) continue; // Skip leader, already tried

            PlayerPowerData data = playerPowerData.get(memberUUID);
            if (data != null && data.getCurrentPower() >= powerNeeded) {
                data.removePower(powerNeeded);
                return true;
            }
        }

        return false;
    }

    /**
     * Restore power when unclaiming a chunk
     */
    public void restorePowerForUnclaim(String factionName) {
        Faction faction = factions.get(factionName);
        if (faction == null) return;

        // Give power back to faction owner first, then others
        UUID ownerUUID = faction.owner;
        PlayerPowerData ownerData = playerPowerData.get(ownerUUID);

        if (ownerData != null && ownerData.getCurrentPower() < ownerData.getMaxPower()) {
            ownerData.addPower(1); // Always restore 1 power
            return;
        }

        // If owner is maxed, give to another member
        for (UUID memberUUID : faction.members.keySet()) {
            if (!memberUUID.equals(ownerUUID)) {
                PlayerPowerData data = playerPowerData.get(memberUUID);
                if (data != null && data.getCurrentPower() < data.getMaxPower()) {
                    data.addPower(1); // Always restore 1 power
                    return;
                }
            }
        }
    }

    /**
     * Get power restored when unclaiming a chunk
     */
    public int getPowerRestoredPerChunk() {
        return 1;
    }

    /**
     * Get total claimed chunks from faction
     */
    public int getTotalClaimedChunks(String factionName) {
        int claimedChunks = 0;
        for (Map<ChunkCoord, String> worldClaim : worldClaims.values()) {
            for (String claimOwner : worldClaim.values()) {
                if (claimOwner.equals(factionName)) {
                    claimedChunks++;
                }
            }
        }
        return claimedChunks;
    }

    /**
     * Handle player login for power regeneration tracking
     */
    public void onPlayerLogin(UUID playerUUID) {
        initializePlayerPower(playerUUID);
        playerLoginTimes.put(playerUUID, System.currentTimeMillis());

        // Update power based on offline time (if they were offline for hours)
        updatePlayerPowerFromOfflineTime(playerUUID);
    }

    /**
     * Handle player logout
     */
    public void onPlayerLogout(UUID playerUUID) {
        // Update their playtime-based power before they log out
        updatePlayerPowerFromPlaytime(playerUUID);
        playerLoginTimes.remove(playerUUID);
    }

    /**
     * Update power based on playtime since login
     */
    private void updatePlayerPowerFromPlaytime(UUID playerUUID) {
        Long loginTime = playerLoginTimes.get(playerUUID);
        if (loginTime == null) return;

        PlayerPowerData data = playerPowerData.get(playerUUID);
        if (data == null) return;

        long currentTime = System.currentTimeMillis();
        long playtimeThisSession = currentTime - loginTime;

        // Calculate power to regenerate (1 power per hour)
        int powerToRegen = (int) (playtimeThisSession / getPowerRestoredPerChunk());

        if (powerToRegen > 0) {
            int newPower = Math.min(data.getCurrentPower() + powerToRegen, data.getMaxPower());
            data.setCurrentPower(newPower);
            data.setLastPowerUpdate(currentTime);

            // Update login time to account for the power we just regenerated
            playerLoginTimes.put(playerUUID, currentTime - (playtimeThisSession % getPowerRestoredPerChunk()));
        }
    }

    /**
     * Update power based on time offline (for when they log back in)
     */
    private void updatePlayerPowerFromOfflineTime(UUID playerUUID) {
        PlayerPowerData data = playerPowerData.get(playerUUID);
        if (data == null) return;

        long currentTime = System.currentTimeMillis();
        long timeSinceLastUpdate = currentTime - data.getLastPowerUpdate();

        // Only regenerate power if they were offline for at least an hour
        if (timeSinceLastUpdate >= getPowerRestoredPerChunk()) {
            int powerToRegen = (int) (timeSinceLastUpdate / getPowerRestoredPerChunk());
            int newPower = Math.min(data.getCurrentPower() + powerToRegen, data.getMaxPower());
            data.setCurrentPower(newPower);
            data.setLastPowerUpdate(currentTime);
        }
    }

    /**
     * Start the power regeneration task
     */
    private void startPowerRegenTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Update power for all online players
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    updatePlayerPowerFromPlaytime(onlinePlayer.getUniqueId());
                }
            }
        }.runTaskTimer(plugin, 20L * 60 * 5, 20L * 60 * 5); // Run every 5 minutes
    }

    /**
     * Send power information to a player
     */
    public void sendPowerInfo(Player player) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        // DEBUG LOGGING
        plugin.getLogger().info("DEBUG: /f power command for player " + player.getName() + " (UUID: " + uuid + ")");

        // Always show personal power first
        int personalCurrentPower = getPlayerPower(uuid);
        int personalMaxPower = getPlayerMaxPower(uuid);

        plugin.getLogger().info("DEBUG: Personal power - Current: " + personalCurrentPower + ", Max: " + personalMaxPower);

        MessageManager.sendBlankMessage(player, "");
        MessageManager.sendBlankMessage(player, "§6§l=== POWER INFORMATION ===");
        MessageManager.sendBlankMessage(player, "§eYour Power: §f" + personalCurrentPower + "§7/§f" + personalMaxPower);

        if (factionName != null) {
            // Player is in a faction - show faction power
            Faction faction = factions.get(factionName);
            plugin.getLogger().info("DEBUG: Faction " + factionName + " has " + faction.members.size() + " members:");

            int factionCurrentPower = getFactionPower(factionName);  // Current total power
            int factionMaxPower = getFactionMaxPower(factionName);            // Max possible power
            boolean canClaim = factionCurrentPower >= getPowerPerChunk();

            // Count claimed chunks for display
            int claimedChunks = getTotalClaimedChunks(factionName);

            // DEBUG: Log each member's power
            for (UUID memberUUID : faction.members.keySet()) {
                int memberCurrent = getPlayerPower(memberUUID);
                int memberMax = getPlayerMaxPower(memberUUID);
                String memberName = Bukkit.getOfflinePlayer(memberUUID).getName();
                plugin.getLogger().info("DEBUG:   Member " + memberName + " - Current: " + memberCurrent + ", Max: " + memberMax);
            }

            plugin.getLogger().info("DEBUG: Faction power calculations:");
            plugin.getLogger().info("DEBUG:   Current total: " + factionCurrentPower);
            plugin.getLogger().info("DEBUG:   Max total: " + factionMaxPower);
            plugin.getLogger().info("DEBUG:   Claimed chunks: " + claimedChunks);

            MessageManager.sendBlankMessage(player, "");
            MessageManager.sendBlankMessage(player, "§eFaction: §f" + factionName);
            MessageManager.sendBlankMessage(player, "§eFaction Power: §f" + factionCurrentPower + "§7/§f" + factionMaxPower);
            MessageManager.sendBlankMessage(player, "§eClaimed Chunks: §f" + claimedChunks);
            MessageManager.sendBlankMessage(player, "§eCan Claim: §f" + (canClaim ? "§aYes" : "§cNo"));
        } else {
            plugin.getLogger().info("DEBUG: Player is not in a faction");
            MessageManager.sendBlankMessage(player, "");
            MessageManager.sendBlankMessage(player, "§eFaction: §7None");
            MessageManager.sendBlankMessage(player, "§eCan Claim: §cNo (Join a faction)");
        }

        MessageManager.sendBlankMessage(player, "§6§l========================");
        MessageManager.sendBlankMessage(player, "§7• Power regenerates " + getPowerRegenIntervalString());
        MessageManager.sendBlankMessage(player, "§7• Each chunk costs " + getPowerPerChunk() + " power to claim (one-time)");
        MessageManager.sendBlankMessage(player, "§7• Unclaiming restores 1 power");
        MessageManager.sendBlankMessage(player, "§7• Maximum power per player: " + getMaxPowerPerPlayer());
    }

    // =================================================================
    // DATA PERSISTENCE METHODS
    // =================================================================

    /**
     * Get all player power data for saving
     */
    public Map<UUID, PlayerPowerData> getAllPlayerPowerData() {
        return playerPowerData;
    }

    /**
     * Load player power data from saved data
     */
    public void loadPlayerPowerData(Map<UUID, PlayerPowerData> data) {
        playerPowerData.clear();
        playerPowerData.putAll(data);
    }

    /**
     * Power information container class
     */
    public static class PowerInfo {
        private final int playerCurrentPower;
        private final int playerMaxPower;
        private final int factionBasePower;
        private final int factionEffectivePower;
        private final int factionUsedPower;
        private final int factionAvailablePower;
        private final int factionConsumedPower;

        public PowerInfo(int playerCurrentPower, int playerMaxPower, int factionBasePower,
                         int factionEffectivePower, int factionUsedPower, int factionAvailablePower,
                         int factionConsumedPower) {
            this.playerCurrentPower = playerCurrentPower;
            this.playerMaxPower = playerMaxPower;
            this.factionBasePower = factionBasePower;
            this.factionEffectivePower = factionEffectivePower;
            this.factionUsedPower = factionUsedPower;
            this.factionAvailablePower = factionAvailablePower;
            this.factionConsumedPower = factionConsumedPower;
        }

        public int getPlayerCurrentPower() { return playerCurrentPower; }
        public int getPlayerMaxPower() { return playerMaxPower; }
        public int getFactionBasePower() { return factionBasePower; }
        public int getFactionEffectivePower() { return factionEffectivePower; }
        public int getFactionUsedPower() { return factionUsedPower; }
        public int getFactionAvailablePower() { return factionAvailablePower; }
        public int getFactionConsumedPower() { return factionConsumedPower; }
    }
}