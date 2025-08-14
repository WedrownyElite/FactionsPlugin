package me.elite.Factions.commands;

import me.elite.Factions.FactionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import me.elite.Factions.data.Faction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Set;

public class FactionsTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>();

            // Basic commands everyone can see
            commands.addAll(Arrays.asList(
                    // Core essentials - what new players need first
                    "menu", "help", "h", "?", "create", "join",

                    // Basic faction management
                    "invite", "inv", "invitations", "invites", "invs", "leave",

                    // Territory and navigation
                    "claim", "home", "sethome", "map", "m", "warp", "warps",

                    // Economy and bank
                    "bank", "worth", "worthtop", "wtop",

                    // Member management
                    "kick", "promote", "demote", "privacy",

                    // Diplomacy and relations
                    "ally", "a", "truce", "t", "neutral", "n", "enemy", "e",

                    // Advanced features
                    "power", "p", "desc", "setwarp", "delhome", "delwarp",

                    // Territory management
                    "unclaim", "unclaimall",

                    // Confirmations and destructive actions
                    "confirm", "cancel", "disband"
            ));

            // Admin commands - only show if player has permission or is op
            if (sender.isOp() || sender.hasPermission("factions.adminclaim")) {
                commands.add("adminclaim");
            }
            if (sender.isOp() || sender.hasPermission("factions.adminunclaim")) {
                commands.add("adminunclaim");
            }
            if (sender.isOp() || sender.hasPermission("factions.adminunclaimall")) {
                commands.add("adminunclaimall");
            }
            if (sender.isOp() || sender.hasPermission("factions.loadall")) {
                commands.add("loadall");
            }
            if (sender.isOp() || sender.hasPermission("factions.load")) {
                commands.add("load");
            }
            if (sender.isOp() || sender.hasPermission("factions.adminjoin")) {
                commands.add("adminjoin");
            }
            if (sender.isOp()) {
                commands.addAll(Arrays.asList("debuginfo", "debugnametags", "reload", "rl",
                        "createtestfactions", "removetestfactions", "recalcworth", "recalc"));
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
                // No tab completion for faction names (player creates their own)
                return Collections.emptyList();

            case "promote":
            case "demote":
                // Show players in the same faction (filtered by typed partial)
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    FactionsPlugin factionsPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
                    if (factionsPlugin != null) {
                        return factionsPlugin.getUtilityManager().getFactionMembers(player.getUniqueId()).stream()
                                .filter(name -> !name.equals(player.getName())) // Don't show self
                                .filter(name -> name.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();

            case "desc":
                // No tab completion for descriptions
                return Collections.emptyList();

            case "invite":
            case "inv":
                // Show online players who aren't already in a faction (filtered by typed partial)
                FactionsPlugin invitePlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
                if (invitePlugin != null && args.length == 2) {
                    return Bukkit.getOnlinePlayers().stream()
                            .filter(p -> p.getName().toLowerCase().startsWith(partial))
                            .filter(p -> !invitePlugin.getUtilityManager().isPlayerInFaction(p.getUniqueId()))
                            .map(Player::getName)
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "kick":
                // Show players in the same faction as the command sender (filtered)
                if (sender instanceof Player && args.length == 2) {
                    Player player = (Player) sender;
                    FactionsPlugin kickPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
                    if (kickPlugin != null) {
                        return kickPlugin.getUtilityManager().getFactionMembers(player.getUniqueId()).stream()
                                .filter(name -> !name.equals(player.getName())) // Don't show self
                                .filter(name -> name.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();

            case "join":
                if (sender instanceof Player && args.length == 2) {
                    Player player = (Player) sender;
                    FactionsPlugin joinPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
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
                if (args.length == 2) {
                    FactionsPlugin relationPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
                    if (relationPlugin != null) {
                        return relationPlugin.getUtilityManager().getAllFactionNames().stream()
                                .filter(name -> !name.equals(getPlayerFaction(sender)))
                                .filter(n -> n.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();

            case "adminclaim":
                if ((sender.isOp() || sender.hasPermission("factions.adminclaim")) && args.length == 2) {
                    FactionsPlugin adminPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
                    if (adminPlugin != null) {
                        return adminPlugin.getUtilityManager().getAllFactionNames().stream()
                                .filter(name -> name.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();

            case "adminunclaimall":
                if ((sender.isOp() || sender.hasPermission("factions.adminunclaimall")) && args.length == 2) {
                    FactionsPlugin adminPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
                    if (adminPlugin != null) {
                        return adminPlugin.getUtilityManager().getAllFactionNames().stream()
                                .filter(name -> name.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();

            case "load":
                if ((sender.isOp() || sender.hasPermission("factions.load")) && args.length == 2) {
                    return Bukkit.getWorlds().stream()
                            .map(World::getName)
                            .filter(name -> name.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "adminjoin":
                if (sender.isOp() || sender.hasPermission("factions.adminjoin")) {
                    if (args.length == 2) {
                        // Second argument: faction name
                        FactionsPlugin adminPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
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
                }
                return Collections.emptyList();

            case "privacy":
                if (args.length == 2) {
                    return Arrays.asList("public", "private", "open", "invite", "closed").stream()
                            .filter(option -> option.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "createtestfactions":
                if (sender.isOp() && args.length == 2) {
                    return Arrays.asList("10", "20", "50", "80", "100").stream()
                            .filter(count -> count.startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "confirm":
                if (args.length == 2) {
                    // For ownership confirmation, suggest faction name
                    String playerFaction = getPlayerFaction(sender);
                    if (playerFaction != null && playerFaction.toLowerCase().startsWith(partial)) {
                        return Arrays.asList(playerFaction);
                    }
                }
                return Collections.emptyList();

            case "debugnametags":
                if (sender.isOp() && args.length == 2) {
                    return Arrays.asList("refresh", "info", "test").stream()
                            .filter(option -> option.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "warp":
            case "warps":
                // Show available warps for the player's faction
                if (sender instanceof Player && args.length == 2) {
                    Player player = (Player) sender;
                    FactionsPlugin warpPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
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

            case "setwarp":
                // No tab completion for new warp names
                return Collections.emptyList();

            case "delwarp":
                // Show available warps for deletion
                if (sender instanceof Player && args.length == 2) {
                    Player player = (Player) sender;
                    FactionsPlugin delWarpPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
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

            case "recalcworth":
            case "recalc":
                if ((sender.isOp() || sender.hasPermission("factions.recalcworth")) && args.length == 2) {
                    FactionsPlugin worthPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
                    if (worthPlugin != null) {
                        return worthPlugin.getUtilityManager().getAllFactionNames().stream()
                                .filter(name -> name.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();

            case "disband":
                if (args.length == 2 && args[1].isEmpty()) {
                    return Arrays.asList("confirm");
                }
                return Collections.emptyList();

            // Commands that don't need arguments or tab completion
            case "claim":
            case "map":
            case "m":
            case "unclaim":
            case "adminunclaim":
            case "unclaimall":
            case "loadall":
            case "menu":
            case "leave":
            case "invitations":
            case "invites":
            case "invs":
            case "removetestfactions":
            case "cancel":
            case "power":
            case "p":
            case "debuginfo":
            case "sethome":
            case "delhome":
            case "home":
            case "reload":
            case "rl":
            case "worth":
                return Collections.emptyList();

            default:
                return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private String getPlayerFaction(CommandSender sender) {
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;
        FactionsPlugin plugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
        if (plugin != null) {
            return plugin.getPlayerFactions().get(player.getUniqueId());
        }
        return null;
    }
}