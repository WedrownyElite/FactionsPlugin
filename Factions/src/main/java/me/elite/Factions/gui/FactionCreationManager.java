package me.elite.Factions.gui;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.Rank;
import me.elite.Factions.constants.FactionsConstants;
import me.elite.Factions.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class FactionCreationManager {
    private final FactionsPlugin plugin;
    private final Map<String, Faction> factions;
    private final Map<UUID, String> playerFactions;
    private final Map<UUID, Boolean> pendingFactionCreation;
    private final Map<UUID, String> pendingFactionNames;

    public FactionCreationManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
        this.pendingFactionCreation = plugin.getPendingFactionCreation();
        this.pendingFactionNames = plugin.getPendingFactionNames();
    }

    /**
     * Open the faction creation interface for the player
     */
    public void openSignGUIForFactionCreation(Player player) {
        // Mark player as pending faction creation
        pendingFactionCreation.put(player.getUniqueId(), true);

        // Get a location in front of the player to place the sign temporarily
        Location signLocation = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        signLocation.setY(Math.max(1, signLocation.getY())); // Ensure it's above bedrock

        // Save the original block
        org.bukkit.block.Block originalBlock = signLocation.getBlock();
        Material originalMaterial = originalBlock.getType();

        // Place a temporary sign
        originalBlock.setType(Material.OAK_WALL_SIGN);

        // Get the sign block state
        BlockState blockState = originalBlock.getState();
        if (blockState instanceof Sign) {
            Sign sign = (Sign) blockState;
            sign.setLine(0, "Enter faction name:");
            sign.setLine(1, "");
            sign.setLine(2, "Line 2 - Don't edit");
            sign.setLine(3, "Line 3 - Don't edit");
            sign.update();

            // Open the sign editor for the player
            player.sendBlockChange(originalBlock.getLocation(), originalBlock.getBlockData());

            player.closeInventory();
            MessageManager.sendGUIFactionCreation(player);

            // Clean up the temporary block
            new BukkitRunnable() {
                @Override
                public void run() {
                    originalBlock.setType(originalMaterial);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    /**
     * Open the faction creation confirmation menu
     */
    public void openFactionCreationConfirmation(Player player, String factionName) {
        Inventory confirmMenu = Bukkit.createInventory(null, FactionsConstants.MEDIUM_GUI_SIZE(), ChatColor.DARK_GRAY + "Confirm: " + factionName);

        // Fill with black glass
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < FactionsConstants.MEDIUM_GUI_SIZE(); i++) {
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

    /**
     * Handle confirmation menu clicks
     */
    public void handleConfirmationMenuClick(Player player, String displayName, String title) {
        UUID uuid = player.getUniqueId();
        String factionName = pendingFactionNames.get(uuid);

        if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "CONFIRM")) {
            // Create the faction
            if (factionName != null && !factions.containsKey(factionName)) {
                Faction f = new Faction(factionName, uuid);
                f.members.put(uuid, Rank.OWNER);
                factions.put(factionName, f);
                playerFactions.put(uuid, factionName);

                player.closeInventory();
                MessageManager.sendFactionCreated(player, factionName);


            } else if (factions.containsKey(factionName)) {
                MessageManager.sendError(player, "A faction with that name already exists!");
                player.closeInventory();
            }

            // Clean up
            pendingFactionCreation.remove(uuid);
            pendingFactionNames.remove(uuid);

        } else if (displayName.equals(ChatColor.RED + "" + ChatColor.BOLD + "CANCEL")) {
            // Cancel creation
            pendingFactionCreation.remove(uuid);
            pendingFactionNames.remove(uuid);
            player.closeInventory();

            // CHANGED: Don't automatically open the menu, let player decide
            MessageManager.sendInfo(player, "Faction creation cancelled.");
        }
    }
}