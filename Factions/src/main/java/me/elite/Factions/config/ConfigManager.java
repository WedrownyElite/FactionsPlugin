package me.elite.Factions.config;

import me.elite.Factions.FactionsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;
import java.util.HashSet;

public class ConfigManager {
    private final FactionsPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // General Settings
    public int getMaxFactionNameLength() {
        return config.getInt("general.max-faction-name-length", 16);
    }

    public int getMaxFactions() {
        return config.getInt("general.max-factions", -1);
    }

    public String getDefaultFactionDescription() {
        return config.getString("general.default-faction-description", "");
    }

    public boolean getDefaultPublicFaction() {
        return config.getBoolean("general.default-public-faction", false);
    }

    public long getMessageCooldown() {
        return config.getLong("general.message-cooldown", 5000L);
    }

    // Power Settings
    public int getStartingPower() {
        return config.getInt("power.starting-power", 10);
    }

    public int getMaxPowerPerPlayer() {
        return config.getInt("power.max-power-per-player", 20);
    }

    public int getPowerPerChunk() {
        return config.getInt("power.power-per-chunk", 2);
    }

    public int getPowerRestorePerChunk() {
        return config.getInt("power.power-restore-per-chunk", 1);
    }

    public int getPowerRegenAmount() {
        return config.getInt("power.regeneration.amount", 2);
    }

    public long getPowerRegenInterval() {
        return config.getLong("power.regeneration.interval-minutes", 60) * 60 * 1000L; // Convert to milliseconds
    }

    public boolean isOfflineRegeneration() {
        return config.getBoolean("power.regeneration.offline-regeneration", true);
    }

    public boolean isOnlineRegeneration() {
        return config.getBoolean("power.regeneration.online-regeneration", true);
    }

    // Territory Settings
    public int getWorldSizeChunks() {
        return config.getInt("territory.world-size-chunks", 1562);
    }

    public String getSpawnName() {
        return config.getString("territory.special-territories.spawn", "Spawn");
    }

    public String getWarzoneName() {
        return config.getString("territory.special-territories.warzone", "Warzone");
    }

    public String getWildernessName() {
        return config.getString("territory.special-territories.wilderness", "Wilderness");
    }

    public int getMapRadiusX() {
        return config.getInt("territory.map.radius-x", 10);
    }

    public int getMapRadiusZ() {
        return config.getInt("territory.map.radius-z", 7);
    }

    // Faction Limits
    public int getMaxMembers() {
        return config.getInt("limits.max-members", -1);
    }

    public int getMaxClaims() {
        return config.getInt("limits.max-claims", -1);
    }

    public int getDefaultWarpLimit() {
        return config.getInt("limits.default-warp-limit", 1);
    }

    public int getMaxWarps() {
        return config.getInt("limits.max-warps", 5);
    }

    // Combat & Teleportation
    public int getTeleportCountdown() {
        return config.getInt("combat.teleport-countdown", 5);
    }

    public boolean isCheckCombatTag() {
        return config.getBoolean("combat.check-combat-tag", true);
    }

    public double getMovementTolerance() {
        return config.getDouble("combat.movement-tolerance", 0.1);
    }

    // Relations
    public int getRequestExpirationDays() {
        return config.getInt("relations.request-expiration-days", 7);
    }

    public boolean isBidirectionalRelations() {
        return config.getBoolean("relations.bidirectional-relations", true);
    }

    public long getCleanupIntervalHours() {
        return config.getLong("relations.cleanup-interval-hours", 1) * 60 * 60 * 1000L; // Convert to milliseconds
    }

    // Nametag Settings
    public boolean isNametagsEnabled() {
        return config.getBoolean("nametags.enabled", true);
    }

    public String getTeamPrefix() {
        return config.getString("nametags.team-prefix", "fac_");
    }

    public int getMaxTeamNameLength() {
        return config.getInt("nametags.max-team-name-length", 16);
    }

    public long getLoginRefreshDelay() {
        return config.getLong("nametags.login-refresh-delay", 60L);
    }

    public long getFactionChangeDelay() {
        return config.getLong("nametags.faction-change-delay", 10L);
    }

    public long getRelationChangeDelay() {
        return config.getLong("nametags.relation-change-delay", 5L);
    }

    public String getFactionSuffix() {
        return config.getString("nametags.suffixes.faction", " §aF");
    }

    public String getAllySuffix() {
        return config.getString("nametags.suffixes.ally", " §dA");
    }

    public String getTruceSuffix() {
        return config.getString("nametags.suffixes.truce", " §9T");
    }

    public String getEnemySuffix() {
        return config.getString("nametags.suffixes.enemy", " §cE");
    }

    public String getNeutralSuffix() {
        return config.getString("nametags.suffixes.neutral", "");
    }

    // GUI Settings
    public int getSmallGuiSize() {
        return config.getInt("gui.small-size", 9);
    }

    public int getMediumGuiSize() {
        return config.getInt("gui.medium-size", 27);
    }

    public int getLargeGuiSize() {
        return config.getInt("gui.large-size", 54);
    }

    public int getFactionsPerPage() {
        return config.getInt("gui.factions-per-page", 36);
    }

    public int getRelationsPerPage() {
        return config.getInt("gui.relations-per-page", 9);
    }

    public int getRequestsPerPage() {
        return config.getInt("gui.requests-per-page", 18);
    }

    public boolean isClearOffhand() {
        return config.getBoolean("gui.clear-offhand", true);
    }

