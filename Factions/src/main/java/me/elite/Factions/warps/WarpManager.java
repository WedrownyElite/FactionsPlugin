package me.elite.Factions.warps;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.FactionPermission;
import me.elite.Factions.data.Rank;
import me.elite.Factions.utils.MessageManager;
import me.elite.combattag.CombatTagAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class WarpManager {
    private final FactionsPlugin plugin;
    private final Map<String, Faction> factions;
    private final Map<UUID, String> playerFactions;

    // Teleport tasks for cancellation
    private final Map<UUID, BukkitTask> teleportTasks = new HashMap<>();

    /**
     * Get warp limit for a faction
     */
    public int getWarpLimit(Faction faction) {
        // TODO: Later can be expanded with faction upgrades
        return plugin.getConfigManager().getDefaultWarpLimit();
    }

    public WarpManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
    }

    /**
     * Set a warp for a faction
     */
    public boolean setWarp(Player player, String warpName) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return false;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        // Check permission
        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.MANAGE_WARPS)) {
            MessageManager.sendError(player, "You lack permission to manage warps.");
            return false;
        }

        // Validate warp name
        if (!isValidWarpName(warpName)) {
            MessageManager.sendError(player, "Invalid warp name! Use only letters, numbers, and underscores (max 16 characters).");
            return false;
        }

        // Check if warp already exists
        if (faction.warps.containsKey(warpName)) {
            MessageManager.sendError(player, "A warp named '" + warpName + "' already exists! Use /f delwarp " + warpName + " to remove it first.");
            return false;
        }

        // Check warp limit
        int currentWarpCount = faction.warps.size();
        int warpLimit = getWarpLimit(faction);

        int maxWarps = plugin.getConfigManager().getMaxWarps();
        if (maxWarps > 0 && currentWarpCount >= maxWarps) {
            MessageManager.sendError(player, "Warp limit reached! Your faction can have " + warpLimit + " warp(s). Delete a warp first.");
            return false;
        }

        //  Warps can be set anywhere
        Location location = player.getLocation();

        // Create and add the warp
        FactionWarp warp = new FactionWarp(warpName, location, uuid);
        faction.warps.put(warpName, warp);

        MessageManager.sendSuccess(player, "Warp '" + warpName + "' has been set at your current location!");
        MessageManager.sendInfo(player, "Warps: " + faction.warps.size() + "/" + warpLimit);

        // Notify other online faction members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(player)) {
                Rank memberRank = faction.members.get(memberUUID);
                if (faction.hasPermission(memberRank, FactionPermission.WARPS_ACCESS)) {
                    MessageManager.sendMemberBasicMessage(member, ChatColor.YELLOW + player.getName() + " set warp '" + warpName + "'!");
                }
            }
        }

        // Save data
        plugin.getDataManager().saveFactionData();
        return true;
    }

    /**
     * Delete a warp
     */
    public boolean deleteWarp(Player player, String warpName) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return false;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        // Check permission
        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.MANAGE_WARPS)) {
            MessageManager.sendError(player, "You lack permission to manage warps.");
            return false;
        }

        // Check if warp exists (case-insensitive)
        String actualWarpName = null;
        for (String existingWarpName : faction.warps.keySet()) {
            if (existingWarpName.equalsIgnoreCase(warpName)) {
                actualWarpName = existingWarpName;
                break;
            }
        }

        if (actualWarpName == null) {
            MessageManager.sendError(player, "No warp named '" + warpName + "' exists!");
            return false;
        }

        // Remove the warp (using actual case-sensitive name)
        faction.warps.remove(actualWarpName);

        MessageManager.sendSuccess(player, "Warp '" + actualWarpName + "' has been deleted!");
        MessageManager.sendInfo(player, "Warps: " + faction.warps.size() + "/" + getWarpLimit(faction));

        // Notify other online faction members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(player)) {
                Rank memberRank = faction.members.get(memberUUID);
                if (faction.hasPermission(memberRank, FactionPermission.WARPS_ACCESS)) {
                    MessageManager.sendMemberBasicMessage(member, ChatColor.YELLOW + player.getName() + " deleted warp '" + actualWarpName + "'!");
                }
            }
        }

        // Save data
        plugin.getDataManager().saveFactionData();
        return true;
    }

    /**
     * Warp to a specific warp
     */
    public boolean warpTo(Player player, String warpName) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return false;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        // Check permission
        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.WARPS_ACCESS)) {
            MessageManager.sendError(player, "You lack permission to use warps.");
            return false;
        }

        // Check if warp exists (case-insensitive)
        FactionWarp warp = null;
        String actualWarpName = null;

        // Find warp by case-insensitive name
        for (Map.Entry<String, FactionWarp> entry : faction.warps.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(warpName)) {
                warp = entry.getValue();
                actualWarpName = entry.getKey();
                break;
            }
        }

        if (warp == null) {
            MessageManager.sendError(player, "No warp named '" + warpName + "' exists!");
            return false;
        }

        // Check combat tag
        if (CombatTagAPI.isAvailable() && CombatTagAPI.isInCombat(player)) {
            int combatTime = CombatTagAPI.getRemainingCombatTime(player);
            MessageManager.sendError(player, "You cannot warp while in combat! (" + combatTime + "s remaining)");
            return false;
        }

        // Check if there's already a pending teleport
        if (teleportTasks.containsKey(uuid)) {
            MessageManager.sendError(player, "You already have a pending teleport!");
            return false;
        }

        // Validate warp location
        Location warpLocation = warp.getLocation();
        if (warpLocation == null || warpLocation.getWorld() == null) {
            MessageManager.sendError(player, "Warp location is invalid! Contact an admin.");
            return false;
        }

        // Start teleport countdown (use actual warp name for display)
        startTeleportCountdown(player, warpLocation, "warp '" + actualWarpName + "'");
        return true;
    }

    /**
     * List all available warps for a player
     */
    public void listWarps(Player player) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        // Check permission
        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.WARPS_ACCESS)) {
            MessageManager.sendError(player, "You lack permission to view warps.");
            return;
        }

        if (faction.warps.isEmpty()) {
            MessageManager.sendInfo(player, "Your faction has no warps set.");
            MessageManager.sendInfo(player, "Use /f setwarp <name> to create one!");
            return;
        }

        int warpLimit = getWarpLimit(faction);
        MessageManager.sendBlankMessage(player, "");
        MessageManager.sendBlankMessage(player, ChatColor.GOLD + "=== FACTION WARPS (" + faction.warps.size() + "/" + warpLimit + ") ===");

        for (Map.Entry<String, FactionWarp> entry : faction.warps.entrySet()) {
            String name = entry.getKey();
            FactionWarp warp = entry.getValue();
            Location loc = warp.getLocation();

            String coordinates = loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
            String world = loc.getWorld().getName();

            MessageManager.sendBlankMessage(player, ChatColor.YELLOW + "• " + ChatColor.WHITE + name +
                    ChatColor.GRAY + " (" + world + " at " + coordinates + ")");
        }

        MessageManager.sendBlankMessage(player, "");
        MessageManager.sendBlankMessage(player, ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/f warp <name>" + ChatColor.GRAY + " to teleport");

        // Show manage options for admins
        if (playerRank == Rank.OWNER || faction.hasPermission(playerRank, FactionPermission.MANAGE_WARPS)) {
            MessageManager.sendBlankMessage(player, ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/f setwarp <name>" + ChatColor.GRAY + " to create");
            MessageManager.sendBlankMessage(player, ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/f delwarp <name>" + ChatColor.GRAY + " to delete");
        }
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
     * Validate warp name
     */
    private boolean isValidWarpName(String name) {
        return name != null &&
                !name.isEmpty() &&
                name.length() <= 16 &&
                name.matches("[a-zA-Z0-9_]+");
    }

    /**
     * Get warps for data persistence
     */
    public Map<String, FactionWarp> getFactionsWarps(String factionName) {
        Faction faction = factions.get(factionName);
        return faction != null ? faction.warps : new HashMap<>();
    }
}