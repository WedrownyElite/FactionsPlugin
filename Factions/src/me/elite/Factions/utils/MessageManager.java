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
        target.sendMessage(PREFIX + ChatColor.GREEN + "You have been invited to " + ChatColor.BOLD + factionName + ChatColor.RESET + ChatColor.GREEN + " by " + senderName + "!");
        target.sendMessage(PREFIX + ChatColor.YELLOW + "Use " + ChatColor.YELLOW + "/f join " + factionName + ChatColor.GREEN +
                " to join, or " + ChatColor.YELLOW + "/f invites " + ChatColor.GREEN + "to see all invitations.");
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
        kicker.sendMessage(PREFIX + ChatColor.GREEN + "Successfully kicked " + targetName + " from " + ChatColor.BOLD + factionName + ChatColor.RESET + ChatColor.GREEN +"!");
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

    public static void sendMemberOwnerTransferSuccess(Player member, Player previousOwner, String newOwner, String factionName) {
        member.sendMessage(ChatColor.YELLOW + "═══ FACTION ANNOUNCEMENT ═══");
        member.sendMessage(ChatColor.WHITE + previousOwner.getDisplayName() + " has transferred ownership");
        member.sendMessage(ChatColor.WHITE + "of " + factionName + " to " + newOwner + "!");
        member.sendMessage(ChatColor.YELLOW + "════════════════════════════");
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
    public static void sendMemberFactionDisbanded(Player member, String factionName, String disbander) {
        member.sendMessage(PREFIX + ChatColor.RED + ChatColor.BOLD + factionName + ChatColor.RESET + ChatColor.RED + " has been disbanded by " + disbander + "!");
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

    // Basic message
    public static void sendBasicMessage(Player player, String message) {
        player.sendMessage(PREFIX + message);
    }

    // Blank message (no prefix)
    public static void sendBlankMessage(Player player, String message) {
        player.sendMessage(message);
    }

    // Success messages
    public static void sendSuccess(Player player, String message) {
        player.sendMessage(PREFIX + ChatColor.GREEN + message);
    }

    // Info messages
    public static void sendInfo(Player player, String message) {
        player.sendMessage(PREFIX + ChatColor.YELLOW + message);
    }

    // Faction GUI Creation Messages
    public static void sendGUIFactionCreation(Player player) {
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "═══════════════════════════════════");
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.GOLD + "CREATE FACTION");
        player.sendMessage(ChatColor.YELLOW + "═══════════════════════════════════");
        player.sendMessage(ChatColor.WHITE + "Please enter your desired faction name:");
        player.sendMessage(ChatColor.WHITE + "• Type your faction name in chat");
        player.sendMessage(ChatColor.WHITE + "• Type " + ChatColor.RED + "cancel" + ChatColor.GRAY + " to abort");
        player.sendMessage(ChatColor.YELLOW + "═══════════════════════════════════");
    }

    // Ownership Transfer Warning Message
    public static void sendOwnerTransferWarning(Player player, String target, String factionName) {
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "═══════════════════════════════════");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "⚠ OWNERSHIP TRANSFER WARNING ⚠");
        player.sendMessage(ChatColor.YELLOW + "═══════════════════════════════════");
        player.sendMessage(ChatColor.WHITE + "You are about to transfer ownership of");
        player.sendMessage(ChatColor.BOLD + factionName + ChatColor.WHITE + " to " + ChatColor.YELLOW + target + ChatColor.WHITE + ".");
        player.sendMessage(ChatColor.YELLOW + "");
        player.sendMessage(ChatColor.RED + "This will:");
        player.sendMessage(ChatColor.RED + "• Make " + target + " the new OWNER");
        player.sendMessage(ChatColor.RED + "• Demote you to ADMIN rank");
        player.sendMessage(ChatColor.RED + "• Cannot be undone without their permission");
        player.sendMessage(ChatColor.YELLOW + "");
        player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GREEN + "/f confirm " + factionName +
                ChatColor.YELLOW + " to proceed");
        player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.RED + "/f cancel" +
                ChatColor.YELLOW + " to cancel this transfer");
        player.sendMessage(ChatColor.WHITE + "This confirmation will expire in 30 seconds");
        player.sendMessage(ChatColor.YELLOW + "═══════════════════════════════════");
    }

    // Ownership Transfer Success Message
    public static void sendOwnerTransferSuccess(Player player, Player target, String factionName) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "═══════════════════════════════════");
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "OWNERSHIP TRANSFERRED");
        player.sendMessage(ChatColor.GREEN + "═══════════════════════════════════");
        player.sendMessage(ChatColor.WHITE + "You have transferred ownership of " + ChatColor.BOLD + factionName);
        player.sendMessage(ChatColor.WHITE + "to " + ChatColor.YELLOW + target.getDisplayName() + ChatColor.WHITE + ".");
        player.sendMessage(ChatColor.WHITE + "You are now an " + ChatColor.BLUE + "ADMIN" + ChatColor.WHITE + ".");
        player.sendMessage(ChatColor.GREEN + "═══════════════════════════════════");

        target.sendMessage(ChatColor.GREEN + "═══════════════════════════════════");
        target.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "YOU ARE NOW THE OWNER!");
        target.sendMessage(ChatColor.GREEN + "═══════════════════════════════════");
        target.sendMessage(ChatColor.WHITE + player.getName() + " has transferred ownership");
        target.sendMessage(ChatColor.WHITE + "of " + ChatColor.BOLD + factionName + ChatColor.WHITE + " to you!");
        target.sendMessage(ChatColor.WHITE + "You now have full control of the faction.");
        target.sendMessage(ChatColor.GREEN + "═══════════════════════════════════");
    }

    private MessageManager() {
        // Prevent instantiation
    }
}