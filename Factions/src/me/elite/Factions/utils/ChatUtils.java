package me.elite.Factions.utils;

import org.bukkit.ChatColor;

public final class ChatUtils {

    /**
     * Format time from milliseconds to human-readable string
     */
    public static String formatTimeString(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days == 1 ? "" : "s");
        } else if (hours > 0) {
            return hours + " hour" + (hours == 1 ? "" : "s");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes == 1 ? "" : "s");
        } else {
            return seconds + " second" + (seconds == 1 ? "" : "s");
        }
    }

    /**
     * Create a centered title with decorative borders
     */
    public static String createTitle(String title, ChatColor color) {
        return color + "" + ChatColor.BOLD + title;
    }

    /**
     * Create a separator line for messages
     */
    public static String createSeparator(char character, int length) {
        return new String(new char[length]).replace('\0', character);
    }

    /**
     * Validate faction name format
     */
    public static boolean isValidFactionName(String name) {
        return name != null &&
                !name.isEmpty() &&
                name.matches("[a-zA-Z0-9_]+") &&
                name.length() <= 16;
    }

    private ChatUtils() {
        // Prevent instantiation
    }
}