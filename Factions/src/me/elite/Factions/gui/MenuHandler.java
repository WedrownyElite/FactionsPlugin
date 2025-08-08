package me.elite.Factions.gui;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.Rank;
import me.elite.Factions.territory.ChunkCoord;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.Set;

import java.util.*;

public class MenuHandler {
    private final FactionsPlugin plugin;
    private final Map<String, Faction> factions;
    private final Map<UUID, String> playerFactions;
    private final Map<UUID, Set<String>> playerInvitations;

    // Track pending kick confirmations
    private final Map<UUID, UUID> pendingKicks = new HashMap<>(); // kicker -> target

    public MenuHandler(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
        this.playerInvitations = plugin.getPlayerInvitations();
    }

    /**
     * Create a "Back" button with left arrow player head
     */
    private ItemStack createBackButton() {
        ItemStack backButton = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) backButton.getItemMeta();

        // Set the display name and lore
        skullMeta.setDisplayName(ChatColor.GRAY + "← Back");
        skullMeta.setLore(Arrays.asList(
                ChatColor.DARK_GRAY + "Click to go back"
        ));

        // Set custom texture using base64
        String texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGUxZGZjMTFhODM3MTExZDIyYjAwMWExNDQ2MWY5YTdmYzA5MzUyMmY4OGM1OGZhZWZkNmFkZWZmY2Q0ZTlhYiJ9fX0=";

        try {
            // Create a GameProfile with a random UUID
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);

            // Add the texture property
            profile.getProperties().put("textures", new Property("textures", texture));

            // Use reflection to set the profile
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (Exception e) {
            // If reflection fails, fall back to a regular player head
            plugin.getLogger().warning("Failed to set custom head texture: " + e.getMessage());
        }

