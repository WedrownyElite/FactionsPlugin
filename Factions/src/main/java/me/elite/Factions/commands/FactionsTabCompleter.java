package me.elite.Factions.commands;

import me.elite.Factions.FactionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.Rank;
import me.elite.Factions.data.FactionPermission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FactionsTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            FactionsPlugin plugin = getPlugin();

            // Basic commands everyone can see
            commands.addAll(Arrays.asList(
                    // Core essentials - what new players need first
                    "menu", "help", "h", "?", "map", "m", "power", "p"
            ));

            // Faction creation (only if not in a faction)
            if (plugin != null && !plugin.getUtilityManager().isPlayerInFaction(player.getUniqueId())) {
                commands.add("create");
            }

            // Join command (only if not in a faction)
            if (plugin != null && !plugin.getUtilityManager().isPlayerInFaction(player.getUniqueId())) {
                commands.add("join");
            }

            // Commands for players in factions
            if (plugin != null && plugin.getUtilityManager().isPlayerInFaction(player.getUniqueId())) {
                String factionName = plugin.getPlayerFactions().get(player.getUniqueId());
                Faction faction = plugin.getFactions().get(factionName);

                if (faction != null) {
                    Rank playerRank = faction.members.get(player.getUniqueId());

                    // Basic faction member commands
                    commands.addAll(Arrays.asList("leave", "home", "bank", "worth"));

                    // Warps (if player has permission)
                    if (faction.hasPermission(playerRank, FactionPermission.WARPS_ACCESS)) {
                        commands.addAll(Arrays.asList("warp", "warps"));
                    }

                    // Claiming (if player has permission)
                    if (faction.hasPermission(playerRank, FactionPermission.CLAIM_LAND)) {
                        commands.add("claim");
                    }

                    // Unclaiming (if player has permission)
                    if (faction.hasPermission(playerRank, FactionPermission.UNCLAIM_LAND)) {
                        commands.add("unclaim");
                    }

                    // Unclaim all (if player has permission)
                    if (faction.hasPermission(playerRank, FactionPermission.UNCLAIM_ALL)) {
                        commands.add("unclaimall");
                    }

                    // Home management (if player has permission)
                    if (faction.hasPermission(playerRank, FactionPermission.SET_HOME)) {
                        commands.addAll(Arrays.asList("sethome", "delhome"));
                    }

                    // Warp management (if player has permission)
                    if (faction.hasPermission(playerRank, FactionPermission.MANAGE_WARPS)) {
                        commands.addAll(Arrays.asList("setwarp", "delwarp"));
                    }

                    // Member management (if player has permission)
                    if (faction.hasPermission(playerRank, FactionPermission.INVITE_MEMBERS)) {
                        commands.addAll(Arrays.asList("invite", "inv"));
                    }

                    if (faction.hasPermission(playerRank, FactionPermission.KICK_MEMBERS)) {
                        commands.add("kick");
                    }

                    if (faction.hasPermission(playerRank, FactionPermission.PROMOTE_MEMBERS)) {
                        commands.add("promote");
                    }

                    if (faction.hasPermission(playerRank, FactionPermission.DEMOTE_MEMBERS)) {
                        commands.add("demote");
                    }

                    // Relations (if player has permission)
                    if (faction.hasPermission(playerRank, FactionPermission.SET_RELATIONS)) {
                        commands.addAll(Arrays.asList("ally", "a", "truce", "t", "neutral", "n", "enemy", "e"));
                    }

                    // Description (if player has permission)
                    if (faction.hasPermission(playerRank, FactionPermission.CHANGE_DESCRIPTION)) {
                        commands.add("desc");
                    }

                    // Privacy (if player has permission)
                    if (faction.hasPermission(playerRank, FactionPermission.OPEN_CLOSE)) {
                        commands.add("privacy");
                    }

                    // Bank logs (if player has permission)
                    if (playerRank == Rank.OWNER || faction.hasPermission(playerRank, FactionPermission.BANK_LOGS)) {
                        commands.addAll(Arrays.asList("banklogs", "banklog"));
                    }

                    // View discord (if player has permission)
                    if (playerRank == Rank.OWNER || faction.hasPermission(playerRank, FactionPermission.VIEW_DISCORD)) {
                        commands.add("discord");
                    }

                    // Set discord (if player has permission
                    if (playerRank == Rank.OWNER || faction.hasPermission(playerRank, FactionPermission.SET_DISCORD)) {
                        commands.addAll(Arrays.asList("setdiscord", "unsetdiscord"));
                    }

                    // Owner-only commands
                    if (playerRank == Rank.OWNER) {
                        commands.addAll(Arrays.asList("disband", "confirm", "cancel"));
                    }
                }
            }

            // View invitations (only if player has invitations)
            if (plugin != null) {
                Set<String> invites = plugin.getPlayerInvitations().get(player.getUniqueId());
                if (invites != null && !invites.isEmpty()) {
                    commands.addAll(Arrays.asList("invitations", "invites", "invs"));
                }
            }

            // Worth commands (everyone can see)
            commands.addAll(Arrays.asList("worthtop", "wtop"));

            // Admin commands - only show if player has permission or is op
            if (player.isOp() || player.hasPermission("factions.adminclaim")) {
                commands.add("adminclaim");
            }
            if (player.isOp() || player.hasPermission("factions.adminunclaim")) {
                commands.add("adminunclaim");
            }
            if (player.isOp() || player.hasPermission("factions.adminunclaimall")) {
                commands.add("adminunclaimall");
            }
            if (player.isOp() || player.hasPermission("factions.loadall")) {
                commands.add("loadall");
            }
            if (player.isOp() || player.hasPermission("factions.load")) {
                commands.add("load");
            }
            if (player.isOp() || player.hasPermission("factions.adminjoin")) {
                commands.add("adminjoin");
            }
            if (player.isOp() || player.hasPermission("factions.reload")) {
                commands.addAll(Arrays.asList("reload", "rl"));
            }
            if (player.isOp() || player.hasPermission("factions.recalcworth")) {
                commands.addAll(Arrays.asList("recalcworth", "recalc"));
            }
            if (player.isOp()) {
                commands.addAll(Arrays.asList("debuginfo", "debugnametags",
                        "createtestfactions", "removetestfactions"));
            }

            // Filter top-level command suggestions by what the player has started typing
            String partial = args[0].toLowerCase();
            return commands.stream()
                    .filter(c -> c.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        // For second+ args we will try to filter suggestions by the partial input typed
        final String partial = args.length > 1 ? args[args.length - 1].toLowerCase() : "";

        switch (args[0].toLowerCase()) {
            case "help":
            case "h":
            case "?":
                // Page numbers for help
                if (args.length == 2) {
                    return Arrays.asList("1", "2", "3", "4").stream()
                            .filter(page -> page.startsWith(partial))
                            .collect(Collectors.toList());
                }
                break;

            case "create":
                // Only allow if not in faction
                FactionsPlugin plugin = getPlugin();
                if (plugin != null && plugin.getUtilityManager().isPlayerInFaction(player.getUniqueId())) {
                    return Collections.emptyList();
                }
                return Collections.emptyList(); // No suggestions for faction names

            case "promote":
            case "demote":
                // Only show if player has permission
                if (!hasPromoteDemotePermission(player, args[0].equalsIgnoreCase("promote"))) {
                    return Collections.emptyList();
                }

                // Show players in the same faction (filtered by typed partial)
                FactionsPlugin factionsPlugin = getPlugin();
                if (factionsPlugin != null) {
                    return factionsPlugin.getUtilityManager().getFactionMembers(player.getUniqueId()).stream()
                            .filter(name -> !name.equals(player.getName())) // Don't show self
                            .filter(name -> name.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "desc":
                // Only allow if player has permission
                if (!hasDescriptionPermission(player)) {
                    return Collections.emptyList();
                }
                return Collections.emptyList(); // No suggestions for descriptions

            case "invite":
            case "inv":
                // Only allow if player has permission
                if (!hasInvitePermission(player)) {
                    return Collections.emptyList();
                }

                // Show online players who aren't already in a faction (filtered by typed partial)
                FactionsPlugin invitePlugin = getPlugin();
                if (invitePlugin != null && args.length == 2) {
                    return Bukkit.getOnlinePlayers().stream()
                            .filter(p -> p.getName().toLowerCase().startsWith(partial))
                            .filter(p -> !invitePlugin.getUtilityManager().isPlayerInFaction(p.getUniqueId()))
                            .map(Player::getName)
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "kick":
                // Only allow if player has permission
                if (!hasKickPermission(player)) {
                    return Collections.emptyList();
                }

                // Show players in the same faction as the command sender (filtered)
                if (args.length == 2) {
                    FactionsPlugin kickPlugin = getPlugin();
                    if (kickPlugin != null) {
                        return kickPlugin.getUtilityManager().getFactionMembers(player.getUniqueId()).stream()
                                .filter(name -> !name.equals(player.getName())) // Don't show self
                                .filter(name -> name.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();

            case "join":
                // Only allow if not in faction
                FactionsPlugin joinCheckPlugin = getPlugin();
                if (joinCheckPlugin != null && joinCheckPlugin.getUtilityManager().isPlayerInFaction(player.getUniqueId())) {
                    return Collections.emptyList();
                }

                if (args.length == 2) {
                    FactionsPlugin joinPlugin = getPlugin();
                    if (joinPlugin != null) {
                        // Show public factions and factions the player is invited to
                        List<String> availableFactions = new ArrayList<>();

                        // Add public factions
                        for (Map.Entry<String, me.elite.Factions.data.Faction> entry : joinPlugin.getFactions().entrySet()) {
                            if (entry.getValue().isPublic) {
                                availableFactions.add(entry.getKey());
                            }
                        }

                        // Add invited factions
                        Set<String> invites = joinPlugin.getPlayerInvitations().get(player.getUniqueId());
                        if (invites != null) {
                            availableFactions.addAll(invites);
                        }

                        // Filter by typed partial
                        return availableFactions.stream()
                                .filter(f -> f.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();

            case "ally":
            case "a":
            case "truce":
            case "t":
            case "enemy":
            case "e":
            case "neutral":
            case "n":
                // Only allow if player has permission
                if (!hasRelationPermission(player)) {
                    return Collections.emptyList();
                }

                if (args.length == 2) {
                    FactionsPlugin relationPlugin = getPlugin();
                    if (relationPlugin != null) {
                        return relationPlugin.getUtilityManager().getAllFactionNames().stream()
                                .filter(name -> !name.equals(getPlayerFaction(player)))
                                .filter(n -> n.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();

            case "claim":
                // Only allow if player has permission
                if (!hasClaimPermission(player)) {
                    return Collections.emptyList();
                }
                return Collections.emptyList();

            case "unclaim":
                // Only allow if player has permission
                if (!hasUnclaimPermission(player)) {
                    return Collections.emptyList();
                }
                return Collections.emptyList();

            case "unclaimall":
                // Only allow if player has permission
                if (!hasUnclaimAllPermission(player)) {
                    return Collections.emptyList();
                }
                return Collections.emptyList();

            case "sethome":
            case "delhome":
                // Only allow if player has permission
                if (!hasHomePermission(player)) {
                    return Collections.emptyList();
                }
                return Collections.emptyList();

            case "setwarp":
                // Only allow if player has permission
                if (!hasWarpManagePermission(player)) {
                    return Collections.emptyList();
                }
                return Collections.emptyList(); // No suggestions for new warp names

            case "delwarp":
                // Only allow if player has permission
                if (!hasWarpManagePermission(player)) {
                    return Collections.emptyList();
                }

                // Show available warps for deletion
                if (args.length == 2) {
                    FactionsPlugin delWarpPlugin = getPlugin();
                    if (delWarpPlugin != null) {
                        String playerFaction = delWarpPlugin.getPlayerFactions().get(player.getUniqueId());
                        if (playerFaction != null) {
                            Faction faction = delWarpPlugin.getFactions().get(playerFaction);
                            if (faction != null) {
                                return faction.warps.keySet().stream()
                                        .filter(warpName -> warpName.toLowerCase().startsWith(partial))
                                        .collect(Collectors.toList());
                            }
                        }
                    }
                }
                return Collections.emptyList();

            case "warp":
            case "warps":
                // Only allow if player has permission
                if (!hasWarpAccessPermission(player)) {
                    return Collections.emptyList();
                }

                // Show available warps for the player's faction
                if (args.length == 2) {
                    FactionsPlugin warpPlugin = getPlugin();
                    if (warpPlugin != null) {
                        String playerFaction = warpPlugin.getPlayerFactions().get(player.getUniqueId());
                        if (playerFaction != null) {
                            Faction faction = warpPlugin.getFactions().get(playerFaction);
                            if (faction != null) {
                                return faction.warps.keySet().stream()
                                        .filter(warpName -> warpName.toLowerCase().startsWith(partial))
                                        .collect(Collectors.toList());
                            }
                        }
                    }
                }
                return Collections.emptyList();

            case "privacy":
                // Only allow if player has permission
                if (!hasPrivacyPermission(player)) {
                    return Collections.emptyList();
                }

                if (args.length == 2) {
                    return Arrays.asList("public", "private").stream()
                            .filter(option -> option.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();
            case "banklogs":
            case "banklog":
                // Only allow if player has permission
                if (!hasBankLogsPermission(player)) {
                    return Collections.emptyList();
                }

                if (args.length == 2) {
                    return Arrays.asList("1", "2", "3", "4", "5").stream()
                            .filter(page -> page.startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "disband":
                // Only allow if player is owner
                if (!isOwner(player)) {
                    return Collections.emptyList();
                }

                if (args.length == 2 && args[1].isEmpty()) {
                    return Arrays.asList("confirm");
                }
                return Collections.emptyList();

            case "confirm":
                // Only allow if player is owner
                if (!isOwner(player)) {
                    return Collections.emptyList();
                }

                if (args.length == 2) {
                    // For ownership confirmation, suggest faction name
                    String playerFaction = getPlayerFaction(player);
                    if (playerFaction != null && playerFaction.toLowerCase().startsWith(partial)) {
                        return Arrays.asList(playerFaction);
                    }
                }
                return Collections.emptyList();

            case "cancel":
                // Only allow if player is owner
                if (!isOwner(player)) {
                    return Collections.emptyList();
                }
                return Collections.emptyList();

            // Admin commands
            case "adminclaim":
                if (!(player.isOp() || player.hasPermission("factions.adminclaim"))) {
                    return Collections.emptyList();
                }
                if (args.length == 2) {
                    FactionsPlugin adminPlugin = getPlugin();
                    if (adminPlugin != null) {
                        return adminPlugin.getUtilityManager().getAllFactionNames().stream()
                                .filter(name -> name.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();

            case "adminunclaim":
                if (!(player.isOp() || player.hasPermission("factions.adminunclaim"))) {
                    return Collections.emptyList();
                }
                return Collections.emptyList();

            case "adminunclaimall":
                if (!(player.isOp() || player.hasPermission("factions.adminunclaimall"))) {
                    return Collections.emptyList();
                }
                if (args.length == 2) {
                    FactionsPlugin adminPlugin = getPlugin();
                    if (adminPlugin != null) {
                        return adminPlugin.getUtilityManager().getAllFactionNames().stream()
                                .filter(name -> name.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();

            case "load":
                if (!(player.isOp() || player.hasPermission("factions.load"))) {
                    return Collections.emptyList();
                }
                if (args.length == 2) {
                    return Bukkit.getWorlds().stream()
                            .map(World::getName)
                            .filter(name -> name.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "loadall":
                if (!(player.isOp() || player.hasPermission("factions.loadall"))) {
                    return Collections.emptyList();
                }
                return Collections.emptyList();

            case "adminjoin":
                if (!(player.isOp() || player.hasPermission("factions.adminjoin"))) {
                    return Collections.emptyList();
                }
                if (args.length == 2) {
                    // Second argument: faction name
                    FactionsPlugin adminPlugin = getPlugin();
                    if (adminPlugin != null) {
                        return adminPlugin.getUtilityManager().getAllFactionNames().stream()
                                .filter(name -> name.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    }
                } else if (args.length == 3) {
                    // Third argument: player name
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "createtestfactions":
                if (!player.isOp()) {
                    return Collections.emptyList();
                }
                if (args.length == 2) {
                    return Arrays.asList("10", "20", "50", "80", "100").stream()
                            .filter(count -> count.startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "removetestfactions":
                if (!player.isOp()) {
                    return Collections.emptyList();
                }
                return Collections.emptyList();

            case "debugnametags":
                if (!player.isOp()) {
                    return Collections.emptyList();
                }
                if (args.length == 2) {
                    return Arrays.asList("refresh", "info", "test").stream()
                            .filter(option -> option.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "debuginfo":
                if (!player.isOp()) {
                    return Collections.emptyList();
                }
                return Collections.emptyList();

            case "reload":
            case "rl":
                if (!(player.isOp() || player.hasPermission("factions.reload"))) {
                    return Collections.emptyList();
                }
                return Collections.emptyList();

            case "recalcworth":
            case "recalc":
                if (!(player.isOp() || player.hasPermission("factions.recalcworth"))) {
                    return Collections.emptyList();
                }
                if (args.length == 2) {
                    FactionsPlugin worthPlugin = getPlugin();
                    if (worthPlugin != null) {
                        return worthPlugin.getUtilityManager().getAllFactionNames().stream()
                                .filter(name -> name.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();

            case "bank":
                // Tab completion for bank subcommands
                if (args.length == 2) {
                    List<String> bankCommands = Arrays.asList("balance", "bal", "deposit", "withdraw");
                    return bankCommands.stream()
                            .filter(cmd -> cmd.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                } else if (args.length == 3 && (args[1].equalsIgnoreCase("deposit") || args[1].equalsIgnoreCase("withdraw"))) {
                    // Suggest common amounts for deposit/withdraw
                    return Arrays.asList("100", "500", "1000", "5000", "10000").stream()
                            .filter(amount -> amount.startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "worthtop":
            case "wtop":
                // Tab complete page numbers
                if (args.length == 2) {
                    return Arrays.asList("1", "2", "3", "4", "5").stream()
                            .filter(page -> page.startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            // Commands that don't need arguments or tab completion
            case "menu":
            case "leave":
            case "map":
            case "m":
            case "power":
            case "p":
            case "home":
            case "worth":
            case "invitations":
            case "invites":
            case "invs":
                return Collections.emptyList();

            default:
                return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    // Helper methods for permission checking
    private FactionsPlugin getPlugin() {
        return (FactionsPlugin) Bukkit.getPluginManager().getPlugin("EclipseFactions");
    }

    private String getPlayerFaction(Player player) {
        FactionsPlugin plugin = getPlugin();
        if (plugin != null) {
            return plugin.getPlayerFactions().get(player.getUniqueId());
        }
        return null;
    }

    private Faction getPlayerFactionObject(Player player) {
        FactionsPlugin plugin = getPlugin();
        if (plugin != null) {
            String factionName = plugin.getPlayerFactions().get(player.getUniqueId());
            if (factionName != null) {
                return plugin.getFactions().get(factionName);
            }
        }
        return null;
    }

    private Rank getPlayerRank(Player player) {
        Faction faction = getPlayerFactionObject(player);
        if (faction != null) {
            return faction.members.get(player.getUniqueId());
        }
        return null;
    }

    private boolean hasPermissionInFaction(Player player, FactionPermission permission) {
        Faction faction = getPlayerFactionObject(player);
        Rank rank = getPlayerRank(player);

        if (faction == null || rank == null) {
            return false;
        }

        return rank == Rank.OWNER || faction.hasPermission(rank, permission);
    }

    private boolean isOwner(Player player) {
        return getPlayerRank(player) == Rank.OWNER;
    }

    private boolean hasClaimPermission(Player player) {
        return hasPermissionInFaction(player, FactionPermission.CLAIM_LAND);
    }

    private boolean hasUnclaimPermission(Player player) {
        return hasPermissionInFaction(player, FactionPermission.UNCLAIM_LAND);
    }

    private boolean hasUnclaimAllPermission(Player player) {
        return hasPermissionInFaction(player, FactionPermission.UNCLAIM_ALL);
    }

    private boolean hasInvitePermission(Player player) {
        return hasPermissionInFaction(player, FactionPermission.INVITE_MEMBERS);
    }

    private boolean hasKickPermission(Player player) {
        return hasPermissionInFaction(player, FactionPermission.KICK_MEMBERS);
    }

    private boolean hasPromoteDemotePermission(Player player, boolean isPromotion) {
        FactionPermission permission = isPromotion ?
                FactionPermission.PROMOTE_MEMBERS : FactionPermission.DEMOTE_MEMBERS;
        return hasPermissionInFaction(player, permission);
    }

    private boolean hasDescriptionPermission(Player player) {
        return hasPermissionInFaction(player, FactionPermission.CHANGE_DESCRIPTION);
    }

    private boolean hasRelationPermission(Player player) {
        return hasPermissionInFaction(player, FactionPermission.SET_RELATIONS);
    }

    private boolean hasHomePermission(Player player) {
        return hasPermissionInFaction(player, FactionPermission.SET_HOME);
    }

    private boolean hasWarpManagePermission(Player player) {
        return hasPermissionInFaction(player, FactionPermission.MANAGE_WARPS);
    }

    private boolean hasWarpAccessPermission(Player player) {
        return hasPermissionInFaction(player, FactionPermission.WARPS_ACCESS);
    }

    private boolean hasPrivacyPermission(Player player) {
        return hasPermissionInFaction(player, FactionPermission.OPEN_CLOSE);
    }

    private boolean hasBankLogsPermission(Player player) {
        Rank rank = getPlayerRank(player);
        if (rank == Rank.OWNER) {
            return true;
        }
        return hasPermissionInFaction(player, FactionPermission.BANK_LOGS);
    }
}