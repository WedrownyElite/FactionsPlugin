package me.elite.Factions.gui.handlers;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.gui.MenuHandler;
import me.elite.Factions.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class SettingsMenuHandler extends BaseMenuHandler {

    public SettingsMenuHandler(FactionsPlugin plugin, MenuHandler parentHandler) {
        super(plugin, parentHandler);
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
     * Handle settings menu clicks
     */
    public void handleSettingsMenuClick(Player player, String displayName, String title) {
        String factionName = playerFactions.get(player.getUniqueId());

        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            parentHandler.openFactionMenu(player, factionName);
        } else if (displayName.equals(ChatColor.RED + "Faction Permissions")) {
            parentHandler.openPermissionsGUI(player, factionName);
        }
    }

    /**
     * Handle invitation menu clicks
     */
    public void handleInvitationMenuClick(Player player, String displayName, String title) {
        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            String factionName = playerFactions.get(player.getUniqueId());
            parentHandler.openFactionMenu(player, factionName);
            return;
        }

        // Handle player invitation clicks
        if (displayName.startsWith(ChatColor.GREEN.toString())) {
            String playerName = ChatColor.stripColor(displayName);
            Player targetPlayer = Bukkit.getPlayerExact(playerName);

            if (targetPlayer == null) {
                MessageManager.sendError(player, "Player " + playerName + " is no longer online.");
                return;
            }

            String factionName = playerFactions.get(player.getUniqueId());
            UUID targetUUID = targetPlayer.getUniqueId();

            // Check if player is already in a faction
            if (playerFactions.containsKey(targetUUID)) {
                MessageManager.sendError(player, playerName + " is already in a faction.");
                return;
            }

            // Add invitation
            playerInvitations.putIfAbsent(targetUUID, new HashSet<>());
            Set<String> invites = playerInvitations.get(targetUUID);

            if (invites.contains(factionName)) {
                MessageManager.sendInfo(player, playerName + " has already been invited to " + factionName + ".");
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
}