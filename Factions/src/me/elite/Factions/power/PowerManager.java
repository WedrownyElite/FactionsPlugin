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

    // Power constants
    public static final int STARTING_POWER = 10;
    public static final int MAX_POWER_PER_PLAYER = 20;
    public static final int POWER_PER_CHUNK = 2;  // Claiming cost
    public static final int POWER_MAINTENANCE_PER_CHUNK = 1;  // Ongoing maintenance cost
    public static final int POWER_RESTORE_PER_CHUNK = 1;  // Unclaiming refund
    public static final long POWER_REGEN_INTERVAL = 60 * 60 * 1000L; // 1 hour in milliseconds
    public static final int POWER_REGEN_AMOUNT = 1;

    // Player power data: UUID -> PlayerPowerData
    private final Map<UUID, PlayerPowerData> playerPowerData = new HashMap<>();

    // Faction consumed power (power permanently lost from claiming): factionName -> consumedPower
    private final Map<String, Integer> factionConsumedPower = new HashMap<>();

    // Debug: Fake power for testing (simulates having more members)
    private final Map<String, Integer> factionFakePower = new HashMap<>();

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
            playerPowerData.put(playerUUID, new PlayerPowerData(STARTING_POWER, STARTING_POWER));
        }
    }

    /**
     * Get a player's current available power
     */
    public int getPlayerPower(UUID playerUUID) {
        PlayerPowerData data = playerPowerData.get(playerUUID);
        return data != null ? data.getCurrentPower() : STARTING_POWER;
    }

    /**
     * Get a player's maximum power
     */
    public int getPlayerMaxPower(UUID playerUUID) {
        PlayerPowerData data = playerPowerData.get(playerUUID);
        return data != null ? data.getMaxPower() : STARTING_POWER;
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
     * Get total power used by faction's claims (for ongoing maintenance)
     */
    public int getFactionUsedPower(String factionName) {
        int usedPower = 0;
        for (Map<ChunkCoord, String> worldClaim : worldClaims.values()) {
            for (String claimOwner : worldClaim.values()) {
                if (claimOwner.equals(factionName)) {
                    usedPower += POWER_MAINTENANCE_PER_CHUNK;  // Use maintenance cost, not claiming cost
                }
            }
        }
        return usedPower;
    }

    /**
     * Get total power permanently consumed by faction (from claiming fees)
     */
    public int getFactionConsumedPower(String factionName) {
        return factionConsumedPower.getOrDefault(factionName, 0);
    }

    /**
     * Get faction's total effective power (base power minus consumed power)
     */
    public int getFactionEffectivePower(String factionName) {
        Faction faction = factions.get(factionName);
        if (faction == null) return 0;

        int basePower = 0;
        for (UUID memberUUID : faction.members.keySet()) {
            basePower += getPlayerPower(memberUUID);
        }

        // Add fake power for testing
        int fakePower = factionFakePower.getOrDefault(factionName, 0);
        int totalBasePower = basePower + fakePower;

        int consumedPower = getFactionConsumedPower(factionName);
        return Math.max(0, totalBasePower - consumedPower);
    }

    /**
     * Check if faction is overclaimed (maintenance exceeds effective power)
     */
    public boolean isFactionOverclaimed(String factionName) {
        int effectivePower = getFactionEffectivePower(factionName);
        int maintenanceCost = getFactionUsedPower(factionName);
        return maintenanceCost > effectivePower;
    }

    /**
     * Get how much power a faction is overclaimed by
     */
    public int getFactionOverclaimedAmount(String factionName) {
        int effectivePower = getFactionEffectivePower(factionName);
        int maintenanceCost = getFactionUsedPower(factionName);
        return Math.max(0, maintenanceCost - effectivePower);
    }

    /**
     * Get faction's available power for claiming (can be negative if overclaimed)
     */
    public int getFactionAvailablePower(String factionName) {
        return getFactionEffectivePower(factionName) - getFactionUsedPower(factionName);
    }

    /**
     * Check if faction has enough power to claim a chunk
     */
    public boolean canFactionClaim(String factionName) {
        // Can't claim if already overclaimed or would become overclaimed
        return getFactionAvailablePower(factionName) >= POWER_MAINTENANCE_PER_CHUNK;
    }

    /**
     * Consume power when claiming a chunk (permanently removes power)
     */
    public boolean consumePowerForClaim(String factionName) {
        if (!canFactionClaim(factionName)) {
            return false;
        }

        // Permanently consume power for claiming
        int currentConsumed = factionConsumedPower.getOrDefault(factionName, 0);
        factionConsumedPower.put(factionName, currentConsumed + POWER_PER_CHUNK);

        return true;
    }

    /**
     * Restore power when unclaiming a chunk (gives back less than claiming cost)
     */
    public void restorePowerForUnclaim(String factionName) {
        int currentConsumed = factionConsumedPower.getOrDefault(factionName, 0);
        int newConsumed = Math.max(0, currentConsumed - POWER_RESTORE_PER_CHUNK);
        factionConsumedPower.put(factionName, newConsumed);
    }

    /**
     * Get power restored when unclaiming a chunk
     */
    public int getPowerRestoredPerChunk() {
        return POWER_RESTORE_PER_CHUNK;
    }

    /**
     * Get power information for a player
     */
    public PowerInfo getPlayerPowerInfo(UUID playerUUID) {
        initializePlayerPower(playerUUID);

        int currentPower = getPlayerPower(playerUUID);
        int maxPower = getPlayerMaxPower(playerUUID);

        String factionName = playerFactions.get(playerUUID);
        int factionBasePower = 0;
        int factionEffectivePower = 0;
        int factionUsedPower = 0;
        int factionAvailablePower = 0;
        int factionConsumedPowerAmount = 0;

        if (factionName != null) {
            factionBasePower = getFactionPower(factionName);
            factionConsumedPowerAmount = getFactionConsumedPower(factionName);
            factionEffectivePower = getFactionEffectivePower(factionName);
            factionUsedPower = getFactionUsedPower(factionName);
            factionAvailablePower = getFactionAvailablePower(factionName);
        }

        return new PowerInfo(currentPower, maxPower, factionBasePower, factionEffectivePower,
                factionUsedPower, factionAvailablePower, factionConsumedPowerAmount);
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
        int powerToRegen = (int) (playtimeThisSession / POWER_REGEN_INTERVAL);

        if (powerToRegen > 0) {
            int newPower = Math.min(data.getCurrentPower() + powerToRegen, data.getMaxPower());
            data.setCurrentPower(newPower);
            data.setLastPowerUpdate(currentTime);

            // Update login time to account for the power we just regenerated
            playerLoginTimes.put(playerUUID, currentTime - (playtimeThisSession % POWER_REGEN_INTERVAL));
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
        if (timeSinceLastUpdate >= POWER_REGEN_INTERVAL) {
            int powerToRegen = (int) (timeSinceLastUpdate / POWER_REGEN_INTERVAL);
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
        PowerInfo info = getPlayerPowerInfo(player.getUniqueId());

        MessageManager.sendBasicMessage(player, "§6§l=== POWER INFORMATION ===");
        MessageManager.sendBasicMessage(player, "§eYour Power: §f" + info.getPlayerCurrentPower() + "§7/§f" + info.getPlayerMaxPower());

        if (info.getFactionBasePower() > 0) {
            String factionName = playerFactions.get(player.getUniqueId());
            int realPower = getFactionPower(factionName);
            int fakePower = factionFakePower.getOrDefault(factionName, 0);
            int totalBasePower = realPower + fakePower;

            MessageManager.sendBasicMessage(player, "§eFaction: §f" + factionName);
            MessageManager.sendBasicMessage(player, "§eBase Power: §f" + totalBasePower + " §7(from all members)");
            if (fakePower > 0) {
                MessageManager.sendBasicMessage(player, "  §7Real: §f" + realPower + " §7+ Fake (Debug): §e" + fakePower);
            }
            MessageManager.sendBasicMessage(player, "§eConsumed Power: §c-" + info.getFactionConsumedPower() + " §7(from claiming fees)");
            MessageManager.sendBasicMessage(player, "§eEffective Power: §f" + info.getFactionEffectivePower());
            MessageManager.sendBasicMessage(player, "§eClaim Maintenance: §c-" + info.getFactionUsedPower() + " §7(ongoing cost)");
            MessageManager.sendBasicMessage(player, "§eAvailable Power: §a" + info.getFactionAvailablePower());
            MessageManager.sendBasicMessage(player, "§eCan Claim: §f" + (info.getFactionAvailablePower() >= 0 ? "§aYes" : "§cNo"));
        } else {
            MessageManager.sendBasicMessage(player, "§7You are not in a faction");
        }

        MessageManager.sendBasicMessage(player, "§6§l========================");
        MessageManager.sendBasicMessage(player, "§7• Power regenerates 1 per hour of playtime");
        MessageManager.sendBasicMessage(player, "§7• Each chunk claimed costs " + POWER_PER_CHUNK + " power");
        MessageManager.sendBasicMessage(player, "§7• Each chunk maintenance costs " + POWER_MAINTENANCE_PER_CHUNK + " power");
        MessageManager.sendBasicMessage(player, "§7• Unclaiming restores " + POWER_RESTORE_PER_CHUNK + " power per chunk");
        MessageManager.sendBasicMessage(player, "§7• Maximum power per player: " + MAX_POWER_PER_PLAYER);
    }

    /**
     * Handle when a player leaves a faction
     */
    public void onPlayerLeaveFaction(UUID playerUUID, String factionName) {
        // Check if faction is now overclaimed
        if (isFactionOverclaimed(factionName)) {
            int overclaimedAmount = getFactionOverclaimedAmount(factionName);
            int chunksToUnclaim = (int) Math.ceil((double) overclaimedAmount / POWER_MAINTENANCE_PER_CHUNK);

            // Notify the faction about overclaiming
            Faction faction = factions.get(factionName);
            if (faction != null) {
                for (UUID memberUUID : faction.members.keySet()) {
                    Player member = Bukkit.getPlayer(memberUUID);
                    if (member != null) {
                        MessageManager.sendBasicMessage(member, "§c§l⚠ FACTION OVERCLAIMED! ⚠");
                        MessageManager.sendBasicMessage(member, "§cA member left and your faction can no longer maintain all claims!");
                        MessageManager.sendBasicMessage(member, "§cOverclaimed by: §f" + overclaimedAmount + " §cpower");
                        MessageManager.sendBasicMessage(member, "§cMust unclaim at least: §f" + chunksToUnclaim + " §cchunks");
                        MessageManager.sendBasicMessage(member, "§cUse §f/f power §cto see details and §f/f unclaim §cor §f/f unclaimall");
                        MessageManager.sendBasicMessage(member, "§c§lWARNING: Overclaimed land may be vulnerable to enemy raids!");
                    }
                }
            }
        }
    }

    /**
     * Remove all consumed power for a disbanded faction
     */
    public void onFactionDisband(String factionName) {
        factionConsumedPower.remove(factionName);
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
     * Get all faction consumed power data for saving
     */
    public Map<String, Integer> getAllFactionConsumedPower() {
        return factionConsumedPower;
    }

    /**
     * Load player power data from saved data
     */
    public void loadPlayerPowerData(Map<UUID, PlayerPowerData> data) {
        playerPowerData.clear();
        playerPowerData.putAll(data);
    }

    /**
     * Load faction consumed power data from saved data
     */
    public void loadFactionConsumedPower(Map<String, Integer> data) {
        factionConsumedPower.clear();
        factionConsumedPower.putAll(data);
    }

    // =================================================================
    // DEBUG METHODS FOR TESTING
    // =================================================================

    /**
     * Debug: Add power to a player
     */
    public void debugAddPower(UUID playerUUID, int amount) {
        initializePlayerPower(playerUUID);
        PlayerPowerData data = playerPowerData.get(playerUUID);
        data.addPower(amount);
    }

    /**
     * Debug: Set a player's current power
     */
    public void debugSetPower(UUID playerUUID, int power) {
        initializePlayerPower(playerUUID);
        PlayerPowerData data = playerPowerData.get(playerUUID);
        data.setCurrentPower(power);
    }

    /**
     * Debug: Set a player's maximum power
     */
    public void debugSetMaxPower(UUID playerUUID, int maxPower) {
        initializePlayerPower(playerUUID);
        PlayerPowerData data = playerPowerData.get(playerUUID);
        data.setMaxPower(maxPower);
    }

    /**
     * Debug: Add fake power to a faction (simulates more members)
     */
    public void debugAddFakePower(String factionName, int amount) {
        int currentFake = factionFakePower.getOrDefault(factionName, 0);
        factionFakePower.put(factionName, currentFake + amount);
    }

    /**
     * Debug: Get fake power for a faction
     */
    public int debugGetFakePower(String factionName) {
        return factionFakePower.getOrDefault(factionName, 0);
    }

    /**
     * Debug: Simulate playtime for power regeneration
     */
    public int debugSimulatePlaytime(UUID playerUUID, double hours) {
        initializePlayerPower(playerUUID);
        PlayerPowerData data = playerPowerData.get(playerUUID);

        // Calculate power to gain (1 per hour)
        int powerToGain = (int) hours;
        int powerBefore = data.getCurrentPower();

        data.addPower(powerToGain);

        int powerAfter = data.getCurrentPower();
        return powerAfter - powerBefore; // Actual power gained (might be less due to max cap)
    }

    /**
     * Debug: Reset a player's power to defaults
     */
    public void debugResetPower(UUID playerUUID) {
        playerPowerData.put(playerUUID, new PlayerPowerData(STARTING_POWER, MAX_POWER_PER_PLAYER));
    }

    /**
     * Debug: Reset faction consumed power
     */
    public void debugResetFactionConsumedPower(String factionName) {
        factionConsumedPower.remove(factionName);
    }

    /**
     * Debug: Clear all fake power
     */
    public void debugClearFakePower() {
        factionFakePower.clear();
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