package me.elite.Factions.constants;

import me.elite.Factions.FactionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public final class FactionsConstants {
    private static FactionsPlugin plugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");

    // GUI Sizes (now use config)
    public static int SMALL_GUI_SIZE() {
        return plugin != null ? plugin.getConfigManager().getSmallGuiSize() : 9;
    }

    public static int MEDIUM_GUI_SIZE() {
        return plugin != null ? plugin.getConfigManager().getMediumGuiSize() : 27;
    }

    public static int LARGE_GUI_SIZE() {
        return plugin != null ? plugin.getConfigManager().getLargeGuiSize() : 54;
    }

    // Cooldowns (now use config)
    public static long MESSAGE_COOLDOWN() {
        return plugin != null ? plugin.getConfigManager().getMessageCooldown() : 5000L;
    }

    // Team Management (now use config)
    public static String TEAM_PREFIX() {
        return plugin != null ? plugin.getConfigManager().getTeamPrefix() : "fac_";
    }

    public static int MAX_TEAM_NAME_LENGTH() {
        return plugin != null ? plugin.getConfigManager().getMaxTeamNameLength() : 16;
    }

    // Faction Limits (now use config)
    public static int MAX_FACTION_NAME_LENGTH() {
        return plugin != null ? plugin.getConfigManager().getMaxFactionNameLength() : 16;
    }

    // Pagination (now use config)
    public static int FACTIONS_PER_PAGE() {
        return plugin != null ? plugin.getConfigManager().getFactionsPerPage() : 36;
    }

    public static int RELATIONS_PER_PAGE() {
        return plugin != null ? plugin.getConfigManager().getRelationsPerPage() : 9;
    }

    public static int REQUESTS_PER_PAGE() {
        return plugin != null ? plugin.getConfigManager().getRequestsPerPage() : 18;
    }

    // Special Territory Names (now use config)
    public static String SPAWN() {
        return plugin != null ? plugin.getConfigManager().getSpawnName() : "Spawn";
    }

    public static String WARZONE() {
        return plugin != null ? plugin.getConfigManager().getWarzoneName() : "Warzone";
    }

    public static String WILDERNESS() {
        return plugin != null ? plugin.getConfigManager().getWildernessName() : "Wilderness";
    }

    // Nametag Suffixes (now use config)
    public static String FACTION_SUFFIX() {
        return plugin != null ? plugin.getConfigManager().getFactionSuffix() : " " + ChatColor.GREEN + "F";
    }

    public static String ALLY_SUFFIX() {
        return plugin != null ? plugin.getConfigManager().getAllySuffix() : " " + ChatColor.LIGHT_PURPLE + "A";
    }

    public static String TRUCE_SUFFIX() {
        return plugin != null ? plugin.getConfigManager().getTruceSuffix() : " " + ChatColor.BLUE + "T";
    }

    public static String ENEMY_SUFFIX() {
        return plugin != null ? plugin.getConfigManager().getEnemySuffix() : " " + ChatColor.RED + "E";
    }

    private FactionsConstants() {
        // Prevent instantiation
    }
}