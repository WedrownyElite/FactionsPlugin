package me.elite.Factions.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class MessageManager {
    private static final String PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.BLUE + "Factions" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET;

    // Faction creation messages
    public static void sendFactionCreated(Player player, String factionName) {
        player.sendMessage(PREFIX + ChatColor.GREEN + "Successfully created faction " + ChatColor.BOLD + factionName + ChatColor.GREEN + "!");
        player.sendMessage(PREFIX + ChatColor.YELLOW + "You are now the owner of " + factionName + "!");
        player.sendMessage(PREFIX + ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/f menu" + ChatColor.GRAY + " to access faction features.");
    }

    // Faction join messages
    public static void sendFactionJoined(Player player, String factionName) {
        player.sendMessage(PREFIX + ChatColor.GREEN + "Successfully joined faction " + ChatColor.BOLD + factionName + ChatColor.GREEN + "!");
        player.sendMessage(PREFIX + ChatColor.YELLOW + "Welcome to " + factionName + "! Use " + ChatColor.YELLOW + "/f menu" + ChatColor.YELLOW + " to access faction features.");
    }

    // Faction leave messages
    public static void sendFactionLeft(Player player, String factionName) {
        player.sendMessage(PREFIX + ChatColor.GREEN + "You have left faction " + factionName + ".");
    }

    // Faction privacy toggle messages
    public static void sendFactionPrivacyToggle(Player target, boolean toggle) {
        if (toggle) {
            target.sendMessage(PREFIX + ChatColor.GREEN + "Faction is now public! Anyone can join without an invitation.");
        }
        else {
            target.sendMessage(PREFIX + ChatColor.GREEN + "Faction is now " + ChatColor.RED + "private" + ChatColor.GREEN + "! Only invited players can join.");
        }
    }

    // Invitation messages
    public static void sendInvitationSent(Player sender, String targetName, String factionName) {
        sender.sendMessage(PREFIX + ChatColor.GREEN + "Successfully invited " + targetName + " to " + factionName + "!");
    }

    public static void sendInvitationReceived(Player target, String senderName, String factionName) {
        target.sendMessage(PREFIX + ChatColor.GREEN + "You have been invited to faction " + factionName + " by " + senderName + "!");
        target.sendMessage(PREFIX + ChatColor.YELLOW + "Use /f join " + factionName + " to join, or /f invites to see all invitations.");
    }

    // Promotion/Demotion messages
    public static void sendPromoted(Player target, String newRank, String promoter) {
        target.sendMessage(PREFIX + ChatColor.YELLOW + "You have been promoted to " + ChatColor.WHITE + newRank + ChatColor.YELLOW + " by " + promoter + "!");
    }

    public static void sendDemoted(Player target, String newRank, String demoter) {
        target.sendMessage(PREFIX + ChatColor.YELLOW + "You have been demoted to " + ChatColor.WHITE + newRank + ChatColor.YELLOW + " by " + demoter + "!");
    }

    public static void sendPromotionSuccess(Player promoter, String targetName, String oldRank, String newRank) {
        promoter.sendMessage(PREFIX + ChatColor.GREEN + "Successfully promoted " + targetName + " from " + ChatColor.WHITE + oldRank + ChatColor.GREEN + " to " + ChatColor.WHITE + newRank);
    }

    public static void sendDemotionSuccess(Player demoter, String targetName, String oldRank, String newRank) {
        demoter.sendMessage(PREFIX + ChatColor.GREEN + "Successfully demoted " + targetName + " from " + ChatColor.WHITE + oldRank + ChatColor.GREEN + " to " + ChatColor.WHITE + newRank);
    }

    // Kick messages
    public static void sendKicked(Player target, String factionName, String kicker) {
        target.sendMessage(PREFIX + ChatColor.RED + "You have been kicked from faction " + factionName + " by " + kicker + "!");
    }

    public static void sendKickSuccess(Player kicker, String targetName, String factionName) {
        kicker.sendMessage(PREFIX + ChatColor.GREEN + "Successfully kicked " + targetName + " from " + factionName + "!");
    }

    // Member notifications
    public static void sendMemberJoined(Player member, String playerName) {
        member.sendMessage(PREFIX + ChatColor.GREEN + playerName + " has joined the faction!");
    }

    public static void sendMemberLeft(Player member, String playerName) {
        member.sendMessage(PREFIX + ChatColor.YELLOW + playerName + " has left the faction.");
    }

    public static void sendMemberPrivacyToggle(Player member, String playerName, String privacy) {
        member.sendMessage(PREFIX + ChatColor.YELLOW + playerName + " made the faction " + privacy + "!");
    }

    public static void sendMemberInfoMessage(Player member, String message) {
        member.sendMessage(PREFIX + ChatColor.YELLOW + message);
    }

    public static void sendMemberWhiteMessage(Player member, String message) {
        member.sendMessage(PREFIX + ChatColor.WHITE + message);
    }

    public static void sendMemberBasicMessage(Player member, String message) {
        member.sendMessage(PREFIX + message);
    }

    // Faction disbanding
    public static void sendFactionDisbanded(Player member, String factionName, String disbander) {
        member.sendMessage(PREFIX + ChatColor.RED + "Faction " + factionName + " has been disbanded by " + disbander + "!");
    }

    public static void sendDisbandSuccess(Player disbander, String factionName) {
        disbander.sendMessage(PREFIX + ChatColor.GREEN + "Faction " + factionName + " has been disbanded.");
    }

    // Relation messages
    public static void sendRelationSet(Player player, String targetFaction, String relation) {
        player.sendMessage(PREFIX + ChatColor.GREEN + "Relation with " + targetFaction + " set to: " + relation);
    }

    // Error messages
    public static void sendError(Player player, String message) {
        player.sendMessage(PREFIX + ChatColor.RED + message);
    }

    // Blank message
    public static void sendBasicMessage(Player player, String message) {
        player.sendMessage(PREFIX + message);
    }

    // Success messages
    public static void sendSuccess(Player player, String message) {
        player.sendMessage(PREFIX + ChatColor.GREEN + message);
    }

    // Info messages
    public static void sendInfo(Player player, String message) {
        player.sendMessage(PREFIX + ChatColor.YELLOW + message);
    }

    private MessageManager() {
        // Prevent instantiation
    }
}