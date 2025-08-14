package me.elite.Factions.gui.handlers;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.BankLog;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.FactionPermission;
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

import java.util.*;

public class BankLogsMenuHandler extends BaseMenuHandler {
    private static final int LOGS_PER_PAGE = 36; // 4 rows of 9 slots each (excluding navigation)

    public BankLogsMenuHandler(FactionsPlugin plugin, MenuHandler parentHandler) {
        super(plugin, parentHandler);
    }

    /**
     * Open the bank logs GUI for a faction
     */
    public void openBankLogsGUI(Player player, String factionName, int page) {
        Faction faction = factions.get(factionName);
        if (faction == null) {
            MessageManager.sendError(player, "Faction not found.");
            return;
        }

        Rank playerRank = faction.members.get(player.getUniqueId());

        // Check permissions
        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.BANK_LOGS)) {
            MessageManager.sendError(player, "You lack permission to view bank logs.");
            return;
        }

        // Get logs grouped by date
        Map<String, List<BankLog>> logsGroupedByDate = plugin.getBankLogsManager().getBankLogsGroupedByDate(factionName);

        // Create a flat list for pagination (dates + logs)
        List<Object> displayItems = createDisplayItemsList(logsGroupedByDate);

        // Calculate pagination
        int totalPages = Math.max(1, (displayItems.size() - 1) / LOGS_PER_PAGE + 1);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int startIndex = page * LOGS_PER_PAGE;
        int endIndex = Math.min(startIndex + LOGS_PER_PAGE, displayItems.size());

        // Create inventory
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Bank Logs: " + factionName);

        // Fill with black glass panes
        ItemStack blackGlass = createGlassPaneFiller();
        for (int i = 0; i < 54; i++) {
            menu.setItem(i, blackGlass);
        }

        // Add display items
        int slot = 0;
        for (int i = startIndex; i < endIndex && slot < 36; i++) {
            Object item = displayItems.get(i);

            if (item instanceof String) {
                // Date separator
                menu.setItem(slot, createDateSeparatorItem((String) item));
            } else if (item instanceof BankLog) {
                // Bank log entry
                menu.setItem(slot, createBankLogItem((BankLog) item));
            }
            slot++;
        }

        // Navigation items in bottom rows (slots 36-53)

        // Back button (slot 45 - bottom left)
        ItemStack backButton = createBackButton();
        menu.setItem(45, backButton);

        // Page navigation
        if (totalPages > 1) {
            // Previous page (slot 46)
            if (page > 0) {
                ItemStack prevButton = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGUxZGZjMTFhODM3MTExZDIyYjAwMWExNDQ2MWY5YTdmYzA5MzUyMmY4OGM1OGZhZWZkNmFkZWZmY2Q0ZTlhYiJ9fX0=");
                ItemMeta prevMeta = prevButton.getItemMeta();
                prevMeta.setDisplayName(ChatColor.GRAY + "← Previous Page");
                prevMeta.setLore(Arrays.asList(
                        ChatColor.DARK_GRAY + "Page " + page + " of " + totalPages,
                        ChatColor.GRAY + "Click to go to previous page"
                ));
                prevButton.setItemMeta(prevMeta);
                menu.setItem(46, prevButton);
            }

            // Page info (slot 49 - center bottom)
            ItemStack pageInfo = new ItemStack(Material.PAPER);
            ItemMeta pageMeta = pageInfo.getItemMeta();
            pageMeta.setDisplayName(ChatColor.YELLOW + "Page " + (page + 1) + " of " + totalPages);

            List<String> pageInfoLore = new ArrayList<>();
            pageInfoLore.add(ChatColor.GRAY + "Showing " + (endIndex - startIndex) + " items");
            pageInfoLore.add(ChatColor.GRAY + "Total logs: " + plugin.getBankLogsManager().getLogCount(factionName));

            // Add faction bank balance
            pageInfoLore.add("");
            pageInfoLore.add(ChatColor.GRAY + "Current Balance: " + ChatColor.GREEN +
                    plugin.getEconomyManager().format(faction.bankBalance));

            pageMeta.setLore(pageInfoLore);
            pageInfo.setItemMeta(pageMeta);
            menu.setItem(49, pageInfo);

            // Next page (slot 52)
            if (page < totalPages - 1) {
                ItemStack nextButton = createCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2M2OWQ0MTA3NmE4ZGVhNGYwNmQzZjFhOWFjNDdjYzk5Njk4OGI3NGEwOTEzYWIyYWMxYTc0Y2FmNzA4MTkxOCJ9fX0=");
                ItemMeta nextMeta = nextButton.getItemMeta();
                nextMeta.setDisplayName(ChatColor.GRAY + "Next Page →");
                nextMeta.setLore(Arrays.asList(
                        ChatColor.DARK_GRAY + "Page " + (page + 2) + " of " + totalPages,
                        ChatColor.GRAY + "Click to go to next page"
                ));
                nextButton.setItemMeta(nextMeta);
                menu.setItem(52, nextButton);
            }
        }

        // If no logs exist
        if (displayItems.isEmpty()) {
            ItemStack noLogs = new ItemStack(Material.BARRIER);
            ItemMeta noLogsMeta = noLogs.getItemMeta();
            noLogsMeta.setDisplayName(ChatColor.RED + "No Bank Logs");
            noLogsMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "This faction has no bank transaction history.",
                    ChatColor.GRAY + "Bank logs will appear here when members",
                    ChatColor.GRAY + "deposit or withdraw from the faction bank."
            ));
            noLogs.setItemMeta(noLogsMeta);
            menu.setItem(22, noLogs); // Center of the display area
        }

        player.openInventory(menu);
    }

    /**
     * Create a flat list of display items (dates and logs) for pagination
     */
    private List<Object> createDisplayItemsList(Map<String, List<BankLog>> logsGroupedByDate) {
        List<Object> displayItems = new ArrayList<>();

        for (Map.Entry<String, List<BankLog>> entry : logsGroupedByDate.entrySet()) {
            String date = entry.getKey();
            List<BankLog> logsForDate = entry.getValue();

            // Add date separator
            displayItems.add(date);

            // Add all logs for this date (already sorted by most recent first)
            displayItems.addAll(logsForDate);
        }

        return displayItems;
    }

    /**
     * Create a date separator item
     */
    private ItemStack createDateSeparatorItem(String date) {
        ItemStack dateItem = new ItemStack(Material.YELLOW_CONCRETE);
        ItemMeta dateMeta = dateItem.getItemMeta();
        dateMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + date);
        dateMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Bank transactions for this date"
        ));
        dateItem.setItemMeta(dateMeta);
        return dateItem;
    }

    /**
     * Create a bank log item
     */
    private ItemStack createBankLogItem(BankLog log) {
        Material material = log.type == BankLog.BankLogType.DEPOSIT ? Material.GREEN_CONCRETE : Material.RED_CONCRETE;
        ItemStack logItem = new ItemStack(material);
        ItemMeta logMeta = logItem.getItemMeta();

        // Format amount with symbol
        String amountDisplay = log.type.getSymbol() + plugin.getEconomyManager().format(log.amount);
        ChatColor nameColor = log.type == BankLog.BankLogType.DEPOSIT ? ChatColor.GREEN : ChatColor.RED;

        logMeta.setDisplayName(nameColor + amountDisplay);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + log.type.getDisplayName());
        lore.add(ChatColor.GRAY + "Player: " + ChatColor.WHITE + log.playerName);
        lore.add(ChatColor.GRAY + "Time: " + ChatColor.WHITE + plugin.getBankLogsManager().formatTime(log.timestamp));
        lore.add("");
        lore.add(ChatColor.GRAY + "Balance Changes:");
        lore.add(ChatColor.GRAY + "• Before: " + ChatColor.YELLOW + plugin.getEconomyManager().format(log.oldBalance));
        lore.add(ChatColor.GRAY + "• After: " + ChatColor.YELLOW + plugin.getEconomyManager().format(log.newBalance));

        // Calculate and show the difference
        double difference = log.newBalance - log.oldBalance;
        String diffColor = difference > 0 ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
        String diffSymbol = difference > 0 ? "+" : "";
        lore.add(ChatColor.GRAY + "• Change: " + diffColor + diffSymbol + plugin.getEconomyManager().format(difference));

        logMeta.setLore(lore);
        logItem.setItemMeta(logMeta);
        return logItem;
    }

    /**
     * Handle bank logs GUI clicks
     */
    public void handleBankLogsClick(Player player, String displayName, String title) {
        // Extract faction name from title
        String factionName = title.replace(ChatColor.DARK_GRAY + "Bank Logs: ", "");

        if (displayName.equals(ChatColor.GRAY + "← Back")) {
            // Go back to bank GUI
            parentHandler.openBankGUI(player, factionName);
            return;
        }

        // Handle pagination
        if (displayName.equals(ChatColor.GRAY + "← Previous Page")) {
            int currentPage = getCurrentPageFromTitle(player);
            openBankLogsGUI(player, factionName, currentPage - 1);
        } else if (displayName.equals(ChatColor.GRAY + "Next Page →")) {
            int currentPage = getCurrentPageFromTitle(player);
            openBankLogsGUI(player, factionName, currentPage + 1);
        }
    }

    /**
     * Get current page from the page info item
     */
    private int getCurrentPageFromTitle(Player player) {
        ItemStack pageItem = player.getOpenInventory().getItem(49);
        if (pageItem != null && pageItem.hasItemMeta() && pageItem.getItemMeta().hasDisplayName()) {
            String displayName = pageItem.getItemMeta().getDisplayName();
            String stripped = ChatColor.stripColor(displayName);
            if (stripped.startsWith("Page ")) {
                try {
                    String[] parts = stripped.split(" ");
                    if (parts.length >= 2) {
                        return Integer.parseInt(parts[1]) - 1; // Convert to 0-based
                    }
                } catch (NumberFormatException e) {
                    // Fallback
                }
            }
        }
        return 0;
    }

    /**
     * Create a glass pane filler
     */
    private ItemStack createGlassPaneFiller() {
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        glassMeta.setDisplayName(" ");
        blackGlass.setItemMeta(glassMeta);
        return blackGlass;
    }
}