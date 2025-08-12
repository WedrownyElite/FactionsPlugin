package me.elite.Factions.gui;

// Multiworlds imports
import me.elite.rtpplugin.RTPPlugin;
import me.elite.rtpplugin.WorldInfo;

// Factions imports
import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.constants.FactionsConstants;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.FactionPermission;
import me.elite.Factions.data.Rank;
import me.elite.Factions.data.Relation;
import me.elite.Factions.data.RelationPermission;
import me.elite.Factions.data.RelationRequest;
import me.elite.Factions.Relations.RelationManager;
import me.elite.Factions.utils.ChatUtils;
import me.elite.Factions.gui.BrowserMenuHandler;
import me.elite.Factions.utils.MessageManager;
import me.elite.Factions.territory.ChunkCoord;
import me.elite.Factions.homes.FactionHome;
import me.elite.Factions.warps.FactionWarp;

// External libraries
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

// Bukkit imports
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

// Java imports
import java.lang.reflect.Field;
import java.util.*;

public class MenuHandler {
    private final FactionsPlugin plugin;
    private final Map<String, Faction> factions;
    private final Map<UUID, String> playerFactions;
    private final Map<UUID, Set<String>> playerInvitations;
    private Rank targetCurrentRank;
    private Rank currentManagerRank;
    private final Map<UUID, String> pendingOwnershipTransfersGUI = new HashMap<>();

    private RTPPlugin rtpPlugin;

    // Track pending kick confirmations
    private final Map<UUID, UUID> pendingKicks = new HashMap<>(); // kicker -> target

    public MenuHandler(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
        this.playerInvitations = plugin.getPlayerInvitations();
        this.rtpPlugin = (RTPPlugin) Bukkit.getPluginManager().getPlugin("RTPPlugin");
    }

