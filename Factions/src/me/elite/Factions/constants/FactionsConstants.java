package me.elite.Factions.constants;

import org.bukkit.ChatColor;

public final class FactionsConstants {

    // GUI Sizes
    public static final int SMALL_GUI_SIZE = 9;
    public static final int MEDIUM_GUI_SIZE = 27;
    public static final int LARGE_GUI_SIZE = 54;

    // Cooldowns (in milliseconds)
    public static final long MESSAGE_COOLDOWN = 5000L; // 5 seconds

    // Team Management
    public static final String TEAM_PREFIX = "fac_";
    public static final int MAX_TEAM_NAME_LENGTH = 16;

    // Faction Limits
    public static final int MAX_FACTION_NAME_LENGTH = 16;

    // Pagination
    public static final int FACTIONS_PER_PAGE = 36;
    public static final int RELATIONS_PER_PAGE = 9;
    public static final int REQUESTS_PER_PAGE = 18;

    // Special Territory Names
    public static final String SPAWN = "Spawn";
    public static final String WARZONE = "Warzone";
    public static final String WILDERNESS = "Wilderness";

    // Nametag Suffixes
    public static final String FACTION_SUFFIX = " " + ChatColor.GREEN + "F";
    public static final String ALLY_SUFFIX = " " + ChatColor.LIGHT_PURPLE + "A";
    public static final String TRUCE_SUFFIX = " " + ChatColor.BLUE + "T";
    public static final String ENEMY_SUFFIX = " " + ChatColor.RED + "E";

    private FactionsConstants() {
        // Prevent instantiation
    }
}