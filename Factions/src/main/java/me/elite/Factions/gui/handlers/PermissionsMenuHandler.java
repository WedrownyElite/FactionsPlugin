package me.elite.Factions.gui.handlers;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.gui.MenuHandler;
import me.elite.Factions.data.*;
import me.elite.Factions.utils.MessageManager;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.EnumSet;
import java.lang.reflect.Field;
import java.util.*;

public class PermissionsMenuHandler extends BaseMenuHandler {

    public PermissionsMenuHandler(FactionsPlugin plugin, MenuHandler parentHandler) {
        super(plugin, parentHandler);
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
     * Handle permissions GUI clicks
     */
    public void handlePermissionsGUIClick(Player player, String displayName, String title) {
        String factionName = playerFactions.get(player.getUniqueId());

        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            parentHandler.settingsHandler.openFactionSettings(player, factionName);
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

    /**
     * Get the equivalent faction permission for a relation permission
     */
    protected  FactionPermission getEquivalentFactionPermission(RelationPermission relationPermission) {
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
}
