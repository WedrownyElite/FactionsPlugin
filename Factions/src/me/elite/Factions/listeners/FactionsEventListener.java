package me.elite.Factions.listeners;

// Factions imports
import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.constants.FactionsConstants;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.FactionPermission;
import me.elite.Factions.data.Rank;
import me.elite.Factions.data.RelationPermission;
import me.elite.Factions.territory.ChunkCoord;
import me.elite.Factions.utils.ChatUtils;
import me.elite.Factions.utils.MessageManager;

// Bukkit imports
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

// Java imports
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class FactionsEventListener implements Listener {
    private final FactionsPlugin plugin;
    private final Map<String, Faction> factions;
    private final Map<UUID, String> playerFactions;
    private final Map<String, Map<ChunkCoord, String>> worldClaims;
    private final Map<UUID, Chunk> lastPlayerChunk;
    private final Map<UUID, Boolean> pendingFactionCreation;
    private final Map<UUID, String> pendingFactionNames;
    private final Map<UUID, Boolean> playerInFactionGUI = new HashMap<>();
    private final Map<UUID, Set<String>> playerInvitations;
    private final Map<UUID, Map<String, Long>> playerMessageCooldowns = new HashMap<>();

    public FactionsEventListener(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
        this.worldClaims = plugin.getWorldClaims();
        this.lastPlayerChunk = plugin.getLastPlayerChunk();
        this.pendingFactionCreation = plugin.getPendingFactionCreation();
        this.pendingFactionNames = plugin.getPendingFactionNames();
        this.playerInvitations = plugin.getPlayerInvitations();
    }

    public String getFactionAtChunk(World world, ChunkCoord coord) {
        return plugin.getUtilityManager().getFactionAtChunk(world, coord);
    }

    public boolean hasPermissionBypass(Player player, String faction, String action) {
        return plugin.hasPermissionBypass(player, faction, action);
    }

    public void openFactionsMenu(Player player) {
        plugin.openFactionsMenu(player);
    }

    public void openFactionCreationConfirmation(Player player, String message) {
        plugin.getFactionCreationManager().openFactionCreationConfirmation(player, message);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Chunk currentChunk = event.getTo().getChunk();
        Chunk lastChunk = lastPlayerChunk.get(player.getUniqueId());

        if (lastChunk != null && lastChunk.equals(currentChunk)) return;

        lastPlayerChunk.put(player.getUniqueId(), currentChunk);

        Map<ChunkCoord, String> claims = worldClaims.getOrDefault(player.getWorld().getName(), new HashMap<>());
        ChunkCoord currentCoord = new ChunkCoord(currentChunk.getX(), currentChunk.getZ());
        ChunkCoord lastCoord = lastChunk != null ? new ChunkCoord(lastChunk.getX(), lastChunk.getZ()) : null;
        String newFaction = claims.get(currentCoord);
        String oldFaction = lastCoord != null ? claims.get(lastCoord) : null;

        if (newFaction != null && !newFaction.equals(oldFaction)) {
            if (newFaction.equalsIgnoreCase(FactionsConstants.SPAWN)) {
                player.sendTitle(ChatColor.AQUA + FactionsConstants.SPAWN, ChatColor.GREEN + "You're safe here", 10, 60, 10);
            } else if (newFaction.equalsIgnoreCase(FactionsConstants.WARZONE)) {
                player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + FactionsConstants.WARZONE, ChatColor.WHITE + "Careful, PvP is allowed here", 10, 60, 10);
            } else if (newFaction.equalsIgnoreCase(FactionsConstants.WILDERNESS)) {
                player.sendTitle(ChatColor.GREEN + FactionsConstants.WILDERNESS, ChatColor.WHITE + "Unclaimed land - claim it or do whatever you want!", 10, 60, 10);
            } else {
                Faction f = factions.get(newFaction);
                if (f != null) {
                    player.sendTitle(ChatColor.RED + newFaction, ChatColor.GRAY + f.description, 10, 60, 10);
                }
            }
        }
    }

    // Protected Claims Listeners
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = event.getBlock().getChunk();
        String faction = getFactionAtChunk(event.getBlock().getWorld(), new ChunkCoord(chunk.getX(), chunk.getZ()));
        if (faction == null) return;

        // Check for bypass permissions
        if (hasPermissionBypass(player, faction, "break")) {
            return; // Allow the action
        }

        if (faction.equalsIgnoreCase(FactionsConstants.SPAWN) || faction.equalsIgnoreCase(FactionsConstants.WARZONE)) {
            event.setCancelled(true);
            sendCooldownMessage(player,ChatColor.RED + "You cannot break blocks here.");
        } else if (faction.equalsIgnoreCase(FactionsConstants.WILDERNESS)) {
            // Wilderness allows everything
            return;
        } else {
            // Check if player is in the faction that owns this claim
            String playerFaction = playerFactions.get(player.getUniqueId());
            if (playerFaction != null && playerFaction.equals(faction)) {
                // Player is in the faction - check faction permissions
                Faction f = factions.get(faction);
                Rank playerRank = f.members.get(player.getUniqueId());

                if (!f.hasPermission(playerRank, FactionPermission.BREAK_BLOCKS)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player,ChatColor.RED + "Your rank doesn't have permission to break blocks.");
                    return;
                }
            } else {
                // Player is from different faction or no faction - check relation permissions
                if (!plugin.getRelationManager().hasRelationPermission(player, faction, RelationPermission.BREAK_BLOCKS)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player,ChatColor.RED + "You cannot break blocks in " + faction + " territory.");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = event.getBlock().getChunk();
        String faction = getFactionAtChunk(event.getBlock().getWorld(), new ChunkCoord(chunk.getX(), chunk.getZ()));
        if (faction == null) return;

        // Check for bypass permissions
        if (hasPermissionBypass(player, faction, "place")) {
            return; // Allow the action
        }

        if (faction.equalsIgnoreCase(FactionsConstants.SPAWN) || faction.equalsIgnoreCase(FactionsConstants.WARZONE)) {
            event.setCancelled(true);
            sendCooldownMessage(player,ChatColor.RED + "You cannot place blocks here.");
        } else if (faction.equalsIgnoreCase(FactionsConstants.WILDERNESS)) {
            // Wilderness allows everything
            return;
        } else {
            // Check if player is in the faction that owns this claim
            String playerFaction = playerFactions.get(player.getUniqueId());
            if (playerFaction != null && playerFaction.equals(faction)) {
                // Player is in the faction - check faction permissions
                Faction f = factions.get(faction);
                Rank playerRank = f.members.get(player.getUniqueId());

                if (!f.hasPermission(playerRank, FactionPermission.PLACE_BLOCKS)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player,ChatColor.RED + "Your rank doesn't have permission to place blocks.");
                    return;
                }
            } else {
                // Player is from different faction or no faction - check relation permissions
                if (!plugin.getRelationManager().hasRelationPermission(player, faction, RelationPermission.PLACE_BLOCKS)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player,ChatColor.RED + "You cannot place blocks in " + faction + " territory.");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Check if there's a clicked block
        if (event.getClickedBlock() == null) return;

        // Only check for actual interactions, not block breaking/placing
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Check if it's actually an interactive block
        Material blockType = event.getClickedBlock().getType();
        if (!isInteractiveBlock(blockType)) return;

        Chunk chunk = event.getClickedBlock().getChunk();
        String faction = getFactionAtChunk(event.getClickedBlock().getWorld(), new ChunkCoord(chunk.getX(), chunk.getZ()));
        if (faction == null) return;

        // Check for bypass permissions
        if (hasPermissionBypass(player, faction, "interact")) {
            return; // Allow the action
        }

        if (faction.equalsIgnoreCase(FactionsConstants.SPAWN) || faction.equalsIgnoreCase(FactionsConstants.WARZONE)) {
            event.setCancelled(true);
            sendCooldownMessage(player,ChatColor.RED + "You cannot interact here.");
        } else if (faction.equalsIgnoreCase(FactionsConstants.WILDERNESS)) {
            // Wilderness allows everything
            return;
        } else {
            // Check if player is in the faction that owns this claim
            String playerFaction = playerFactions.get(player.getUniqueId());
            if (playerFaction != null && playerFaction.equals(faction)) {
                // Player is in the faction - check faction permissions
                Faction f = factions.get(faction);
                Rank playerRank = f.members.get(player.getUniqueId());

                if (!f.hasPermission(playerRank, FactionPermission.INTERACT)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player,ChatColor.RED + "Your rank doesn't have permission to interact.");
                    return;
                }
            } else {
                // Player is from different faction or no faction - check relation permissions
                if (!plugin.getRelationManager().hasRelationPermission(player, faction, RelationPermission.INTERACT)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player,ChatColor.RED + "You cannot interact in " + faction + " territory.");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Player vs Player
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            Player damager = (Player) event.getDamager();
            Chunk chunk = damaged.getLocation().getChunk();
            String faction = getFactionAtChunk(damaged.getWorld(), new ChunkCoord(chunk.getX(), chunk.getZ()));

            String damagerFaction = playerFactions.get(damager.getUniqueId());
            String damagedFaction = playerFactions.get(damaged.getUniqueId());

            // 1) SPAWN PROTECTION
            if (FactionsConstants.SPAWN.equalsIgnoreCase(faction)) {
                event.setCancelled(true);
                MessageManager.sendError(damager,"PvP is disabled in Spawn!");
                return;
            }

            // 2) RELATIONSHIP CHECKS
            if (damagerFaction != null && damagerFaction.equals(damagedFaction)) {
                event.setCancelled(true);
                MessageManager.sendError(damager,"You cannot attack your faction members!");
                return;
            }

            if (damagerFaction != null && damagedFaction != null && !damagerFaction.equals(damagedFaction)) {
                me.elite.Factions.data.Relation relation = plugin.getRelationManager()
                        .getRelation(damagerFaction, damagedFaction);

                if (relation == me.elite.Factions.data.Relation.ALLY) {
                    event.setCancelled(true);
                    MessageManager.sendError(damager,"You cannot attack allied faction members!");
                    return;
                }
                if (relation == me.elite.Factions.data.Relation.TRUCE) {
                    event.setCancelled(true);
                    MessageManager.sendError(damager,"You cannot attack truced faction members!");
                    return;
                }
            }

            // 3) TERRITORY-SPECIFIC CHECKS
            if (FactionsConstants.WARZONE.equalsIgnoreCase(faction)) {
                if (hasPermissionBypass(damager, faction, "pvp_disable")) {
                    event.setCancelled(true);
                    MessageManager.sendInfo(damager,"You have PvP protection in Warzone.");
                    return;
                }
            }

            // Check bypass for any territory
            if (hasPermissionBypass(damager, faction != null ? faction : FactionsConstants.WILDERNESS, "pvp")) {
                return; // Allow PvP
            }

            // Wilderness or other unclaimed = allow PvP unless blocked above
            return;
        }

        // Player vs Mob
        else if (!(event.getEntity() instanceof Player) && event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            Chunk chunk = event.getEntity().getLocation().getChunk();
            String faction = getFactionAtChunk(event.getEntity().getWorld(), new ChunkCoord(chunk.getX(), chunk.getZ()));

            if (faction == null) return;

            // Mob damage bypass check
            if (hasPermissionBypass(player, faction, "damage_mobs")) {
                return;
            }

            // Block in spawn & warzone
            if (FactionsConstants.SPAWN.equalsIgnoreCase(faction) || FactionsConstants.WARZONE.equalsIgnoreCase(faction)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.getNametagManager().onPlayerJoin(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Handle nametag cleanup
        plugin.getNametagManager().onPlayerLeave(player);

        // Reset player's display name when they leave (for chat)
        event.getPlayer().setDisplayName(event.getPlayer().getName());
        event.getPlayer().setPlayerListName(event.getPlayer().getName());
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Chunk chunk = event.getLocation().getChunk();
        String faction = getFactionAtChunk(event.getLocation().getWorld(), new ChunkCoord(chunk.getX(), chunk.getZ()));
        if (faction == null) return;

        // Cancel natural mob spawning in Spawn and Warzone only
        if ((faction.equalsIgnoreCase(FactionsConstants.SPAWN) || faction.equalsIgnoreCase(FactionsConstants.WARZONE))
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        String title = event.getView().getTitle();

        // Handle faction GUI tracking
        if (title.contains("Faction") || title.contains("Members:") || title.contains("Confirm:") ||
                title.contains("Kick:") || title.contains("Leave:") || title.contains("Disband:") ||
                title.equals(ChatColor.DARK_GRAY + "Browse Factions") ||
                title.equals(ChatColor.DARK_GRAY + "Public Factions") ||
                title.equals(ChatColor.DARK_GRAY + "Your Invitations") ||
                title.equals(ChatColor.DARK_GRAY + "Relations & Requests") ||
                title.startsWith(ChatColor.DARK_RED + "Remove: ")) {

            UUID uuid = player.getUniqueId();
            playerInFactionGUI.put(uuid, true);

            // Clear offhand immediately when opening GUI
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            return; // Don't check container permissions for faction GUIs
        }

        // Handle container access protection (only for world containers, not GUIs)
        if (event.getInventory().getLocation() == null) return;

        Chunk chunk = event.getInventory().getLocation().getChunk();
        String faction = getFactionAtChunk(event.getInventory().getLocation().getWorld(),
                new ChunkCoord(chunk.getX(), chunk.getZ()));
        if (faction == null) return;

        // Check for bypass permissions
        if (hasPermissionBypass(player, faction, "container")) {
            return; // Allow the action
        }

        if (faction.equalsIgnoreCase(FactionsConstants.SPAWN) || faction.equalsIgnoreCase(FactionsConstants.WARZONE)) {
            event.setCancelled(true);
            sendCooldownMessage(player,ChatColor.RED + "You cannot access containers here.");
        } else if (faction.equalsIgnoreCase(FactionsConstants.WILDERNESS)) {
            // Wilderness allows everything
            return;
        } else {
            // Check if player is in the faction that owns this claim
            String playerFaction = playerFactions.get(player.getUniqueId());
            if (playerFaction != null && playerFaction.equals(faction)) {
                // Player is in the faction - check faction permissions
                Faction f = factions.get(faction);
                Rank playerRank = f.members.get(player.getUniqueId());

                // Check for ender chest specifically
                if (event.getInventory().getType() == org.bukkit.event.inventory.InventoryType.ENDER_CHEST) {
                    if (!f.hasPermission(playerRank, FactionPermission.ENDER_CHEST_ACCESS)) {
                        event.setCancelled(true);
                        sendCooldownMessage(player,ChatColor.RED + "Your rank doesn't have permission to access ender chests.");
                        return;
                    }
                } else {
                    // Regular container access
                    if (!f.hasPermission(playerRank, FactionPermission.CONTAINER_ACCESS)) {
                        event.setCancelled(true);
                        sendCooldownMessage(player,ChatColor.RED + "Your rank doesn't have permission to access containers.");
                        return;
                    }
                }
            } else {
                // Player is from different faction or no faction - check relation permissions
                RelationPermission requiredPermission = RelationPermission.CONTAINER_ACCESS;
                if (event.getInventory().getType() == org.bukkit.event.inventory.InventoryType.ENDER_CHEST) {
                    requiredPermission = RelationPermission.ENDER_CHEST_ACCESS;
                }

                if (!plugin.getRelationManager().hasRelationPermission(player, faction, requiredPermission)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player,ChatColor.RED + "You cannot access containers in " + faction + " territory.");
                    return;
                }
            }
        }
    }

    /**
     * Handle spawner placement and breaking
     */
    @EventHandler
    public void onSpawnerPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) return;

        Player player = event.getPlayer();
        Chunk chunk = event.getBlock().getChunk();
        String faction = getFactionAtChunk(player.getWorld(), new ChunkCoord(chunk.getX(), chunk.getZ()));
        if (faction == null) return;

        // Check for bypass permissions
        if (hasPermissionBypass(player, faction, "spawner_place")) {
            return; // Allow the action
        }

        if (faction.equalsIgnoreCase(FactionsConstants.SPAWN) || faction.equalsIgnoreCase(FactionsConstants.WARZONE)) {
            event.setCancelled(true);
            sendCooldownMessage(player,ChatColor.RED + "You cannot place spawners here.");
        } else if (faction.equalsIgnoreCase(FactionsConstants.WILDERNESS)) {
            // Wilderness allows everything
            return;
        } else {
            // Check if player is in the faction that owns this claim
            String playerFaction = playerFactions.get(player.getUniqueId());
            if (playerFaction != null && playerFaction.equals(faction)) {
                // Player is in the faction - check faction permissions
                Faction f = factions.get(faction);
                Rank playerRank = f.members.get(player.getUniqueId());
                if (!f.hasPermission(playerRank, FactionPermission.PLACE_SPAWNERS)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player,ChatColor.RED + "Your rank doesn't have permission to place spawners.");
                    return;
                }
            } else {
                // Player is from different faction or no faction - check relation permissions
                if (!plugin.getRelationManager().hasRelationPermission(player, faction, RelationPermission.PLACE_SPAWNERS)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player,ChatColor.RED + "You cannot place spawners in " + faction + " territory.");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onSpawnerBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) return;

        Player player = event.getPlayer();
        Chunk chunk = event.getBlock().getChunk();
        String faction = getFactionAtChunk(player.getWorld(), new ChunkCoord(chunk.getX(), chunk.getZ()));
        if (faction == null) return;

        // Check for bypass permissions
        if (hasPermissionBypass(player, faction, "spawner_break")) {
            return; // Allow the action
        }

        if (faction.equalsIgnoreCase(FactionsConstants.SPAWN) || faction.equalsIgnoreCase(FactionsConstants.WARZONE)) {
            event.setCancelled(true);
            sendCooldownMessage(player,ChatColor.RED + "You cannot break spawners here.");
        } else if (faction.equalsIgnoreCase(FactionsConstants.WILDERNESS)) {
            // Wilderness allows everything
            return;
        } else {
            // Check if player is in the faction that owns this claim
            String playerFaction = playerFactions.get(player.getUniqueId());
            if (playerFaction != null && playerFaction.equals(faction)) {
                // Player is in the faction - check faction permissions
                Faction f = factions.get(faction);
                Rank playerRank = f.members.get(player.getUniqueId());
                if (!f.hasPermission(playerRank, FactionPermission.BREAK_SPAWNERS)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player,ChatColor.RED + "Your rank doesn't have permission to break spawners.");
                    return;
                }
            } else {
                // Player is from different faction or no faction - check relation permissions
                if (!plugin.getRelationManager().hasRelationPermission(player, faction, RelationPermission.BREAK_SPAWNERS)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player,ChatColor.RED + "You cannot break spawners in " + faction + " territory.");
                    return;
                }
            }
        }
    }

    /**
     * Handle entity interactions (item frames, armor stands, etc.)
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        // Only protect certain entities
        if (!(event.getRightClicked() instanceof ItemFrame) &&
                !(event.getRightClicked() instanceof ArmorStand)) {
            return;
        }

        Chunk chunk = event.getRightClicked().getLocation().getChunk();
        String faction = getFactionAtChunk(event.getRightClicked().getWorld(),
                new ChunkCoord(chunk.getX(), chunk.getZ()));
        if (faction == null) return;

        // Check for bypass permissions
        if (hasPermissionBypass(player, faction, "entity_interact")) {
            return; // Allow the action
        }

        if (faction.equalsIgnoreCase(FactionsConstants.SPAWN) || faction.equalsIgnoreCase(FactionsConstants.WARZONE)) {
            event.setCancelled(true);
            sendCooldownMessage(player,ChatColor.RED + "You cannot interact with entities here.");
        } else if (faction.equalsIgnoreCase(FactionsConstants.WILDERNESS)) {
            // Wilderness allows everything
            return;
        } else {
            // Check if player is in the faction that owns this claim
            String playerFaction = playerFactions.get(player.getUniqueId());
            if (playerFaction != null && playerFaction.equals(faction)) {
                // Player is in the faction - check faction permissions
                Faction f = factions.get(faction);
                Rank playerRank = f.members.get(player.getUniqueId());
                if (!f.hasPermission(playerRank, FactionPermission.INTERACT)) {
                    event.setCancelled(true);
                    sendCooldownMessage(player,ChatColor.RED + "Your rank doesn't have permission to interact with entities.");
                    return;
                }
            } else {
                // Player is from different faction or no faction - check relation permissions
                if (!plugin.getRelationManager().hasRelationPermission(player, faction, RelationPermission.INTERACT)) {
                    event.setCancelled(true);
                    MessageManager.sendError(player,"You cannot interact with entities in " + faction + " territory.");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();

        String title = event.getView().getTitle();
        if (title.contains("Faction") || title.contains("Members:") || title.contains("Confirm:") ||
                title.contains("Kick:") || title.contains("Leave:") || title.contains("Disband:") ||
                title.equals(ChatColor.DARK_GRAY + "Browse Factions") ||
                title.equals(ChatColor.DARK_GRAY + "Public Factions") ||
                title.equals(ChatColor.DARK_GRAY + "Your Invitations") ||
                title.equals(ChatColor.DARK_GRAY + "Relation Requests") ||
                title.equals(ChatColor.DARK_GRAY + "Faction Relations") ||
                title.startsWith(ChatColor.DARK_RED + "Remove: ")) {

            // Clear offhand when closing GUI
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));

            // Clean up tracking
            playerInFactionGUI.remove(uuid);

            // Clear pending kick if player closes kick confirmation
            if (title.contains("Kick:")) {
                plugin.getMenuHandler().clearPendingKick(uuid);
            }
        }
    }

    @EventHandler
    public void onPlayerItemHeld(org.bukkit.event.player.PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Clear offhand if player is in faction GUI
        if (playerInFactionGUI.getOrDefault(uuid, false)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onPlayerInteractMainHand(org.bukkit.event.player.PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Clear offhand if player is in faction GUI
        if (playerInFactionGUI.getOrDefault(uuid, false)) {
            if (player.getInventory().getItemInOffHand().getType() != Material.AIR) {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSwapHandItems(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Always prevent swapping if player is in faction GUI
        if (playerInFactionGUI.getOrDefault(uuid, false)) {
            event.setCancelled(true);

            // Clear offhand to be safe
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        String title = event.getView().getTitle();

        // Handle faction GUI clicks
        if (title.contains("Faction") || title.contains("Members:") || title.contains("Confirm:") ||
                title.contains("Kick:") || title.contains("Leave:") || title.contains("Disband:") ||
                title.equals(ChatColor.DARK_GRAY + "Browse Factions") ||
                title.equals(ChatColor.DARK_GRAY + "Public Factions") ||
                title.equals(ChatColor.DARK_GRAY + "Your Invitations") ||
                title.equals(ChatColor.DARK_GRAY + "Invite Players") ||
                title.equals(ChatColor.DARK_GRAY + "Faction Settings") ||
                title.equals(ChatColor.DARK_GRAY + "Faction Permissions") ||
                title.equals(ChatColor.DARK_GRAY + "Relations & Requests") ||
                title.startsWith(ChatColor.DARK_RED + "Remove: ") ||
                title.contains(" Permissions")) {

            // Cancel ALL clicks in faction GUIs
            event.setCancelled(true);

            // Clear offhand immediately
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));

            // Only process menu clicks if it's a LEFT or RIGHT click on a valid item
            if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) return;
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            ItemStack item = event.getCurrentItem();
            if (item.getItemMeta() == null) return;
            String displayName = item.getItemMeta().getDisplayName();

            // Handle different menu interactions
            if (title.equals(ChatColor.DARK_GRAY + "Factions Menu")) {
                if (event.getClick() == ClickType.LEFT) {
                    plugin.getMenuHandler().handleNoFactionMenuClick(player, displayName);
                }
            } else if (title.startsWith(ChatColor.DARK_GRAY + "Faction: ")) {
                if (event.getClick() == ClickType.LEFT) {
                    plugin.getMenuHandler().handleFactionMenuClick(player, displayName);
                }
            } else if (title.startsWith(ChatColor.DARK_GRAY + "Members: ")) {
                plugin.getMenuHandler().handleMembersMenuClick(player, item, event.getClick());
            } else if (title.startsWith(ChatColor.DARK_GRAY + "Confirm: ")) {
                if (event.getClick() == ClickType.LEFT) {
                    plugin.getMenuHandler().handleConfirmationMenuClick(player, displayName, title);
                }
            } else if (title.startsWith(ChatColor.DARK_RED + "Kick: ")) {
                if (event.getClick() == ClickType.LEFT) {
                    plugin.getMenuHandler().handleKickConfirmationClick(player, displayName, title);
                }
            } else if (title.startsWith(ChatColor.DARK_RED + "Leave: ") || title.startsWith(ChatColor.DARK_RED + "Disband: ")) {
                if (event.getClick() == ClickType.LEFT) {
                    if (title.startsWith(ChatColor.DARK_RED + "Leave: ")) {
                        plugin.getMenuHandler().handleLeaveConfirmationClick(player, displayName, title);
                    } else {
                        plugin.getMenuHandler().handleDisbandConfirmationClick(player, displayName, title);
                    }
                }
            } else if (title.equals(ChatColor.DARK_GRAY + "Browse Factions")) {
                if (event.getClick() == ClickType.LEFT) {
                    plugin.getBrowserMenuHandler().handleFactionBrowserClick(player, displayName);
                }
            } else if (title.equals(ChatColor.DARK_GRAY + "Public Factions")) {
                if (event.getClick() == ClickType.LEFT) {
                    plugin.getBrowserMenuHandler().handlePublicFactionsBrowserClick(player, displayName, event.getSlot());
                }
            } else if (title.equals(ChatColor.DARK_GRAY + "Your Invitations")) {
                if (event.getClick() == ClickType.LEFT) {
                    plugin.getBrowserMenuHandler().handleInvitationsBrowserClick(player, displayName, event.getSlot());
                }
            } else if (title.equals(ChatColor.DARK_GRAY + "Invite Players")) {
                if (event.getClick() == ClickType.LEFT) {
                    plugin.getMenuHandler().handleInvitationMenuClick(player, displayName, title);
                }
            } else if (title.equals(ChatColor.DARK_GRAY + "Faction Settings")) {
                if (event.getClick() == ClickType.LEFT) {
                    plugin.getMenuHandler().handleSettingsMenuClick(player, displayName, title);
                }
            } else if (title.equals(ChatColor.DARK_GRAY + "Faction Permissions")) {
                if (event.getClick() == ClickType.LEFT) {
                    plugin.getMenuHandler().handlePermissionsGUIClick(player, displayName, title);
                }
            } else if (title.contains(" Permissions") && !title.equals(ChatColor.DARK_GRAY + "Faction Permissions")) {
                // Handle rank/relation specific permission GUIs
                if (event.getClick() == ClickType.LEFT) {
                    if (title.contains("ADMIN") || title.contains("MOD") || title.contains("MEMBER") || title.contains("RECRUIT")) {
                        plugin.getMenuHandler().handleRankPermissionsClick(player, displayName, title);
                    } else {
                        plugin.getMenuHandler().handleRelationPermissionsClick(player, displayName, title);
                    }
                }
            } else if (title.equals(ChatColor.DARK_GRAY + "Relations & Requests")) {
                // UPDATED: Handle the new combined relations GUI
                plugin.getMenuHandler().handleRelationsViewClick(player, item, event.getClick(), title);
            }
            return;
        }

        // If player is in faction GUI, prevent any inventory manipulation
        if (playerInFactionGUI.getOrDefault(uuid, false)) {
            event.setCancelled(true);

            // Clear offhand
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    private void checkAndClearOffhand(Player player) {
        UUID uuid = player.getUniqueId();

        // Only proceed if player is still in a faction GUI
        if (!playerInFactionGUI.getOrDefault(uuid, false)) return;

        ItemStack currentOffhand = player.getInventory().getItemInOffHand();

        // Check if offhand contains a faction GUI item
        if (isFactionGUIItem(currentOffhand)) {
            // Clear the offhand
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        String title = event.getView().getTitle();
        if (title.contains("Faction") || title.contains("Members:") || title.contains("Confirm:") || title.contains("Kick:") || title.contains("Leave:") || title.contains("Disband:") || title.contains("Browse Factions")) {
            event.setCancelled(true);
            // Clear offhand
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            return;
        }

        // If player is in faction GUI, prevent dragging
        if (playerInFactionGUI.getOrDefault(uuid, false)) {
            event.setCancelled(true);
            // Clear offhand
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Check if player is pending faction creation
        if (pendingFactionCreation.getOrDefault(uuid, false)) {
            event.setCancelled(true); // Cancel the chat message

            String message = event.getMessage().trim();

            // Handle cancel
            if (message.equalsIgnoreCase("cancel")) {
                pendingFactionCreation.remove(uuid);

                // Schedule sync task for player interaction
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        MessageManager.sendError(player,"Faction creation cancelled.");
                        openFactionsMenu(player); // Reopen the menu
                    }
                }.runTask(plugin);
                return;
            }

            // Validate faction name
            if (message.isEmpty()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        MessageManager.sendError(player,"Faction name cannot be empty!");
                        MessageManager.sendInfo(player,"Please enter a valid faction name or type 'cancel':");
                    }
                }.runTask(plugin);
                return;
            }

            // Validate faction name format
            if (!ChatUtils.isValidFactionName(message)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (message.length() > FactionsConstants.MAX_FACTION_NAME_LENGTH) {
                            MessageManager.sendError(player, "Faction name is too long! Maximum " + FactionsConstants.MAX_FACTION_NAME_LENGTH + " characters.");
                        } else if (!message.matches("[a-zA-Z0-9_]+")) {
                            MessageManager.sendError(player,"Faction name can only contain letters, numbers, and underscores!");
                        } else {
                            MessageManager.sendError(player,"Invalid faction name!");
                        }
                        MessageManager.sendInfo(player,"Please enter a valid name or type 'cancel':");
                    }
                }.runTask(plugin);
                return;
            }

            // Check if faction name already exists
            if (factions.containsKey(message)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        MessageManager.sendError(player,"A faction with the name '" + message + "' already exists!");
                        MessageManager.sendInfo(player,"Please choose a different name or type 'cancel':");
                    }
                }.runTask(plugin);
                return;
            }

            // Store the name and ask for confirmation
            pendingFactionNames.put(uuid, message);

            new BukkitRunnable() {
                @Override
                public void run() {
                    openFactionCreationConfirmation(player, message);
                }
            }.runTask(plugin);

            new BukkitRunnable() {
                @Override
                public void run() {
                    openFactionCreationConfirmation(player, message);
                }
            }.runTask(plugin);
        }
    }

    private boolean isFactionGUIItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        String displayName = item.getItemMeta().getDisplayName();

        // Define a comprehensive list of faction GUI item names
        Set<String> factionGUIItems = new HashSet<>();

        // Original faction GUI items
        factionGUIItems.add(ChatColor.GREEN + "Create Faction");
        factionGUIItems.add(ChatColor.BLUE + "Browse Factions");
        factionGUIItems.add(ChatColor.YELLOW + "View Members");
        factionGUIItems.add(ChatColor.AQUA + "Faction Info");
        factionGUIItems.add(ChatColor.GREEN + "Territory");
        factionGUIItems.add(ChatColor.LIGHT_PURPLE + "Invite Players");
        factionGUIItems.add(ChatColor.RED + "Faction Settings");
        factionGUIItems.add(ChatColor.RED + "Leave Faction");
        factionGUIItems.add(ChatColor.RED + "Disband Faction");
        factionGUIItems.add(ChatColor.LIGHT_PURPLE + "Faction Visibility");

        // Confirmation dialog items
        factionGUIItems.add(ChatColor.GREEN + "" + ChatColor.BOLD + "CONFIRM");
        factionGUIItems.add(ChatColor.RED + "" + ChatColor.BOLD + "CANCEL");
        factionGUIItems.add(ChatColor.RED + "" + ChatColor.BOLD + "KICK PLAYER");
        factionGUIItems.add(ChatColor.RED + "" + ChatColor.BOLD + "LEAVE FACTION");
        factionGUIItems.add(ChatColor.RED + "" + ChatColor.BOLD + "DISBAND FACTION");

        // ADDED: Relation removal confirmation items
        factionGUIItems.add(ChatColor.RED + "" + ChatColor.BOLD + "REMOVE RELATION");
        factionGUIItems.add(ChatColor.GREEN + "" + ChatColor.BOLD + "CANCEL");

        // Browse menu items
        factionGUIItems.add(ChatColor.GREEN + "" + ChatColor.BOLD + "PUBLIC FACTIONS");
        factionGUIItems.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "YOUR INVITATIONS");
        factionGUIItems.add(ChatColor.GRAY + "No Invitations");

        // Navigation items
        factionGUIItems.add(ChatColor.GRAY + "← Back");
        factionGUIItems.add(ChatColor.GRAY + "← Previous Page");
        factionGUIItems.add(ChatColor.GRAY + "Next Page →");

        // Misc items
        factionGUIItems.add(" "); // Black glass pane
        factionGUIItems.add(ChatColor.RED + "No Invitations");

        // Check exact matches first
        if (factionGUIItems.contains(displayName)) {
            return true;
        }

        // Check for player heads in members menu (they start with yellow color)
        if (item.getType() == Material.PLAYER_HEAD && displayName.startsWith(ChatColor.YELLOW.toString())) {
            return true;
        }

        // Check for faction name items (start with "Faction Name:")
        if (displayName.startsWith(ChatColor.YELLOW + "Faction Name:")) {
            return true;
        }

        // Check for kick confirmation items (start with "Kick:")
        if (displayName.startsWith(ChatColor.YELLOW + "Kick:")) {
            return true;
        }

        // ADDED: Check for relation removal confirmation items (start with "Remove Relation?")
        if (displayName.startsWith(ChatColor.YELLOW + "Remove Relation?")) {
            return true;
        }

        // Check for page info items
        if (displayName.startsWith(ChatColor.YELLOW + "Page ")) {
            return true;
        }

        // Check for "Leave:" or "Disband:" confirmation items
        if (displayName.startsWith(ChatColor.YELLOW + "Leave ") || displayName.startsWith(ChatColor.RED + "" + ChatColor.BOLD + "DANGER!")) {
            return true;
        }

        return false;
    }

    public void disbandFaction(String factionName, Player disbander) {
        Faction faction = factions.get(factionName);
        if (faction == null) return;

        // Notify all members and update nametags
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null) {
                MessageManager.sendMemberFactionDisbanded(member, factionName, disbander.getDisplayName());

                // UPDATE NAMETAGS when faction is disbanded
                plugin.getEventListener().onPlayerLeaveFaction(member);
            }
            playerFactions.remove(memberUUID);
        }

        // Remove all claims
        for (Map<ChunkCoord, String> worldClaim : plugin.getWorldClaims().values()) {
            worldClaim.entrySet().removeIf(entry -> entry.getValue().equals(factionName));
        }

        plugin.getRelationManager().removeAllRelationsForFaction(factionName);

        // Remove faction from invitations
        for (Set<String> invites : playerInvitations.values()) {
            invites.remove(factionName);
        }
        playerInvitations.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        // Remove faction
        factions.remove(factionName);

        MessageManager.sendDisbandSuccess(disbander, factionName);

        // Save data
        plugin.getDataManager().saveFactionData();
    }

    /**
     * Update name colors when a player joins a faction
     */
    public void onPlayerJoinFaction(Player player) {
        plugin.getLogger().info("Player " + player.getName() + " joined a faction, updating nametags");

        // Update after a delay to ensure faction data is updated
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getNametagManager().onFactionChange(player);
        }, 5L);
    }

    /**
     * Update name colors when a player leaves a faction
     */
    public void onPlayerLeaveFaction(Player player) {
        plugin.getLogger().info("Player " + player.getName() + " left a faction, updating nametags");

        // Update after a delay to ensure faction data is updated
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getNametagManager().onFactionChange(player);

            // Keep chat display name reset for chat purposes
            player.setDisplayName(player.getName());
        }, 5L);
    }

    /**
     * Update name colors when faction relations change
     */
    public void onFactionRelationChange(String faction1, String faction2) {
        plugin.getNametagManager().onRelationChange(faction1, faction2);
    }

    /**
     * Send a message to player with cooldown to prevent spam
     */
    private void sendCooldownMessage(Player player, String message) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        Map<String, Long> playerCooldowns = playerMessageCooldowns.computeIfAbsent(playerUUID, k -> new HashMap<>());

        Long lastSent = playerCooldowns.get(message);
        if (lastSent == null || (currentTime - lastSent) >= FactionsConstants.MESSAGE_COOLDOWN) {
            MessageManager.sendError(player, message);
            playerCooldowns.put(message, currentTime);
        }
    }

    private boolean isInteractiveBlock(Material material) {
        return material.name().contains("BUTTON") ||
                material.name().contains("LEVER") ||
                material.name().contains("PRESSURE_PLATE") ||
                material.name().contains("DOOR") ||
                material.name().contains("GATE") ||
                material == Material.REPEATER ||
                material == Material.COMPARATOR ||
                material.name().contains("TRAPDOOR") ||
                material.name().contains("FENCE_GATE") ||
                material == Material.TRIPWIRE_HOOK ||
                material == Material.DAYLIGHT_DETECTOR ||
                material == Material.REDSTONE_TORCH ||
                material == Material.REDSTONE_WALL_TORCH;
    }
}