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
import me.elite.Factions.gui.handlers.BaseMenuHandler;
import me.elite.Factions.gui.handlers.ConfirmationMenuHandler;
import me.elite.Factions.gui.handlers.MembersMenuHandler;
import me.elite.Factions.gui.handlers.PermissionsMenuHandler;
import me.elite.Factions.gui.handlers.SettingsMenuHandler;

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
    public final PermissionsMenuHandler permissionsHandler;
    public final MembersMenuHandler membersHandler;
    public final ConfirmationMenuHandler confirmationHandler;
    public final SettingsMenuHandler settingsHandler;

    // Track pending kick confirmations
    private final Map<UUID, UUID> pendingKicks = new HashMap<>(); // kicker -> target

    public MenuHandler(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
        this.playerInvitations = plugin.getPlayerInvitations();
        this.rtpPlugin = (RTPPlugin) Bukkit.getPluginManager().getPlugin("RTPPlugin");

        this.permissionsHandler = new PermissionsMenuHandler(plugin, this);
        this.membersHandler = new MembersMenuHandler(plugin, this);
        this.confirmationHandler = new ConfirmationMenuHandler(plugin, this);
        this.settingsHandler = new SettingsMenuHandler(plugin, this);
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
            settingsHandler.openInvitationMenu(player, factionName);
        } else if (displayName.equals(ChatColor.RED + "Faction Settings")) {
            // Open settings menu
            settingsHandler.openFactionSettings(player, factionName);
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
     * Clear pending kick for a player
     */
    public void clearPendingKick(UUID kickerUUID) {
        pendingKicks.remove(kickerUUID);
    }

    public void openPermissionsGUI(Player player, String factionName) {
        permissionsHandler.openPermissionsGUI(player, factionName);
    }

    public void openMembersMenu(Player player, String factionName) {
        membersHandler.openMembersMenu(player, factionName);
    }

    public void openLeaveConfirmation(Player player, String factionName) {
        confirmationHandler.openLeaveConfirmation(player, factionName);
    }
}