package me.elite.Factions.utils;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Faction;
import me.elite.Factions.territory.ChunkCoord;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class FactionUtilityManager {
    private final FactionsPlugin plugin;
    private final Map<String, Faction> factions;
    private final Map<UUID, String> playerFactions;
    private final Map<String, Map<ChunkCoord, String>> worldClaims;

    public FactionUtilityManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
        this.worldClaims = plugin.getWorldClaims();
    }

    /**
     * Get the faction that owns a specific chunk
     */
    public String getFactionAtChunk(World world, ChunkCoord coord) {
        Map<ChunkCoord, String> claims = worldClaims.get(world.getName());
        if (claims == null) return null;
        return claims.get(coord);
    }

    /**
     * Check if a player is in any faction
     */
    public boolean isPlayerInFaction(UUID playerUUID) {
        return playerFactions.containsKey(playerUUID);
    }

    /**
     * Get all member names in the same faction as the given player
     */
    public List<String> getFactionMembers(UUID playerUUID) {
        String factionName = playerFactions.get(playerUUID);
        if (factionName == null) return Collections.emptyList();

        Faction faction = factions.get(factionName);
        if (faction == null) return Collections.emptyList();

        return faction.members.keySet().stream()
                .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get all faction names for tab completion
     */
    public List<String> getAllFactionNames() {
        return new ArrayList<>(factions.keySet());
    }

    /**
     * Get the faction name that a player belongs to
     */
    public String getPlayerFaction(UUID playerUUID) {
        return playerFactions.get(playerUUID);
    }

    /**
     * Get the faction object by name
     */
    public Faction getFactionByName(String factionName) {
        return factions.get(factionName);
    }

    /**
     * Check if a faction exists
     */
    public boolean factionExists(String factionName) {
        return factions.containsKey(factionName);
    }

    /**
     * Get all online players in a specific faction
     */
    public List<Player> getOnlinePlayersInFaction(String factionName) {
        List<Player> onlinePlayers = new ArrayList<>();

        for (Map.Entry<UUID, String> entry : playerFactions.entrySet()) {
            if (entry.getValue().equals(factionName)) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    onlinePlayers.add(player);
                }
            }
        }

        return onlinePlayers;
    }

    /**
     * Get the number of members in a faction
     */
    public int getFactionMemberCount(String factionName) {
        Faction faction = factions.get(factionName);
        return faction != null ? faction.members.size() : 0;
    }

    /**
     * Get the number of claims a faction has in a specific world
     */
    public int getFactionClaimCount(String factionName, String worldName) {
        Map<ChunkCoord, String> claims = worldClaims.get(worldName);
        if (claims == null) return 0;

        return (int) claims.values().stream()
                .filter(owner -> owner.equals(factionName))
                .count();
    }

    /**
     * Get total claims across all worlds for a faction
     */
    public int getTotalFactionClaims(String factionName) {
        int totalClaims = 0;
        for (Map<ChunkCoord, String> worldClaim : worldClaims.values()) {
            totalClaims += worldClaim.values().stream()
                    .mapToInt(owner -> owner.equals(factionName) ? 1 : 0)
                    .sum();
        }
        return totalClaims;
    }

    /**
     * Get corner claims for a specific world
     * Returns a map with corner positions as keys and faction names as values
     */
    /**
     * Get corner claims for a specific world
     * Returns a map with corner positions as keys and faction names as values
     */
    public Map<String, String> getWorldCornerClaims(String worldName) {
        Map<String, String> cornerClaims = new HashMap<>();

        // Try to get corner coordinates from world config
        ChunkCoord[] corners = null;
        try {
            // Check if this is a MultiWorlds configured world
            FactionsPlugin factionsPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("EclipseFactions");
            if (factionsPlugin != null && factionsPlugin.getConfigManager().getConfiguredWorlds().contains(worldName)) {
                // Use config-based corner calculation
                int sizeChunks = factionsPlugin.getConfigManager().getWorldSizeChunks(worldName);
                int halfSize = sizeChunks / 2;
                corners = new ChunkCoord[] {
                        new ChunkCoord(-halfSize, -halfSize),       // Bottom-left (-, -)
                        new ChunkCoord(-halfSize, halfSize - 1),    // Top-left (-, +)
                        new ChunkCoord(halfSize - 1, halfSize - 1), // Top-right (+, +)
                        new ChunkCoord(halfSize - 1, -halfSize)     // Bottom-right (+, -)
                };
            }
        } catch (Exception e) {
            // Fall back to default if config reading fails
        }

        // Fall back to default corner calculation if config method failed
        if (corners == null) {
            corners = new ChunkCoord[] {
                    new ChunkCoord(-781, -781),   // Default corners
                    new ChunkCoord(-781, 780),
                    new ChunkCoord(780, 780),
                    new ChunkCoord(780, -781)
            };
        }

        // Get claims for this world
        Map<ChunkCoord, String> claims = worldClaims.get(worldName);
        if (claims == null) {
            // No claims in this world - all corners are unclaimed
            cornerClaims.put("- -", "Unclaimed");
            cornerClaims.put("- +", "Unclaimed");
            cornerClaims.put("+ +", "Unclaimed");
            cornerClaims.put("+ -", "Unclaimed");
            return cornerClaims;
        }

        // Check each corner
        cornerClaims.put("- -", getClaimOwner(claims, corners[0]));
        cornerClaims.put("- +", getClaimOwner(claims, corners[1]));
        cornerClaims.put("+ +", getClaimOwner(claims, corners[2]));
        cornerClaims.put("+ -", getClaimOwner(claims, corners[3]));

        return cornerClaims;
    }

    /**
     * Get the owner of a specific chunk claim
     */
    private String getClaimOwner(Map<ChunkCoord, String> worldClaims, ChunkCoord coord) {
        String owner = worldClaims.get(coord);
        if (owner == null || owner.equals("Wilderness")) {
            return "Unclaimed";
        }
        return owner;
    }

    /**
     * Format corner claims for display in lore
     */
    public List<String> formatCornerClaimsForLore(String worldName) {
        Map<String, String> corners = getWorldCornerClaims(worldName);

        List<String> lore = new ArrayList<>();
        lore.add("§7Corners:");

        for (Map.Entry<String, String> entry : corners.entrySet()) {
            String position = entry.getKey();
            String owner = entry.getValue();

            // Color code based on claim status
            String color = owner.equals("Unclaimed") ? "§7" : "§a"; // Gray for unclaimed, green for claimed
            lore.add("§7 " + position + " " + color + owner);
        }

        return lore;
    }

    /**
     * Get a summary of corner claims for a world (for quick checks)
     */
    public String getCornerClaimSummary(String worldName) {
        Map<String, String> corners = getWorldCornerClaims(worldName);

        long claimedCount = corners.values().stream()
                .filter(owner -> !owner.equals("Unclaimed"))
                .count();

        if (claimedCount == 0) {
            return "§7All corners unclaimed";
        } else if (claimedCount == 4) {
            return "§c All corners claimed";
        } else {
            return "§e" + claimedCount + "/4 corners claimed";
        }
    }
}