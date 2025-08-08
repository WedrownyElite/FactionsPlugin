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

    private void openFactionBrowser(Player player, int publicPage, int invitePage) {
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Browse Factions"); // 6 rows (max allowed)

        // Fill with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 54; i++) { // Changed from 63 to 54
            menu.setItem(i, blackGlass);
        }

        // Row 1: Public factions header
        ItemStack publicHeader = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmM2MjExMGQ4MTg4NDQxZDIxNzk0NDM0ZjY3ZDEyYTAyMWI3NDAyYzhkYWE0MmQ0ZmVhMzIzZTdlMTllMGJiNyJ9fX0=");
        ItemMeta publicHeaderMeta = publicHeader.getItemMeta();
        publicHeaderMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "PUBLIC FACTIONS");
        publicHeaderMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "These factions are open to everyone",
                ChatColor.GRAY + "Click on any faction to join!"
        ));
        publicHeader.setItemMeta(publicHeaderMeta);
        menu.setItem(0, publicHeader);

        // Get public factions
        List<String> publicFactions = new ArrayList<>();
        for (Map.Entry<String, Faction> entry : factions.entrySet()) {
            if (entry.getValue().isPublic) {
                publicFactions.add(entry.getKey());
            }
        }

        // Public factions section (rows 2-3, slots 9-26)
        int publicSlotsPerPage = 18; // 2 rows * 9 slots
        int publicStartIndex = publicPage * publicSlotsPerPage;
        int publicSlot = 9; // Start of row 2

        for (int i = publicStartIndex; i < Math.min(publicStartIndex + publicSlotsPerPage, publicFactions.size()); i++) {
            String factionName = publicFactions.get(i);
            Faction faction = factions.get(factionName);
            ItemStack factionHead = createFactionHead(factionName, faction, true);
            menu.setItem(publicSlot, factionHead);
            publicSlot++;
            if (publicSlot == 18) publicSlot = 18; // Skip to next row
            if (publicSlot >= 27) break; // Don't go beyond allocated space
        }

        // Row 4: Invitations header and navigation
        ItemStack inviteHeader = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzQ1ZmQxNzRmMjUwMzdiN2Y5ZWNhNzMzY2ZkMDQ2YThiNjM1MTEyMDI2NDg1MzcwNWJjYWE1YjYzZTE3YzE3In19fQ==");
        ItemMeta inviteHeaderMeta = inviteHeader.getItemMeta();
        inviteHeaderMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "YOUR INVITATIONS");
        inviteHeaderMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Factions that have invited you",
                ChatColor.GRAY + "Click on any faction to join!"
        ));
        inviteHeader.setItemMeta(inviteHeaderMeta);
        menu.setItem(27, inviteHeader);

        // Public faction navigation (in row 4)
        boolean hasMorePublicFactions = (publicStartIndex + publicSlotsPerPage) < publicFactions.size();
        boolean hasPublicPreviousPage = publicPage > 0;

        if (hasPublicPreviousPage) {
            ItemStack leftArrow = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGUxZGZjMTFhODM3MTExZDIyYjAwMWExNDQ2MWY5YTdmYzA5MzUyMmY4OGM1OGZhZWZkNmFkZWZmY2Q0ZTlhYiJ9fX0=");
            ItemMeta leftMeta = leftArrow.getItemMeta();
            leftMeta.setDisplayName(ChatColor.GRAY + "← Previous Public Factions");
            leftMeta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "Page " + publicPage + " of " + ((publicFactions.size() - 1) / publicSlotsPerPage + 1)));
            leftArrow.setItemMeta(leftMeta);
            menu.setItem(34, leftArrow);
        }

        if (hasMorePublicFactions) {
            ItemStack rightArrow = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2M2OWQ0MTA3NmE4ZGVhNGYwNmQzZjFhOWFjNDdjYzk5Njk4OGI3NGEwOTEzYWIyYWMxYTc0Y2FmNzA4MTkxOCJ9fX0=");
            ItemMeta rightMeta = rightArrow.getItemMeta();
            rightMeta.setDisplayName(ChatColor.GRAY + "Next Public Factions →");
            rightMeta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "Page " + (publicPage + 2) + " of " + ((publicFactions.size() - 1) / publicSlotsPerPage + 1)));
            rightArrow.setItemMeta(rightMeta);
            menu.setItem(35, rightArrow);
        }

        // Get player invitations
        Set<String> playerInvites = playerInvitations.get(player.getUniqueId());
        List<String> invitedFactions = playerInvites != null ? new ArrayList<>(playerInvites) : new ArrayList<>();

        // Invitations section (rows 5-6, slots 36-53)
        int inviteSlotsPerPage = 18; // 2 rows * 9 slots
        int inviteStartIndex = invitePage * inviteSlotsPerPage;
        int inviteSlot = 36; // Start of row 5

        for (int i = inviteStartIndex; i < Math.min(inviteStartIndex + inviteSlotsPerPage, invitedFactions.size()); i++) {
            String factionName = invitedFactions.get(i);
            Faction faction = factions.get(factionName);
            if (faction != null) {
                ItemStack factionHead = createFactionHead(factionName, faction, false);
                menu.setItem(inviteSlot, factionHead);
                inviteSlot++;
                if (inviteSlot == 45) inviteSlot = 45; // Skip to next row
                if (inviteSlot >= 54) break; // Don't go beyond allocated space (changed from 54 to 54)
            }
        }

        // Row 6: Bottom navigation (changed from row 7)
        ItemStack backButton = createBackButton();
        menu.setItem(45, backButton); // Changed from slot 54 to 45

        // Invitation navigation (in row 6) - adjusted slot numbers
        boolean hasMoreInvitations = (inviteStartIndex + inviteSlotsPerPage) < invitedFactions.size();
        boolean hasInvitePreviousPage = invitePage > 0;

        if (hasInvitePreviousPage) {
            ItemStack leftArrow = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGUxZGZjMTFhODM3MTExZDIyYjAwMWExNDQ2MWY5YTdmYzA5MzUyMmY4OGM1OGZhZWZkNmFkZWZmY2Q0ZTlhYiJ9fX0=");
            ItemMeta leftMeta = leftArrow.getItemMeta();
            leftMeta.setDisplayName(ChatColor.GRAY + "← Previous Invitations");
            leftMeta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "Page " + invitePage + " of " + ((invitedFactions.size() - 1) / inviteSlotsPerPage + 1)));
            leftArrow.setItemMeta(leftMeta);
            menu.setItem(52, leftArrow); // Changed from slot 61 to 52
        }

        if (hasMoreInvitations) {
            ItemStack rightArrow = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2M2OWQ0MTA3NmE4ZGVhNGYwNmQzZjFhOWFjNDdjYzk5Njk4OGI3NGEwOTEzYWIyYWMxYTc0Y2FmNzA4MTkxOCJ9fX0=");
            ItemMeta rightMeta = rightArrow.getItemMeta();
            rightMeta.setDisplayName(ChatColor.GRAY + "Next Invitations →");
            rightMeta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "Page " + (invitePage + 2) + " of " + ((invitedFactions.size() - 1) / inviteSlotsPerPage + 1)));
            rightArrow.setItemMeta(rightMeta);
            menu.setItem(53, rightArrow); // Changed from slot 62 to 53
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

        // Admin and Owner options
        if (playerRank == Rank.OWNER || playerRank == Rank.ADMIN || playerRank == Rank.MOD) {
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

        // Settings for Admins and Owners only
        if (playerRank == Rank.OWNER || playerRank == Rank.ADMIN) {
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
        player.sendMessage(ChatColor.YELLOW + "Welcome to " + factionName + "! Use /f menu to access faction features.");

        // Notify other members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(player)) {
                member.sendMessage(ChatColor.GREEN + player.getName() + " has joined the faction!");
            }
        }

        // Save data
        plugin.getDataManager().saveFactionData();

        // Open faction menu after a short delay
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    openFactionsMenu(player);
                }
            }
        }.runTaskLater(plugin, 40L); // 2 seconds
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

        // Handle rank permission clicks (green squares)
        if (displayName.startsWith(ChatColor.GREEN.toString())) {
            if (displayName.contains("ADMIN")) {
                openRankPermissionsGUI(player, factionName, Rank.ADMIN);
            } else if (displayName.contains("MOD")) {
                openRankPermissionsGUI(player, factionName, Rank.MOD);
            } else if (displayName.contains("MEMBER")) {
                openRankPermissionsGUI(player, factionName, Rank.MEMBER);
            } else if (displayName.contains("RECRUIT")) {
                openRankPermissionsGUI(player, factionName, Rank.RECRUIT);
            }
        }
        // Handle relation permission clicks (purple squares)
        else if (displayName.startsWith(ChatColor.LIGHT_PURPLE.toString())) {
            if (displayName.contains("Neutral")) {
                openRelationPermissionsGUI(player, factionName, Relation.NEUTRAL);
            } else if (displayName.contains("Truce")) {
                openRelationPermissionsGUI(player, factionName, Relation.TRUCE);
            } else if (displayName.contains("Ally")) {
                openRelationPermissionsGUI(player, factionName, Relation.ALLY);
            } else if (displayName.contains("Enemy")) {
                openRelationPermissionsGUI(player, factionName, Relation.ENEMY);
            }
        }
    }

    /**
     * Handle rank permissions GUI clicks
     */
    public void handleRankPermissionsClick(Player player, String displayName, String title) {
        String factionName = playerFactions.get(player.getUniqueId());

        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            openPermissionsGUI(player, factionName);
            return;
        }

        // Extract rank from title
        String rankName = title.replace(ChatColor.DARK_GRAY + "", "").replace(" Permissions", "");
        Rank rank;
        try {
            rank = Rank.valueOf(rankName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Error: Invalid rank detected.");
            return;
        }

        // Find the permission that was clicked
        for (FactionPermission permission : FactionPermission.values()) {
            if (displayName.contains(permission.getDisplayName())) {
                Faction faction = factions.get(factionName);
                faction.togglePermission(rank, permission);

                // Save data
                plugin.getDataManager().saveFactionData();

                // Reopen the menu to show updated permissions
                openRankPermissionsGUI(player, factionName, rank);

                player.sendMessage(ChatColor.GREEN + "Toggled " + permission.getDisplayName() +
                        " for " + rank.name() + " rank!");
                return;
            }
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
     * Open the main permissions GUI with rank and relation sections
     */
    public void openPermissionsGUI(Player player, String factionName) {
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Faction Permissions");

        // Fill with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 54; i++) {
            menu.setItem(i, blackGlass);
        }

        // Rank permissions section (Green squares - 2x2 each)
        // ADMIN (slots 10, 11, 19, 20)
        createRankPermissionSquare(menu, Rank.ADMIN, 10);

        // MOD (slots 12, 13, 21, 22)
        createRankPermissionSquare(menu, Rank.MOD, 12);

        // MEMBER (slots 14, 15, 23, 24)
        createRankPermissionSquare(menu, Rank.MEMBER, 14);

        // RECRUIT (slots 16, 17, 25, 26)
        createRankPermissionSquare(menu, Rank.RECRUIT, 16);

        // Relation permissions section (Purple squares - 2x2 each)
        // NEUTRAL (slots 28, 29, 37, 38)
        createRelationPermissionSquare(menu, Relation.NEUTRAL, 28);

        // TRUCE (slots 30, 31, 39, 40)
        createRelationPermissionSquare(menu, Relation.TRUCE, 30);

        // ALLY (slots 32, 33, 41, 42)
        createRelationPermissionSquare(menu, Relation.ALLY, 32);

        // ENEMY (slots 34, 35, 43, 44)
        createRelationPermissionSquare(menu, Relation.ENEMY, 34);

        // Back button
        ItemStack backButton = createBackButton();
        menu.setItem(45, backButton); // Bottom left

        player.openInventory(menu);
    }

    private void createRankPermissionSquare(Inventory menu, Rank rank, int startSlot) {
        ItemStack greenGlass = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta greenMeta = greenGlass.getItemMeta();
        greenMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + rank.name());
        greenMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click to manage permissions",
                ChatColor.GRAY + "for " + rank.name() + " rank"
        ));
        greenGlass.setItemMeta(greenMeta);

        // Place in 2x2 square
        menu.setItem(startSlot, greenGlass);
        menu.setItem(startSlot + 1, greenGlass);
        menu.setItem(startSlot + 9, greenGlass);
        menu.setItem(startSlot + 10, greenGlass);
    }

    private void createRelationPermissionSquare(Inventory menu, Relation relation, int startSlot) {
        ItemStack purpleGlass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta purpleMeta = purpleGlass.getItemMeta();
        purpleMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + relation.getDisplayName());
        purpleMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click to manage permissions",
                ChatColor.GRAY + "for " + relation.getDisplayName() + " relations"
        ));
        purpleGlass.setItemMeta(purpleMeta);

        // Place in 2x2 square
        menu.setItem(startSlot, purpleGlass);
        menu.setItem(startSlot + 1, purpleGlass);
        menu.setItem(startSlot + 9, purpleGlass);
        menu.setItem(startSlot + 10, purpleGlass);
    }

    /**
     * Open rank-specific permissions GUI
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
        Set<FactionPermission> currentPerms = faction.rankPermissions.getOrDefault(rank, EnumSet.noneOf(FactionPermission.class));

        // Add permission items
        FactionPermission[] permissions = FactionPermission.values();
        for (int i = 0; i < permissions.length && i < 45; i++) {
            FactionPermission perm = permissions[i];
            boolean hasPermission = currentPerms.contains(perm);

            ItemStack permItem = new ItemStack(hasPermission ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
            ItemMeta permMeta = permItem.getItemMeta();
            permMeta.setDisplayName((hasPermission ? ChatColor.GREEN : ChatColor.RED) + perm.getDisplayName());
            permMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Status: " + (hasPermission ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"),
                    "",
                    ChatColor.YELLOW + "Click to " + (hasPermission ? "disable" : "enable") + "!"
            ));
            permItem.setItemMeta(permMeta);
            menu.setItem(i, permItem);
        }

        player.openInventory(menu);
    }

    /**
     * Open relation-specific permissions GUI
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
        Set<RelationPermission> currentPerms = faction.relationPermissions.getOrDefault(relation, EnumSet.noneOf(RelationPermission.class));

        // Add permission items
        RelationPermission[] permissions = RelationPermission.values();
        for (int i = 0; i < permissions.length && i < 45; i++) {
            RelationPermission perm = permissions[i];
            boolean hasPermission = currentPerms.contains(perm);

            ItemStack permItem = new ItemStack(hasPermission ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
            ItemMeta permMeta = permItem.getItemMeta();
            permMeta.setDisplayName((hasPermission ? ChatColor.GREEN : ChatColor.RED) + perm.getDisplayName());
            permMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Status: " + (hasPermission ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"),
                    "",
                    ChatColor.YELLOW + "Click to " + (hasPermission ? "disable" : "enable") + "!"
            ));
            permItem.setItemMeta(permMeta);
            menu.setItem(i, permItem);
        }

        player.openInventory(menu);
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