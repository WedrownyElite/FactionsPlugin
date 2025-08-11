package me.elite.Factions.homes;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.FactionPermission;
import me.elite.Factions.data.Rank;
import me.elite.Factions.territory.ChunkCoord;
import me.elite.Factions.utils.MessageManager;
import me.elite.combattag.CombatTagAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeManager {
    private final FactionsPlugin plugin;
    private final Map<String, Faction> factions;
    private final Map<UUID, String> playerFactions;
    private final Map<String, Map<ChunkCoord, String>> worldClaims;

    // Teleport tasks for cancellation
    private final Map<UUID, BukkitTask> teleportTasks = new HashMap<>();

    public HomeManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
        this.worldClaims = plugin.getWorldClaims();
    }

    /**
     * Set faction home
     */
    public boolean setHome(Player player) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return false;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        // Check permission
        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.SET_HOME)) {
            MessageManager.sendError(player, "You lack permission to set the faction home.");
            return false;
        }

        // Check if location is in faction territory
        Location location = player.getLocation();
        ChunkCoord coord = new ChunkCoord(location.getChunk().getX(), location.getChunk().getZ());
        String chunkOwner = plugin.getUtilityManager().getFactionAtChunk(location.getWorld(), coord);

        if (!factionName.equals(chunkOwner)) {
            MessageManager.sendError(player, "You can only set the faction home in your faction's territory!");
            return false;
        }

        // Set the home
        FactionHome home = new FactionHome(location, uuid);
        faction.home = home;

        MessageManager.sendSuccess(player, "Faction home has been set at your current location!");

        // Notify other online faction members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(player)) {
                Rank memberRank = faction.members.get(memberUUID);
                if (memberRank == Rank.OWNER || faction.hasPermission(memberRank, FactionPermission.USE_HOME)) {
                    MessageManager.sendMemberBasicMessage(member, ChatColor.YELLOW + player.getName() + " set the faction home!");
                }
            }
        }

        // Save data
        plugin.getDataManager().saveFactionData();
        return true;
    }

    /**
     * Delete faction home
     */
    public boolean deleteHome(Player player) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return false;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        // Check permission
        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.SET_HOME)) {
            MessageManager.sendError(player, "You lack permission to delete the faction home.");
            return false;
        }

        // Check if home exists
        if (faction.home == null) {
            MessageManager.sendError(player, "Your faction doesn't have a home set!");
            return false;
        }

        // Delete the home
        faction.home = null;

        MessageManager.sendSuccess(player, "Faction home has been deleted!");

        // Notify other online faction members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(player)) {
                Rank memberRank = faction.members.get(memberUUID);
                if (memberRank == Rank.OWNER || faction.hasPermission(memberRank, FactionPermission.USE_HOME)) {
                    MessageManager.sendMemberBasicMessage(member, ChatColor.YELLOW + player.getName() + " deleted the faction home!");
                }
            }
        }

        // Save data
        plugin.getDataManager().saveFactionData();
        return true;
    }

    /**
     * Teleport to faction home
     */
    public boolean goHome(Player player) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return false;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        // Check permission
        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.USE_HOME)) {
            MessageManager.sendError(player, "You lack permission to use the faction home.");
            return false;
        }

        // Check if home exists
        if (faction.home == null) {
            MessageManager.sendError(player, "Your faction doesn't have a home set!");
            MessageManager.sendInfo(player, "Use /f sethome to set one!");
            return false;
        }

        // Check combat tag
        if (CombatTagAPI.isAvailable() && CombatTagAPI.isInCombat(player)) {
            int combatTime = CombatTagAPI.getRemainingCombatTime(player);
            MessageManager.sendError(player, "You cannot teleport home while in combat! (" + combatTime + "s remaining)");
            return false;
        }

        // Check if there's already a pending teleport
        if (teleportTasks.containsKey(uuid)) {
            MessageManager.sendError(player, "You already have a pending teleport!");
            return false;
        }

        // Validate home location (check if chunk is still claimed)
        Location homeLocation = faction.home.getLocation();
        if (homeLocation == null || homeLocation.getWorld() == null) {
            MessageManager.sendError(player, "Faction home location is invalid! Contact an admin.");
            return false;
        }

        ChunkCoord coord = new ChunkCoord(homeLocation.getChunk().getX(), homeLocation.getChunk().getZ());
        String chunkOwner = plugin.getUtilityManager().getFactionAtChunk(homeLocation.getWorld(), coord);

        if (!factionName.equals(chunkOwner)) {
            MessageManager.sendError(player, "Faction home is no longer in your territory! It has been automatically removed.");

            // Remove the invalid home
            faction.home = null;
            plugin.getDataManager().saveFactionData();

            return false;
        }

        // Start teleport countdown
        startTeleportCountdown(player, homeLocation, "faction home");
        return true;
    }

    /**
     * Start teleport countdown
     */
    private void startTeleportCountdown(Player player, Location destination, String destinationType) {
        UUID uuid = player.getUniqueId();
        Location startLocation = player.getLocation().clone();

        MessageManager.sendInfo(player, "Teleporting to " + destinationType + " in 5 seconds...");
        MessageManager.sendInfo(player, ChatColor.RED + "Don't move or take damage!");

        BukkitTask task = new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                // Check if player is still online
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                // Check if player moved
                if (hasPlayerMoved(player.getLocation(), startLocation)) {
                    MessageManager.sendError(player, "Teleport cancelled - you moved!");
                    cancel();
                    return;
                }

                // Check combat tag
                if (CombatTagAPI.isAvailable() && CombatTagAPI.isInCombat(player)) {
                    MessageManager.sendError(player, "Teleport cancelled - you entered combat!");
                    cancel();
                    return;
                }

                if (countdown <= 0) {
                    // Teleport the player
                    player.teleport(destination);
                    MessageManager.sendSuccess(player, "Teleported to " + destinationType + "!");
                    cancel();
                } else {
                    // Show countdown
                    if (countdown <= 3) {
                        MessageManager.sendInfo(player, ChatColor.YELLOW + "Teleporting in " + countdown + "...");
                    }
                    countdown--;
                }
            }

            @Override
            public void cancel() {
                super.cancel();
                teleportTasks.remove(uuid);
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second

        teleportTasks.put(uuid, task);
    }

    /**
     * Check if player has moved significantly
     */
    private boolean hasPlayerMoved(Location current, Location start) {
        if (!current.getWorld().equals(start.getWorld())) {
            return true;
        }

        double distance = current.distance(start);
        return distance > 0.1; // Allow tiny movements due to floating point precision
    }

    /**
     * Cancel pending teleport for a player
     */
    public void cancelTeleport(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = teleportTasks.get(uuid);
        if (task != null) {
            task.cancel();
            teleportTasks.remove(uuid);
        }
    }

    /**
     * Validate faction home location (called when chunks are unclaimed)
     */
    public void validateHomeInTerritory(String factionName) {
        Faction faction = factions.get(factionName);
        if (faction == null || faction.home == null) {
            return;
        }

        Location homeLocation = faction.home.getLocation();
        if (homeLocation == null || homeLocation.getWorld() == null) {
            // Invalid location, remove home
            faction.home = null;
            plugin.getDataManager().saveFactionData();
            return;
        }

        ChunkCoord coord = new ChunkCoord(homeLocation.getChunk().getX(), homeLocation.getChunk().getZ());
        String chunkOwner = plugin.getUtilityManager().getFactionAtChunk(homeLocation.getWorld(), coord);

        if (!factionName.equals(chunkOwner)) {
            // Home is no longer in faction territory, remove it
            faction.home = null;

            // Notify online faction members
            for (UUID memberUUID : faction.members.keySet()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null) {
                    MessageManager.sendError(member, "Faction home was removed because the chunk is no longer claimed!");
                }
            }

            plugin.getDataManager().saveFactionData();
        }
    }

    /**
     * Get faction home for data persistence
     */
    public FactionHome getFactionHome(String factionName) {
        Faction faction = factions.get(factionName);
        return faction != null ? faction.home : null;
    }
}