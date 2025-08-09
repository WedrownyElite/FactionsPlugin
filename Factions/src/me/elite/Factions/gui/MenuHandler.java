package me.elite.Factions.gui;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.Rank;
import me.elite.Factions.territory.ChunkCoord;
import me.elite.Factions.data.FactionPermission;
import me.elite.Factions.data.RelationPermission;
import me.elite.Factions.data.Relation;
import me.elite.Factions.data.RelationRequest;
import me.elite.Factions.Relations.RelationManager;
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
import java.util.EnumSet;

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
        ItemStack createFaction = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta createMeta = createFaction.getItemMeta();
        createMeta.setDisplayName(ChatColor.GREEN + "Create Faction");
        createMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click to create a new faction",
                ChatColor.GRAY + "You will be prompted for a name"
        ));
        createFaction.setItemMeta(createMeta);
        menu.setItem(3, createFaction);

        // Join faction option (placeholder for now)
        ItemStack joinFaction = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmM2MjExMGQ4MTg4NDQxZDIxNzk0NDM0ZjY3ZDEyYTAyMWI3NDAyYzhkYWE0MmQ0ZmVhMzIzZTdlMTllMGJiNyJ9fX0=");
        ItemMeta joinMeta = joinFaction.getItemMeta();
        joinMeta.setDisplayName(ChatColor.BLUE + "Browse Factions");
        joinMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "View factions accepting members"
        ));
        joinFaction.setItemMeta(joinMeta);
        menu.setItem(5, joinFaction);

        // Back button (closes GUI for no-faction menu)
        ItemStack backButton = createBackButton();
        menu.setItem(0, backButton);

        player.openInventory(menu);
    }

    /**
     * Open the faction browser menu
     */
    public void openFactionBrowser(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Browse Factions");

        // Fill with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 27; i++) {
            menu.setItem(i, blackGlass);
        }

        // Public Factions option
        ItemStack publicFactions = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmM2MjExMGQ4MTg4NDQxZDIxNzk0NDM0ZjY3ZDEyYTAyMWI3NDAyYzhkYWE0MmQ0ZmVhMzIzZTdlMTllMGJiNyJ9fX0=");
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
            invitations = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzQ1ZmQxNzRmMjUwMzdiN2Y5ZWNhNzMzY2ZkMDQ2YThiNjM1MTEyMDI2NDg1MzcwNWJjYWE1YjYzZTE3YzE3In19fQ==");
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
        ItemStack backButton = createBackButton();
        menu.setItem(18, backButton); // Bottom left

        player.openInventory(menu);
    }

    /**
     * Open the public factions browser
     */
    public void openPublicFactionsBrowser(Player player, int page) {
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Public Factions");

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

        // Header icon - top left
        ItemStack headerIcon = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmM2MjExMGQ4MTg4NDQxZDIxNzk0NDM0ZjY3ZDEyYTAyMWI3NDAyYzhkYWE0MmQ0ZmVhMzIzZTdlMTllMGJiNyJ9fX0=");
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
        int factionsPerPage = 36; // 4 rows * 9 slots (rows 1-4, slots 9-44)
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
        ItemStack backButton = createBackButton();
        menu.setItem(45, backButton);

        // Navigation arrows
        if (page > 0) {
            // Previous page arrow - second to last slot
            ItemStack leftArrow = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGUxZGZjMTFhODM3MTExZDIyYjAwMWExNDQ2MWY5YTdmYzA5MzUyMmY4OGM1OGZhZWZkNmFkZWZmY2Q0ZTlhYiJ9fX0=");
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
            ItemStack rightArrow = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2M2OWQ0MTA3NmE4ZGVhNGYwNmQzZjFhOWFjNDdjYzk5Njk4OGI3NGEwOTEzYWIyYWMxYTc0Y2FmNzA4MTkxOCJ9fX0=");
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
        ItemStack headerIcon = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzQ1ZmQxNzRmMjUwMzdiN2Y5ZWNhNzMzY2ZkMDQ2YThiNjM1MTEyMDI2NDg1MzcwNWJjYWE1YjYzZTE3YzE3In19fQ==");
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
        int factionsPerPage = 36; // 4 rows * 9 slots (rows 1-4, slots 9-44)
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
        ItemStack backButton = createBackButton();
        menu.setItem(45, backButton);

        // Navigation arrows (only if there are invitations)
        if (!invitedFactions.isEmpty()) {
            if (page > 0) {
                // Previous page arrow - second to last slot
                ItemStack leftArrow = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGUxZGZjMTFhODM3MTExZDIyYjAwMWExNDQ2MWY5YTdmYzA5MzUyMmY4OGM1OGZhZWZkNmFkZWZmY2Q0ZTlhYiJ9fX0=");
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
                ItemStack rightArrow = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2M2OWQ0MTA3NmE4ZGVhNGYwNmQzZjFhOWFjNDdjYzk5Njk4OGI3NGEwOTEzYWIyYWMxYTc0Y2FmNzA4MTkxOCJ9fX0=");
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
     * Create a custom player head with base64 texture
     */
    private ItemStack createCustomHead(String texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", texture));

            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set custom head texture: " + e.getMessage());
        }

        head.setItemMeta(skullMeta);
        return head;
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

    /**
     * Menu for players with a faction - CHANGED TO PUBLIC
     */
    public void openFactionMenu(Player player, String factionName) {
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
        if (playerRank == Rank.OWNER || faction.hasPermission(playerRank, FactionPermission.OPEN_CLOSE)) {
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

        // Check permissions for inviting
        if (playerRank == Rank.OWNER || faction.hasPermission(playerRank, FactionPermission.INVITE_MEMBERS)) {
            // Invite players - Updated to actually work
            ItemStack invite = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta inviteMeta = invite.getItemMeta();
            inviteMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Invite Players");
            inviteMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Invite online players to your faction",
                    "",
                    ChatColor.YELLOW + "Click to open invitation menu!"
            ));
            invite.setItemMeta(inviteMeta);
            menu.setItem(28, invite);
        }

        // Settings for Owners and perm enabled players
        if (playerRank == Rank.OWNER || faction.hasPermission(playerRank, FactionPermission.MANAGE_PERMISSIONS)) {
            // Manage settings - Updated to work
            ItemStack settings = new ItemStack(Material.REDSTONE);
            ItemMeta settingsMeta = settings.getItemMeta();
            settingsMeta.setDisplayName(ChatColor.RED + "Faction Settings");
            settingsMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Manage faction settings",
                    ChatColor.GRAY + "• Permissions",
                    ChatColor.GRAY + "• Relations",
                    "",
                    ChatColor.YELLOW + "Click to open settings!"
            ));
            settings.setItemMeta(settingsMeta);
            menu.setItem(34, settings);
        }

        /**
        // Relation requests for owners and permission enabled players
        if (playerRank == Rank.OWNER || faction.hasPermission(playerRank, FactionPermission.SET_RELATIONS)) {
            List<RelationRequest> requests = plugin.getRelationManager().getPendingRequests(factionName);

            ItemStack relationRequests = new ItemStack(Material.PAPER);
            ItemMeta requestsMeta = relationRequests.getItemMeta();
            requestsMeta.setDisplayName(ChatColor.GOLD + "Relation Requests");

            if (requests.isEmpty()) {
                requestsMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "No pending relation requests"
                ));
            } else {
                requestsMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Pending requests: " + ChatColor.WHITE + requests.size(),
                        ChatColor.GRAY + "• Ally requests",
                        ChatColor.GRAY + "• Truce requests",
                        "",
                        ChatColor.YELLOW + "Click to manage requests!"
                ));
            }

            relationRequests.setItemMeta(requestsMeta);
            menu.setItem(31, relationRequests); // Bottom row, center-left
        }
         */


        ItemStack relations = new ItemStack(Material.COMPASS);
        ItemMeta relationsMeta = relations.getItemMeta();
        if (relationsMeta == null) {
            // Safety: if meta is null (shouldn't be for COMPASS) bail out gracefully
            return;
        }
        relationsMeta.setDisplayName(ChatColor.BLUE + "View Relations");

        // Safe retrievals (avoid NPEs)
        List<RelationRequest> requests = plugin.getRelationManager().getPendingRequests(factionName);
        if (requests == null) requests = Collections.emptyList();

        Map<String, Relation> factionRelations = plugin.getRelationManager().getFactionRelations(factionName);
        int relationCount = factionRelations == null ? 0 : factionRelations.size();

        // Build pending request string safely (avoid ChatColor + int compilation error)
        String pendingRequest;
        int pendingCount = requests.size();
        if (pendingCount == 0) {
            pendingRequest = ChatColor.RED + "No pending requests";
        } else if (pendingCount == 1) {
            pendingRequest = ChatColor.YELLOW + "1 pending request!";
        } else {
            pendingRequest = ChatColor.YELLOW + String.valueOf(pendingCount) + " pending requests!";
        }

        // Color the relation count based on value (makes it more noticeable)
        ChatColor countColor = relationCount == 0 ? ChatColor.RED : ChatColor.GREEN;
        String relationsCountText = countColor + String.valueOf(relationCount);

        // Build lore using Strings (no direct ChatColor + int)
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "View your faction's relations");
        lore.add(ChatColor.GRAY + "Current relations: " + relationsCountText);
        lore.add(ChatColor.GRAY + "Pending requests: " + pendingRequest);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to view relations!");

        relationsMeta.setLore(lore);
        relations.setItemMeta(relationsMeta);
        menu.setItem(31, relations); // Bottom row, center-right

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
     * Create an item representing a relation request
     */
    private ItemStack createRelationRequestItem(RelationRequest request) {
        Faction fromFaction = factions.get(request.fromFaction);
        if (fromFaction == null) {
            // Fallback item
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Invalid Request");
            item.setItemMeta(meta);
            return item;
        }

        // Use faction owner's head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        OfflinePlayer owner = Bukkit.getOfflinePlayer(fromFaction.owner);
        skullMeta.setOwningPlayer(owner);

        // Color based on relation type
        ChatColor relationColor = RelationManager.getRelationColor(request.requestedRelation);
        skullMeta.setDisplayName(relationColor + request.fromFaction);

        // Calculate time since request
        long timeSince = System.currentTimeMillis() - request.timestamp;
        String timeString = formatTimeString(timeSince);

        OfflinePlayer requester = Bukkit.getOfflinePlayer(request.requestedBy);
        String requesterName = requester.getName() != null ? requester.getName() : "Unknown";

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Requested: " + relationColor + request.requestedRelation.getDisplayName());
        lore.add(ChatColor.GRAY + "From: " + ChatColor.WHITE + request.fromFaction);
        lore.add(ChatColor.GRAY + "By: " + ChatColor.WHITE + requesterName);
        lore.add(ChatColor.GRAY + "Sent: " + ChatColor.WHITE + timeString + " ago");

        if (!fromFaction.description.isEmpty()) {
            lore.add(ChatColor.GRAY + "Description: " + ChatColor.WHITE + fromFaction.description);
        }

        lore.add(ChatColor.GRAY + "Members: " + ChatColor.WHITE + fromFaction.members.size());
        lore.add("");
        lore.add(ChatColor.GREEN + "Left Click: Accept Request");
        lore.add(ChatColor.RED + "Right Click: Reject Request");

        skullMeta.setLore(lore);
        head.setItemMeta(skullMeta);

        return head;
    }

    public void openRelationsViewMenu(Player player, String factionName) {
        openRelationsViewMenu(player, factionName, 0, 0); // Default to page 0 for both
    }

    public void openRelationsViewMenu(Player player, String factionName, int relationsPage, int requestsPage) {
        Map<String, Relation> allRelations = plugin.getRelationManager().getFactionRelations(factionName);
        List<RelationRequest> allRequests = plugin.getRelationManager().getPendingRequests(factionName);

        // Convert relations to list for pagination
        List<Map.Entry<String, Relation>> relationsList = new ArrayList<>(allRelations.entrySet());

        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Relations & Requests");

        // Fill divider rows with black glass
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        // Fill top row (0-8), middle row (18-26), and bottom row (45-53)
        for (int i = 0; i < 9; i++) {
            menu.setItem(i, blackGlass);
            menu.setItem(27 + i, blackGlass);
            menu.setItem(45 + i, blackGlass);
        }

        // Back button
        ItemStack backButton = createBackButton();
        menu.setItem(45, backButton);

        // Header sections
        ItemStack relationsHeader = new ItemStack(Material.COMPASS);
        ItemMeta relationsHeaderMeta = relationsHeader.getItemMeta();
        relationsHeaderMeta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "CURRENT RELATIONS");
        relationsHeaderMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Your faction's active relations",
                ChatColor.GRAY + "Right-click any relation to remove it"
        ));
        relationsHeader.setItemMeta(relationsHeaderMeta);
        menu.setItem(0, relationsHeader);

        ItemStack requestsHeader = new ItemStack(Material.PAPER);
        ItemMeta requestsHeaderMeta = requestsHeader.getItemMeta();
        requestsHeaderMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "PENDING REQUESTS");
        requestsHeaderMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Relation requests awaiting your response",
                ChatColor.GRAY + "Left-click to accept, Right-click to reject"
        ));
        requestsHeader.setItemMeta(requestsHeaderMeta);
        menu.setItem(27, requestsHeader);

        // Relations pagination (slots 9-26, 1 row = 9 slots)
        int relationsPerPage = 9;
        int relationsStartIndex = relationsPage * relationsPerPage;
        int relationsEndIndex = Math.min(relationsStartIndex + relationsPerPage, relationsList.size());
        int relationsTotalPages = Math.max(1, (relationsList.size() - 1) / relationsPerPage + 1);

        // Display current relations (slots 9-26)
        if (relationsList.isEmpty()) {
            ItemStack noRelations = new ItemStack(Material.RED_CONCRETE);
            ItemMeta noRelationsMeta = noRelations.getItemMeta();
            noRelationsMeta.setDisplayName(ChatColor.GRAY + "No Relations");
            noRelationsMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Your faction has no special relations",
                    ChatColor.GRAY + "All other factions are " + ChatColor.WHITE + "Neutral"
            ));
            noRelations.setItemMeta(noRelationsMeta);
            menu.setItem(13, noRelations); // Center of relations area
        } else {
            int relationSlot = 9;
            for (int i = relationsStartIndex; i < relationsEndIndex && relationSlot <= 26; i++) {
                Map.Entry<String, Relation> entry = relationsList.get(i);
                ItemStack relationItem = createRelationViewItem(entry.getKey(), entry.getValue());
                menu.setItem(relationSlot, relationItem);
                relationSlot++;
            }

            // Relations navigation arrows
            if (relationsPage > 0) {
                ItemStack leftArrow = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGUxZGZjMTFhODM3MTExZDIyYjAwMWExNDQ2MWY5YTdmYzA5MzUyMmY4OGM1OGZhZWZkNmFkZWZmY2Q0ZTlhYiJ9fX0=");
                ItemMeta leftMeta = leftArrow.getItemMeta();
                leftMeta.setDisplayName(ChatColor.GRAY + "← Previous Relations");
                leftMeta.setLore(Arrays.asList(
                        ChatColor.DARK_GRAY + "Relations Page " + (relationsPage) + " of " + relationsTotalPages,
                        ChatColor.GRAY + "Click to go to previous page"
                ));
                leftArrow.setItemMeta(leftMeta);
                menu.setItem(1, leftArrow); // Top left area
            }

            if (relationsPage < relationsTotalPages - 1) {
                ItemStack rightArrow = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2M2OWQ0MTA3NmE4ZGVhNGYwNmQzZjFhOWFjNDdjYzk5Njk4OGI3NGEwOTEzYWIyYWMxYTc0Y2FmNzA4MTkxOCJ9fX0=");
                ItemMeta rightMeta = rightArrow.getItemMeta();
                rightMeta.setDisplayName(ChatColor.GRAY + "Next Relations →");
                rightMeta.setLore(Arrays.asList(
                        ChatColor.DARK_GRAY + "Relations Page " + (relationsPage + 2) + " of " + relationsTotalPages,
                        ChatColor.GRAY + "Click to go to next page"
                ));
                rightArrow.setItemMeta(rightMeta);
                menu.setItem(7, rightArrow); // Top right area
            }

            // Page info for relations
            if (relationsTotalPages > 1) {
                ItemStack pageInfo = new ItemStack(Material.PAPER);
                ItemMeta pageMeta = pageInfo.getItemMeta();
                pageMeta.setDisplayName(ChatColor.YELLOW + "Relations Page " + (relationsPage + 1) + "/" + relationsTotalPages);
                pageMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Showing " + (relationsEndIndex - relationsStartIndex) + " of " + relationsList.size() + " relations"
                ));
                pageInfo.setItemMeta(pageMeta);
                menu.setItem(4, pageInfo); // Top center
            }
        }

        // Requests pagination (slots 27-44, 2 rows = 18 slots)
        int requestsPerPage = 18;
        int requestsStartIndex = requestsPage * requestsPerPage;
        int requestsEndIndex = Math.min(requestsStartIndex + requestsPerPage, allRequests.size());
        int requestsTotalPages = Math.max(1, (allRequests.size() - 1) / requestsPerPage + 1);

        // Display pending requests (slots 27-44)
        if (allRequests.isEmpty()) {
            ItemStack noRequests = new ItemStack(Material.RED_CONCRETE);
            ItemMeta noRequestsMeta = noRequests.getItemMeta();
            noRequestsMeta.setDisplayName(ChatColor.GRAY + "No Pending Requests");
            noRequestsMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "No factions want to ally with you",
                    ChatColor.GRAY + "How sad! Maybe be nicer to them?"
            ));
            noRequests.setItemMeta(noRequestsMeta);
            menu.setItem(40, noRequests); // Center of requests area
        } else {
            int requestSlot = 36;
            for (int i = requestsStartIndex; i < requestsEndIndex && requestSlot <= 44; i++) {
                RelationRequest request = allRequests.get(i);
                ItemStack requestItem = createRelationRequestItem(request);
                menu.setItem(requestSlot, requestItem);
                requestSlot++;
            }

            // Requests navigation arrows
            if (requestsPage > 0) {
                ItemStack leftArrow = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGUxZGZjMTFhODM3MTExZDIyYjAwMWExNDQ2MWY5YTdmYzA5MzUyMmY4OGM1OGZhZWZkNmFkZWZmY2Q0ZTlhYiJ9fX0=");
                ItemMeta leftMeta = leftArrow.getItemMeta();
                leftMeta.setDisplayName(ChatColor.GRAY + "← Previous Requests");
                leftMeta.setLore(Arrays.asList(
                        ChatColor.DARK_GRAY + "Requests Page " + (requestsPage) + " of " + requestsTotalPages,
                        ChatColor.GRAY + "Click to go to previous page"
                ));
                leftArrow.setItemMeta(leftMeta);
                menu.setItem(19, leftArrow); // Requests header area left
            }

            if (requestsPage < requestsTotalPages - 1) {
                ItemStack rightArrow = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2M2OWQ0MTA3NmE4ZGVhNGYwNmQzZjFhOWFjNDdjYzk5Njk4OGI3NGEwOTEzYWIyYWMxYTc0Y2FmNzA4MTkxOCJ9fX0=");
                ItemMeta rightMeta = rightArrow.getItemMeta();
                rightMeta.setDisplayName(ChatColor.GRAY + "Next Requests →");
                rightMeta.setLore(Arrays.asList(
                        ChatColor.DARK_GRAY + "Requests Page " + (requestsPage + 2) + " of " + requestsTotalPages,
                        ChatColor.GRAY + "Click to go to next page"
                ));
                rightArrow.setItemMeta(rightMeta);
                menu.setItem(25, rightArrow); // Requests header area right
            }

            // Page info for requests
            if (requestsTotalPages > 1) {
                ItemStack pageInfo = new ItemStack(Material.PAPER);
                ItemMeta pageMeta = pageInfo.getItemMeta();
                pageMeta.setDisplayName(ChatColor.YELLOW + "Requests Page " + (requestsPage + 1) + "/" + requestsTotalPages);
                pageMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Showing " + (requestsEndIndex - requestsStartIndex) + " of " + allRequests.size() + " requests"
                ));
                pageInfo.setItemMeta(pageMeta);
                menu.setItem(22, pageInfo); // Requests header center
            }
        }

        player.openInventory(menu);
    }

    /**
     * Create an item representing a faction relation
     */
    private ItemStack createRelationViewItem(String factionName, Relation relation) {
        Faction faction = factions.get(factionName);
        if (faction == null) {
            // Fallback item
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Invalid Faction");
            item.setItemMeta(meta);
            return item;
        }

        // Use faction owner's head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        OfflinePlayer owner = Bukkit.getOfflinePlayer(faction.owner);
        skullMeta.setOwningPlayer(owner);

        // Color based on relation type
        ChatColor relationColor = RelationManager.getRelationColor(relation);
        skullMeta.setDisplayName(relationColor + factionName);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Relation: " + relationColor + relation.getDisplayName());
        lore.add(ChatColor.GRAY + "Owner: " + ChatColor.WHITE + (owner.getName() != null ? owner.getName() : "Unknown"));
        lore.add(ChatColor.GRAY + "Members: " + ChatColor.WHITE + faction.members.size());

        if (!faction.description.isEmpty()) {
            lore.add(ChatColor.GRAY + "Description: " + ChatColor.WHITE + faction.description);
        }

        lore.add("");

        // Add relation-specific information
        switch (relation) {
            case ALLY:
                lore.add(ChatColor.GREEN + "✓ Allied Faction");
                lore.add(ChatColor.GRAY + "• Can build in your territory");
                lore.add(ChatColor.GRAY + "• No friendly fire");
                break;
            case TRUCE:
                lore.add(ChatColor.YELLOW + "~ Truce Faction");
                lore.add(ChatColor.GRAY + "• Limited interactions");
                lore.add(ChatColor.GRAY + "• No PvP");
                break;
            case ENEMY:
                lore.add(ChatColor.RED + "⚔ Enemy Faction");
                lore.add(ChatColor.GRAY + "• Cannot interact in territory");
                lore.add(ChatColor.GRAY + "• PvP allowed");
                break;
            case NEUTRAL:
                lore.add(ChatColor.WHITE + "○ Neutral Faction");
                lore.add(ChatColor.GRAY + "• Default relation");
                break;
        }

        // Add right-click instruction
        lore.add("");
        lore.add(ChatColor.RED + "Right click to remove relation");

        skullMeta.setLore(lore);
        head.setItemMeta(skullMeta);

        return head;
    }
    /**
     * Handle the combined relations view menu clicks - FIXED VERSION
     */
    public void handleRelationsViewClick(Player player, ItemStack item, ClickType clickType, String title) {
        if (item.getItemMeta() == null) return;
        String displayName = item.getItemMeta().getDisplayName();

        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            String factionName = playerFactions.get(player.getUniqueId());
            openFactionMenu(player, factionName);
            return;
        }

        // Handle navigation arrows
        if (displayName.contains("Previous") || displayName.contains("Next")) {
            handleRelationsNavigation(player, displayName);
            return;
        }

        // Handle clicks on relations and requests
        if (item.getType() == Material.PLAYER_HEAD) {
            String factionName = playerFactions.get(player.getUniqueId());
            String targetFaction = ChatColor.stripColor(displayName);

            // Check if this is a current relation or a pending request
            Map<String, Relation> relations = plugin.getRelationManager().getFactionRelations(factionName);
            List<RelationRequest> requests = plugin.getRelationManager().getPendingRequests(factionName);

            // First check if it's a current relation
            if (relations.containsKey(targetFaction)) {
                if (clickType == ClickType.RIGHT) {
                    handleRelationRemoval(player, factionName, targetFaction);
                }
                // Left clicks on relations do nothing
                return;
            }

            // Check if it's a pending request
            RelationRequest targetRequest = null;
            for (RelationRequest request : requests) {
                if (request.fromFaction.equals(targetFaction)) {
                    targetRequest = request;
                    break;
                }
            }

            if (targetRequest != null) {
                if (clickType == ClickType.LEFT) {
                    handleRequestAcceptance(player, factionName, targetRequest);
                } else if (clickType == ClickType.RIGHT) {
                    handleRequestRejection(player, factionName, targetRequest);
                }
            }
        }
    }

    /**
     * Handle navigation arrows in relations GUI
     */
    private void handleRelationsNavigation(Player player, String displayName) {
        String factionName = playerFactions.get(player.getUniqueId());

        // Parse current pages from the current inventory
        int currentRelationsPage = getCurrentRelationsPage(player);
        int currentRequestsPage = getCurrentRequestsPage(player);

        if (displayName.equals(ChatColor.GRAY + "← Previous Relations")) {
            openRelationsViewMenu(player, factionName, Math.max(0, currentRelationsPage - 1), currentRequestsPage);
        } else if (displayName.equals(ChatColor.GRAY + "Next Relations →")) {
            openRelationsViewMenu(player, factionName, currentRelationsPage + 1, currentRequestsPage);
        } else if (displayName.equals(ChatColor.GRAY + "← Previous Requests")) {
            openRelationsViewMenu(player, factionName, currentRelationsPage, Math.max(0, currentRequestsPage - 1));
        } else if (displayName.equals(ChatColor.GRAY + "Next Requests →")) {
            openRelationsViewMenu(player, factionName, currentRelationsPage, currentRequestsPage + 1);
        }
    }

    /**
     * Get current relations page from GUI
     */
    private int getCurrentRelationsPage(Player player) {
        ItemStack pageItem = player.getOpenInventory().getItem(4); // Relations page info slot
        if (pageItem != null && pageItem.hasItemMeta() && pageItem.getItemMeta().hasDisplayName()) {
            String displayName = pageItem.getItemMeta().getDisplayName();
            String stripped = ChatColor.stripColor(displayName);
            if (stripped.startsWith("Relations Page ")) {
                try {
                    String[] parts = stripped.split(" ");
                    if (parts.length >= 3) {
                        String pageStr = parts[2].split("/")[0];
                        return Integer.parseInt(pageStr) - 1; // Convert to 0-based
                    }
                } catch (NumberFormatException e) {
                    // Fallback
                }
            }
        }
        return 0;
    }

    /**
     * Get current requests page from GUI
     */
    private int getCurrentRequestsPage(Player player) {
        ItemStack pageItem = player.getOpenInventory().getItem(22); // Requests page info slot
        if (pageItem != null && pageItem.hasItemMeta() && pageItem.getItemMeta().hasDisplayName()) {
            String displayName = pageItem.getItemMeta().getDisplayName();
            String stripped = ChatColor.stripColor(displayName);
            if (stripped.startsWith("Requests Page ")) {
                try {
                    String[] parts = stripped.split(" ");
                    if (parts.length >= 3) {
                        String pageStr = parts[2].split("/")[0];
                        return Integer.parseInt(pageStr) - 1; // Convert to 0-based
                    }
                } catch (NumberFormatException e) {
                    // Fallback
                }
            }
        }
        return 0;
    }

    private void handleRelationRemoval(Player player, String factionName, String targetFaction) {
        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(player.getUniqueId());

        if (!faction.hasPermission(playerRank, FactionPermission.SET_RELATIONS)) {
            player.sendMessage(ChatColor.RED + "You lack permission to manage faction relations.");
            return;
        }

        boolean success = plugin.getRelationManager().removeRelation(factionName, targetFaction, player.getUniqueId());
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Removed relation with " + targetFaction + "!");
            plugin.getDataManager().saveFactionData();
        } else {
            player.sendMessage(ChatColor.RED + "Failed to remove relation.");
        }

        // Refresh the menu with current pages
        int relationsPage = getCurrentRelationsPage(player);
        int requestsPage = getCurrentRequestsPage(player);
        openRelationsViewMenu(player, factionName, relationsPage, requestsPage);
    }

    private void handleRequestAcceptance(Player player, String factionName, RelationRequest request) {
        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(player.getUniqueId());

        if (!faction.hasPermission(playerRank, FactionPermission.SET_RELATIONS)) {
            player.sendMessage(ChatColor.RED + "You lack permission to manage faction relations.");
            return;
        }

        boolean success = plugin.getRelationManager().acceptRelationRequest(
                factionName, request.fromFaction, request.requestedRelation, player.getUniqueId());

        if (success) {
            player.sendMessage(ChatColor.GREEN + "Accepted " +
                    RelationManager.getRelationColor(request.requestedRelation) +
                    request.requestedRelation.getDisplayName() + ChatColor.GREEN +
                    " request from " + request.fromFaction + "!");
            plugin.getDataManager().saveFactionData();
        } else {
            player.sendMessage(ChatColor.RED + "Failed to accept request.");
        }

        // Refresh the menu with current pages
        int relationsPage = getCurrentRelationsPage(player);
        int requestsPage = getCurrentRequestsPage(player);
        openRelationsViewMenu(player, factionName, relationsPage, requestsPage);
    }

    private void handleRequestRejection(Player player, String factionName, RelationRequest request) {
        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(player.getUniqueId());

        if (!faction.hasPermission(playerRank, FactionPermission.SET_RELATIONS)) {
            player.sendMessage(ChatColor.RED + "You lack permission to manage faction relations.");
            return;
        }

        boolean success = plugin.getRelationManager().rejectRelationRequest(
                factionName, request.fromFaction, request.requestedRelation, player.getUniqueId());

        if (success) {
            player.sendMessage(ChatColor.YELLOW + "Rejected " +
                    RelationManager.getRelationColor(request.requestedRelation) +
                    request.requestedRelation.getDisplayName() + ChatColor.YELLOW +
                    " request from " + request.fromFaction + ".");
            plugin.getDataManager().saveFactionData();
        } else {
            player.sendMessage(ChatColor.RED + "Failed to reject request.");
        }

        // Refresh the menu with current pages
        int relationsPage = getCurrentRelationsPage(player);
        int requestsPage = getCurrentRequestsPage(player);
        openRelationsViewMenu(player, factionName, relationsPage, requestsPage);
    }

    /**
     * Format milliseconds into a readable time string
     */
    private String formatTimeString(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days == 1 ? "" : "s");
        } else if (hours > 0) {
            return hours + " hour" + (hours == 1 ? "" : "s");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes == 1 ? "" : "s");
        } else {
            return seconds + " second" + (seconds == 1 ? "" : "s");
        }
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

    // Menu click handlers
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
            openFactionsMenu(player);
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

    public void handleNoFactionMenuClick(Player player, String displayName) {
        if (displayName.equals(ChatColor.GREEN + "Create Faction")) {
            player.closeInventory();
            plugin.getFactionCreationManager().openSignGUIForFactionCreation(player);
        } else if (displayName.equals(ChatColor.BLUE + "Browse Factions")) {
            openFactionBrowser(player); // This now opens the selection menu
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
            // Open invitation menu
            openInvitationMenu(player, factionName);
        } else if (displayName.equals(ChatColor.RED + "Faction Settings")) {
            // Open settings menu
            openFactionSettings(player, factionName);
        } else if (displayName.equals(ChatColor.LIGHT_PURPLE + "Faction Visibility")) {
            Faction faction = factions.get(factionName);
            Rank playerRank = faction.members.get(player.getUniqueId());

            if (playerRank == Rank.OWNER || playerRank == Rank.ADMIN) {
                faction.isPublic = !faction.isPublic;
                player.sendMessage(ChatColor.GREEN + "Faction visibility changed to: " +
                        (faction.isPublic ? ChatColor.GREEN + "Public" : ChatColor.RED + "Private"));

                // Save the data
                plugin.getDataManager().saveFactionData();

                // Reopen menu to show updated status
                openFactionMenu(player, factionName);
            } else {
                player.sendMessage(ChatColor.RED + "You don't have permission to change faction visibility.");
            }
        } else if (displayName.equals(ChatColor.RED + "Leave Faction")) {
            player.closeInventory();
            Faction faction = factions.get(factionName);
            Rank playerRank = faction.members.get(player.getUniqueId());

            if (playerRank == Rank.OWNER) {
                openDisbandConfirmation(player, factionName);
            } else {
                openLeaveConfirmation(player, factionName);
            }
        } else if (displayName.equals(ChatColor.RED + "Disband Faction")) {
            player.closeInventory();
            openDisbandConfirmation(player, factionName);
        } else if (displayName.equals(ChatColor.GRAY + "← Back")) {
            player.closeInventory();
        } else if (displayName.equals(ChatColor.GOLD + "Relation Requests")) {
            openRelationsViewMenu(player, factionName);
        } else if (displayName.equals(ChatColor.BLUE + "View Relations")) {
            openRelationsViewMenu(player, factionName);
        }
    }

    /**
     * Handle settings menu clicks
     */
    public void handleSettingsMenuClick(Player player, String displayName, String title) {
        String factionName = playerFactions.get(player.getUniqueId());

        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            openFactionMenu(player, factionName);
        } else if (displayName.equals(ChatColor.RED + "Faction Permissions")) {
            openPermissionsGUI(player, factionName);
        }
    }

    /**
     * Handle permissions GUI clicks
     */
    public void handlePermissionsGUIClick(Player player, String displayName, String title) {
        String factionName = playerFactions.get(player.getUniqueId());

        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            openFactionSettings(player, factionName);
            return;
        }

        // Handle rank permission clicks (green panes)
        if (displayName.startsWith(ChatColor.GREEN.toString())) {
            for (Rank rank : Rank.values()) {
                if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + rank.name())) {
                    openRankPermissionsGUI(player, factionName, rank);
                    return;
                }
            }
        }
        // Handle relation permission clicks (purple panes)
        else if (displayName.startsWith(ChatColor.LIGHT_PURPLE.toString())) {
            for (Relation relation : Relation.values()) {
                if (displayName.equals(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + relation.getDisplayName())) {
                    openRelationPermissionsGUI(player, factionName, relation);
                    return;
                }
            }
        }
    }

    /**
     * Handle rank permissions GUI clicks with comprehensive permission checking
     */
    public void handleRankPermissionsClick(Player player, String displayName, String title) {
        String factionName = playerFactions.get(player.getUniqueId());

        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            openPermissionsGUI(player, factionName);
            return;
        }

        // Handle reset button click
        if (displayName.equals(ChatColor.YELLOW + "" + ChatColor.BOLD + "RESET TO DEFAULT")) {
            handleResetPermissionsClick(player, title, true);
            return;
        }

        // Extract rank from title
        String rankName = title.replace(ChatColor.DARK_GRAY + "", "").replace(" Permissions", "");
        Rank targetRank;
        try {
            targetRank = Rank.valueOf(rankName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Error: Invalid rank detected.");
            return;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(player.getUniqueId());

        // Check if player can modify permissions for this rank
        if (!canModifyRankPermissions(player, playerRank, targetRank, faction)) {
            return; // Error message already sent by the method
        }

        // Find the permission that was clicked
        for (FactionPermission permission : FactionPermission.values()) {
            if (displayName.contains(permission.getDisplayName())) {

                // Check if this permission is locked (visual indicator check)
                if (displayName.contains("🔒")) {
                    // This is a locked permission - explain why it can't be changed
                    if (!faction.hasPermission(playerRank, permission)) {
                        player.sendMessage(ChatColor.RED + "You cannot toggle " + permission.getDisplayName() +
                                " because your rank (" + playerRank.name() + ") doesn't have this permission.");
                    } else {
                        player.sendMessage(ChatColor.RED + "You cannot modify permissions for this rank.");
                    }
                    return;
                }

                // Check if player can toggle this specific permission
                if (!canTogglePermission(player, playerRank, targetRank, permission, faction)) {
                    return; // Error message already sent by the method
                }

                // Toggle the permission
                faction.togglePermission(targetRank, permission);

                // Save data
                plugin.getDataManager().saveFactionData();

                // Reopen the menu to show updated permissions
                openRankPermissionsGUI(player, factionName, targetRank);

                boolean hasPermission = faction.hasPermission(targetRank, permission);
                player.sendMessage(ChatColor.GREEN + (hasPermission ? "Enabled" : "Disabled") +
                        " " + permission.getDisplayName() + " for " + targetRank.name() + " rank!");
                return;
            }
        }
    }

    /**
     * Check if a player can modify permissions for a specific rank
     */
    private boolean canModifyRankPermissions(Player player, Rank playerRank, Rank targetRank, Faction faction) {
        // Check if player has permission to manage permissions at all
        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.MANAGE_PERMISSIONS)) {
            player.sendMessage(ChatColor.RED + "You lack permission to manage faction permissions.");
            return false;
        }

        // Cannot modify permissions for your own rank or higher (unless you're owner)
        if (playerRank != Rank.OWNER && targetRank.ordinal() >= playerRank.ordinal()) {
            if (targetRank == playerRank) {
                player.sendMessage(ChatColor.RED + "You cannot modify permissions for your own rank.");
            } else {
                player.sendMessage(ChatColor.RED + "You cannot modify permissions for ranks equal to or higher than yours.");
            }
            return false;
        }

        return true;
    }

    /**
     * Check if a player can toggle a specific permission for a rank
     */
    private boolean canTogglePermission(Player player, Rank playerRank, Rank targetRank,
                                        FactionPermission permission, Faction faction) {

        // Owners can do anything
        if (playerRank == Rank.OWNER) {
            return true;
        }

        // If the player's rank doesn't have this permission, they cannot grant it to lower ranks
        if (!faction.hasPermission(playerRank, permission)) {
            player.sendMessage(ChatColor.RED + "You cannot toggle " + permission.getDisplayName() +
                    " because your rank (" + playerRank.name() + ") doesn't have this permission.");
            return false;
        }

        return true;
    }

    /**
     * Get the equivalent faction permission for a relation permission
     */
    private FactionPermission getEquivalentFactionPermission(RelationPermission relationPermission) {
        switch (relationPermission) {
            case BREAK_BLOCKS:
                return FactionPermission.BREAK_BLOCKS;
            case PLACE_BLOCKS:
                return FactionPermission.PLACE_BLOCKS;
            case PLACE_SPAWNERS:
                return FactionPermission.PLACE_SPAWNERS;
            case BREAK_SPAWNERS:
                return FactionPermission.BREAK_SPAWNERS;
            case INTERACT:
                return FactionPermission.INTERACT;
            case CONTAINER_ACCESS:
                return FactionPermission.CONTAINER_ACCESS;
            case ENDER_CHEST_ACCESS:
                return FactionPermission.ENDER_CHEST_ACCESS;
            case FLY:
                return FactionPermission.FLY;
            default:
                return null; // No direct equivalent
        }
    }

    /**
     * Handle relation permissions GUI clicks
     */
    public void handleRelationPermissionsClick(Player player, String displayName, String title) {
        String factionName = playerFactions.get(player.getUniqueId());

        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            openPermissionsGUI(player, factionName);
            return;
        }

        // Handle reset button click
        if (displayName.equals(ChatColor.YELLOW + "" + ChatColor.BOLD + "RESET TO DEFAULT")) {
            handleResetPermissionsClick(player, title, false);
            return;
        }

        // Extract relation from title
        String relationName = title.replace(ChatColor.DARK_GRAY + "", "").replace(" Permissions", "");
        Relation relation;
        try {
            relation = Relation.valueOf(relationName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try to match by display name
            relation = null;
            for (Relation r : Relation.values()) {
                if (r.getDisplayName().equals(relationName)) {
                    relation = r;
                    break;
                }
            }
            if (relation == null) {
                player.sendMessage(ChatColor.RED + "Error: Invalid relation detected.");
                return;
            }
        }

        // Find the permission that was clicked
        for (RelationPermission permission : RelationPermission.values()) {
            if (displayName.contains(permission.getDisplayName())) {
                Faction faction = factions.get(factionName);
                faction.toggleRelationPermission(relation, permission);

                // Save data
                plugin.getDataManager().saveFactionData();

                // Reopen the menu to show updated permissions
                openRelationPermissionsGUI(player, factionName, relation);

                player.sendMessage(ChatColor.GREEN + "Toggled " + permission.getDisplayName() +
                        " for " + relation.getDisplayName() + " relations!");
                return;
            }
        }
    }

    /**
     * Handle invitation menu clicks
     */
    public void handleInvitationMenuClick(Player player, String displayName, String title) {
        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            String factionName = playerFactions.get(player.getUniqueId());
            openFactionMenu(player, factionName);
            return;
        }

        // Handle player invitation clicks
        if (displayName.startsWith(ChatColor.GREEN.toString())) {
            String playerName = ChatColor.stripColor(displayName);
            Player targetPlayer = Bukkit.getPlayerExact(playerName);

            if (targetPlayer == null) {
                player.sendMessage(ChatColor.RED + "Player " + playerName + " is no longer online.");
                return;
            }

            String factionName = playerFactions.get(player.getUniqueId());
            UUID targetUUID = targetPlayer.getUniqueId();

            // Check if player is already in a faction
            if (playerFactions.containsKey(targetUUID)) {
                player.sendMessage(ChatColor.RED + playerName + " is already in a faction.");
                return;
            }

            // Add invitation
            playerInvitations.putIfAbsent(targetUUID, new HashSet<>());
            Set<String> invites = playerInvitations.get(targetUUID);

            if (invites.contains(factionName)) {
                player.sendMessage(ChatColor.YELLOW + playerName + " has already been invited to " + factionName + ".");
                return;
            }

            invites.add(factionName);

            // Notify both players
            player.sendMessage(ChatColor.GREEN + "Successfully invited " + playerName + " to " + factionName + "!");
            targetPlayer.sendMessage(ChatColor.GREEN + "You have been invited to faction " + factionName + " by " + player.getName() + "!");
            targetPlayer.sendMessage(ChatColor.YELLOW + "Use /f join " + factionName + " to join, or /f invites to see all invitations.");

            // Save data
            plugin.getDataManager().saveFactionData();

            // Reopen menu to refresh the list
            openInvitationMenu(player, factionName);
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

                plugin.getEventListener().onPlayerLeaveFaction(onlineTarget);
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
                player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/f menu" + ChatColor.GRAY + " to access faction features.");

                // REMOVED: Automatic menu opening
                // No longer automatically opens the faction menu

            } else if (factions.containsKey(factionName)) {
                player.sendMessage(ChatColor.RED + "A faction with that name already exists!");
                // Don't open any menu, just close the current one
                player.closeInventory();
            }

            // Clean up
            plugin.getPendingFactionCreation().remove(uuid);
            plugin.getPendingFactionNames().remove(uuid);

        } else if (displayName.equals(ChatColor.RED + "" + ChatColor.BOLD + "CANCEL")) {
            // Cancel creation
            plugin.getPendingFactionCreation().remove(uuid);
            plugin.getPendingFactionNames().remove(uuid);
            player.closeInventory();

            player.sendMessage(ChatColor.YELLOW + "Faction creation cancelled.");
        }
    }

    /**
     * Handle reset permissions button clicks
     */
    public void handleResetPermissionsClick(Player player, String title, boolean isRankPermissions) {
        String factionName = playerFactions.get(player.getUniqueId());
        if (factionName == null) {
            player.sendMessage(ChatColor.RED + "You are not in a faction.");
            return;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(player.getUniqueId());

        // Check if player has permission to manage permissions
        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.MANAGE_PERMISSIONS)) {
            player.sendMessage(ChatColor.RED + "You lack permission to reset faction permissions.");
            return;
        }

        if (isRankPermissions) {
            // Extract rank from title (e.g. "ADMIN Permissions" -> "ADMIN")
            String rankName = title.replace(ChatColor.DARK_GRAY + "", "").replace(" Permissions", "");
            Rank targetRank;
            try {
                targetRank = Rank.valueOf(rankName);
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Error: Invalid rank detected.");
                return;
            }

            // Check if player can modify this rank's permissions
            if (!canModifyRankPermissionsCheck(playerRank, targetRank, faction)) {
                if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.MANAGE_PERMISSIONS)) {
                    player.sendMessage(ChatColor.RED + "You lack permission to manage faction permissions.");
                } else if (targetRank.ordinal() >= playerRank.ordinal()) {
                    if (targetRank == playerRank) {
                        player.sendMessage(ChatColor.RED + "You cannot reset permissions for your own rank.");
                    } else {
                        player.sendMessage(ChatColor.RED + "You cannot reset permissions for ranks equal to or higher than yours.");
                    }
                }
                return;
            }

            resetRankPermissions(player, faction, targetRank, playerRank);

        } else {
            // This is a relation permissions reset - extract relation from title
            String relationName = title.replace(ChatColor.DARK_GRAY + "", "").replace(" Permissions", "");
            Relation relation;
            try {
                relation = Relation.valueOf(relationName.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Try to match by display name
                relation = null;
                for (Relation r : Relation.values()) {
                    if (r.getDisplayName().equals(relationName)) {
                        relation = r;
                        break;
                    }
                }
                if (relation == null) {
                    player.sendMessage(ChatColor.RED + "Error: Invalid relation detected.");
                    return;
                }
            }

            resetRelationPermissions(player, faction, relation, playerRank);
        }

        // Save data
        plugin.getDataManager().saveFactionData();
    }

    /**
     * Reset rank permissions to default, respecting player's permission limitations
     */
    private void resetRankPermissions(Player player, Faction faction, Rank targetRank, Rank playerRank) {
        // Get current permissions
        Set<FactionPermission> currentPermissions = faction.rankPermissions.getOrDefault(targetRank, EnumSet.noneOf(FactionPermission.class));

        // Get default permissions for this rank
        Set<FactionPermission> defaultPermissions = getDefaultPermissionsForRank(targetRank);

        // Track what permissions were actually reset
        Set<FactionPermission> permissionsReset = EnumSet.noneOf(FactionPermission.class);
        Set<FactionPermission> permissionsSkipped = EnumSet.noneOf(FactionPermission.class);

        // If player is owner, they can reset everything
        if (playerRank == Rank.OWNER) {
            // Clear current permissions and set to default
            faction.rankPermissions.put(targetRank, EnumSet.copyOf(defaultPermissions));
            permissionsReset.addAll(defaultPermissions);

            // Add any permissions that were removed
            for (FactionPermission perm : FactionPermission.values()) {
                if (currentPermissions.contains(perm) && !defaultPermissions.contains(perm)) {
                    permissionsReset.add(perm);
                }
            }
        } else {
            // Non-owner players can only reset permissions they have access to
            Set<FactionPermission> newPermissions = EnumSet.copyOf(currentPermissions);

            for (FactionPermission permission : FactionPermission.values()) {
                boolean currentlyHas = currentPermissions.contains(permission);
                boolean shouldHave = defaultPermissions.contains(permission);

                // Only modify if there's a difference and player has permission to toggle it
                if (currentlyHas != shouldHave && faction.hasPermission(playerRank, permission)) {
                    if (shouldHave) {
                        newPermissions.add(permission);
                    } else {
                        newPermissions.remove(permission);
                    }
                    permissionsReset.add(permission);
                } else if (currentlyHas != shouldHave) {
                    // Player lacks permission to change this - skip it
                    permissionsSkipped.add(permission);
                }
            }

            faction.rankPermissions.put(targetRank, newPermissions);
        }

        // Send feedback to player
        if (!permissionsReset.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "Successfully reset " + permissionsReset.size() +
                    " permissions for " + targetRank.name() + " rank to default!");

            if (permissionsReset.size() <= 5) {
                // Show individual permissions if not too many
                for (FactionPermission perm : permissionsReset) {
                    boolean enabled = defaultPermissions.contains(perm);
                    player.sendMessage(ChatColor.GRAY + "  • " + perm.getDisplayName() + ": " +
                            (enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                }
            }
        }

        if (!permissionsSkipped.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Skipped " + permissionsSkipped.size() +
                    " permissions you don't have access to:");

            if (permissionsSkipped.size() <= 5) {
                for (FactionPermission perm : permissionsSkipped) {
                    player.sendMessage(ChatColor.GRAY + "  • " + ChatColor.RED + perm.getDisplayName() +
                            ChatColor.GRAY + " (you lack this permission)");
                }
            }
        }

        if (permissionsReset.isEmpty() && permissionsSkipped.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + targetRank.name() + " rank permissions are already at default (or you cannot modify any of them).");
        }

        // Reopen the permissions GUI to show changes
        String factionName = playerFactions.get(player.getUniqueId());
        openRankPermissionsGUI(player, factionName, targetRank);
    }

    /**
     * Reset relation permissions to default, respecting player's permission limitations
     */
    private void resetRelationPermissions(Player player, Faction faction, Relation relation, Rank playerRank) {
        // Get current permissions
        Set<RelationPermission> currentPermissions = faction.relationPermissions.getOrDefault(relation, EnumSet.noneOf(RelationPermission.class));

        // Get default permissions for this relation
        Set<RelationPermission> defaultPermissions = getDefaultPermissionsForRelation(relation);

        // Track what permissions were actually reset
        Set<RelationPermission> permissionsReset = EnumSet.noneOf(RelationPermission.class);
        Set<RelationPermission> permissionsSkipped = EnumSet.noneOf(RelationPermission.class);

        // If player is owner, they can reset everything
        if (playerRank == Rank.OWNER) {
            // Clear current permissions and set to default
            faction.relationPermissions.put(relation, EnumSet.copyOf(defaultPermissions));
            permissionsReset.addAll(defaultPermissions);

            // Add any permissions that were removed
            for (RelationPermission perm : RelationPermission.values()) {
                if (currentPermissions.contains(perm) && !defaultPermissions.contains(perm)) {
                    permissionsReset.add(perm);
                }
            }
        } else {
            // Non-owner players can only reset permissions they have equivalent access to
            Set<RelationPermission> newPermissions = EnumSet.copyOf(currentPermissions);

            for (RelationPermission permission : RelationPermission.values()) {
                boolean currentlyHas = currentPermissions.contains(permission);
                boolean shouldHave = defaultPermissions.contains(permission);

                // Check if player has equivalent faction permission
                FactionPermission equivalentPermission = getEquivalentFactionPermission(permission);
                boolean canModify = equivalentPermission == null || faction.hasPermission(playerRank, equivalentPermission);

                // Only modify if there's a difference and player has permission to toggle it
                if (currentlyHas != shouldHave && canModify) {
                    if (shouldHave) {
                        newPermissions.add(permission);
                    } else {
                        newPermissions.remove(permission);
                    }
                    permissionsReset.add(permission);
                } else if (currentlyHas != shouldHave) {
                    // Player lacks permission to change this - skip it
                    permissionsSkipped.add(permission);
                }
            }

            faction.relationPermissions.put(relation, newPermissions);
        }

        // Send feedback to player
        if (!permissionsReset.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "Successfully reset " + permissionsReset.size() +
                    " permissions for " + relation.getDisplayName() + " relations to default!");

            if (permissionsReset.size() <= 5) {
                // Show individual permissions if not too many
                for (RelationPermission perm : permissionsReset) {
                    boolean enabled = defaultPermissions.contains(perm);
                    player.sendMessage(ChatColor.GRAY + "  • " + perm.getDisplayName() + ": " +
                            (enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                }
            }
        }

        if (!permissionsSkipped.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Skipped " + permissionsSkipped.size() +
                    " permissions you don't have equivalent access to:");

            if (permissionsSkipped.size() <= 5) {
                for (RelationPermission perm : permissionsSkipped) {
                    FactionPermission equiv = getEquivalentFactionPermission(perm);
                    String reason = equiv != null ? "you lack " + equiv.getDisplayName() : "no equivalent permission";
                    player.sendMessage(ChatColor.GRAY + "  • " + ChatColor.RED + perm.getDisplayName() +
                            ChatColor.GRAY + " (" + reason + ")");
                }
            }
        }

        if (permissionsReset.isEmpty() && permissionsSkipped.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + relation.getDisplayName() + " relation permissions are already at default (or you cannot modify any of them).");
        }

        // Reopen the permissions GUI to show changes
        String factionName = playerFactions.get(player.getUniqueId());
        openRelationPermissionsGUI(player, factionName, relation);
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

            plugin.getEventListener().onPlayerLeaveFaction(player);

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
            plugin.getEventListener().disbandFaction(factionName, player);
            player.closeInventory();

        } else if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "CANCEL")) {
            // Return to faction menu
            openFactionMenu(player, factionName);
        }
    }

    /**
     * Open the invitation menu for admins/owners to invite players
     */
    public void openInvitationMenu(Player player, String factionName) {
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Invite Players");

        // Fill with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 54; i++) {
            menu.setItem(i, blackGlass);
        }

        // Back button
        ItemStack backButton = createBackButton();
        menu.setItem(45, backButton);

        // Get online players who aren't in any faction
        List<Player> invitablePlayers = new ArrayList<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!playerFactions.containsKey(onlinePlayer.getUniqueId())) {
                invitablePlayers.add(onlinePlayer);
            }
        }

        // Display invitable players (slots 0-44, excluding slot 45 for back button)
        int slot = 0;
        for (Player invitablePlayer : invitablePlayers) {
            if (slot >= 45) break; // Don't overwrite back button

            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
            skullMeta.setOwningPlayer(invitablePlayer);
            skullMeta.setDisplayName(ChatColor.GREEN + invitablePlayer.getName());
            skullMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Status: " + ChatColor.GREEN + "Online",
                    ChatColor.GRAY + "Faction: " + ChatColor.RED + "None",
                    "",
                    ChatColor.YELLOW + "Click to invite to " + factionName + "!"
            ));
            playerHead.setItemMeta(skullMeta);
            menu.setItem(slot, playerHead);
            slot++;
        }

        // If no players to invite, show message
        if (invitablePlayers.isEmpty()) {
            ItemStack noPlayers = new ItemStack(Material.BARRIER);
            ItemMeta noPlayersMeta = noPlayers.getItemMeta();
            noPlayersMeta.setDisplayName(ChatColor.RED + "No Players Available");
            noPlayersMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "All online players are already",
                    ChatColor.GRAY + "in factions or not suitable",
                    ChatColor.GRAY + "for invitation."
            ));
            noPlayers.setItemMeta(noPlayersMeta);
            menu.setItem(22, noPlayers); // Center of GUI
        }

        player.openInventory(menu);
    }

    /**
     * Open the faction settings menu
     */
    public void openFactionSettings(Player player, String factionName) {
        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Faction Settings");

        // Fill with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 27; i++) {
            menu.setItem(i, blackGlass);
        }

        // Permissions option
        ItemStack permissions = new ItemStack(Material.REDSTONE);
        ItemMeta permissionsMeta = permissions.getItemMeta();
        permissionsMeta.setDisplayName(ChatColor.RED + "Faction Permissions");
        permissionsMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Manage faction permissions",
                ChatColor.GRAY + "for ranks and relations",
                "",
                ChatColor.YELLOW + "Click to manage permissions!"
        ));
        permissions.setItemMeta(permissionsMeta);
        menu.setItem(13, permissions); // Center

        // Back button
        ItemStack backButton = createBackButton();
        menu.setItem(18, backButton); // Bottom left

        player.openInventory(menu);
    }

    /**
     * Open the main permissions GUI with rank and relation sections - Updated with visual indicators
     */
    public void openPermissionsGUI(Player player, String factionName) {
        Inventory menu = Bukkit.createInventory(null, 36, ChatColor.DARK_GRAY + "Faction Permissions");

        // Fill with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 36; i++) {
            menu.setItem(i, blackGlass);
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(player.getUniqueId());

        // Left 2x2 Green Square - Individual Rank Permissions
        createRankPermissionPane(menu, 11, Rank.ADMIN, player, playerRank, faction);
        createRankPermissionPane(menu, 12, Rank.MOD, player, playerRank, faction);
        createRankPermissionPane(menu, 20, Rank.MEMBER, player, playerRank, faction);
        createRankPermissionPane(menu, 21, Rank.RECRUIT, player, playerRank, faction);

        // Right 2x2 Purple Square - Individual Relation Permissions
        createRelationPermissionPane(menu, 14, Relation.NEUTRAL, player, playerRank, faction);
        createRelationPermissionPane(menu, 15, Relation.TRUCE, player, playerRank, faction);
        createRelationPermissionPane(menu, 23, Relation.ALLY, player, playerRank, faction);
        createRelationPermissionPane(menu, 24, Relation.ENEMY, player, playerRank, faction);

        // Back button
        ItemStack backButton = createBackButton();
        menu.setItem(27, backButton);

        player.openInventory(menu);
    }

    private void createRankPermissionPane(Inventory menu, int slot, Rank rank, Player player, Rank playerRank, Faction faction) {
        boolean canModify = canModifyRankPermissionsCheck(playerRank, rank, faction);

        ItemStack rankGlass = new ItemStack(canModify ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta rankMeta = rankGlass.getItemMeta();

        if (canModify) {
            rankMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + rank.name());
            rankMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click to manage permissions",
                    ChatColor.GRAY + "for " + rank.name() + " rank",
                    "",
                    ChatColor.GREEN + "✓ You can modify these permissions"
            ));
        } else {
            rankMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + rank.name() + " 🔒");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Permissions for " + rank.name() + " rank");
            lore.add("");

            if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.MANAGE_PERMISSIONS)) {
                lore.add(ChatColor.RED + "✗ You lack MANAGE_PERMISSIONS");
            } else if (rank.ordinal() >= playerRank.ordinal()) {
                if (rank == playerRank) {
                    lore.add(ChatColor.RED + "✗ Cannot modify your own rank");
                } else {
                    lore.add(ChatColor.RED + "✗ Cannot modify higher ranks");
                }
            }

            lore.add(ChatColor.DARK_RED + "🔒 Locked - Cannot modify");
            rankMeta.setLore(lore);
        }

        rankGlass.setItemMeta(rankMeta);
        menu.setItem(slot, rankGlass);
    }

    private void createRelationPermissionPane(Inventory menu, int slot, Relation relation, Player player, Rank playerRank, Faction faction) {
        boolean canModify = canModifyRelationPermissionsCheck(playerRank, faction);

        ItemStack relationGlass = new ItemStack(canModify ? Material.PURPLE_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta relationMeta = relationGlass.getItemMeta();

        if (canModify) {
            relationMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + relation.getDisplayName());
            relationMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click to manage permissions",
                    ChatColor.GRAY + "for " + relation.getDisplayName() + " relations",
                    "",
                    ChatColor.GREEN + "✓ You can modify these permissions"
            ));
        } else {
            relationMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + relation.getDisplayName() + " 🔒");
            relationMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Permissions for " + relation.getDisplayName() + " relations",
                    "",
                    ChatColor.RED + "✗ You lack MANAGE_PERMISSIONS",
                    ChatColor.DARK_RED + "🔒 Locked - Cannot modify"
            ));
        }

        relationGlass.setItemMeta(relationMeta);
        menu.setItem(slot, relationGlass);
    }

    /**
     * Open rank-specific permissions GUI with visual indicators
     */
    public void openRankPermissionsGUI(Player player, String factionName, Rank rank) {
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + rank.name() + " Permissions");

        // Fill bottom row with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 45; i < 54; i++) {
            menu.setItem(i, blackGlass);
        }

        // Back button
        ItemStack backButton = createBackButton();
        menu.setItem(45, backButton);

        // Get faction to check current permissions
        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(player.getUniqueId());
        Set<FactionPermission> currentPerms = faction.rankPermissions.getOrDefault(rank, EnumSet.noneOf(FactionPermission.class));

        // Reset permissions button (center of bottom row - slot 49)
        ItemStack resetButton = createResetPermissionsButton(playerRank, rank, faction, true);
        menu.setItem(49, resetButton);

        // Add permission items with visual indicators
        FactionPermission[] permissions = FactionPermission.values();
        for (int i = 0; i < permissions.length && i < 45; i++) {
            FactionPermission perm = permissions[i];
            boolean hasPermission = currentPerms.contains(perm);
            boolean canToggle = canTogglePermissionCheck(playerRank, rank, perm, faction);

            Material material;
            ChatColor nameColor;
            List<String> lore = new ArrayList<>();

            if (canToggle) {
                material = hasPermission ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
                nameColor = hasPermission ? ChatColor.GREEN : ChatColor.RED;

                lore.add(ChatColor.GRAY + "Status: " + (hasPermission ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to " + (hasPermission ? "disable" : "enable") + "!");
            } else {
                material = Material.GRAY_STAINED_GLASS_PANE;
                nameColor = ChatColor.GRAY;

                lore.add(ChatColor.GRAY + "Status: " + (hasPermission ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                lore.add("");

                // Explain why it's locked
                if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, perm)) {
                    lore.add(ChatColor.RED + "✗ Your rank lacks this permission");
                    lore.add(ChatColor.GRAY + "You need " + perm.getDisplayName());
                    lore.add(ChatColor.GRAY + "to grant it to others");
                } else {
                    lore.add(ChatColor.RED + "✗ Cannot modify this rank's permissions");
                }

                lore.add("");
                lore.add(ChatColor.DARK_RED + "🔒 Locked - Cannot toggle");
            }

            ItemStack permItem = new ItemStack(material);
            ItemMeta permMeta = permItem.getItemMeta();
            permMeta.setDisplayName(nameColor + perm.getDisplayName() + (canToggle ? "" : " 🔒"));
            permMeta.setLore(lore);
            permItem.setItemMeta(permMeta);

            menu.setItem(i, permItem);
        }

        player.openInventory(menu);
    }

    /**
     * Open relation-specific permissions GUI with visual indicators
     */
    public void openRelationPermissionsGUI(Player player, String factionName, Relation relation) {
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + relation.getDisplayName() + " Permissions");

        // Fill bottom row with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 45; i < 54; i++) {
            menu.setItem(i, blackGlass);
        }

        // Back button
        ItemStack backButton = createBackButton();
        menu.setItem(45, backButton);

        // Get faction to check current permissions
        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(player.getUniqueId());
        Set<RelationPermission> currentPerms = faction.relationPermissions.getOrDefault(relation, EnumSet.noneOf(RelationPermission.class));

        // Reset permissions button (center of bottom row - slot 49)
        ItemStack resetButton = createResetPermissionsButton(playerRank, null, faction, false);
        menu.setItem(49, resetButton);

        // Add permission items with visual indicators
        RelationPermission[] permissions = RelationPermission.values();
        for (int i = 0; i < permissions.length && i < 45; i++) {
            RelationPermission perm = permissions[i];
            boolean hasPermission = currentPerms.contains(perm);
            boolean canToggle = canToggleRelationPermissionCheck(playerRank, relation, perm, faction);

            Material material;
            ChatColor nameColor;
            List<String> lore = new ArrayList<>();

            if (canToggle) {
                material = hasPermission ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
                nameColor = hasPermission ? ChatColor.GREEN : ChatColor.RED;

                lore.add(ChatColor.GRAY + "Status: " + (hasPermission ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to " + (hasPermission ? "disable" : "enable") + "!");
            } else {
                material = Material.GRAY_STAINED_GLASS_PANE;
                nameColor = ChatColor.GRAY;

                lore.add(ChatColor.GRAY + "Status: " + (hasPermission ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                lore.add("");

                // Explain why it's locked
                FactionPermission equivalentPermission = getEquivalentFactionPermission(perm);
                if (playerRank != Rank.OWNER && equivalentPermission != null && !faction.hasPermission(playerRank, equivalentPermission)) {
                    lore.add(ChatColor.RED + "✗ Your rank lacks equivalent permission");
                    lore.add(ChatColor.GRAY + "You need " + equivalentPermission.getDisplayName());
                    lore.add(ChatColor.GRAY + "to grant " + perm.getDisplayName());
                } else {
                    lore.add(ChatColor.RED + "✗ Cannot modify relation permissions");
                }

                lore.add("");
                lore.add(ChatColor.DARK_RED + "🔒 Locked - Cannot toggle");
            }

            ItemStack permItem = new ItemStack(material);
            ItemMeta permMeta = permItem.getItemMeta();
            permMeta.setDisplayName(nameColor + perm.getDisplayName() + (canToggle ? "" : " 🔒"));
            permMeta.setLore(lore);
            permItem.setItemMeta(permMeta);

            menu.setItem(i, permItem);
        }

        player.openInventory(menu);
    }

    // Helper methods for checking permissions without sending error messages (for GUI display)
    private boolean canModifyRankPermissionsCheck(Rank playerRank, Rank targetRank, Faction faction) {
        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.MANAGE_PERMISSIONS)) {
            return false;
        }
        return playerRank == Rank.OWNER || targetRank.ordinal() < playerRank.ordinal();
    }

    private boolean canModifyRelationPermissionsCheck(Rank playerRank, Faction faction) {
        return playerRank == Rank.OWNER || faction.hasPermission(playerRank, FactionPermission.MANAGE_PERMISSIONS);
    }

    private boolean canTogglePermissionCheck(Rank playerRank, Rank targetRank, FactionPermission permission, Faction faction) {
        if (playerRank == Rank.OWNER) return true;
        return faction.hasPermission(playerRank, permission);
    }

    private boolean canToggleRelationPermissionCheck(Rank playerRank, Relation targetRelation, RelationPermission permission, Faction faction) {
        if (playerRank == Rank.OWNER) return true;

        FactionPermission equivalentPermission = getEquivalentFactionPermission(permission);
        if (equivalentPermission != null) {
            return faction.hasPermission(playerRank, equivalentPermission);
        }
        return true;
    }

    /**
     * Create reset permissions button with custom head texture
     */
    private ItemStack createResetPermissionsButton(Rank playerRank, Rank targetRank, Faction faction, boolean isRankPermissions) {
        ItemStack resetButton = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) resetButton.getItemMeta();

        // Set custom texture
        String texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjQ2ZGRiN2ZhMjQxN2E5Yjg4N2Q3ZDUwOTM1ZGVkY2FmNDVmZTkwYWM2ZmI0MTc4OGY4YWE0ZjVlODQ2ZDVkZiJ9fX0=";

        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", texture));

            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set custom head texture for reset button: " + e.getMessage());
        }

        boolean canReset = playerRank == Rank.OWNER || faction.hasPermission(playerRank, FactionPermission.MANAGE_PERMISSIONS);

        if (canReset) {
            skullMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "RESET TO DEFAULT");

            List<String> lore = new ArrayList<>();
            if (isRankPermissions && targetRank != null) {
                lore.add(ChatColor.GRAY + "Reset " + targetRank.name() + " permissions to default");
            } else {
                lore.add(ChatColor.GRAY + "Reset relation permissions to default");
            }
            lore.add("");

            if (playerRank == Rank.OWNER) {
                lore.add(ChatColor.GREEN + "✓ Will reset ALL permissions");
            } else {
                lore.add(ChatColor.YELLOW + "⚠ Will only reset permissions you have");
                lore.add(ChatColor.GRAY + "Permissions you lack will be unchanged");
            }

            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to reset permissions!");
            lore.add(ChatColor.RED + "This action cannot be undone!");

            skullMeta.setLore(lore);
        } else {
            skullMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "RESET TO DEFAULT 🔒");
            skullMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Reset permissions to default",
                    "",
                    ChatColor.RED + "✗ You lack MANAGE_PERMISSIONS",
                    ChatColor.DARK_RED + "🔒 Locked - Cannot reset"
            ));
        }

        resetButton.setItemMeta(skullMeta);
        return resetButton;
    }

    /**
     * Clear pending kick for a player
     */
    public void clearPendingKick(UUID kickerUUID) {
        pendingKicks.remove(kickerUUID);
    }

    /**
     * Get default permissions for a rank (copied from Faction.java)
     */
    private Set<FactionPermission> getDefaultPermissionsForRank(Rank rank) {
        switch (rank) {
            case RECRUIT:
                return EnumSet.of(
                        FactionPermission.BREAK_BLOCKS,
                        FactionPermission.PLACE_BLOCKS,
                        FactionPermission.INTERACT,
                        FactionPermission.CONTAINER_ACCESS,
                        FactionPermission.USE_HOME,
                        FactionPermission.WARPS_ACCESS,
                        FactionPermission.VIEW_DISCORD,
                        FactionPermission.BANK_DEPOSIT
                );
            case MEMBER:
                Set<FactionPermission> memberPerms = EnumSet.of(
                        FactionPermission.BREAK_BLOCKS,
                        FactionPermission.PLACE_BLOCKS,
                        FactionPermission.INTERACT,
                        FactionPermission.CONTAINER_ACCESS,
                        FactionPermission.USE_HOME,
                        FactionPermission.WARPS_ACCESS,
                        FactionPermission.VIEW_DISCORD,
                        FactionPermission.BANK_DEPOSIT
                );
                memberPerms.addAll(EnumSet.of(
                        FactionPermission.ENDER_CHEST_ACCESS,
                        FactionPermission.SET_HOME,
                        FactionPermission.FACTION_CHEST_ACCESS,
                        FactionPermission.BANK_WITHDRAW
                ));
                return memberPerms;
            case MOD:
                Set<FactionPermission> modPerms = EnumSet.of(
                        FactionPermission.BREAK_BLOCKS,
                        FactionPermission.PLACE_BLOCKS,
                        FactionPermission.INTERACT,
                        FactionPermission.CONTAINER_ACCESS,
                        FactionPermission.USE_HOME,
                        FactionPermission.WARPS_ACCESS,
                        FactionPermission.VIEW_DISCORD,
                        FactionPermission.BANK_DEPOSIT,
                        FactionPermission.ENDER_CHEST_ACCESS,
                        FactionPermission.SET_HOME,
                        FactionPermission.FACTION_CHEST_ACCESS,
                        FactionPermission.BANK_WITHDRAW
                );
                modPerms.addAll(EnumSet.of(
                        FactionPermission.PLACE_SPAWNERS,
                        FactionPermission.BREAK_SPAWNERS,
                        FactionPermission.INVITE_MEMBERS,
                        FactionPermission.KICK_MEMBERS,
                        FactionPermission.PROMOTE_MEMBERS,
                        FactionPermission.DEMOTE_MEMBERS,
                        FactionPermission.MANAGE_WARPS,
                        FactionPermission.CLAIM_LAND,
                        FactionPermission.UNCLAIM_LAND,
                        FactionPermission.FLY,
                        FactionPermission.FACTION_CHEST_LOGS
                ));
                return modPerms;
            case ADMIN:
                Set<FactionPermission> adminPerms = EnumSet.of(
                        FactionPermission.BREAK_BLOCKS,
                        FactionPermission.PLACE_BLOCKS,
                        FactionPermission.INTERACT,
                        FactionPermission.CONTAINER_ACCESS,
                        FactionPermission.USE_HOME,
                        FactionPermission.WARPS_ACCESS,
                        FactionPermission.VIEW_DISCORD,
                        FactionPermission.BANK_DEPOSIT,
                        FactionPermission.ENDER_CHEST_ACCESS,
                        FactionPermission.SET_HOME,
                        FactionPermission.FACTION_CHEST_ACCESS,
                        FactionPermission.BANK_WITHDRAW,
                        FactionPermission.PLACE_SPAWNERS,
                        FactionPermission.BREAK_SPAWNERS,
                        FactionPermission.INVITE_MEMBERS,
                        FactionPermission.KICK_MEMBERS,
                        FactionPermission.PROMOTE_MEMBERS,
                        FactionPermission.DEMOTE_MEMBERS,
                        FactionPermission.MANAGE_WARPS,
                        FactionPermission.CLAIM_LAND,
                        FactionPermission.UNCLAIM_LAND,
                        FactionPermission.FLY,
                        FactionPermission.FACTION_CHEST_LOGS
                );
                adminPerms.addAll(EnumSet.of(
                        FactionPermission.SET_TITLES,
                        FactionPermission.BANK_LOGS,
                        FactionPermission.SET_RELATIONS,
                        FactionPermission.UNCLAIM_ALL,
                        FactionPermission.CHANGE_DESCRIPTION,
                        FactionPermission.SET_DISCORD,
                        FactionPermission.SET_ANNOUNCEMENTS,
                        FactionPermission.OPEN_CLOSE
                ));
                return adminPerms;
            case OWNER:
                // Owner gets all permissions
                return EnumSet.allOf(FactionPermission.class);
            default:
                return EnumSet.noneOf(FactionPermission.class);
        }
    }

    /**
     * Get default permissions for a relation (copied from Faction.java)
     */
    private Set<RelationPermission> getDefaultPermissionsForRelation(Relation relation) {
        switch (relation) {
            case NEUTRAL:
                return EnumSet.noneOf(RelationPermission.class); // No permissions for neutrals
            case TRUCE:
                return EnumSet.of(RelationPermission.INTERACT);
            case ALLY:
                return EnumSet.of(
                        RelationPermission.BREAK_BLOCKS,
                        RelationPermission.PLACE_BLOCKS,
                        RelationPermission.INTERACT,
                        RelationPermission.CONTAINER_ACCESS,
                        RelationPermission.FLY
                );
            case ENEMY:
                return EnumSet.noneOf(RelationPermission.class); // No permissions for enemies
            default:
                return EnumSet.noneOf(RelationPermission.class);
        }
    }
}