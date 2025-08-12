package me.elite.Factions.gui.handlers;

import me.elite.Factions.data.Faction;
import me.elite.Factions.data.FactionPermission;
import me.elite.Factions.data.Rank;
import me.elite.Factions.utils.MessageManager;
import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.gui.MenuHandler;

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

import java.util.*;

public class MembersMenuHandler extends BaseMenuHandler {

    public MembersMenuHandler(FactionsPlugin plugin, MenuHandler parentHandler) {
        super(plugin, parentHandler);
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

    private void openDisbandConfirmation(Player player, String factionName) {
        parentHandler.confirmationHandler.openDisbandConfirmation(player, factionName);
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

    public void handleMembersMenuClick(Player player, ItemStack item, ClickType clickType) {
        if (item.getItemMeta() == null) return;
        String displayName = item.getItemMeta().getDisplayName();

        // Handle back button click
        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            // Go back to faction menu
            String factionName = playerFactions.get(player.getUniqueId());
            if (factionName != null) {
                parentHandler.openFactionMenu(player, factionName);
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
     * Clear pending ownership transfer (for cleanup)
     */
    public void clearPendingOwnershipTransfer(UUID playerUUID) {
        pendingOwnershipTransfersGUI.remove(playerUUID);
    }

    /**
     * Get pending GUI ownership transfer target name
     */
    public String getPendingGUIOwnershipTransfer(UUID playerUUID) {
        return pendingOwnershipTransfersGUI.get(playerUUID);
    }
}
