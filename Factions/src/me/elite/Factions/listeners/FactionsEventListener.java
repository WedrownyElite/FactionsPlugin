package me.elite.Factions.listeners;
import me.elite.Factions.data.Relation;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.Rank;
import me.elite.Factions.territory.ChunkCoord;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventPriority;
import org.bukkit.Bukkit;
import java.util.Set;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;


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

    public void handleConfirmationMenuClick(Player player, String displayName, String title) {
        plugin.getFactionCreationManager().handleConfirmationMenuClick(player, displayName, title);
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
            if (newFaction.equalsIgnoreCase("Spawn")) {
                player.sendTitle(ChatColor.AQUA + "Spawn", ChatColor.GREEN + "You're safe here", 10, 60, 10);
            } else if (newFaction.equalsIgnoreCase("Warzone")) {
                player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "Warzone", ChatColor.WHITE + "Careful, PvP is allowed here", 10, 60, 10);
            } else if (newFaction.equalsIgnoreCase("Wilderness")) {
                player.sendTitle(ChatColor.GREEN + "Wilderness", ChatColor.WHITE + "Unclaimed land - claim it or do whatever you want!", 10, 60, 10);
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
        Chunk chunk = player.getLocation().getChunk();
        String faction = getFactionAtChunk(player.getWorld(), new ChunkCoord(chunk.getX(), chunk.getZ()));
        if (faction == null) return;

        // Check for bypass permissions
        if (hasPermissionBypass(player, faction, "break")) {
            return; // Allow the action
        }

        if (faction.equalsIgnoreCase("Spawn") || faction.equalsIgnoreCase("Warzone")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot break blocks here.");
        } else if (faction.equalsIgnoreCase("Wilderness")) {
            // Wilderness allows everything
            return;
        } else {
            // Check if player is in the faction that owns this claim
            String playerFaction = playerFactions.get(player.getUniqueId());
            if (playerFaction == null || !playerFaction.equals(faction)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot break blocks in " + faction + " territory.");
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = player.getLocation().getChunk();
        String faction = getFactionAtChunk(player.getWorld(), new ChunkCoord(chunk.getX(), chunk.getZ()));
        if (faction == null) return;

        // Check for bypass permissions
        if (hasPermissionBypass(player, faction, "place")) {
            return; // Allow the action
        }

        if (faction.equalsIgnoreCase("Spawn") || faction.equalsIgnoreCase("Warzone")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot place blocks here.");
        } else if (faction.equalsIgnoreCase("Wilderness")) {
            // Wilderness allows everything
            return;
        } else {
            // Check if player is in the faction that owns this claim
            String playerFaction = playerFactions.get(player.getUniqueId());
            if (playerFaction == null || !playerFaction.equals(faction)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot place blocks in " + faction + " territory.");
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getLocation() == null) return;
        Chunk chunk = player.getLocation().getChunk();
        String faction = getFactionAtChunk(player.getWorld(), new ChunkCoord(chunk.getX(), chunk.getZ()));
        if (faction == null) return;

        // Check for bypass permissions
        if (hasPermissionBypass(player, faction, "interact")) {
            return; // Allow the action
        }

        if (faction.equalsIgnoreCase("Spawn") || faction.equalsIgnoreCase("Warzone")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot interact here.");
        } else if (faction.equalsIgnoreCase("Wilderness")) {
            // Wilderness allows everything
            return;
        } else {
            // Check if player is in the faction that owns this claim
            String playerFaction = playerFactions.get(player.getUniqueId());
            if (playerFaction == null || !playerFaction.equals(faction)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot interact in " + faction + " territory.");
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Cancel PvP in Spawn, allow in Warzone
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            Player damager = (Player) event.getDamager();
            Chunk chunk = damaged.getLocation().getChunk();
            String faction = getFactionAtChunk(damaged.getWorld(), new ChunkCoord(chunk.getX(), chunk.getZ()));
            if (faction == null) return;

            if (faction.equalsIgnoreCase("Spawn")) {
                // Check for bypass permissions
                if (hasPermissionBypass(damager, faction, "pvp")) {
                    return; // Allow PvP
                }
                event.setCancelled(true);
            } else if (faction.equalsIgnoreCase("Warzone")) {
                // PvP allowed in warzone by default, unless bypassed
                if (hasPermissionBypass(damager, faction, "pvp_disable")) {
                    event.setCancelled(true); // Disable PvP for this player
                }
            } else if (faction.equalsIgnoreCase("Wilderness")) {
                // Wilderness allows everything including PvP
                return;
            } else {
                // In faction territory - check if both players are in the same faction
                String damagerFaction = playerFactions.get(damager.getUniqueId());
                String damagedFaction = playerFactions.get(damaged.getUniqueId());

                // Check for bypass permissions
                if (hasPermissionBypass(damager, faction, "pvp")) {
                    return; // Allow PvP
                }

                // Cancel friendly fire within same faction
                if (damagerFaction != null && damagerFaction.equals(damagedFaction)) {
                    event.setCancelled(true);
                }
            }
        }

        // Prevent damage to mobs in Spawn and Warzone
        else if (event.getEntity() instanceof org.bukkit.entity.Entity && !(event.getEntity() instanceof Player)) {
            if (event.getDamager() instanceof Player) {
                Player player = (Player) event.getDamager();
                Chunk chunk = event.getEntity().getLocation().getChunk();
                String faction = getFactionAtChunk(event.getEntity().getWorld(), new ChunkCoord(chunk.getX(), chunk.getZ()));
                if (faction == null) return;

                // Check for bypass permissions
                if (hasPermissionBypass(player, faction, "damage_mobs")) {
                    return; // Allow mob damage
                }

                if (faction.equalsIgnoreCase("Spawn") || faction.equalsIgnoreCase("Warzone")) {
                    event.setCancelled(true);
                } else if (faction.equalsIgnoreCase("Wilderness")) {
                    // Wilderness allows everything
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Chunk chunk = event.getLocation().getChunk();
        String faction = getFactionAtChunk(event.getLocation().getWorld(), new ChunkCoord(chunk.getX(), chunk.getZ()));
        if (faction == null) return;

        // Cancel natural mob spawning in Spawn and Warzone only
        if ((faction.equalsIgnoreCase("Spawn") || faction.equalsIgnoreCase("Warzone"))
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
        if (title.contains("Faction") || title.contains("Members:") || title.contains("Confirm:") ||
                title.contains("Kick:") || title.contains("Leave:") || title.contains("Disband:") ||
                title.equals(ChatColor.DARK_GRAY + "Browse Factions") ||
                title.equals(ChatColor.DARK_GRAY + "Public Factions") ||
                title.equals(ChatColor.DARK_GRAY + "Your Invitations")) {

            UUID uuid = player.getUniqueId();
            playerInFactionGUI.put(uuid, true);

            // Clear offhand immediately when opening GUI
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
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
                title.equals(ChatColor.DARK_GRAY + "Your Invitations")) {

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
                    plugin.getMenuHandler().handleFactionBrowserClick(player, displayName);
                }
            } else if (title.equals(ChatColor.DARK_GRAY + "Public Factions")) {
                if (event.getClick() == ClickType.LEFT) {
                    plugin.getMenuHandler().handlePublicFactionsBrowserClick(player, displayName, event.getSlot());
                }
            } else if (title.equals(ChatColor.DARK_GRAY + "Your Invitations")) {
                if (event.getClick() == ClickType.LEFT) {
                    plugin.getMenuHandler().handleInvitationsBrowserClick(player, displayName, event.getSlot());
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
                        player.sendMessage(ChatColor.RED + "Faction creation cancelled.");
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
                        player.sendMessage(ChatColor.RED + "Faction name cannot be empty!");
                        player.sendMessage(ChatColor.YELLOW + "Please enter a valid faction name or type 'cancel':");
                    }
                }.runTask(plugin);
                return;
            }

            // Check if name is too long
            if (message.length() > 16) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.sendMessage(ChatColor.RED + "Faction name is too long! Maximum 16 characters.");
                        player.sendMessage(ChatColor.YELLOW + "Please enter a shorter name or type 'cancel':");
                    }
                }.runTask(plugin);
                return;
            }

            // Check for invalid characters
            if (!message.matches("[a-zA-Z0-9_]+")) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.sendMessage(ChatColor.RED + "Faction name can only contain letters, numbers, and underscores!");
                        player.sendMessage(ChatColor.YELLOW + "Please enter a valid name or type 'cancel':");
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

    public void handleNoFactionMenuClick(Player player, String displayName) {
        if (displayName.equals(ChatColor.GREEN + "Create Faction")) {
            player.closeInventory();
            plugin.getFactionCreationManager().openSignGUIForFactionCreation(player);
        } else if (displayName.equals(ChatColor.BLUE + "Browse Factions")) {
            plugin.getMenuHandler().openFactionBrowser(player); // This now opens the selection menu
        } else if (displayName.equals(ChatColor.GRAY + "← Back")) {
            // Close GUI for no-faction menu
            player.closeInventory();
        }
    }

    public void handleFactionMenuClick(Player player, String displayName) {
        String factionName = playerFactions.get(player.getUniqueId());

        if (displayName.equals(ChatColor.YELLOW + "View Members")) {
            // Use the MenuHandler to open members menu
            plugin.getMenuHandler().openMembersMenu(player, factionName);
        } else if (displayName.equals(ChatColor.GREEN + "Territory")) {
            player.closeInventory();
            plugin.displayFactionMap(player);
        } else if (displayName.equals(ChatColor.LIGHT_PURPLE + "Invite Players")) {
            player.sendMessage(ChatColor.YELLOW + "Invite feature coming soon! Use /f invite <player> for now.");
        } else if (displayName.equals(ChatColor.LIGHT_PURPLE + "Faction Visibility")) {
            // Handle privacy toggle
            Faction faction = factions.get(factionName);
            Rank playerRank = faction.members.get(player.getUniqueId());

            if (playerRank == Rank.OWNER || playerRank == Rank.ADMIN) {
                faction.isPublic = !faction.isPublic;
                player.sendMessage(ChatColor.GREEN + "Faction visibility changed to: " +
                        (faction.isPublic ? ChatColor.GREEN + "Public" : ChatColor.RED + "Private"));

                // Save the data
                plugin.getDataManager().saveFactionData();

                // Reopen menu to show updated status
                plugin.getMenuHandler().openFactionMenu(player, factionName);
            } else {
                player.sendMessage(ChatColor.RED + "You don't have permission to change faction visibility.");
            }
        } else if (displayName.equals(ChatColor.RED + "Leave Faction")) {
            player.closeInventory();
            Faction faction = factions.get(factionName);
            Rank playerRank = faction.members.get(player.getUniqueId());

            if (playerRank == Rank.OWNER) {
                // This shouldn't happen as owners should see "Disband Faction", but handle it anyway
                plugin.getMenuHandler().openDisbandConfirmation(player, factionName);
            } else {
                plugin.getMenuHandler().openLeaveConfirmation(player, factionName);
            }
        } else if (displayName.equals(ChatColor.RED + "Disband Faction")) {
            player.closeInventory();
            plugin.getMenuHandler().openDisbandConfirmation(player, factionName);
        } else if (displayName.equals(ChatColor.GRAY + "← Back")) {
            // Close GUI for faction menu
            player.closeInventory();
        }
    }

    private void handleBrowseFactionsClick(Player player, int slot, String displayName) {
        // Handle back button
        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            plugin.getMenuHandler().openFactionsMenu(player);
            return;
        }

        // Handle faction joins (faction heads)
        if (displayName.startsWith(ChatColor.YELLOW.toString())) {
            String factionName = ChatColor.stripColor(displayName);
            joinFactionFromBrowser(player, factionName);
            return;
        }

        // Handle navigation arrows - this would need to be implemented with page tracking
        if (displayName.contains("Previous") || displayName.contains("Next")) {
            player.sendMessage(ChatColor.YELLOW + "Pagination coming soon!");
            return;
        }
    }

    private void joinFactionFromBrowser(Player player, String factionName) {
        UUID uuid = player.getUniqueId();

        if (playerFactions.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "You are already in a faction.");
            return;
        }

        Faction faction = factions.get(factionName);
        if (faction == null) {
            player.sendMessage(ChatColor.RED + "Faction no longer exists.");
            return;
        }

        // Check if can join (public or invited)
        boolean canJoin = faction.isPublic;
        Set<String> invites = playerInvitations.get(uuid);
        if (!canJoin && invites != null && invites.contains(factionName)) {
            canJoin = true;
            // Remove invitation
            invites.remove(factionName);
            if (invites.isEmpty()) {
                playerInvitations.remove(uuid);
            }
        }

        if (!canJoin) {
            player.sendMessage(ChatColor.RED + "You cannot join this faction.");
            return;
        }

        // Join the faction
        faction.members.put(uuid, Rank.RECRUIT);
        playerFactions.put(uuid, factionName);

        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Successfully joined faction " + factionName + "!");

        // Notify other members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(player)) {
                member.sendMessage(ChatColor.GREEN + player.getName() + " has joined the faction!");
            }
        }

        // Save data
        plugin.getDataManager().saveFactionData();
    }

    public void handleMembersMenuClick(Player player, ItemStack item, ClickType clickType) {
        if (item.getItemMeta() == null) return;
        String displayName = item.getItemMeta().getDisplayName();

        // DEBUG: Log click information
        player.sendMessage(ChatColor.YELLOW + "DEBUG: Clicked item: " + displayName + " with " + clickType);

        // Handle back button click FIRST
        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            // Go back to faction menu using MenuHandler
            plugin.getMenuHandler().openFactionsMenu(player);
            return;
        }

        // Handle member clicks
        if (item.getType() != Material.PLAYER_HEAD) {
            player.sendMessage(ChatColor.RED + "DEBUG: Not a player head!");
            return;
        }

        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        OfflinePlayer targetPlayer = skullMeta.getOwningPlayer();

        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "DEBUG: OfflinePlayer is null!");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "DEBUG: Target player found: " + targetPlayer.getName());

        String factionName = playerFactions.get(player.getUniqueId());
        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(player.getUniqueId());

        if (playerRank != Rank.OWNER && playerRank != Rank.ADMIN) {
            return; // No permission to manage
        }

        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            return; // Can't manage self
        }

        if (clickType == ClickType.LEFT) {
            // Manage rank - implement later
            player.sendMessage(ChatColor.YELLOW + "Rank management coming soon! Use /f promote or /f demote for now.");
        } else if (clickType == ClickType.RIGHT) {
            // Check rank permissions for kicking
            Rank targetRank = faction.members.get(targetPlayer.getUniqueId());
            if (playerRank == Rank.ADMIN && (targetRank == Rank.ADMIN || targetRank == Rank.OWNER)) {
                player.sendMessage(ChatColor.RED + "You cannot kick a player of equal or higher rank.");
                return;
            }
            if (targetRank == Rank.OWNER) {
                player.sendMessage(ChatColor.RED + "You cannot kick the faction owner.");
                return;
            }

            // Open kick confirmation dialog
            plugin.getMenuHandler().openKickConfirmation(player, targetPlayer);
        }
    }
}