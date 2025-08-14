package me.elite.Factions.economy;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Faction;
import me.elite.Factions.territory.ChunkCoord;
import dev.rosewood.rosestacker.api.RoseStackerAPI;
import dev.rosewood.rosestacker.stack.StackedSpawner;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WorthCalculator {
    private final FactionsPlugin plugin;
    private final Map<String, Faction> factions;
    private final Map<String, Map<ChunkCoord, String>> worldClaims;

    // Cached worth values
    private final Map<String, Double> cachedWorth = new ConcurrentHashMap<>();
    private long lastCalculation = 0;

    // Spawner values (in dollars) - loaded from config
    private final Map<EntityType, Double> spawnerValues = new HashMap<>();
    private double defaultSpawnerValue = 100.0;

    // Configuration
    private FileConfiguration spawnerConfig;
    private File spawnerConfigFile;

    // RoseStacker integration
    private boolean useRoseStacker = false;
    private RoseStackerAPI roseStackerAPI;

    public WorthCalculator(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.factions = plugin.getFactions();
        this.worldClaims = plugin.getWorldClaims();

        loadSpawnerConfig();
        initializeRoseStacker();
        startWorthCalculationTask();
    }

    /**
     * Load spawner values from config file
     */
    private void loadSpawnerConfig() {
        spawnerConfigFile = new File(plugin.getDataFolder(), "spawner-values.yml");

        // Create config file if it doesn't exist
        if (!spawnerConfigFile.exists()) {
            try {
                // Copy default config from resources
                InputStream defaultConfig = plugin.getResource("spawner-values.yml");
                if (defaultConfig != null) {
                    Files.copy(defaultConfig, spawnerConfigFile.toPath());
                    plugin.getLogger().info("Created default spawner-values.yml configuration file");
                } else {
                    // Create basic config if resource doesn't exist
                    spawnerConfigFile.createNewFile();
                    plugin.getLogger().warning("Could not find default spawner-values.yml, created empty file");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create spawner-values.yml: " + e.getMessage());
            }
        }

        spawnerConfig = YamlConfiguration.loadConfiguration(spawnerConfigFile);

        // Load default value
        defaultSpawnerValue = spawnerConfig.getDouble("DEFAULT", 100.0);

        // Load spawner values
        spawnerValues.clear();
        for (String key : spawnerConfig.getKeys(false)) {
            if (key.equals("DEFAULT")) continue;

            try {
                EntityType entityType = EntityType.valueOf(key.toUpperCase());
                double value = spawnerConfig.getDouble(key, defaultSpawnerValue);
                spawnerValues.put(entityType, value);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown entity type in spawner-values.yml: " + key);
            }
        }

        plugin.getLogger().info("Loaded " + spawnerValues.size() + " spawner values from configuration");
    }

    /**
     * Save spawner config
     */
    public void saveSpawnerConfig() {
        try {
            spawnerConfig.save(spawnerConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save spawner-values.yml: " + e.getMessage());
        }
    }

    /**
     * Reload spawner config
     */
    public void reloadSpawnerConfig() {
        spawnerConfig = YamlConfiguration.loadConfiguration(spawnerConfigFile);
        loadSpawnerConfig();
        plugin.getLogger().info("Reloaded spawner values configuration");
    }

    private void initializeRoseStacker() {
        if (Bukkit.getPluginManager().getPlugin("RoseStacker") != null) {
            try {
                roseStackerAPI = RoseStackerAPI.getInstance();
                useRoseStacker = true;
                plugin.getLogger().info("WorthCalculator: Successfully hooked into RoseStacker");
            } catch (Exception e) {
                plugin.getLogger().warning("WorthCalculator: Failed to hook into RoseStacker: " + e.getMessage());
                useRoseStacker = false;
            }
        } else {
            plugin.getLogger().info("WorthCalculator: RoseStacker not found, using basic spawner counting");
            useRoseStacker = false;
        }
    }

    /**
     * Get faction worth (cached value)
     */
    public double getFactionWorth(String factionName) {
        return cachedWorth.getOrDefault(factionName, 0.0);
    }

    /**
     * Get all faction worth values sorted by value (descending)
     */
    public List<Map.Entry<String, Double>> getSortedFactionWorth() {
        List<Map.Entry<String, Double>> sortedList = new ArrayList<>(cachedWorth.entrySet());
        sortedList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return sortedList;
    }

    /**
     * Force recalculate worth for a specific faction
     */
    public void recalculateFactionWorth(String factionName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                double worth = calculateFactionWorthSync(factionName);
                cachedWorth.put(factionName, worth);
                plugin.getLogger().info("Recalculated worth for faction " + factionName + ": " +
                        plugin.getEconomyManager().format(worth));
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Recalculate all faction worth values
     */
    public void recalculateAllWorth() {
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("Starting faction worth recalculation for " + factions.size() + " factions...");
                long startTime = System.currentTimeMillis();

                Map<String, Double> newWorthValues = new HashMap<>();

                for (String factionName : factions.keySet()) {
                    try {
                        double worth = calculateFactionWorthSync(factionName);
                        newWorthValues.put(factionName, worth);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to calculate worth for faction " + factionName + ": " + e.getMessage());
                        newWorthValues.put(factionName, 0.0);
                    }
                }

                // Update cached values
                cachedWorth.clear();
                cachedWorth.putAll(newWorthValues);
                lastCalculation = System.currentTimeMillis();

                long elapsed = System.currentTimeMillis() - startTime;
                plugin.getLogger().info("Completed faction worth recalculation in " + elapsed + "ms");
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Calculate faction worth synchronously
     */
    private double calculateFactionWorthSync(String factionName) {
        plugin.getLogger().info("=== CALCULATING WORTH FOR FACTION: " + factionName + " ===");

        Faction faction = factions.get(factionName);
        if (faction == null) {
            plugin.getLogger().warning("Faction " + factionName + " not found!");
            return 0.0;
        }

        double totalWorth = 0.0;

        // Add bank balance
        double bankBalance = faction.bankBalance;
        plugin.getLogger().info("Bank balance: " + bankBalance);
        totalWorth += bankBalance;
        plugin.getLogger().info("Total worth after bank: " + totalWorth);

        // Add spawner values
        double spawnerWorth = calculateSpawnerWorth(factionName);
        plugin.getLogger().info("Spawner worth calculated: " + spawnerWorth);
        totalWorth += spawnerWorth;
        plugin.getLogger().info("Total worth after spawners: " + totalWorth);

        // Final calculation
        plugin.getLogger().info("=== FINAL CALCULATION FOR " + factionName + " ===");
        plugin.getLogger().info("Bank Balance: " + bankBalance);
        plugin.getLogger().info("Spawner Worth: " + spawnerWorth);
        plugin.getLogger().info("Total Worth: " + totalWorth);
        plugin.getLogger().info("=== END CALCULATION ===");

        return totalWorth;
    }

    /**
     * Calculate total spawner worth for a faction
     */
    private double calculateSpawnerWorth(String factionName) {
        plugin.getLogger().info("--- Starting spawner calculation for faction: " + factionName + " ---");

        double spawnerWorth = 0.0;
        int totalSpawners = 0;
        int chunksChecked = 0;
        int claimsFound = 0;

        plugin.getLogger().info("Available worlds: " + worldClaims.keySet());

        for (Map.Entry<String, Map<ChunkCoord, String>> worldEntry : worldClaims.entrySet()) {
            String worldName = worldEntry.getKey();
            plugin.getLogger().info("Checking world: " + worldName);

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World " + worldName + " is null, skipping");
                continue;
            }

            Map<ChunkCoord, String> claims = worldEntry.getValue();
            plugin.getLogger().info("Total claims in " + worldName + ": " + claims.size());

            int factionClaimsInWorld = 0;
            for (Map.Entry<ChunkCoord, String> claimEntry : claims.entrySet()) {
                String claimOwner = claimEntry.getValue();

                if (claimOwner.equals(factionName)) {
                    factionClaimsInWorld++;
                    claimsFound++;

                    ChunkCoord coord = claimEntry.getKey();
                    plugin.getLogger().info("Found faction claim at " + coord.x + "," + coord.z + " in " + worldName);

                    // Force load chunk to count spawners
                    Chunk chunk = world.getChunkAt(coord.x, coord.z);
                    boolean wasLoaded = chunk.isLoaded();

                    if (!wasLoaded) {
                        plugin.getLogger().info("Loading chunk " + coord.x + "," + coord.z);
                        chunk.load();
                    }

                    double chunkWorth = calculateChunkSpawnerWorth(chunk);
                    plugin.getLogger().info("Chunk " + coord.x + "," + coord.z + " spawner worth: " + chunkWorth);

                    spawnerWorth += chunkWorth;
                    chunksChecked++;

                    if (chunkWorth > 0) {
                        totalSpawners++;
                    }
                }
            }

            plugin.getLogger().info("Faction " + factionName + " has " + factionClaimsInWorld + " claims in world " + worldName);
        }

        plugin.getLogger().info("--- Spawner calculation summary for " + factionName + " ---");
        plugin.getLogger().info("Total claims found: " + claimsFound);
        plugin.getLogger().info("Chunks checked: " + chunksChecked);
        plugin.getLogger().info("Spawner locations found: " + totalSpawners);
        plugin.getLogger().info("Total spawner worth: " + spawnerWorth);
        plugin.getLogger().info("--- End spawner calculation ---");

        return spawnerWorth;
    }

    /**
     * Calculate spawner worth in a specific chunk
     */
    private double calculateChunkSpawnerWorth(Chunk chunk) {
        double chunkWorth = 0.0;
        int spawnersInChunk = 0;

        plugin.getLogger().info("Scanning chunk " + chunk.getX() + "," + chunk.getZ() + " in world " + chunk.getWorld().getName());

        // Iterate through all blocks in the chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < chunk.getWorld().getMaxHeight(); y++) {
                    Block block = chunk.getBlock(x, y, z);

                    if (block.getType() == Material.SPAWNER) {
                        spawnersInChunk++;

                        try {
                            CreatureSpawner spawner = (CreatureSpawner) block.getState();
                            EntityType entityType = spawner.getSpawnedType();

                            double spawnerValue = spawnerValues.getOrDefault(entityType, defaultSpawnerValue);
                            plugin.getLogger().info("Found spawner at " + x + "," + y + "," + z + " - Type: " + entityType + ", Base value: " + spawnerValue);

                            if (useRoseStacker) {
                                // Use RoseStacker to get stack amount
                                try {
                                    StackedSpawner stackedSpawner = roseStackerAPI.getStackedSpawner(block);
                                    if (stackedSpawner != null) {
                                        int stackAmount = stackedSpawner.getStackSize();
                                        double totalValue = spawnerValue * stackAmount;
                                        chunkWorth += totalValue;
                                        plugin.getLogger().info("Stacked spawner - Stack size: " + stackAmount + ", Total value: " + totalValue);
                                    } else {
                                        chunkWorth += spawnerValue; // Single spawner
                                        plugin.getLogger().info("Single spawner (not stacked), value: " + spawnerValue);
                                    }
                                } catch (Exception e) {
                                    chunkWorth += spawnerValue; // Fallback to single spawner
                                    plugin.getLogger().warning("Error getting stacked spawner info at " + x + "," + y + "," + z + ": " + e.getMessage());
                                }
                            } else {
                                chunkWorth += spawnerValue; // Single spawner
                                plugin.getLogger().info("Single spawner (RoseStacker not available), value: " + spawnerValue);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error processing spawner at " + x + "," + y + "," + z + ": " + e.getMessage());
                        }
                    }
                }
            }
        }

        plugin.getLogger().info("Chunk scan complete - Found " + spawnersInChunk + " spawners, total worth: " + chunkWorth);
        return chunkWorth;
    }

    /**
     * Start the periodic worth calculation task
     */
    private void startWorthCalculationTask() {
        // Recalculate worth every hour (72000 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                recalculateAllWorth();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60 * 60, 20L * 60 * 60); // 1 hour interval

        // Initial calculation after 1 minute
        new BukkitRunnable() {
            @Override
            public void run() {
                recalculateAllWorth();
            }
        }.runTaskLaterAsynchronously(plugin, 20L * 60); // 1 minute delay
    }

    /**
     * Get spawner value for a specific entity type
     */
    public double getSpawnerValue(EntityType entityType) {
        return spawnerValues.getOrDefault(entityType, defaultSpawnerValue);
    }

    /**
     * Set spawner value for a specific entity type
     */
    public void setSpawnerValue(EntityType entityType, double value) {
        spawnerValues.put(entityType, value);
        spawnerConfig.set(entityType.name(), value);
        saveSpawnerConfig();
    }

    /**
     * Format worth for display
     */
    public String formatWorth(double worth) {
        return plugin.getEconomyManager().format(worth);
    }

    /**
     * Get last calculation time
     */
    public long getLastCalculationTime() {
        return lastCalculation;
    }
}