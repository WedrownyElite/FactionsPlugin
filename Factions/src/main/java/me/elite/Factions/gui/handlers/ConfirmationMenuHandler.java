package me.elite.Factions.gui.handlers;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.Rank;
import me.elite.Factions.gui.MenuHandler;
import me.elite.Factions.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.UUID;

public class ConfirmationMenuHandler extends BaseMenuHandler {

    public ConfirmationMenuHandler(FactionsPlugin plugin, MenuHandler parentHandler) {
        super(plugin, parentHandler);
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
            parentHandler.openFactionMenu(player, factionName);
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
            parentHandler.openFactionMenu(player, factionName);
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
}