    // Protection Settings
    public boolean canBreakInSpawn() {
        return config.getBoolean("protection.spawn.break-blocks", false);
    }

    public boolean canPlaceInSpawn() {
        return config.getBoolean("protection.spawn.place-blocks", false);
    }

    public boolean canInteractInSpawn() {
        return config.getBoolean("protection.spawn.interact", false);
    }

    public boolean isPvpInSpawn() {
        return config.getBoolean("protection.spawn.pvp", false);
    }

    public boolean canDamageMobsInSpawn() {
        return config.getBoolean("protection.spawn.mob-damage", false);
    }

    public boolean canMobsSpawnInSpawn() {
        return config.getBoolean("protection.spawn.mob-spawning", false);
    }

    public boolean canAccessContainersInSpawn() {
        return config.getBoolean("protection.spawn.container-access", false);
    }

    // Warzone settings
    public boolean canBreakInWarzone() {
        return config.getBoolean("protection.warzone.break-blocks", false);
    }

    public boolean canPlaceInWarzone() {
        return config.getBoolean("protection.warzone.place-blocks", false);
    }

    public boolean canInteractInWarzone() {
        return config.getBoolean("protection.warzone.interact", false);
    }

    public boolean isPvpInWarzone() {
        return config.getBoolean("protection.warzone.pvp", true);
    }

    public boolean canDamageMobsInWarzone() {
        return config.getBoolean("protection.warzone.mob-damage", false);
    }

    public boolean canMobsSpawnInWarzone() {
        return config.getBoolean("protection.warzone.mob-spawning", false);
    }

    public boolean canAccessContainersInWarzone() {
        return config.getBoolean("protection.warzone.container-access", false);
    }

    // Performance Settings
    public int getChunkBatchSize() {
        return config.getInt("performance.chunk-batch-size", 5000);
    }

    public int getProgressReportInterval() {
        return config.getInt("performance.progress-report-interval", 50000);
    }

    public long getNametagBatchDelay() {
        return config.getLong("performance.nametag-batch-delay", 1L);
    }

    // Debug Settings
    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }

    public boolean isLogNametags() {
        return config.getBoolean("debug.log-nametags", false);
    }

    public boolean isLogPower() {
        return config.getBoolean("debug.log-power", false);
    }

    public boolean isLogTerritory() {
        return config.getBoolean("debug.log-territory", false);
    }

    public boolean isLogRelations() {
        return config.getBoolean("debug.log-relations", false);
    }

    // Storage Settings
    public int getAutoSaveInterval() {
        return config.getInt("storage.auto-save-interval", 10);
    }

    public boolean isSaveOnShutdown() {
        return config.getBoolean("storage.save-on-shutdown", true);
    }

    public boolean isBackupData() {
        return config.getBoolean("storage.backup-data", true);
    }

    public int getBackupCount() {
        return config.getInt("storage.backup-count", 5);
    }

    // Message Settings
    public String getMessagePrefix() {
        return config.getString("messages.prefix", "&8[&9Factions&8] &r").replace("&", "§");
    }

    public String getMessage(String key, String defaultValue) {
        return config.getString("messages." + key, defaultValue).replace("&", "§");
    }

    // Integration Settings
    public boolean isCombatTagEnabled() {
        return config.getBoolean("integration.combat-tag.enabled", true);
    }

    public boolean isPreventTeleportInCombat() {
        return config.getBoolean("integration.combat-tag.prevent-teleport", true);
    }

    public boolean isMultiWorldsEnabled() {
        return config.getBoolean("integration.multiworlds.enabled", true);
    }

    public boolean useMultiWorldsDisplayNames() {
        return config.getBoolean("integration.multiworlds.use-display-names", true);
    }

    // MultiWorlds GUI Configuration
    public int getMultiWorldsGuiSize() {
        return config.getInt("multiworlds-gui.size", 27);
    }

    public String getMultiWorldsGuiTitle() {
        return config.getString("multiworlds-gui.title", "Choose a world").replace("&", "§");
    }

    public int getWorldSlot(String worldName) {
        return config.getInt("multiworlds-gui.world-slots." + worldName, -1);
    }

    public Set<String> getConfiguredWorldSlots() {
        if (config.getConfigurationSection("multiworlds-gui.world-slots") != null) {
            return config.getConfigurationSection("multiworlds-gui.world-slots").getKeys(false);
        }
        return new HashSet<>();
    }

    // World Configuration
    public Set<String> getConfiguredWorlds() {
        if (config.getConfigurationSection("worlds") != null) {
            return config.getConfigurationSection("worlds").getKeys(false);
        }
        return new HashSet<>();
    }

    public String getWorldDisplayName(String worldName) {
        return config.getString("worlds." + worldName + ".display-name", worldName);
    }

    public String getWorldType(String worldName) {
        return config.getString("worlds." + worldName + ".world-type", "Unknown");
    }

    public String getWorldItemType(String worldName) {
        return config.getString("worlds." + worldName + ".item-type", "GRASS_BLOCK");
    }

    public String getWorldSkullTexture(String worldName) {
        return config.getString("worlds." + worldName + ".skull-texture", "");
    }

    public int getWorldSizeBlocks(String worldName) {
        return config.getInt("worlds." + worldName + ".size-blocks", 25000);
    }

    public int getWorldSizeChunks(String worldName) {
        int blocks = getWorldSizeBlocks(worldName);
        return (blocks + 8) / 16; // Round up to nearest chunk
    }
}