package me.elite.Factions.territory;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Faction;
import me.elite.Factions.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.List;
import java.util.ArrayList;

import java.util.*;

public class ClaimManager {
    private final FactionsPlugin plugin;
    private final Map<String, Map<ChunkCoord, String>> worldClaims;
    private final Map<String, Faction> factions;
    private final Map<UUID, String> playerFactions;

    public ClaimManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.worldClaims = plugin.getWorldClaims();
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
    }

    public String getFactionAtChunk(World world, ChunkCoord coord) {
        Map<ChunkCoord, String> claims = worldClaims.get(world.getName());
        if (claims == null) return null;
        return claims.get(coord);
    }

    public boolean claimChunk(Player player, String factionName) {
        Chunk chunk = player.getLocation().getChunk();
        String world = player.getWorld().getName();
        worldClaims.putIfAbsent(world, new HashMap<>());
        Map<ChunkCoord, String> claims = worldClaims.get(world);

        ChunkCoord coord = new ChunkCoord(chunk.getX(), chunk.getZ());
        String existingClaim = claims.get(coord);
        if (existingClaim != null && !existingClaim.equalsIgnoreCase("Wilderness")) {
            MessageManager.sendError(player,"This chunk is already claimed by: " + existingClaim);
            return false;
        }

        // Check faction power
        if (!plugin.getPowerManager().canFactionClaim(factionName)) {
            int availablePower = plugin.getPowerManager().getFactionPower(factionName);
            MessageManager.sendError(player, "Insufficient faction power to claim! Available: " + availablePower + ", Required: " + plugin.getPowerManager().getPowerPerChunk());
            return false;
        }

        // Consume power for the claim
        if (!plugin.getPowerManager().consumePowerForClaim(factionName)) {
            MessageManager.sendError(player, "Failed to consume power for claim!");
            return false;
        }

        claims.put(coord, factionName);
        MessageManager.sendSuccess(player,"Chunk claimed for faction: " + factionName);

        // Show remaining power
        int remainingPower = plugin.getPowerManager().getFactionPower(factionName);
        MessageManager.sendInfo(player, "Available power: " + remainingPower);

        return true;
    }

    public boolean unclaimChunk(Player player, String factionName) {
        Chunk chunk = player.getLocation().getChunk();
        String world = player.getWorld().getName();
        Map<ChunkCoord, String> claims = worldClaims.get(world);
        if (claims == null) {
            MessageManager.sendError(player,"No claims found in this world.");
            return false;
        }

        ChunkCoord coord = new ChunkCoord(chunk.getX(), chunk.getZ());
        String currentOwner = claims.get(coord);
        if (currentOwner == null) {
            MessageManager.sendError(player,"This chunk is not claimed.");
            return false;
        }

        if (!currentOwner.equals(factionName)) {
            MessageManager.sendError(player,"This chunk is not owned by your faction.");
            return false;
        }

        claims.remove(coord);
        claims.put(coord, "Wilderness");

        // Restore power
        plugin.getPowerManager().restorePowerForUnclaim(factionName);

        // ADDED: Validate faction home in unclaimed territory
        plugin.getHomeManager().validateHomeInTerritory(factionName);

        MessageManager.sendInfo(player,"Chunk unclaimed and returned to Wilderness.");

        // Show current available power
        int availablePower = plugin.getPowerManager().getFactionPower(factionName);
        MessageManager.sendInfo(player, "Available power: " + availablePower);

        return true;
    }

    public int unclaimAll(Player player, String factionName) {
        String world = player.getWorld().getName();
        Map<ChunkCoord, String> claims = worldClaims.get(world);
        if (claims == null) {
            MessageManager.sendError(player,"No claims found in this world.");
            return 0;
        }

        int unclaimedCount = 0;

        // Create a list of chunks to unclaim to avoid ConcurrentModificationException
        List<ChunkCoord> chunksToUnclaim = new ArrayList<>();

        // First pass: identify chunks to unclaim
        for (Map.Entry<ChunkCoord, String> entry : claims.entrySet()) {
            if (entry.getValue().equals(factionName)) {
                chunksToUnclaim.add(entry.getKey());
            }
        }

        // Second pass: actually unclaim them
        for (ChunkCoord coord : chunksToUnclaim) {
            claims.remove(coord);
            claims.put(coord, "Wilderness");
            unclaimedCount++;

            // Restore power for each unclaimed chunk
            if (plugin.getPowerManager() != null) {
                plugin.getPowerManager().restorePowerForUnclaim(factionName);
            }
        }

        // ADDED: Validate faction home after mass unclaim
        if (unclaimedCount > 0) {
            plugin.getHomeManager().validateHomeInTerritory(factionName);
        }

        if (unclaimedCount == 0) {
            MessageManager.sendError(player,"Your faction has no claims in this world.");
        } else {
            MessageManager.sendInfo(player,"Unclaimed " + unclaimedCount + " chunks and returned them to Wilderness.");

            // Show power restoration info
            if (plugin.getPowerManager() != null) {
                int totalPowerRestored = unclaimedCount * plugin.getPowerManager().getPowerRestoredPerChunk();
                int availablePower = plugin.getPowerManager().getFactionPower(factionName);
                MessageManager.sendInfo(player, "Power restored: " + totalPowerRestored + " (Available: " + availablePower + ")");
            }
        }

        return unclaimedCount;
    }

    // Admin methods
    public boolean adminClaim(Player player, String factionName) {
        if (!factions.containsKey(factionName)) {
            Faction f = new Faction(factionName, player.getUniqueId());
            factions.put(factionName, f);
        }

        Chunk chunk = player.getLocation().getChunk();
        String world = player.getWorld().getName();
        worldClaims.putIfAbsent(world, new HashMap<>());
        Map<ChunkCoord, String> claims = worldClaims.get(world);
        claims.put(new ChunkCoord(chunk.getX(), chunk.getZ()), factionName);

        MessageManager.sendSuccess(player, "Chunk claimed for faction: " + factionName);
        return true;
    }

    public boolean adminUnclaim(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        String world = player.getWorld().getName();
        Map<ChunkCoord, String> claims = worldClaims.get(world);
        if (claims == null) {
            MessageManager.sendError(player,"No claims found in this world.");
            return false;
        }

        ChunkCoord coord = new ChunkCoord(chunk.getX(), chunk.getZ());
        String currentOwner = claims.get(coord);
        if (currentOwner == null) {
            MessageManager.sendError(player,"This chunk is not claimed.");
            return false;
        }

        claims.remove(coord);
        claims.put(coord, "Wilderness");

        // ADDED: Validate faction home for the affected faction
        plugin.getHomeManager().validateHomeInTerritory(currentOwner);

        MessageManager.sendSuccess(player, "Chunk unclaimed from " + currentOwner + " and returned to Wilderness.");
        return true;
    }

    public int adminUnclaimAll(Player player, String targetFaction) {
        String world = player.getWorld().getName();
        Map<ChunkCoord, String> claims = worldClaims.get(world);
        if (claims == null) {
            MessageManager.sendError(player,"No claims found in this world.");
            return 0;
        }

        int unclaimedCount = 0;

        // Create a list of chunks to unclaim to avoid ConcurrentModificationException
        List<ChunkCoord> chunksToUnclaim = new ArrayList<>();

        // First pass: identify chunks to unclaim
        for (Map.Entry<ChunkCoord, String> entry : claims.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(targetFaction)) {
                chunksToUnclaim.add(entry.getKey());
            }
        }

        // Second pass: actually unclaim them
        for (ChunkCoord coord : chunksToUnclaim) {
            claims.remove(coord);
            claims.put(coord, "Wilderness");
            unclaimedCount++;

            // Restore power for each unclaimed chunk (for admin unclaims too)
            if (plugin.getPowerManager() != null) {
                plugin.getPowerManager().restorePowerForUnclaim(targetFaction);
            }
        }

        // ADDED: Validate faction home after admin mass unclaim
        if (unclaimedCount > 0) {
            plugin.getHomeManager().validateHomeInTerritory(targetFaction);
        }

        if (unclaimedCount == 0) {
            MessageManager.sendError(player,"Faction '" + targetFaction + "' has no claims in this world.");
        } else {
            MessageManager.sendSuccess(player, "Unclaimed " + unclaimedCount + " chunks from " + targetFaction + " and returned them to Wilderness.");

            // Show power restoration info
            if (plugin.getPowerManager() != null) {
                int totalPowerRestored = unclaimedCount * plugin.getPowerManager().getPowerRestoredPerChunk();
                MessageManager.sendInfo(player, "Power restored to " + targetFaction + ": " + totalPowerRestored);
            }
        }

        return unclaimedCount;
    }

    private static final char[] UNIQUE_CHARS = {
            '!', '@', '#', '$', '%', '^', '&', '*', '~', '?', '+', '=', '-', ':', ';'
    };

    public void displayFactionMap(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        int centerX = loc.getChunk().getX();
        int centerZ = loc.getChunk().getZ();
        int radiusX = plugin.getConfigManager().getMapRadiusX();
        int radiusZ = plugin.getConfigManager().getMapRadiusZ();

        Map<String, ChatColor> factionColors = new HashMap<>();
        ChatColor[] availableColors = Arrays.stream(ChatColor.values())
                .filter(c -> c.isColor() && c != ChatColor.MAGIC)
                .filter(c -> c != ChatColor.AQUA && c != ChatColor.DARK_RED) // reserved colors
                .toArray(ChatColor[]::new);
        int[] colorIndex = {0};

        Random random = new Random();
        boolean hasSpawn = false;
        boolean hasWarzone = false;

        MessageManager.sendBlankMessage(player, "");
        MessageManager.sendBasicMessage(player,ChatColor.GRAY + "Faction Map (North ↑):");

        for (int dz = -radiusZ; dz <= radiusZ; dz++) {
            StringBuilder line = new StringBuilder();

            for (int dx = -radiusX; dx < radiusX; dx++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                ChunkCoord coord = new ChunkCoord(x, z);
                String factionName = getFactionAtChunk(world, coord);

                if (dx == 0 && dz == 0) {
                    line.append(ChatColor.YELLOW).append("X").append(ChatColor.RESET);
                } else if (factionName != null && !factionName.equalsIgnoreCase("Wilderness")) {
                    ChatColor color;
                    if (factionName.equalsIgnoreCase("Spawn")) {
                        color = ChatColor.AQUA;
                        hasSpawn = true;
                    } else if (factionName.equalsIgnoreCase("Warzone")) {
                        color = ChatColor.DARK_RED;
                        hasWarzone = true;
                    } else {
                        color = factionColors.computeIfAbsent(factionName,
                                k -> availableColors[(colorIndex[0]++) % availableColors.length]);
                    }
                    line.append(color).append("#").append(ChatColor.WHITE);
                } else {
                    // Wilderness or unclaimed - show as empty
                    line.append("-");
                }
            }

            for (int i = 0; i < 2; i++) {
                line.append(UNIQUE_CHARS[random.nextInt(UNIQUE_CHARS.length)]);
            }

            MessageManager.sendBlankMessage(player, line.toString());
        }

        // Display legend
        if (hasSpawn) {
            MessageManager.sendBlankMessage(player,ChatColor.AQUA + "# " + ChatColor.WHITE + "Spawn");
        }
        if (hasWarzone) {
            MessageManager.sendBlankMessage(player,ChatColor.DARK_RED + "# " + ChatColor.WHITE + "Warzone");
        }
        if (!factionColors.isEmpty()) {
            for (Map.Entry<String, ChatColor> entry : factionColors.entrySet()) {
                MessageManager.sendBlankMessage(player,entry.getValue() + "# " + ChatColor.WHITE + entry.getKey());
            }
        }
    }

    /**
     * Initialize wilderness for a single world
     */
    public void initializeSingleWorld(World world, Player requester) {
        String worldName = world.getName();

        new BukkitRunnable() {
            private int currentX = 0;
            private int currentZ = 0;
            private int processedChunks = 0;

            // 25k blocks = 1562.5 chunks, so we'll use 1562 chunks (24,992 blocks)
            private final int worldSizeChunks = plugin.getConfigManager().getWorldSizeChunks();
            private final int halfSize = worldSizeChunks / 2; // 781 chunks from center

            private Map<ChunkCoord, String> claims;
            private int startX, endX, startZ, endZ;
            private long startTime;
            private int totalChunksToProcess;

            @Override
            public void run() {
                if (claims == null) {
                    // Initialize
                    worldClaims.putIfAbsent(worldName, new HashMap<>());
                    claims = worldClaims.get(worldName);

                    startX = -halfSize;
                    endX = halfSize;
                    startZ = -halfSize;
                    endZ = halfSize;

                    currentX = startX;
                    currentZ = startZ;
                    startTime = System.currentTimeMillis();
                    totalChunksToProcess = (endX - startX + 1) * (endZ - startZ + 1);

                    plugin.getLogger().info("==================================================");
                    plugin.getLogger().info("Initializing Wilderness for world: " + worldName);
                    plugin.getLogger().info("Area: " + worldSizeChunks + "x" + worldSizeChunks + " chunks (" + (worldSizeChunks * 16) + "x" + (worldSizeChunks * 16) + " blocks)");
                    plugin.getLogger().info("Total chunks to process: " + String.format("%,d", totalChunksToProcess));
                    plugin.getLogger().info("Requested by: " + requester.getName());
                    plugin.getLogger().info("==================================================");
                }

                // Process chunks in batches
                int batchSize = 5000;
                int processed = 0;

                while (processed < batchSize && currentX <= endX) {
                    if (currentZ <= endZ) {
                        ChunkCoord coord = new ChunkCoord(currentX, currentZ);

                        // Only claim as wilderness if not already claimed
                        if (!claims.containsKey(coord)) {
                            claims.put(coord, "Wilderness");
                            processedChunks++;
                        }

                        processed++;
                        currentZ++;
                    } else {
                        currentZ = startZ;
                        currentX++;
                    }
                }

                // Check if done
                if (currentX > endX) {
                    long elapsed = System.currentTimeMillis() - startTime;

                    plugin.getLogger().info("Completed wilderness initialization for " + worldName + " in " + (elapsed / 1000.0) + " seconds");
                    plugin.getLogger().info("Processed " + String.format("%,d", processedChunks) + " chunks");

                    if (requester.isOnline()) {
                        MessageManager.sendSuccess(requester, "Wilderness initialization completed for " + worldName + "!");
                        MessageManager.sendSuccess(requester, "Processed " + String.format("%,d", processedChunks) + " chunks in " + (elapsed / 1000.0) + " seconds");
                    }

                    // Save data
                    plugin.getDataManager().saveFactionData();
                    this.cancel();
                    return;
                }

                // Progress reporting
                if (processedChunks > 0 && processedChunks % 50000 == 0) {
                    double progress = (double) processedChunks / totalChunksToProcess * 100;
                    plugin.getLogger().info("Progress: " + String.format("%,d", processedChunks) + "/" +
                            String.format("%,d", totalChunksToProcess) + " chunks (" +
                            String.format("%.1f", progress) + "%) in " + worldName);
                }
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }

    /**
     * Initialize wilderness for all worlds
     */
    public void initializeAllWorlds(Player requester) {
        List<World> allWorlds = new ArrayList<>(Bukkit.getWorlds());
        plugin.getLogger().info("Starting wilderness initialization for " + allWorlds.size() + " worlds:");
        for (World world : allWorlds) {
            plugin.getLogger().info("  - " + world.getName() + " (" + world.getEnvironment() + ")");
        }

        new BukkitRunnable() {
            private int currentWorldIndex = 0;
            private final List<World> worlds = allWorlds;
            private int currentX = 0;
            private int currentZ = 0;
            private int processedChunks = 0;
            private int totalProcessedChunks = 0;

            // 25k blocks = 1562.5 chunks, so we'll use 1562 chunks (24,992 blocks)
            private final int worldSizeChunks = plugin.getConfigManager().getWorldSizeChunks();
            private final int halfSize = worldSizeChunks / 2;

            private World currentWorld;
            private Map<ChunkCoord, String> currentClaims;
            private int startX, endX, startZ, endZ;
            private long startTime;
            private long worldStartTime;
            private int totalChunksToProcess;
            private int worldStartChunks;

            @Override
            public void run() {
                // Initialize new world if needed
                if (currentWorld == null && currentWorldIndex < worlds.size()) {
                    currentWorld = worlds.get(currentWorldIndex);
                    String worldName = currentWorld.getName();

                    worldClaims.putIfAbsent(worldName, new HashMap<>());
                    currentClaims = worldClaims.get(worldName);

                    startX = -halfSize;
                    endX = halfSize;
                    startZ = -halfSize;
                    endZ = halfSize;

                    currentX = startX;
                    currentZ = startZ;
                    worldStartTime = System.currentTimeMillis();
                    if (currentWorldIndex == 0) startTime = worldStartTime;
                    worldStartChunks = totalProcessedChunks;
                    totalChunksToProcess = (endX - startX + 1) * (endZ - startZ + 1);
                    processedChunks = 0;

                    plugin.getLogger().info("==================================================");
                    plugin.getLogger().info("Initializing Wilderness for world: " + worldName + " (" + (currentWorldIndex + 1) + "/" + worlds.size() + ")");
                    plugin.getLogger().info("World Environment: " + currentWorld.getEnvironment());
                    plugin.getLogger().info("Area: " + worldSizeChunks + "x" + worldSizeChunks + " chunks (" + (worldSizeChunks * 16) + "x" + (worldSizeChunks * 16) + " blocks)");
                    plugin.getLogger().info("Total chunks to process: " + String.format("%,d", totalChunksToProcess));
                    plugin.getLogger().info("==================================================");
                }

                // Process chunks in batches
                int batchSize = 5000;
                int processed = 0;

                while (processed < batchSize && currentWorld != null) {
                    if (currentX <= endX && currentZ <= endZ) {
                        ChunkCoord coord = new ChunkCoord(currentX, currentZ);

                        if (!currentClaims.containsKey(coord)) {
                            currentClaims.put(coord, "Wilderness");
                            processedChunks++;
                            totalProcessedChunks++;
                        }

                        processed++;

                        currentZ++;
                        if (currentZ > endZ) {
                            currentZ = startZ;
                            currentX++;
                        }
                    } else {
                        // Finished current world
                        long elapsed = System.currentTimeMillis() - worldStartTime;

                        plugin.getLogger().info("Completed " + currentWorld.getName() + " in " + (elapsed / 1000.0) + " seconds");
                        plugin.getLogger().info("  └─ Processed " + String.format("%,d", processedChunks) + " chunks");

                        currentWorldIndex++;
                        currentWorld = null;

                        // Check if done with all worlds
                        if (currentWorldIndex >= worlds.size()) {
                            long totalTime = System.currentTimeMillis() - startTime;

                            plugin.getLogger().info("==================================================");
                            plugin.getLogger().info("WILDERNESS INITIALIZATION COMPLETE!");
                            plugin.getLogger().info("Total chunks processed: " + String.format("%,d", totalProcessedChunks));
                            plugin.getLogger().info("Total worlds processed: " + worlds.size());
                            plugin.getLogger().info("Total time: " + (totalTime / 1000.0) + " seconds");
                            plugin.getLogger().info("Requested by: " + requester.getName());
                            plugin.getLogger().info("==================================================");

                            if (requester.isOnline()) {
                                MessageManager.sendSuccess(requester, "Wilderness initialization completed for ALL worlds!");
                                MessageManager.sendSuccess(requester, "Total: " + String.format("%,d", totalProcessedChunks) + " chunks across " + worlds.size() + " worlds");
                                MessageManager.sendSuccess(requester, "Time: " + (totalTime / 1000.0) + " seconds");
                            }

                            plugin.getDataManager().saveFactionData();
                            this.cancel();
                            return;
                        }
                        break;
                    }
                }

                // Progress reporting
                if (currentWorld != null && processedChunks > 0 && processedChunks % 50000 == 0) {
                    double worldProgress = (double) processedChunks / totalChunksToProcess * 100;
                    plugin.getLogger().info("Progress: " + String.format("%,d", processedChunks) + "/" +
                            String.format("%,d", totalChunksToProcess) + " chunks (" +
                            String.format("%.1f", worldProgress) + "%) in " + currentWorld.getName());
                }
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }

    /**
     * Check if a player can perform an action in a chunk
     */
    public boolean canPlayerActInChunk(Player player, ChunkCoord coord, String action) {
        String faction = getFactionAtChunk(player.getWorld(), coord);
        if (faction == null) return true; // No claim = allowed

        if (faction.equalsIgnoreCase("Wilderness")) return true;

        // Check bypass permissions
        if (plugin.hasPermissionBypass(player, faction, action)) {
            return true;
        }

        if (faction.equalsIgnoreCase("Spawn") || faction.equalsIgnoreCase("Warzone")) {
            return false; // Protected areas
        }

        String playerFaction = playerFactions.get(player.getUniqueId());
        if (playerFaction != null && playerFaction.equals(faction)) {
            return true; // Same faction - individual permissions checked elsewhere
        }

        // Different faction - check relations
        return plugin.getRelationManager().canPerformAction(player, faction, getRelationPermissionForAction(action));
    }

    private me.elite.Factions.data.RelationPermission getRelationPermissionForAction(String action) {
        switch (action.toLowerCase()) {
            case "break": return me.elite.Factions.data.RelationPermission.BREAK_BLOCKS;
            case "place": return me.elite.Factions.data.RelationPermission.PLACE_BLOCKS;
            case "interact": return me.elite.Factions.data.RelationPermission.INTERACT;
            case "container": return me.elite.Factions.data.RelationPermission.CONTAINER_ACCESS;
            default: return me.elite.Factions.data.RelationPermission.INTERACT;
        }
    }
}