    /**
     * Create a "Back" button with left arrow player head
     */
    public ItemStack createBackButton() {
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
        Inventory menu = Bukkit.createInventory(null, FactionsConstants.SMALL_GUI_SIZE, ChatColor.DARK_GRAY + "Factions Menu");// 2 rows

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
     * Create a custom player head with base64 texture
     */
    public ItemStack createCustomHead(String texture) {
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
        if (playerRank == Rank.OWNER || faction.hasPermission(playerRank, FactionPermission.OPEN_CLOSE)) {
            infoMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Name: " + ChatColor.WHITE + factionName,
                    ChatColor.GRAY + "Description: " + ChatColor.WHITE + faction.description,
                    ChatColor.GRAY + "Your Rank: " + ChatColor.WHITE + playerRank.name(),
                    ChatColor.GRAY + "Members: " + ChatColor.WHITE + faction.members.size(),
                    "",
                    ChatColor.LIGHT_PURPLE + "Faction Visibility: " + (faction.isPublic ? ChatColor.GREEN + "Public" : ChatColor.RED + "Private"),
                    ChatColor.GRAY + (faction.isPublic ? ChatColor.GRAY + "Anyone can join" : ChatColor.GRAY + "Invite only"),
                    ChatColor.GRAY + "Left click to toggle"
                    ));
        }
        else {
            infoMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Name: " + ChatColor.WHITE + factionName,
                    ChatColor.GRAY + "Description: " + ChatColor.WHITE + faction.description,
                    ChatColor.GRAY + "Your Rank: " + ChatColor.WHITE + playerRank.name(),
                    ChatColor.GRAY + "Members: " + ChatColor.WHITE + faction.members.size(),
                    "",
                    ChatColor.LIGHT_PURPLE + "Faction Visibility: " + (faction.isPublic ? ChatColor.GREEN + "Public" : ChatColor.RED + "Private"),
                    ChatColor.GRAY + (faction.isPublic ? ChatColor.GRAY + "Anyone can join" : ChatColor.GRAY + "Invite only")
            ));
        }
        info.setItemMeta(infoMeta);
        menu.setItem(13, info);

        // Claims info
        ItemStack claims = new ItemStack(Material.MAP);
        ItemMeta claimsMeta = claims.getItemMeta();
        claimsMeta.setDisplayName(ChatColor.GREEN + "Territory");

        // Build claims per world breakdown
        List<String> claimsLore = new ArrayList<>();
        Map<String, Integer> claimsPerWorld = getFactionsClaimsPerWorld(factionName);
        int totalClaims = claimsPerWorld.values().stream().mapToInt(Integer::intValue).sum();

        claimsLore.add(ChatColor.GRAY + "Total Claims: " + ChatColor.WHITE + totalClaims);
        claimsLore.add("");

        if (claimsPerWorld.isEmpty()) {
            claimsLore.add(ChatColor.RED + "No claims in any world");
        } else {
            claimsLore.add(ChatColor.YELLOW + "Claims by World:");
            for (Map.Entry<String, Integer> entry : claimsPerWorld.entrySet()) {
                String worldName = entry.getKey();
                int claims_count = entry.getValue();
                if (claims_count <= 1) {
                    claimsLore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + worldName + ChatColor.GRAY + ": " +
                            ChatColor.GREEN + claims_count + ChatColor.GRAY + " chunk");
                }
                else {
                    claimsLore.add(ChatColor.GRAY + "• " + ChatColor.WHITE + worldName + ChatColor.GRAY + ": " +
                            ChatColor.GREEN + claims_count + ChatColor.GRAY + " chunks");
                }
            }
        }

        claimsLore.add("");
        claimsLore.add(ChatColor.GRAY + "View nearby faction claims");
        claimsLore.add(ChatColor.GRAY + "Click to see faction map");

        claimsMeta.setLore(claimsLore);
        claims.setItemMeta(claimsMeta);
        menu.setItem(16, claims);

        ItemStack homesWarps = new ItemStack(Material.COMPASS);
        ItemMeta homesWarpsMeta = homesWarps.getItemMeta();
        homesWarpsMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Homes & Warps");

        List<String> homesWarpsLore = new ArrayList<>();

        // Check home
        if (faction.home != null) {
            String homeWorldDisplay = getWorldDisplayName(faction.home.getWorldName());
            homesWarpsLore.add(ChatColor.GREEN + "✓ Home: " + ChatColor.WHITE + homeWorldDisplay);
        } else {
            homesWarpsLore.add(ChatColor.RED + "✗ No home set");
        }

        // Check warps
        if (faction.warps.isEmpty()) {
            homesWarpsLore.add(ChatColor.RED + "✗ No warps set");
        } else {
            homesWarpsLore.add(ChatColor.GREEN + "✓ Warps: " + ChatColor.WHITE + faction.warps.size());
        }

        homesWarpsLore.add("");

        // Check permissions
        boolean canUseHome = faction.hasPermission(playerRank, FactionPermission.USE_HOME);
        boolean canUseWarps = faction.hasPermission(playerRank, FactionPermission.WARPS_ACCESS);

        if (canUseHome || canUseWarps) {
            homesWarpsLore.add(ChatColor.YELLOW + "Click to manage teleportation!");
            if (canUseHome && faction.home != null) {
                homesWarpsLore.add(ChatColor.GRAY + "• Teleport to home");
            }
            if (canUseWarps && !faction.warps.isEmpty()) {
                homesWarpsLore.add(ChatColor.GRAY + "• Teleport to warps");
            }
        } else {
            homesWarpsLore.add(ChatColor.RED + "You lack permission to use teleportation");
        }

        homesWarpsMeta.setLore(homesWarpsLore);
        homesWarps.setItemMeta(homesWarpsMeta);
        menu.setItem(25, homesWarps);

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
        String timeString = ChatUtils.formatTimeString(timeSince);

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
        int relationsPerPage = FactionsConstants.RELATIONS_PER_PAGE;
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
        int requestsPerPage = FactionsConstants.REQUESTS_PER_PAGE;
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
            MessageManager.sendError(player, "You lack permission to manage faction relations.");
            return;
        }

        boolean success = plugin.getRelationManager().removeRelation(factionName, targetFaction, player.getUniqueId());
        if (success) {
            MessageManager.sendSuccess(player, "Removed relation with " + targetFaction + "!");
            plugin.getDataManager().saveFactionData();
        } else {
            MessageManager.sendError(player, "Failed to remove relation.");
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
            MessageManager.sendError(player, "You lack permission to manage faction relations.");
            return;
        }

        boolean success = plugin.getRelationManager().acceptRelationRequest(
                factionName, request.fromFaction, request.requestedRelation, player.getUniqueId());

        if (success) {
            MessageManager.sendSuccess(player,ChatColor.GREEN + "Accepted " +
                    RelationManager.getRelationColor(request.requestedRelation) +
                    request.requestedRelation.getDisplayName() + ChatColor.GREEN +
                    " request from " + request.fromFaction + "!");
            plugin.getDataManager().saveFactionData();
        } else {
            MessageManager.sendError(player, "Failed to accept request.");
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
            MessageManager.sendError(player,"You lack permission to manage faction relations.");
            return;
        }

        boolean success = plugin.getRelationManager().rejectRelationRequest(
                factionName, request.fromFaction, request.requestedRelation, player.getUniqueId());

        if (success) {
            MessageManager.sendInfo(player,"Rejected " +
                    RelationManager.getRelationColor(request.requestedRelation) +
                    request.requestedRelation.getDisplayName() + ChatColor.YELLOW +
                    " request from " + request.fromFaction + ".");
            plugin.getDataManager().saveFactionData();
        } else {
            MessageManager.sendError(player,"Failed to reject request.");
        }

        // Refresh the menu with current pages
        int relationsPage = getCurrentRelationsPage(player);
        int requestsPage = getCurrentRequestsPage(player);
        openRelationsViewMenu(player, factionName, relationsPage, requestsPage);
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

    public void handleNoFactionMenuClick(Player player, String displayName) {
        if (displayName.equals(ChatColor.GREEN + "Create Faction")) {
            player.closeInventory();
            plugin.getFactionCreationManager().openSignGUIForFactionCreation(player);
        } else if (displayName.equals(ChatColor.BLUE + "Browse Factions")) {
            plugin.getBrowserMenuHandler().openFactionBrowser(player); // This now opens the selection menu
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
        } else if (displayName.equals(ChatColor.LIGHT_PURPLE + "Homes & Warps")) {
            // NEW: Open homes and warps menu
            openHomesWarpsMenu(player, factionName);
        } else if (displayName.equals(ChatColor.LIGHT_PURPLE + "Invite Players")) {
            // Open invitation menu
            openInvitationMenu(player, factionName);
        } else if (displayName.equals(ChatColor.RED + "Faction Settings")) {
            // Open settings menu
            openFactionSettings(player, factionName);
        } else if (displayName.equals(ChatColor.AQUA + "Faction Info")) {
            Faction faction = factions.get(factionName);
            Rank playerRank = faction.members.get(player.getUniqueId());

            if (playerRank == Rank.OWNER || faction.hasPermission(playerRank, FactionPermission.OPEN_CLOSE)) {
                faction.isPublic = !faction.isPublic;
                MessageManager.sendFactionPrivacyToggle(player, faction.isPublic);

                // Save the data
                plugin.getDataManager().saveFactionData();

                // Reopen menu to show updated status
                openFactionMenu(player, factionName);
            } else {
                MessageManager.sendError(player,"You don't have permission to change faction visibility.");
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
     * Handle homes and warps menu clicks
     */
    public void handleHomesWarpsMenuClick(Player player, String displayName, String title) {
        String factionName = playerFactions.get(player.getUniqueId());

        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            openFactionMenu(player, factionName);
            return;
        }

        Faction faction = factions.get(factionName);
        if (faction == null) return;

        Rank playerRank = faction.members.get(player.getUniqueId());

        // Handle home teleport
        if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "FACTION HOME")) {
            if (!faction.hasPermission(playerRank, FactionPermission.USE_HOME)) {
                MessageManager.sendError(player, "You lack permission to use the faction home.");
                return;
            }

            player.closeInventory();

            // Use existing home manager to teleport
            plugin.getHomeManager().goHome(player);
            return;
        }

        // Handle warp teleport
        if (displayName.startsWith(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD)) {
            if (!faction.hasPermission(playerRank, FactionPermission.WARPS_ACCESS)) {
                MessageManager.sendError(player, "You lack permission to use warps.");
                return;
            }

            // Extract warp name from display name
            String warpName = ChatColor.stripColor(displayName).toLowerCase();

            player.closeInventory();

            // Use existing warp manager to teleport
            plugin.getWarpManager().warpTo(player, warpName);
            return;
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
            MessageManager.sendError(player, "Error: Invalid rank detected.");
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
                        MessageManager.sendError(player,"You cannot toggle " + permission.getDisplayName() +
                                " because your rank (" + playerRank.name() + ") doesn't have this permission.");
                    } else {
                        MessageManager.sendError(player,"You cannot modify permissions for this rank.");
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
                MessageManager.sendSuccess(player,(hasPermission ? "Enabled" : "Disabled") +
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
            MessageManager.sendError(player,"You lack permission to manage faction permissions.");
            return false;
        }

        // Cannot modify permissions for your own rank or higher (unless you're owner)
        if (playerRank != Rank.OWNER && targetRank.ordinal() >= playerRank.ordinal()) {
            if (targetRank == playerRank) {
                MessageManager.sendError(player,"You cannot modify permissions for your own rank.");
            } else {
                MessageManager.sendError(player,"You cannot modify permissions for ranks equal to or higher than yours.");
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
            MessageManager.sendError(player,"You cannot toggle " + permission.getDisplayName() +
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
                MessageManager.sendError(player,"Error: Invalid relation detected.");
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

                MessageManager.sendSuccess(player,"Toggled " + permission.getDisplayName() +
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
                MessageManager.sendError(player,"Player " + playerName + " is no longer online.");
                return;
            }

            String factionName = playerFactions.get(player.getUniqueId());
            UUID targetUUID = targetPlayer.getUniqueId();

            // Check if player is already in a faction
            if (playerFactions.containsKey(targetUUID)) {
                MessageManager.sendError(player,playerName + " is already in a faction.");
                return;
            }

            // Add invitation
            playerInvitations.putIfAbsent(targetUUID, new HashSet<>());
            Set<String> invites = playerInvitations.get(targetUUID);

            if (invites.contains(factionName)) {
                MessageManager.sendInfo(player,playerName + " has already been invited to " + factionName + ".");
                return;
            }

            invites.add(factionName);

            // Notify both players
            MessageManager.sendInvitationSent(player, playerName, factionName);
            MessageManager.sendInvitationReceived(targetPlayer, player.getName(), factionName);

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
            MessageManager.sendError(player, "You cannot kick a player of equal or higher rank.");
            return;
        }
        if (targetRank == Rank.OWNER) {
            MessageManager.sendError(player, "You cannot kick the faction owner.");
            return;
        }

        if (clickType == ClickType.LEFT) {
            // Open rank management GUI
            openRankManagementGUI(player, targetPlayer, factionName);
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
            MessageManager.sendError(player, "Error: No pending kick operation found.");
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
            MessageManager.sendKickSuccess(player, targetName, factionName);

            if (target.isOnline()) {
                Player onlineTarget = (Player) target;
                MessageManager.sendKicked(onlineTarget, factionName, player.getName());

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
            }.runTaskLater(plugin, 1L);

        } else if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "CANCEL")) {
            // Cancel kick
            pendingKicks.remove(kickerUUID);

            // Return to members menu
            openMembersMenu(player, factionName);
        }
    }

    /**
     * Open rank management GUI for a specific player
     */
    public void openRankManagementGUI(Player manager, OfflinePlayer target, String factionName) {
        Faction faction = factions.get(factionName);
        if (faction == null) return;

        String targetName = target.getName();
        if (targetName == null) targetName = "Unknown Player";

        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Manage: " + targetName);

        // Fill with black glass
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 27; i++) {
            menu.setItem(i, blackGlass);
        }

        Rank managerRank = faction.members.get(manager.getUniqueId());
        Rank targetRank = faction.members.get(target.getUniqueId());

        // ADD THESE LINES HERE - Store ranks for use in helper methods
        this.targetCurrentRank = targetRank;
        this.currentManagerRank = managerRank;

        // Create rank items (slots 11-15 for the 5 ranks)
        Rank[] ranks = {Rank.OWNER, Rank.ADMIN, Rank.MOD, Rank.MEMBER, Rank.RECRUIT};
        int[] slots = {11, 12, 13, 14, 15};

        for (int i = 0; i < ranks.length; i++) {
            Rank rank = ranks[i];
            boolean isCurrentRank = (targetRank == rank);
            boolean canPromoteTo = canPromoteToRank(managerRank, targetRank, rank, manager.getUniqueId().equals(target.getUniqueId()));

            ItemStack rankItem = createRankItem(rank, isCurrentRank, canPromoteTo, faction);
            menu.setItem(slots[i], rankItem);
        }

        // Target player info in center top
        ItemStack playerInfo = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerSkullMeta = (SkullMeta) playerInfo.getItemMeta();
        playerSkullMeta.setOwningPlayer(target);
        playerSkullMeta.setDisplayName(ChatColor.YELLOW + targetName);
        playerSkullMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Current Rank: " + ChatColor.WHITE + targetRank.name(),
                ChatColor.GRAY + "Status: " + (target.isOnline() ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline"),
                "",
                ChatColor.YELLOW + "Select a rank below to promote/demote"
        ));
        playerInfo.setItemMeta(playerSkullMeta);
        menu.setItem(4, playerInfo);

        // Back button
        ItemStack backButton = createBackButton();
        menu.setItem(18, backButton);

        manager.openInventory(menu);
    }

    /**
     * Create a rank item for the rank management GUI
     */
    private ItemStack createRankItem(Rank rank, boolean isCurrentRank, boolean canPromoteTo, Faction faction) {
        Material material;
        ChatColor nameColor;

        if (isCurrentRank) {
            material = Material.GREEN_CONCRETE;
            nameColor = ChatColor.GREEN;
        } else if (canPromoteTo) {
            material = Material.YELLOW_CONCRETE;
            nameColor = ChatColor.YELLOW;
        } else {
            material = Material.RED_CONCRETE;
            nameColor = ChatColor.RED;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String displayName = nameColor + "" + ChatColor.BOLD + rank.name();
        if (isCurrentRank) {
            displayName += ChatColor.GREEN + " (Current)";
        } else if (!canPromoteTo) {
            displayName += ChatColor.RED + " (Locked)";
        }

        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();

        if (isCurrentRank) {
            lore.add(ChatColor.GREEN + "✓ This is their current rank");
            lore.add("");
        }

        // Add rank description
        lore.addAll(getRankDescription(rank));
        lore.add("");

        // Add permission summary
        lore.add(ChatColor.GOLD + "Key Permissions:");
        lore.addAll(getKeyPermissions(rank, faction));

        if (!isCurrentRank) {
            lore.add("");
            if (canPromoteTo) {
                lore.add(ChatColor.YELLOW + "Click to " + (rank.ordinal() > getTargetCurrentRank().ordinal() ? "promote" : "demote") + " to " + rank.name());
            } else {
                lore.add(ChatColor.RED + "You cannot assign this rank");
                if (rank.ordinal() >= getCurrentManagerRank().ordinal()) {
                    lore.add(ChatColor.GRAY + "Reason: Equal or higher than your rank");
                }
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Get rank description
     */
    private List<String> getRankDescription(Rank rank) {
        List<String> desc = new ArrayList<>();

        switch (rank) {
            case OWNER:
                desc.add(ChatColor.LIGHT_PURPLE + "Full control of the faction");
                desc.add(ChatColor.GRAY + "• Can do anything in the faction");
                desc.add(ChatColor.GRAY + "• Can transfer ownership");
                desc.add(ChatColor.GRAY + "• Cannot be kicked or demoted");
                break;
            case ADMIN:
                desc.add(ChatColor.BLUE + "High-level faction management");
                desc.add(ChatColor.GRAY + "• Can manage most faction settings");
                desc.add(ChatColor.GRAY + "• Can promote/demote lower ranks");
                desc.add(ChatColor.GRAY + "• Can kick lower ranked members");
                break;
            case MOD:
                desc.add(ChatColor.GREEN + "Moderate faction activities");
                desc.add(ChatColor.GRAY + "• Can invite and kick members");
                desc.add(ChatColor.GRAY + "• Can claim and unclaim land");
                desc.add(ChatColor.GRAY + "• Can manage warps and spawners");
                break;
            case MEMBER:
                desc.add(ChatColor.YELLOW + "Trusted faction member");
                desc.add(ChatColor.GRAY + "• Can access faction chest");
                desc.add(ChatColor.GRAY + "• Can use and set home");
                desc.add(ChatColor.GRAY + "• Can deposit and withdraw from bank");
                break;
            case RECRUIT:
                desc.add(ChatColor.WHITE + "New faction member");
                desc.add(ChatColor.GRAY + "• Basic building permissions");
                desc.add(ChatColor.GRAY + "• Can use faction home");
                desc.add(ChatColor.GRAY + "• Can deposit to bank");
                break;
        }

        return desc;
    }

    /**
     * Get key permissions for a rank
     */
    private List<String> getKeyPermissions(Rank rank, Faction faction) {
        List<String> perms = new ArrayList<>();
        Set<FactionPermission> rankPerms = faction.rankPermissions.getOrDefault(rank, EnumSet.noneOf(FactionPermission.class));

        // Show most important permissions (limit to 4-5 for space)
        List<FactionPermission> importantPerms = Arrays.asList(
                FactionPermission.INVITE_MEMBERS,
                FactionPermission.KICK_MEMBERS,
                FactionPermission.PROMOTE_MEMBERS,
                FactionPermission.CLAIM_LAND,
                FactionPermission.MANAGE_PERMISSIONS
        );

        int count = 0;
        for (FactionPermission perm : importantPerms) {
            if (count >= 4) break;
            if (rankPerms.contains(perm)) {
                perms.add(ChatColor.GREEN + "✓ " + ChatColor.GRAY + perm.getDisplayName());
                count++;
            }
        }

        if (count == 0) {
            perms.add(ChatColor.GRAY + "Basic building and interaction");
        }

        return perms;
    }

    /**
     * Check if manager can promote target to a specific rank
     */
    private boolean canPromoteToRank(Rank managerRank, Rank targetRank, Rank newRank, boolean isSelf) {
        // Can't promote/demote yourself
        if (isSelf) return false;

        // Owners can do anything
        if (managerRank == Rank.OWNER) return true;

        // Can't promote to your own rank or higher
        if (newRank.ordinal() >= managerRank.ordinal()) return false;

        // Can only modify players below your rank
        if (targetRank.ordinal() >= managerRank.ordinal()) return false;

        return true;
    }

    /**
     * Handle rank management GUI clicks
     */
    public void handleRankManagementClick(Player manager, String displayName, String title) {
        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            String factionName = playerFactions.get(manager.getUniqueId());
            openMembersMenu(manager, factionName);
            return;
        }

        // Extract target player name from title
        String targetName = title.replace(ChatColor.DARK_GRAY + "Manage: ", "");

        // Find the target player (online or offline)
        OfflinePlayer target = null;

        // First try to find online player
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        if (onlineTarget != null) {
            target = onlineTarget;
        } else {
            // Try to find offline player by searching faction members
            String factionName = playerFactions.get(manager.getUniqueId());
            Faction faction = factions.get(factionName);
            if (faction != null) {
                for (UUID memberUUID : faction.members.keySet()) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUUID);
                    if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(targetName)) {
                        target = offlinePlayer;
                        break;
                    }
                }
            }
        }

        if (target == null) {
            MessageManager.sendError(manager, "Could not find player: " + targetName);
            return;
        }

        String factionName = playerFactions.get(manager.getUniqueId());
        Faction faction = factions.get(factionName);

        if (faction == null) {
            MessageManager.sendError(manager, "Faction not found.");
            return;
        }

        Rank managerRank = faction.members.get(manager.getUniqueId());
        Rank targetRank = faction.members.get(target.getUniqueId());

        // Verify target is still in the faction
        if (targetRank == null) {
            MessageManager.sendError(manager, targetName + " is no longer in your faction.");
            return;
        }

        // Parse clicked rank from display name
        Rank clickedRank = null;
        for (Rank rank : Rank.values()) {
            if (displayName.contains(rank.name())) {
                clickedRank = rank;
                break;
            }
        }

        if (clickedRank == null) {
            // Not a rank button, ignore
            return;
        }

        // Check if this is current rank (no action needed)
        if (targetRank == clickedRank) {
            MessageManager.sendInfo(manager, targetName + " is already " + clickedRank.name() + "!");
            return;
        }

        // Check permissions using the existing helper method
        if (!canPromoteToRank(managerRank, targetRank, clickedRank, manager.getUniqueId().equals(target.getUniqueId()))) {
            if (manager.getUniqueId().equals(target.getUniqueId())) {
                MessageManager.sendError(manager, "You cannot change your own rank!");
            } else if (clickedRank.ordinal() >= managerRank.ordinal()) {
                MessageManager.sendError(manager, "You cannot promote " + targetName + " to " + clickedRank.name() + " (equal or higher than your rank)!");
            } else if (targetRank.ordinal() >= managerRank.ordinal()) {
                MessageManager.sendError(manager, "You cannot modify the rank of " + targetName + " (they are equal or higher rank than you)!");
            } else {
                MessageManager.sendError(manager, "You cannot assign the rank " + clickedRank.name() + " to " + targetName + "!");
            }
            return;
        }

        // Special handling for promoting to OWNER (ownership transfer)
        if (clickedRank == Rank.OWNER && managerRank == Rank.OWNER && targetRank == Rank.ADMIN) {
            // Initiate ownership transfer confirmation
            pendingOwnershipTransfersGUI.put(manager.getUniqueId(), targetName);

            // Close current GUI and show ownership transfer warning
            manager.closeInventory();
            MessageManager.sendOwnerTransferWarning(manager, targetName, factionName);

            // Schedule expiration of the confirmation
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (pendingOwnershipTransfersGUI.containsKey(manager.getUniqueId())) {
                    pendingOwnershipTransfersGUI.remove(manager.getUniqueId());
                    if (manager.isOnline()) {
                        MessageManager.sendError(manager, "Ownership transfer confirmation expired.");
                    }
                }
            }, 20L * 30); // 30 seconds

            return;
        } else if (clickedRank == Rank.OWNER && managerRank == Rank.OWNER) {
            MessageManager.sendError(manager, "Can only promote ADMINs to OWNER. " + targetName + " must be ADMIN first.");
            return;
        }

        // Perform the rank change
        faction.members.put(target.getUniqueId(), clickedRank);

        // Determine if this was a promotion or demotion
        boolean isPromotion = clickedRank.ordinal() > targetRank.ordinal();
        String action = isPromotion ? "promoted" : "demoted";

        // Send success message to manager
        MessageManager.sendSuccess(manager, "Successfully " + action + " " + targetName + " from " +
                ChatColor.WHITE + targetRank.name() + ChatColor.GREEN + " to " + ChatColor.WHITE + clickedRank.name() + ChatColor.GREEN + "!");

        // Send message to target if they're online
        if (target.isOnline()) {
            Player onlineTargetPlayer = (Player) target;
            if (isPromotion) {
                MessageManager.sendPromoted(onlineTargetPlayer, clickedRank.name(), manager.getName());
            } else {
                MessageManager.sendDemoted(onlineTargetPlayer, clickedRank.name(), manager.getName());
            }
        }

        // Notify other online faction members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(manager) && !member.getUniqueId().equals(target.getUniqueId())) {
                MessageManager.sendMemberInfoMessage(member, targetName + " was " + action + " to " +
                        clickedRank.name() + " by " + manager.getName());
            }
        }

        // Save data
        plugin.getDataManager().saveFactionData();

        // Reopen the same GUI to reflect changes (DO NOT CLOSE)
        openRankManagementGUI(manager, target, factionName);
    }

    /**
     * Handle ownership transfer confirmation from GUI context
     */
    public boolean handleGUIOwnershipTransfer(Player player, String targetName, String factionName) {
        UUID uuid = player.getUniqueId();

        // Check if there's a pending transfer
        String pendingTarget = pendingOwnershipTransfersGUI.get(uuid);
        if (pendingTarget == null || !pendingTarget.equals(targetName)) {
            MessageManager.sendError(player, "No pending ownership transfer found or confirmation expired.");
            return false;
        }

        // Remove the pending transfer
        pendingOwnershipTransfersGUI.remove(uuid);

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            MessageManager.sendError(player, "Target player " + targetName + " is no longer online.");
            return false;
        }

        UUID targetUUID = target.getUniqueId();

        // Verify target is still in the faction and is ADMIN
        if (!factionName.equals(playerFactions.get(targetUUID))) {
            MessageManager.sendError(player, targetName + " is no longer in your faction.");
            return false;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);
        Rank targetRank = faction.members.get(targetUUID);

        if (playerRank != Rank.OWNER) {
            MessageManager.sendError(player, "You are no longer the faction owner.");
            return false;
        }

        if (targetRank != Rank.ADMIN) {
            MessageManager.sendError(player, targetName + " is no longer an Admin.");
            return false;
        }

        // Perform the ownership transfer
        faction.members.put(targetUUID, Rank.OWNER); // Promote target to OWNER
        faction.members.put(uuid, Rank.ADMIN);       // Demote current owner to ADMIN
        faction.owner = targetUUID;                  // Update faction owner field

        // Send messages
        MessageManager.sendOwnerTransferSuccess(player, target, factionName);

        // Notify all other faction members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(player) && !member.equals(target)) {
                MessageManager.sendMemberOwnerTransferSuccess(member, player, targetName, factionName);
            }
        }

        // Save data
        plugin.getDataManager().saveFactionData();

        return true;
    }

    /**
     * Clear pending ownership transfer (for cleanup)
     */
    public void clearPendingOwnershipTransfer(UUID playerUUID) {
        pendingOwnershipTransfersGUI.remove(playerUUID);
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
                MessageManager.sendFactionCreated(player, factionName);

                // REMOVED: Automatic menu opening
                // No longer automatically opens the faction menu

            } else if (factions.containsKey(factionName)) {
                MessageManager.sendError(player, "A faction with that name already exists!");
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

            MessageManager.sendInfo(player, "Faction creation cancelled.");
        }
    }

    /**
     * Handle reset permissions button clicks
     */
    public void handleResetPermissionsClick(Player player, String title, boolean isRankPermissions) {
        String factionName = playerFactions.get(player.getUniqueId());
        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(player.getUniqueId());

        // Check if player has permission to manage permissions
        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.MANAGE_PERMISSIONS)) {
            MessageManager.sendError(player, "You lack permission to reset faction permissions.");
            return;
        }

        if (isRankPermissions) {
            // Extract rank from title (e.g. "ADMIN Permissions" -> "ADMIN")
            String rankName = title.replace(ChatColor.DARK_GRAY + "", "").replace(" Permissions", "");
            Rank targetRank;
            try {
                targetRank = Rank.valueOf(rankName);
            } catch (IllegalArgumentException e) {
                MessageManager.sendError(player, "Error: Invalid rank detected.");
                return;
            }

            // Check if player can modify this rank's permissions
            if (!canModifyRankPermissionsCheck(playerRank, targetRank, faction)) {
                if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.MANAGE_PERMISSIONS)) {
                    MessageManager.sendError(player, "You lack permission to manage faction permissions.");
                } else if (targetRank.ordinal() >= playerRank.ordinal()) {
                    if (targetRank == playerRank) {
                        MessageManager.sendError(player, "You cannot reset permissions for your own rank.");
                    } else {
                        MessageManager.sendError(player, "You cannot reset permissions for ranks equal to or higher than yours.");
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
                    MessageManager.sendError(player, "Error: Invalid relation detected.");
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
            MessageManager.sendSuccess(player,"Successfully reset " + permissionsReset.size() +
                    " permissions for " + targetRank.name() + " rank to default!");

            if (permissionsReset.size() <= 5) {
                // Show individual permissions if not too many
                for (FactionPermission perm : permissionsReset) {
                    boolean enabled = defaultPermissions.contains(perm);
                    MessageManager.sendBasicMessage(player, ChatColor.WHITE + "  • " + perm.getDisplayName() + ": " +
                            (enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                }
            }
        }

        if (!permissionsSkipped.isEmpty()) {
            MessageManager.sendInfo(player, "Skipped " + permissionsSkipped.size() +
                    " permissions you don't have access to:");

            if (permissionsSkipped.size() <= 5) {
                for (FactionPermission perm : permissionsSkipped) {
                    MessageManager.sendBasicMessage(player, ChatColor.WHITE + "  • " + ChatColor.RED + perm.getDisplayName() +
                            ChatColor.GRAY + " (you lack this permission)");
                }
            }
        }

        if (permissionsReset.isEmpty() && permissionsSkipped.isEmpty()) {
            MessageManager.sendInfo(player, targetRank.name() + " rank permissions are already at default (or you cannot modify any of them).");
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
            MessageManager.sendSuccess(player, "Successfully reset " + permissionsReset.size() +
                    " permissions for " + relation.getDisplayName() + " relations to default!");

            if (permissionsReset.size() <= 5) {
                // Show individual permissions if not too many
                for (RelationPermission perm : permissionsReset) {
                    boolean enabled = defaultPermissions.contains(perm);
                    MessageManager.sendBasicMessage(player, ChatColor.WHITE + "  • " + perm.getDisplayName() + ": " +
                            (enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                }
            }
        }

        if (!permissionsSkipped.isEmpty()) {
            MessageManager.sendInfo(player, "Skipped " + permissionsSkipped.size() +
                    " permissions you don't have equivalent access to:");

            if (permissionsSkipped.size() <= 5) {
                for (RelationPermission perm : permissionsSkipped) {
                    FactionPermission equiv = getEquivalentFactionPermission(perm);
                    String reason = equiv != null ? "you lack " + equiv.getDisplayName() : "no equivalent permission";
                    MessageManager.sendBasicMessage(player, ChatColor.WHITE + "  • " + ChatColor.RED + perm.getDisplayName() +
                            ChatColor.GRAY + " (" + reason + ")");
                }
            }
        }

        if (permissionsReset.isEmpty() && permissionsSkipped.isEmpty()) {
            MessageManager.sendInfo(player, relation.getDisplayName() + " relation permissions are already at default (or you cannot modify any of them).");
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
            MessageManager.sendFactionLeft(player, factionName);

            plugin.getEventListener().onPlayerLeaveFaction(player);

            // Notify other online faction members
            for (UUID memberUUID : faction.members.keySet()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null) {
                    MessageManager.sendMemberLeft(member, player.getName());
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

    /**
     * Get claims per world for a faction
    */
    private Map<String, Integer> getFactionsClaimsPerWorld(String factionName) {
        // Use LinkedHashMap to preserve insertion order
        Map<String, Integer> claimsPerWorld = new LinkedHashMap<>();

        // Define the desired world order
        String[] worldOrder = {"world", "world2", "world_nether", "world_the_end"};

        // Process worlds in the specified order
        for (String worldName : worldOrder) {
            Map<ChunkCoord, String> worldClaims = plugin.getWorldClaims().get(worldName);
            if (worldClaims == null) continue; // Skip if world doesn't exist

            int claimCount = 0;
            for (String claimOwner : worldClaims.values()) {
                if (factionName.equals(claimOwner)) {
                    claimCount++;
                }
            }

            if (claimCount > 0) {
                // Get display name from RTP plugin
                String displayName = getWorldDisplayName(worldName);
                claimsPerWorld.put(displayName, claimCount);
            }
        }

        // Handle any additional worlds not in our predefined order
        // (in case there are custom worlds added later)
        for (Map.Entry<String, Map<ChunkCoord, String>> worldEntry : plugin.getWorldClaims().entrySet()) {
            String actualWorldName = worldEntry.getKey();

            // Skip if we already processed this world
            boolean alreadyProcessed = false;
            for (String orderedWorld : worldOrder) {
                if (orderedWorld.equals(actualWorldName)) {
                    alreadyProcessed = true;
                    break;
                }
            }
            if (alreadyProcessed) continue;

            Map<ChunkCoord, String> worldClaims = worldEntry.getValue();
            int claimCount = 0;
            for (String claimOwner : worldClaims.values()) {
                if (factionName.equals(claimOwner)) {
                    claimCount++;
                }
            }

            if (claimCount > 0) {
                // Get display name from RTP plugin
                String displayName = getWorldDisplayName(actualWorldName);
                claimsPerWorld.put(displayName, claimCount);
            }
        }

        return claimsPerWorld;
    }

    // Helper method to get world display name from RTP plugin
    private String getWorldDisplayName(String worldName) {
        if (rtpPlugin != null) {
            WorldInfo worldInfo = rtpPlugin.worlds.get(worldName);
            if (worldInfo != null) {
                return ChatColor.stripColor(worldInfo.getDisplayName()); // Strip color codes for clean display
            }
        }

        // Fallback mapping if RTP plugin is not available
        switch (worldName.toLowerCase()) {
            case "world":
                return "Earth";
            case "world2":
                return "Fire Planet";
            case "world_nether":
                return "Hell";
            case "world_the_end":
                return "End";
            default:
                return worldName; // Return original name as last resort
        }
    }

    /**
     * Open the homes and warps teleportation menu
     */
    public void openHomesWarpsMenu(Player player, String factionName) {
        Faction faction = factions.get(factionName);
        if (faction == null) return;

        Rank playerRank = faction.members.get(player.getUniqueId());
        boolean canUseHome = faction.hasPermission(playerRank, FactionPermission.USE_HOME);
        boolean canUseWarps = faction.hasPermission(playerRank, FactionPermission.WARPS_ACCESS);

        // Calculate menu size based on available items
        int itemCount = 0;
        if (canUseHome && faction.home != null) itemCount++;
        if (canUseWarps) itemCount += faction.warps.size();

        // Use appropriate inventory size
        int size = 27; // Default to 3 rows
        if (itemCount > 18) size = 54; // 6 rows if more than 18 items
        else if (itemCount > 9) size = 36; // 4 rows if more than 9 items

        Inventory menu = Bukkit.createInventory(null, size, ChatColor.DARK_GRAY + "Homes & Warps");

        // Fill bottom row with black glass panes
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        // Fill bottom row
        for (int i = size - 9; i < size; i++) {
            menu.setItem(i, blackGlass);
        }

        // Back button
        ItemStack backButton = createBackButton();
        menu.setItem(size - 9, backButton); // Bottom left

        int slot = 0;

        // Add home if available and player has permission
        if (canUseHome && faction.home != null) {
            ItemStack homeItem = createHomeItem(faction.home);
            menu.setItem(slot++, homeItem);
        }

        // Add warps if player has permission
        if (canUseWarps && !faction.warps.isEmpty()) {
            for (Map.Entry<String, FactionWarp> warpEntry : faction.warps.entrySet()) {
                if (slot >= size - 9) break; // Don't overwrite navigation row

                ItemStack warpItem = createWarpItem(warpEntry.getValue());
                menu.setItem(slot++, warpItem);
            }
        }

        // If no items to show
        if (slot == 0) {
            ItemStack noItems = new ItemStack(Material.BARRIER);
            ItemMeta noItemsMeta = noItems.getItemMeta();
            noItemsMeta.setDisplayName(ChatColor.RED + "No Teleportation Available");

            List<String> lore = new ArrayList<>();
            if (!canUseHome && !canUseWarps) {
                lore.add(ChatColor.GRAY + "You lack permission to use");
                lore.add(ChatColor.GRAY + "homes and warps");
            } else {
                lore.add(ChatColor.GRAY + "No homes or warps are set");
                if (faction.hasPermission(playerRank, FactionPermission.SET_HOME)) {
                    lore.add(ChatColor.GRAY + "Use /f sethome to set home");
                }
                if (faction.hasPermission(playerRank, FactionPermission.MANAGE_WARPS)) {
                    lore.add(ChatColor.GRAY + "Use /f setwarp <name> to set warps");
                }
            }

            noItemsMeta.setLore(lore);
            noItems.setItemMeta(noItemsMeta);
            menu.setItem(13, noItems); // Center of menu
        }

        player.openInventory(menu);
    }

    /**
     * Create home item for the teleportation menu
     */
    private ItemStack createHomeItem(FactionHome home) {
        ItemStack item = new ItemStack(Material.RED_BED);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "FACTION HOME");

        List<String> lore = new ArrayList<>();

        // Use display name from RTP plugin
        String actualWorldName = home.getWorldName();
        String displayWorldName = getWorldDisplayName(actualWorldName);

        lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + displayWorldName);
        lore.add(ChatColor.GRAY + "Coordinates: " + ChatColor.WHITE +
                (int)home.getX() + ", " + (int)home.getY() + ", " + (int)home.getZ());

        // Add world type info from RTP plugin
        String worldType = getWorldType(actualWorldName);
        lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + worldType);

        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to teleport home!");

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Create warp item for the teleportation menu
     */
    private ItemStack createWarpItem(FactionWarp warp) {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + warp.getName().toUpperCase());

        List<String> lore = new ArrayList<>();

        // Use display name from RTP plugin
        String actualWorldName = warp.getWorldName();
        String displayWorldName = getWorldDisplayName(actualWorldName);

        lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + displayWorldName);
        lore.add(ChatColor.GRAY + "Coordinates: " + ChatColor.WHITE +
                (int)warp.getX() + ", " + (int)warp.getY() + ", " + (int)warp.getZ());

        // Add world type info from RTP plugin
        String worldType = getWorldType(actualWorldName);
        lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + worldType);

        // Add creation info
        String creatorName = Bukkit.getOfflinePlayer(warp.getCreatedBy()).getName();
        if (creatorName != null) {
            lore.add(ChatColor.GRAY + "Created by: " + ChatColor.WHITE + creatorName);
        }

        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to teleport to warp!");

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Get world type based on world name
     */
    private String getWorldType(String worldName) {
        if (rtpPlugin != null) {
            WorldInfo worldInfo = rtpPlugin.worlds.get(worldName);
            if (worldInfo != null) {
                return worldInfo.getWorldType();
            }
        }

        // Fallback mapping
        switch (worldName.toLowerCase()) {
            case "world":
                return "Overworld";
            case "world2":
                return "Overworld";
            case "world_nether":
                return "Nether";
            case "world_the_end":
                return "The End";
            default:
                // Try to determine by world environment
                org.bukkit.World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    switch (world.getEnvironment()) {
                        case NORMAL:
                            return "Overworld";
                        case NETHER:
                            return "Nether";
                        case THE_END:
                            return "End";
                        default:
                            return "Unknown";
                    }
                }
                return "Unknown";
        }
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

    private Rank getTargetCurrentRank() {
        return targetCurrentRank;
    }

    private Rank getCurrentManagerRank() {
        return currentManagerRank;
    }

    /**
     * Get pending GUI ownership transfer target name
     */
    public String getPendingGUIOwnershipTransfer(UUID playerUUID) {
        return pendingOwnershipTransfersGUI.get(playerUUID);
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