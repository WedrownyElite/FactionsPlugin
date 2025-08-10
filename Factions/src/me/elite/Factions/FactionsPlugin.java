package me.elite.Factions;

import me.elite.Factions.commands.CommandManager;
import me.elite.Factions.commands.FactionsTabCompleter;
import me.elite.Factions.data.DataManager;
import me.elite.Factions.data.Faction;
import me.elite.Factions.gui.FactionCreationManager;
import me.elite.Factions.gui.MenuHandler;
import me.elite.Factions.listeners.FactionsEventListener;
import me.elite.Factions.permissions.PermissionManager;
import me.elite.Factions.territory.ChunkCoord;
import me.elite.Factions.territory.ClaimManager;
import me.elite.Factions.utils.FactionUtilityManager;
import me.elite.Factions.Relations.RelationManager;
import me.elite.Factions.nametags.PacketNametagManager;
import me.elite.Factions.gui.BrowserMenuHandler;
import me.elite.Factions.power.PowerManager;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class FactionsPlugin extends JavaPlugin implements Listener {

    // Core data storage
    private final Map<String, Faction> factions = new HashMap<>();
    private final Map<UUID, String> playerFactions = new HashMap<>();
    private final Map<String, Map<ChunkCoord, String>> worldClaims = new HashMap<>();
    private final Map<UUID, Chunk> lastPlayerChunk = new HashMap<>();
    private final Map<UUID, Boolean> pendingFactionCreation = new HashMap<>();
    private final Map<UUID, String> pendingFactionNames = new HashMap<>();
    private final Map<UUID, Set<String>> playerInvitations = new HashMap<>();


    // Manager instances
    private DataManager dataManager;
    private PermissionManager permissionManager;
    private MenuHandler menuHandler;
    private ClaimManager claimManager;
    private CommandManager commandManager;
    private FactionsEventListener eventListener;
    private FactionCreationManager factionCreationManager;
    private FactionUtilityManager utilityManager;
    private RelationManager relationManager;
    private PacketNametagManager nametagManager;
    private BrowserMenuHandler browserMenuHandler;
    private PowerManager powerManager;

    @Override
    public void onEnable() {
        // Create data folder if it doesn't exist
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        // Initialize all managers
        dataManager = new DataManager(this);
        permissionManager = new PermissionManager(this);
        utilityManager = new FactionUtilityManager(this);
        claimManager = new ClaimManager(this);
        menuHandler = new MenuHandler(this);
        factionCreationManager = new FactionCreationManager(this);
        commandManager = new CommandManager(this);
        eventListener = new FactionsEventListener(this);
        relationManager = new RelationManager(this);
        nametagManager = new PacketNametagManager(this);
        browserMenuHandler = new BrowserMenuHandler(this);
        powerManager = new PowerManager(this);

        // Load data
        dataManager.loadFactionData();

        // Register commands and events
        getCommand("f").setExecutor(commandManager);
        getCommand("f").setTabCompleter(new FactionsTabCompleter());
        Bukkit.getPluginManager().registerEvents(eventListener, this);

        // Start periodic cleanup task for expired relation requests
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            relationManager.cleanupExpiredRequests();
        }, 20L * 60 * 60, 20L * 60 * 60); // Run every hour

        Bukkit.getScheduler().runTaskLater(this, () -> {
            nametagManager.refreshAllNametags();
        }, 20L);

        getLogger().info("Custom Factions plugin enabled.");
    }

    @Override
    public void onDisable() {
        dataManager.saveFactionData();
        getLogger().info("Custom Factions plugin disabled.");
    }

    // =================================================================
    // GETTER METHODS FOR MANAGERS
    // =================================================================

    public Map<UUID, Set<String>> getPlayerInvitations() {
        return playerInvitations;
    }

    public PowerManager getPowerManager() {
        return powerManager;
    }

    public BrowserMenuHandler getBrowserMenuHandler() {return browserMenuHandler;}

    public FactionsEventListener getEventListener() {return eventListener;}

    public RelationManager getRelationManager() {return relationManager;}

    public PacketNametagManager getNametagManager() {return nametagManager;}

    public DataManager getDataManager() {
        return dataManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public MenuHandler getMenuHandler() {
        return menuHandler;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public FactionCreationManager getFactionCreationManager() {
        return factionCreationManager;
    }

    public FactionUtilityManager getUtilityManager() {
        return utilityManager;
    }

    // =================================================================
    // GETTER METHODS FOR CORE DATA
    // =================================================================

    public Map<String, Faction> getFactions() {
        return factions;
    }

    public Map<UUID, String> getPlayerFactions() {
        return playerFactions;
    }

    public Map<String, Map<ChunkCoord, String>> getWorldClaims() {
        return worldClaims;
    }

    public Map<UUID, Chunk> getLastPlayerChunk() {
        return lastPlayerChunk;
    }

    public Map<UUID, Boolean> getPendingFactionCreation() {
        return pendingFactionCreation;
    }

    public Map<UUID, String> getPendingFactionNames() {
        return pendingFactionNames;
    }

    // =================================================================
    // DELEGATION METHODS (for backward compatibility and convenience)
    // =================================================================

    /**
     * Open the main factions menu - delegates to MenuHandler
     */
    public void openFactionsMenu(org.bukkit.entity.Player player) {
        menuHandler.openFactionsMenu(player);
    }

    /**
     * Open faction creation confirmation - delegates to FactionCreationManager
     */
    public void openFactionCreationConfirmation(org.bukkit.entity.Player player, String factionName) {
        factionCreationManager.openFactionCreationConfirmation(player, factionName);
    }

    /**
     * Display faction map - delegates to ClaimManager
     */
    public void displayFactionMap(org.bukkit.entity.Player player) {
        claimManager.displayFactionMap(player);
    }

    /**
     * Get faction at chunk - delegates to UtilityManager
     */
    public String getFactionAtChunk(org.bukkit.World world, ChunkCoord coord) {
        return utilityManager.getFactionAtChunk(world, coord);
    }

    /**
     * Check permission bypass - delegates to PermissionManager
     */
    public boolean hasPermissionBypass(org.bukkit.entity.Player player, String faction, String action) {
        return permissionManager.hasPermissionBypass(player, faction, action);
    }

    // =================================================================
    // MENU CLICK HANDLERS (delegate to appropriate managers)
    // =================================================================

    public void handleConfirmationMenuClick(org.bukkit.entity.Player player, String displayName, String title) {
        factionCreationManager.handleConfirmationMenuClick(player, displayName, title);
    }

    public void handleNoFactionMenuClick(org.bukkit.entity.Player player, String displayName) {
        menuHandler.handleNoFactionMenuClick(player, displayName);
    }

    public void handleFactionMenuClick(org.bukkit.entity.Player player, String displayName) {
        menuHandler.handleFactionMenuClick(player, displayName);
    }

    public void handleMembersMenuClick(org.bukkit.entity.Player player, ItemStack item, ClickType clickType) {
        menuHandler.handleMembersMenuClick(player, item, clickType);
    }
}