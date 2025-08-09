package me.elite.Factions.gui;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.constants.FactionsConstants;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Handles faction browsing menus (public factions, invitations, etc.)
 */
public class BrowserMenuHandler {
    private final FactionsPlugin plugin;
    private final Map<String, Faction> factions;
    private final Map<UUID, String> playerFactions;
    private final Map<UUID, Set<String>> playerInvitations;

    public BrowserMenuHandler(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
        this.playerInvitations = plugin.getPlayerInvitations();
    }

    /**
     * Open the faction browser menu
     */
    public void openFactionBrowser(Player player) {
        Inventory menu = Bukkit.createInventory(null, FactionsConstants.MEDIUM_GUI_SIZE, ChatColor.DARK_GRAY + "Browse Factions");

        // Fill with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < FactionsConstants.MEDIUM_GUI_SIZE; i++) {
            menu.setItem(i, blackGlass);
        }

        // Public Factions option
        ItemStack publicFactions = plugin.getMenuHandler().createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmM2MjExMGQ4MTg4NDQxZDIxNzk0NDM0ZjY3ZDEyYTAyMWI3NDAyYzhkYWE0MmQ0ZmVhMzIzZTdlMTllMGJiNyJ9fX0=");
        ItemMeta publicMeta = publicFactions.getItemMeta();
        publicMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "PUBLIC FACTIONS");

        // Count public factions
        int publicCount = 0;
        for (Faction faction : factions.values()) {
            if (faction.isPublic) publicCount++;
        }

        publicMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Browse factions open to everyone",
                ChatColor.GRAY + "Available: " + ChatColor.WHITE + publicCount + " factions",
                "",
                ChatColor.GREEN + "Click to browse public factions!"
        ));
        publicFactions.setItemMeta(publicMeta);
        menu.setItem(11, publicFactions);

        // Invitations option
        Set<String> playerInvites = playerInvitations.get(player.getUniqueId());
        int inviteCount = playerInvites != null ? playerInvites.size() : 0;

        ItemStack invitations;
        ItemMeta inviteMeta;

        if (inviteCount > 0) {
            // Player has invitations - use mailbox head
            invitations = plugin.getMenuHandler().createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzQ1ZmQxNzRmMjUwMzdiN2Y5ZWNhNzMzY2ZkMDQ2YThiNjM1MTEyMDI2NDg1MzcwNWJjYWE1YjYzZTE3YzE3In19fQ==");
            inviteMeta = invitations.getItemMeta();
            inviteMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "YOUR INVITATIONS");
            inviteMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "View factions that invited you",
                    ChatColor.GRAY + "Pending: " + ChatColor.WHITE + inviteCount + " invitations",
                    "",
                    ChatColor.YELLOW + "Click to view your invitations!"
            ));
        } else {
            // No invitations - use barrier
            invitations = new ItemStack(Material.BARRIER);
            inviteMeta = invitations.getItemMeta();
            inviteMeta.setDisplayName(ChatColor.GRAY + "No Invitations");
            inviteMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "You have no pending invitations",
                    ChatColor.GRAY + "Join a public faction or ask for",
                    ChatColor.GRAY + "an invitation from faction leaders!"
            ));
        }

        invitations.setItemMeta(inviteMeta);
        menu.setItem(15, invitations);

        // Back button
        ItemStack backButton = plugin.getMenuHandler().createBackButton();
        menu.setItem(18, backButton); // Bottom left

        player.openInventory(menu);
    }    /**
     * Open the public factions browser
     */
    public void openPublicFactionsBrowser(Player player, int page) {
        Inventory menu = Bukkit.createInventory(null, FactionsConstants.LARGE_GUI_SIZE, ChatColor.DARK_GRAY + "Public Factions");

        // Fill top and bottom rows with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        // Fill top row (0-8) and bottom row (45-53)
        for (int i = 0; i < FactionsConstants.SMALL_GUI_SIZE; i++) {
            menu.setItem(i, blackGlass);
            menu.setItem(45 + i, blackGlass);
        }

        // Header icon - top left
        ItemStack headerIcon = plugin.getMenuHandler().createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmM2MjExMGQ4MTg4NDQxZDIxNzk0NDM0ZjY3ZDEyYTAyMWI3NDAyYzhkYWE0MmQ0ZmVhMzIzZTdlMTllMGJiNyJ9fX0=");
        ItemMeta headerMeta = headerIcon.getItemMeta();
        headerMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "PUBLIC FACTIONS");
        headerMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Factions open to everyone",
                ChatColor.GRAY + "Click any faction to join!"
        ));
        headerIcon.setItemMeta(headerMeta);
        menu.setItem(0, headerIcon);

        // Get public factions
        List<String> publicFactions = new ArrayList<>();
        for (Map.Entry<String, Faction> entry : factions.entrySet()) {
            if (entry.getValue().isPublic) {
                publicFactions.add(entry.getKey());
            }
        }

        // Calculate pagination
        int factionsPerPage = FactionsConstants.FACTIONS_PER_PAGE; // 4 rows * 9 slots (rows 1-4, slots 9-44)
        int startIndex = page * factionsPerPage;
        int endIndex = Math.min(startIndex + factionsPerPage, publicFactions.size());
        int totalPages = (publicFactions.size() - 1) / factionsPerPage + 1;

        // Display factions
        int slot = 9; // Start from slot 9 (second row)
        for (int i = startIndex; i < endIndex; i++) {
            String factionName = publicFactions.get(i);
            Faction faction = factions.get(factionName);
            if (faction != null) {
                ItemStack factionHead = createFactionHead(factionName, faction, true);
                menu.setItem(slot, factionHead);
                slot++;
                if (slot == 45) break; // Don't go into bottom row
            }
        }

        // Back button - bottom left
        ItemStack backButton = plugin.getMenuHandler().createBackButton();
        menu.setItem(45, backButton);

        // Navigation arrows
        if (page > 0) {
            // Previous page arrow - second to last slot
            ItemStack leftArrow = plugin.getMenuHandler().createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGUxZGZjMTFhODM3MTExZDIyYjAwMWExNDQ2MWY5YTdmYzA5MzUyMmY4OGM1OGZhZWZkNmFkZWZmY2Q0ZTlhYiJ9fX0=");
            ItemMeta leftMeta = leftArrow.getItemMeta();
            leftMeta.setDisplayName(ChatColor.GRAY + "← Previous Page");
            leftMeta.setLore(Arrays.asList(
                    ChatColor.DARK_GRAY + "Page " + page + " of " + totalPages,
                    ChatColor.GRAY + "Click to go to previous page"
            ));
            leftArrow.setItemMeta(leftMeta);
            menu.setItem(52, leftArrow);
        }

        if (page < totalPages - 1) {
            // Next page arrow - last slot
            ItemStack rightArrow = plugin.getMenuHandler().createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2M2OWQ0MTA3NmE4ZGVhNGYwNmQzZjFhOWFjNDdjYzk5Njk4OGI3NGEwOTEzYWIyYWMxYTc0Y2FmNzA4MTkxOCJ9fX0=");
            ItemMeta rightMeta = rightArrow.getItemMeta();
            rightMeta.setDisplayName(ChatColor.GRAY + "Next Page →");
            rightMeta.setLore(Arrays.asList(
                    ChatColor.DARK_GRAY + "Page " + (page + 2) + " of " + totalPages,
                    ChatColor.GRAY + "Click to go to next page"
            ));
            rightArrow.setItemMeta(rightMeta);
            menu.setItem(53, rightArrow);
        }

        // Page info in top right
        if (totalPages > 1) {
            ItemStack pageInfo = new ItemStack(Material.PAPER);
            ItemMeta pageMeta = pageInfo.getItemMeta();
            pageMeta.setDisplayName(ChatColor.YELLOW + "Page " + (page + 1) + " of " + totalPages);
            pageMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Showing " + (endIndex - startIndex) + " of " + publicFactions.size() + " factions"
            ));
            pageInfo.setItemMeta(pageMeta);
            menu.setItem(8, pageInfo);
        }

        player.openInventory(menu);
    }

    /**
     * Open the invitations browser
     */
    public void openInvitationsBrowser(Player player, int page) {
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Your Invitations");

        // Fill top and bottom rows with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        // Fill top row (0-8) and bottom row (45-53)
        for (int i = 0; i < 9; i++) {
            menu.setItem(i, blackGlass);
            menu.setItem(45 + i, blackGlass);
        }

        // Header icon - top left (mailbox) - FIXED: Use different color to prevent click conflicts
        ItemStack headerIcon = plugin.getMenuHandler().createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzQ1ZmQxNzRmMjUwMzdiN2Y5ZWNhNzMzY2ZkMDQ2YThiNjM1MTEyMDI2NDg1MzcwNWJjYWE1YjYzZTE3YzE3In19fQ==");
        ItemMeta headerMeta = headerIcon.getItemMeta();
        headerMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "YOUR INVITATIONS"); // Changed from YELLOW to AQUA
        headerMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Factions that invited you",
                ChatColor.GRAY + "Click any faction to join!"
        ));
        headerIcon.setItemMeta(headerMeta);
        menu.setItem(0, headerIcon);

        // Get player invitations
        Set<String> playerInvites = playerInvitations.get(player.getUniqueId());
        List<String> invitedFactions = playerInvites != null ? new ArrayList<>(playerInvites) : new ArrayList<>();

        // Calculate pagination
        int factionsPerPage = FactionsConstants.FACTIONS_PER_PAGE; // 4 rows * 9 slots (rows 1-4, slots 9-44)
        int startIndex = page * factionsPerPage;
        int endIndex = Math.min(startIndex + factionsPerPage, invitedFactions.size());
        int totalPages = Math.max(1, (invitedFactions.size() - 1) / factionsPerPage + 1);

        // Display factions
        int slot = 9; // Start from slot 9 (second row)
        for (int i = startIndex; i < endIndex; i++) {
            String factionName = invitedFactions.get(i);
            Faction faction = factions.get(factionName);
            if (faction != null) {
                ItemStack factionHead = createFactionHead(factionName, faction, false);
                menu.setItem(slot, factionHead);
                slot++;
                if (slot == 45) break; // Don't go into bottom row
            }
        }

        // If no invitations, show message
        if (invitedFactions.isEmpty()) {
            ItemStack noInvites = new ItemStack(Material.BARRIER);
            ItemMeta noInvitesMeta = noInvites.getItemMeta();
            noInvitesMeta.setDisplayName(ChatColor.RED + "No Invitations");
            noInvitesMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "You have no pending invitations",
                    "",
                    ChatColor.GRAY + "Ask faction leaders for invitations",
                    ChatColor.GRAY + "or join public factions instead!"
            ));
            noInvites.setItemMeta(noInvitesMeta);
            menu.setItem(22, noInvites); // Center of the GUI
        }

        // Back button - bottom left
        ItemStack backButton = plugin.getMenuHandler().createBackButton();
        menu.setItem(45, backButton);

        // Navigation arrows (only if there are invitations)
        if (!invitedFactions.isEmpty()) {
            if (page > 0) {
                // Previous page arrow - second to last slot
                ItemStack leftArrow = plugin.getMenuHandler().createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGUxZGZjMTFhODM3MTExZDIyYjAwMWExNDQ2MWY5YTdmYzA5MzUyMmY4OGM1OGZhZWZkNmFkZWZmY2Q0ZTlhYiJ9fX0=");
                ItemMeta leftMeta = leftArrow.getItemMeta();
                leftMeta.setDisplayName(ChatColor.GRAY + "← Previous Page");
                leftMeta.setLore(Arrays.asList(
                        ChatColor.DARK_GRAY + "Page " + page + " of " + totalPages,
                        ChatColor.GRAY + "Click to go to previous page"
                ));
                leftArrow.setItemMeta(leftMeta);
                menu.setItem(52, leftArrow);
            }

            if (page < totalPages - 1) {
                // Next page arrow - last slot
                ItemStack rightArrow = plugin.getMenuHandler().createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2M2OWQ0MTA3NmE4ZGVhNGYwNmQzZjFhOWFjNDdjYzk5Njk4OGI3NGEwOTEzYWIyYWMxYTc0Y2FmNzA4MTkxOCJ9fX0=");
                ItemMeta rightMeta = rightArrow.getItemMeta();
                rightMeta.setDisplayName(ChatColor.GRAY + "Next Page →");
                rightMeta.setLore(Arrays.asList(
                        ChatColor.DARK_GRAY + "Page " + (page + 2) + " of " + totalPages,
                        ChatColor.GRAY + "Click to go to next page"
                ));
                rightArrow.setItemMeta(rightMeta);
                menu.setItem(53, rightArrow);
            }

            // Page info in top right
            if (totalPages > 1) {
                ItemStack pageInfo = new ItemStack(Material.PAPER);
                ItemMeta pageMeta = pageInfo.getItemMeta();
                pageMeta.setDisplayName(ChatColor.YELLOW + "Page " + (page + 1) + " of " + totalPages);
                pageMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Showing " + (endIndex - startIndex) + " of " + invitedFactions.size() + " invitations"
                ));
                pageInfo.setItemMeta(pageMeta);
                menu.setItem(8, pageInfo);
            }
        }

        player.openInventory(menu);
    }

    /**
     * Handle faction browser selection clicks
     */
    public void handleFactionBrowserClick(Player player, String displayName) {
        if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "PUBLIC FACTIONS")) {
            openPublicFactionsBrowser(player, 0);
        } else if (displayName.equals(ChatColor.YELLOW + "" + ChatColor.BOLD + "YOUR INVITATIONS")) {
            // Only allow if player has invitations
            Set<String> playerInvites = playerInvitations.get(player.getUniqueId());
            if (playerInvites != null && !playerInvites.isEmpty()) {
                openInvitationsBrowser(player, 0);
            }
            // If no invitations, do nothing (item should be unclickable)
        } else if (displayName.equals(ChatColor.GRAY + "← Back")) {
            plugin.getMenuHandler().openFactionsMenu(player);
        }
    }

    /**
     * Handle public factions browser clicks
     */
    public void handlePublicFactionsBrowserClick(Player player, String displayName, int currentSlot) {
        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            openFactionBrowser(player);
        } else if (displayName.equals(ChatColor.GRAY + "← Previous Page")) {
            // Extract page number from current title and go to previous page
            String title = player.getOpenInventory().getTitle();
            int currentPage = getCurrentPageFromPublicBrowser(player);
            openPublicFactionsBrowser(player, Math.max(0, currentPage - 1));
        } else if (displayName.equals(ChatColor.GRAY + "Next Page →")) {
            // Extract page number and go to next page
            int currentPage = getCurrentPageFromPublicBrowser(player);
            openPublicFactionsBrowser(player, currentPage + 1);
        } else if (displayName.startsWith(ChatColor.YELLOW.toString())) {
            // Faction head clicked - join faction
            String factionName = ChatColor.stripColor(displayName);
            joinFactionFromBrowser(player, factionName);
        }
    }

    /**
     * Handle invitations browser clicks
     */
    public void handleInvitationsBrowserClick(Player player, String displayName, int currentSlot) {
        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            openFactionBrowser(player);
        } else if (displayName.equals(ChatColor.GRAY + "← Previous Page")) {
            int currentPage = getCurrentPageFromInvitationsBrowser(player);
            openInvitationsBrowser(player, Math.max(0, currentPage - 1));
        } else if (displayName.equals(ChatColor.GRAY + "Next Page →")) {
            int currentPage = getCurrentPageFromInvitationsBrowser(player);
            openInvitationsBrowser(player, currentPage + 1);
        } else if (displayName.equals(ChatColor.AQUA + "" + ChatColor.BOLD + "YOUR INVITATIONS")) {
            // Header icon clicked - do nothing (this fixes the bug)
            return;
        } else if (displayName.startsWith(ChatColor.YELLOW.toString())) {
            // Faction head clicked - join faction
            String factionName = ChatColor.stripColor(displayName);
            joinFactionFromBrowser(player, factionName);
        }
    }

    /**
     * Join a faction from the browser interface
     */
    private void joinFactionFromBrowser(Player player, String factionName) {
        UUID uuid = player.getUniqueId();

        if (playerFactions.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "You are already in a faction.");
            return;
        }

        Faction faction = factions.get(factionName);
        if (faction == null) {
            player.sendMessage(ChatColor.RED + "Faction no longer exists.");
            player.closeInventory();
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
        player.sendMessage(ChatColor.GREEN + "Successfully joined faction " + ChatColor.BOLD + factionName + ChatColor.GREEN + "!");
        player.sendMessage(ChatColor.YELLOW + "Welcome to " + factionName + "! Use " + ChatColor.YELLOW + "/f menu" + ChatColor.YELLOW + " to access faction features.");

        // UPDATE NAMETAGS when player joins faction
        plugin.getEventListener().onPlayerJoinFaction(player);

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

    /**
     * Get current page number from public factions browser
     */
    private int getCurrentPageFromPublicBrowser(Player player) {
        // Look for the page info item in slot 8
        ItemStack pageItem = player.getOpenInventory().getItem(8);
        if (pageItem != null && pageItem.hasItemMeta() && pageItem.getItemMeta().hasDisplayName()) {
            String displayName = pageItem.getItemMeta().getDisplayName();
            // Parse "Page X of Y" format
            String stripped = ChatColor.stripColor(displayName);
            if (stripped.startsWith("Page ")) {
                try {
                    String[] parts = stripped.split(" ");
                    if (parts.length >= 2) {
                        return Integer.parseInt(parts[1]) - 1; // Convert to 0-based
                    }
                } catch (NumberFormatException e) {
                    // Fallback to page 0
                }
            }
        }
        return 0; // Default to first page
    }

    /**
     * Get current page number from invitations browser
     */
    private int getCurrentPageFromInvitationsBrowser(Player player) {
        // Look for the page info item in slot 8
        ItemStack pageItem = player.getOpenInventory().getItem(8);
        if (pageItem != null && pageItem.hasItemMeta() && pageItem.getItemMeta().hasDisplayName()) {
            String displayName = pageItem.getItemMeta().getDisplayName();
            // Parse "Page X of Y" format
            String stripped = ChatColor.stripColor(displayName);
            if (stripped.startsWith("Page ")) {
                try {
                    String[] parts = stripped.split(" ");
                    if (parts.length >= 2) {
                        return Integer.parseInt(parts[1]) - 1; // Convert to 0-based
                    }
                } catch (NumberFormatException e) {
                    // Fallback to page 0
                }
            }
        }
        return 0; // Default to first page
    }
    
    /**
     * Create a faction head item
     */
    private ItemStack createFactionHead(String factionName, Faction faction, boolean isPublic) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        // Set the head to the faction owner's head
        OfflinePlayer owner = Bukkit.getOfflinePlayer(faction.owner);
        skullMeta.setOwningPlayer(owner);

        // Set display name and lore
        skullMeta.setDisplayName(ChatColor.YELLOW + factionName);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Owner: " + ChatColor.WHITE + (owner.getName() != null ? owner.getName() : "Unknown"));
        lore.add(ChatColor.GRAY + "Members: " + ChatColor.WHITE + faction.members.size());
        if (!faction.description.isEmpty()) {
            lore.add(ChatColor.GRAY + "Description: " + ChatColor.WHITE + faction.description);
        }
        lore.add("");
        if (isPublic) {
            lore.add(ChatColor.GREEN + "✓ Public Faction");
            lore.add(ChatColor.YELLOW + "Click to join!");
        } else {
            lore.add(ChatColor.BLUE + "✉ You're invited!");
            lore.add(ChatColor.YELLOW + "Click to join!");
        }

        skullMeta.setLore(lore);
        head.setItemMeta(skullMeta);

        return head;
    }

}