        backButton.setItemMeta(skullMeta);
        return backButton;
    }

    /**
     * Open the main factions menu
     */
    public void openFactionsMenu(Player player) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            // Player has no faction - show simple menu
            openNoFactionMenu(player);
        } else {
            // Player has faction - show full menu
            openFactionMenu(player, factionName);
        }
    }

    /**
     * Menu for players without a faction
     */
    private void openNoFactionMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 9, ChatColor.DARK_GRAY + "Factions Menu"); // 2 rows

        // Fill with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 9; i++) {
            menu.setItem(i, blackGlass);
        }

        // Create faction option
        ItemStack createFaction = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta createMeta = createFaction.getItemMeta();
        createMeta.setDisplayName(ChatColor.GREEN + "Create Faction");
        createMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click to create a new faction",
                ChatColor.GRAY + "You will be prompted for a name"
        ));
        createFaction.setItemMeta(createMeta);
        menu.setItem(3, createFaction);

        // Join faction option (placeholder for now)
        ItemStack joinFaction = new ItemStack(Material.DIAMOND_BLOCK);
        ItemMeta joinMeta = joinFaction.getItemMeta();
        joinMeta.setDisplayName(ChatColor.BLUE + "Browse Factions");
        joinMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "View factions accepting members",
                ChatColor.RED + "Coming Soon!"
        ));
        joinFaction.setItemMeta(joinMeta);
        menu.setItem(5, joinFaction);

        // Back button (closes GUI for no-faction menu)
        ItemStack backButton = createBackButton();
        menu.setItem(0, backButton);

        player.openInventory(menu);
    }

    /**
     * Menu for players with a faction
     */
    private void openFactionMenu(Player player, String factionName) {
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Faction: " + factionName);

        // Fill with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 54; i++) {
            menu.setItem(i, blackGlass);
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(player.getUniqueId());

        // Members option
        ItemStack members = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta membersMeta = members.getItemMeta();
        membersMeta.setDisplayName(ChatColor.YELLOW + "View Members");
        membersMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "View all faction members",
                ChatColor.GRAY + "Members: " + faction.members.size()
        ));
        members.setItemMeta(membersMeta);
        menu.setItem(10, members);

        // Faction info
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.AQUA + "Faction Info");
        infoMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Name: " + ChatColor.WHITE + factionName,
                ChatColor.GRAY + "Description: " + ChatColor.WHITE + faction.description,
                ChatColor.GRAY + "Your Rank: " + ChatColor.WHITE + playerRank.name(),
                ChatColor.GRAY + "Members: " + ChatColor.WHITE + faction.members.size()
        ));
        info.setItemMeta(infoMeta);
        menu.setItem(13, info);

        // Claims info
        ItemStack claims = new ItemStack(Material.MAP);
        ItemMeta claimsMeta = claims.getItemMeta();
        claimsMeta.setDisplayName(ChatColor.GREEN + "Territory");
        claimsMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "View faction claims",
                ChatColor.GRAY + "Click to see faction map"
        ));
        claims.setItemMeta(claimsMeta);
        menu.setItem(16, claims);

        // Public/Private toggle for admins and owners, view-only for members
        ItemStack publicPrivate = new ItemStack(Material.PAPER);
        ItemMeta publicMeta = publicPrivate.getItemMeta();
        if (playerRank == Rank.OWNER || playerRank == Rank.ADMIN) {
            publicMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Faction Visibility");
            publicMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Current: " + (faction.isPublic ? ChatColor.GREEN + "Public" : ChatColor.RED + "Private"),
                    ChatColor.GRAY + "Public: Anyone can join",
                    ChatColor.GRAY + "Private: Invite only",
                    "",
                    ChatColor.YELLOW + "Click to toggle"
            ));
        } else {
            publicMeta.setDisplayName(ChatColor.GRAY + "Faction Visibility");
            publicMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Current: " + (faction.isPublic ? ChatColor.GREEN + "Public" : ChatColor.RED + "Private"),
                    ChatColor.GRAY + "Public: Anyone can join",
                    ChatColor.GRAY + "Private: Invite only"
            ));
        }
        publicPrivate.setItemMeta(publicMeta);
        menu.setItem(22, publicPrivate); // Center bottom row

        // Admin options
        if (playerRank == Rank.OWNER || playerRank == Rank.ADMIN) {
            // Invite players
            ItemStack invite = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta inviteMeta = invite.getItemMeta();
            inviteMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Invite Players");
            inviteMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Invite online players to your faction"
            ));
            invite.setItemMeta(inviteMeta);
            menu.setItem(28, invite);

            // Manage settings
            ItemStack settings = new ItemStack(Material.REDSTONE);
            ItemMeta settingsMeta = settings.getItemMeta();
            settingsMeta.setDisplayName(ChatColor.RED + "Faction Settings");
            settingsMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Manage faction settings",
                    ChatColor.RED + "Coming Soon!"
            ));
            settings.setItemMeta(settingsMeta);
            menu.setItem(34, settings);
        }

        // Leave/Disband faction
        ItemStack leave = new ItemStack(Material.RED_CONCRETE);
        ItemMeta leaveMeta = leave.getItemMeta();
        if (playerRank == Rank.OWNER) {
            leaveMeta.setDisplayName(ChatColor.RED + "Disband Faction");
            leaveMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Disband your faction permanently",
                    ChatColor.RED + "This action cannot be undone!",
                    ChatColor.RED + "All claims will be lost!"
            ));
        } else {
            leaveMeta.setDisplayName(ChatColor.RED + "Leave Faction");
            leaveMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Leave your current faction",
                    ChatColor.RED + "This action cannot be undone!"
            ));
        }
        leave.setItemMeta(leaveMeta);
        menu.setItem(49, leave);

        // Back button (closes GUI for faction menu)
        ItemStack backButton = createBackButton();
        menu.setItem(45, backButton); // Bottom left

        player.openInventory(menu);
    }

    /**
     * Open members list menu with proper navigation
     */
    public void openMembersMenu(Player player, String factionName) {
        Faction faction = factions.get(factionName);
        if (faction == null) return;

        // Use a larger inventory - 4 rows (36 slots) for better display
        Inventory menu = Bukkit.createInventory(null, 36, ChatColor.DARK_GRAY + "Members: " + factionName);

        // Fill bottom row (slots 27-35) with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        // Fill bottom row with glass panes
        for (int i = 27; i < 36; i++) {
            menu.setItem(i, blackGlass);
        }

        // Back button in bottom left (slot 27)
        ItemStack backButton = createBackButton();
        menu.setItem(27, backButton);

        Rank playerRank = faction.members.get(player.getUniqueId());
        boolean canManage = (playerRank == Rank.OWNER || playerRank == Rank.ADMIN);

        int slot = 0;
        // Put player first
        addMemberToMenu(menu, slot++, player.getUniqueId(), faction, canManage, false);

        // Add other members (slots 0-26 are available, 27-35 are reserved for navigation)
        for (Map.Entry<UUID, Rank> entry : faction.members.entrySet()) {
            if (entry.getKey().equals(player.getUniqueId())) continue; // Skip player (already added)
            if (slot >= 27) break; // Don't go into navigation area

            addMemberToMenu(menu, slot++, entry.getKey(), faction, canManage, true);
        }

        player.openInventory(menu);
    }

    /**
     * Add a member head to the members menu
     */
    private void addMemberToMenu(Inventory menu, int slot, UUID memberUUID, Faction faction, boolean canManage, boolean canKick) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUUID);
        String playerName = offlinePlayer.getName();
        if (playerName == null) {
            playerName = "Unknown Player";
        }

        skullMeta.setOwningPlayer(offlinePlayer);
        skullMeta.setDisplayName(ChatColor.YELLOW + playerName);

        Rank memberRank = faction.members.get(memberUUID);
        boolean isOnline = offlinePlayer.isOnline();

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Rank: " + ChatColor.WHITE + memberRank.name());
        lore.add(ChatColor.GRAY + "Status: " + (isOnline ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline"));

        // Add empty line for spacing
        if (canManage && canKick) {
            lore.add("");
            lore.add(ChatColor.GREEN + "Left Click: Manage Rank");
            lore.add(ChatColor.RED + "Right Click: Kick Player");
        } else if (canManage) {
            lore.add("");
            lore.add(ChatColor.GREEN + "Left Click: Manage Rank");
        }

        skullMeta.setLore(lore);
        head.setItemMeta(skullMeta);

        menu.setItem(slot, head);
    }

    /**
     * Open kick confirmation dialog
     */
    public void openKickConfirmation(Player kicker, OfflinePlayer target) {
        String targetName = target.getName();
        if (targetName == null) targetName = "Unknown Player";

        // Store the pending kick
        pendingKicks.put(kicker.getUniqueId(), target.getUniqueId());

        Inventory confirmMenu = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Kick: " + targetName);

        // Fill with black glass
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 27; i++) {
            confirmMenu.setItem(i, blackGlass);
        }

        // Confirm button
        ItemStack confirm = new ItemStack(Material.RED_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "KICK PLAYER");
        confirmMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Kick: " + ChatColor.WHITE + targetName,
                "",
                ChatColor.RED + "This action cannot be undone!",
                ChatColor.GRAY + "The player will be removed from the faction."
        ));
        confirm.setItemMeta(confirmMeta);
        confirmMenu.setItem(11, confirm);

        // Cancel button
        ItemStack cancel = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "CANCEL");
        cancelMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Cancel kick operation",
                "",
                ChatColor.GREEN + "Go back to members list"
        ));
        cancel.setItemMeta(cancelMeta);
        confirmMenu.setItem(15, cancel);

        // Player head in center
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerSkullMeta = (SkullMeta) playerHead.getItemMeta();
        playerSkullMeta.setOwningPlayer(target);
        playerSkullMeta.setDisplayName(ChatColor.YELLOW + "Kick: " + targetName);

        Rank targetRank = factions.get(playerFactions.get(kicker.getUniqueId())).members.get(target.getUniqueId());
        boolean isOnline = target.isOnline();

        playerSkullMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Player: " + ChatColor.WHITE + targetName,
                ChatColor.GRAY + "Rank: " + ChatColor.WHITE + targetRank.name(),
                ChatColor.GRAY + "Status: " + (isOnline ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline"),
                "",
                ChatColor.RED + "Are you sure you want to kick this player?"
        ));
        playerHead.setItemMeta(playerSkullMeta);
        confirmMenu.setItem(13, playerHead);

        kicker.openInventory(confirmMenu);
    }

    public void openFactionCreationConfirmation(Player player, String factionName) {
        Inventory confirmMenu = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Confirm: " + factionName);

        // Fill with black glass
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 27; i++) {
            confirmMenu.setItem(i, blackGlass);
        }

        // Confirm button
        ItemStack confirm = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "CONFIRM");
        confirmMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Create faction: " + ChatColor.WHITE + factionName,
                "",
                ChatColor.GREEN + "Click to create your faction!"
        ));
        confirm.setItemMeta(confirmMeta);
        confirmMenu.setItem(11, confirm);

        // Cancel button
        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "CANCEL");
        cancelMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Cancel faction creation",
                "",
                ChatColor.RED + "Click to go back to menu"
        ));
        cancel.setItemMeta(cancelMeta);
        confirmMenu.setItem(15, cancel);

        // Info item
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.YELLOW + "Faction Name: " + factionName);
        infoMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Are you sure you want to",
                ChatColor.GRAY + "create this faction?",
                "",
                ChatColor.YELLOW + "This action cannot be undone!"
        ));
        info.setItemMeta(infoMeta);
        confirmMenu.setItem(13, info);

        player.openInventory(confirmMenu);
    }

    // Menu click handlers
    public void handleNoFactionMenuClick(Player player, String displayName) {
        if (displayName.equals(ChatColor.GREEN + "Create Faction")) {
            player.closeInventory();
            plugin.getFactionCreationManager().openSignGUIForFactionCreation(player);
        } else if (displayName.equals(ChatColor.BLUE + "Browse Factions")) {
            player.sendMessage(ChatColor.RED + "This feature is coming soon!");
        } else if (displayName.equals(ChatColor.GRAY + "← Back")) {
            // Close GUI for no-faction menu
            player.closeInventory();
        }
    }

    public void handleFactionMenuClick(Player player, String displayName) {
        String factionName = playerFactions.get(player.getUniqueId());

        if (displayName.equals(ChatColor.YELLOW + "View Members")) {
            openMembersMenu(player, factionName);
        } else if (displayName.equals(ChatColor.GREEN + "Territory")) {
            player.closeInventory();
            plugin.displayFactionMap(player);
        } else if (displayName.equals(ChatColor.LIGHT_PURPLE + "Invite Players")) {
            player.sendMessage(ChatColor.YELLOW + "Invite feature coming soon! Use /f invite <player> for now.");
        } else if (displayName.equals(ChatColor.RED + "Leave Faction")) {
            player.sendMessage(ChatColor.RED + "Leave faction feature coming soon! This needs confirmation dialog.");
        } else if (displayName.equals(ChatColor.LIGHT_PURPLE + "Faction Visibility")) {
            Faction faction = factions.get(factionName);
            Rank playerRank = faction.members.get(player.getUniqueId());

            if (playerRank == Rank.OWNER || playerRank == Rank.ADMIN) {
                faction.isPublic = !faction.isPublic;
                player.sendMessage(ChatColor.GREEN + "Faction visibility changed to: " +
                        (faction.isPublic ? ChatColor.GREEN + "Public" : ChatColor.RED + "Private"));
                // Reopen menu to show updated status
                openFactionMenu(player, factionName);
            }
        } else if (displayName.equals(ChatColor.RED + "Leave Faction") || displayName.equals(ChatColor.RED + "Disband Faction")) {
            player.closeInventory();
            Faction faction = factions.get(factionName);
            Rank playerRank = faction.members.get(player.getUniqueId());

            if (playerRank == Rank.OWNER) {
                openDisbandConfirmation(player, factionName);
            } else {
                openLeaveConfirmation(player, factionName);
            }
        } else if (displayName.equals(ChatColor.GRAY + "← Back")) {
            // Close GUI for faction menu
            player.closeInventory();
        }
    }

    public void handleMembersMenuClick(Player player, ItemStack item, ClickType clickType) {
        if (item.getItemMeta() == null) return;
        String displayName = item.getItemMeta().getDisplayName();

        // Handle back button click
        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            // Go back to faction menu
            String factionName = playerFactions.get(player.getUniqueId());
            if (factionName != null) {
                openFactionMenu(player, factionName);
            }
            return;
        }

        // Handle member clicks
        if (item.getType() != Material.PLAYER_HEAD) return;

        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        OfflinePlayer targetPlayer = skullMeta.getOwningPlayer();
        if (targetPlayer == null) return;

        String factionName = playerFactions.get(player.getUniqueId());
        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(player.getUniqueId());

        if (playerRank != Rank.OWNER && playerRank != Rank.ADMIN) {
            return; // No permission to manage
        }

        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            return; // Can't manage self
        }

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

        if (clickType == ClickType.LEFT) {
            // Manage rank - implement later
            player.sendMessage(ChatColor.YELLOW + "Rank management coming soon! Use /f promote or /f demote for now.");
        } else if (clickType == ClickType.RIGHT) {
            // Open kick confirmation dialog
            openKickConfirmation(player, targetPlayer);
        }
    }

    public void handleKickConfirmationClick(Player player, String displayName, String title) {
        UUID kickerUUID = player.getUniqueId();
        UUID targetUUID = pendingKicks.get(kickerUUID);

        if (targetUUID == null) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "Error: No pending kick operation found.");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        String targetName = target.getName();
        if (targetName == null) targetName = "Unknown Player";

        String factionName = playerFactions.get(kickerUUID);
        Faction faction = factions.get(factionName);

        if (displayName.equals(ChatColor.RED + "" + ChatColor.BOLD + "KICK PLAYER")) {
            // Confirm kick - remove player from faction
            faction.members.remove(targetUUID);
            playerFactions.remove(targetUUID);

            // Clear pending kick
            pendingKicks.remove(kickerUUID);

            // Notify both players
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "Successfully kicked " + targetName + " from " + factionName + "!");

            if (target.isOnline()) {
                Player onlineTarget = (Player) target;
                onlineTarget.sendMessage(ChatColor.RED + "You have been kicked from faction " + factionName + " by " + player.getName() + "!");
            }

            // Save data
            plugin.getDataManager().saveFactionData();

            // Return to members menu after a short delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    openMembersMenu(player, factionName);
                }
            }.runTaskLater(plugin, 20L);

        } else if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "CANCEL")) {
            // Cancel kick
            pendingKicks.remove(kickerUUID);

            // Return to members menu
            openMembersMenu(player, factionName);
        }
    }

    public void handleConfirmationMenuClick(Player player, String displayName, String title) {
        UUID uuid = player.getUniqueId();
        String factionName = plugin.getPendingFactionNames().get(uuid);

        if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "CONFIRM")) {
            // Create the faction
            if (factionName != null && !factions.containsKey(factionName)) {
                Faction f = new Faction(factionName, uuid);
                f.members.put(uuid, Rank.OWNER);
                factions.put(factionName, f);
                playerFactions.put(uuid, factionName);

                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Successfully created faction: " + ChatColor.BOLD + factionName);
                player.sendMessage(ChatColor.YELLOW + "You are now the owner of " + factionName + "!");

                // Open the faction menu
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        openFactionsMenu(player);
                    }
                }.runTaskLater(plugin, 20L); // Wait 1 second

            } else if (factions.containsKey(factionName)) {
                player.sendMessage(ChatColor.RED + "A faction with that name already exists!");
                openFactionsMenu(player);
            }

            // Clean up
            plugin.getPendingFactionCreation().remove(uuid);
            plugin.getPendingFactionNames().remove(uuid);

        } else if (displayName.equals(ChatColor.RED + "" + ChatColor.BOLD + "CANCEL")) {
            // Cancel creation
            plugin.getPendingFactionCreation().remove(uuid);
            plugin.getPendingFactionNames().remove(uuid);
            player.closeInventory();
            openFactionsMenu(player);
        }
    }

    public void openLeaveConfirmation(Player player, String factionName) {
        Inventory confirmMenu = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Leave: " + factionName);

        // Fill with black glass
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 27; i++) {
            confirmMenu.setItem(i, blackGlass);
        }

        // Confirm button
        ItemStack confirm = new ItemStack(Material.RED_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "LEAVE FACTION");
        confirmMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Leave: " + ChatColor.WHITE + factionName,
                "",
                ChatColor.RED + "This action cannot be undone!",
                ChatColor.GRAY + "You will lose access to faction territory."
        ));
        confirm.setItemMeta(confirmMeta);
        confirmMenu.setItem(11, confirm);

        // Cancel button
        ItemStack cancel = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "CANCEL");
        cancelMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Stay in the faction",
                "",
                ChatColor.GREEN + "Go back to faction menu"
        ));
        cancel.setItemMeta(cancelMeta);
        confirmMenu.setItem(15, cancel);

        // Info item
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.YELLOW + "Leave Faction?");
        infoMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Are you sure you want to",
                ChatColor.GRAY + "leave " + factionName + "?",
                "",
                ChatColor.RED + "You can rejoin if invited again!"
        ));
        info.setItemMeta(infoMeta);
        confirmMenu.setItem(13, info);

        player.openInventory(confirmMenu);
    }

    public void openDisbandConfirmation(Player player, String factionName) {
        Inventory confirmMenu = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Disband: " + factionName);

        // Fill with black glass
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 27; i++) {
            confirmMenu.setItem(i, blackGlass);
        }

        // Confirm button
        ItemStack confirm = new ItemStack(Material.RED_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "DISBAND FACTION");
        confirmMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Disband: " + ChatColor.WHITE + factionName,
                "",
                ChatColor.RED + "THIS CANNOT BE UNDONE!",
                ChatColor.RED + "All claims will be lost!",
                ChatColor.RED + "All members will be kicked!"
        ));
        confirm.setItemMeta(confirmMeta);
        confirmMenu.setItem(11, confirm);

        // Cancel button
        ItemStack cancel = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "CANCEL");
        cancelMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Keep the faction",
                "",
                ChatColor.GREEN + "Go back to faction menu"
        ));
        cancel.setItemMeta(cancelMeta);
        confirmMenu.setItem(15, cancel);

        // Warning item
        ItemStack warning = new ItemStack(Material.BARRIER);
        ItemMeta warningMeta = warning.getItemMeta();
        warningMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "DANGER!");
        warningMeta.setLore(Arrays.asList(
                ChatColor.RED + "Disbanding " + factionName + " will:",
                ChatColor.RED + "• Remove ALL faction claims",
                ChatColor.RED + "• Kick ALL faction members",
                ChatColor.RED + "• Delete the faction permanently",
                "",
                ChatColor.DARK_RED + "This action is IRREVERSIBLE!"
        ));
        warning.setItemMeta(warningMeta);
        confirmMenu.setItem(13, warning);

        player.openInventory(confirmMenu);
    }

    public void handleLeaveConfirmationClick(Player player, String displayName, String title) {
        String factionName = playerFactions.get(player.getUniqueId());

        if (displayName.equals(ChatColor.RED + "" + ChatColor.BOLD + "LEAVE FACTION")) {
            // Leave the faction
            Faction faction = factions.get(factionName);
            faction.members.remove(player.getUniqueId());
            playerFactions.remove(player.getUniqueId());

            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "You have left faction " + factionName + ".");

            // Notify other online faction members
            for (UUID memberUUID : faction.members.keySet()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null) {
                    member.sendMessage(ChatColor.YELLOW + player.getName() + " has left the faction.");
                }
            }

            // Save data
            plugin.getDataManager().saveFactionData();

        } else if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "CANCEL")) {
            // Return to faction menu
            openFactionMenu(player, factionName);
        }
    }

    public void handleDisbandConfirmationClick(Player player, String displayName, String title) {
        String factionName = playerFactions.get(player.getUniqueId());

        if (displayName.equals(ChatColor.RED + "" + ChatColor.BOLD + "DISBAND FACTION")) {
            // Disband the faction
            disbandFaction(factionName, player);
            player.closeInventory();

        } else if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "CANCEL")) {
            // Return to faction menu
            openFactionMenu(player, factionName);
        }
    }

    private void disbandFaction(String factionName, Player disbander) {
        Faction faction = factions.get(factionName);
        if (faction == null) return;

        // Notify all members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null) {
                member.sendMessage(ChatColor.RED + "Faction " + factionName + " has been disbanded by " + disbander.getName() + "!");
            }
            playerFactions.remove(memberUUID);
        }

        // Remove all claims
        for (Map<ChunkCoord, String> worldClaim : plugin.getWorldClaims().values()) {
            worldClaim.entrySet().removeIf(entry -> entry.getValue().equals(factionName));
        }

        // Remove faction from invitations
        for (Set<String> invites : playerInvitations.values()) {
            invites.remove(factionName);
        }
        playerInvitations.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        // Remove faction
        factions.remove(factionName);

        disbander.sendMessage(ChatColor.GREEN + "Faction " + factionName + " has been disbanded.");

        // Save data
        plugin.getDataManager().saveFactionData();
    }

    /**
     * Get pending kick target for a player
     */
    public UUID getPendingKickTarget(UUID kickerUUID) {
        return pendingKicks.get(kickerUUID);
    }

    /**
     * Clear pending kick for a player
     */
    public void clearPendingKick(UUID kickerUUID) {
        pendingKicks.remove(kickerUUID);
    }
}