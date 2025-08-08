package me.elite.Factions.commands;

import me.elite.Factions.FactionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

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
            commands.addAll(Arrays.asList("create", "claim", "promote", "demote", "desc", "map", "unclaim", "unclaimall", "invite", "kick", "menu", "join", "leave", "disband", "invitations", "privacy"));

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

            return commands;
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "promote":
                case "demote":
                    // Show players in the same faction
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        FactionsPlugin factionsPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
                        if (factionsPlugin != null) {
                            return factionsPlugin.getUtilityManager().getFactionMembers(player.getUniqueId()).stream()
                                    .filter(name -> !name.equals(player.getName())) // Don't show self
                                    .collect(Collectors.toList());
                        }
                    }
                    return Collections.emptyList();

                case "invite":
                    // Show online players who aren't already in a faction
                    FactionsPlugin invitePlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
                    if (invitePlugin != null) {
                        return Bukkit.getOnlinePlayers().stream()
                                .filter(p -> !invitePlugin.getUtilityManager().isPlayerInFaction(p.getUniqueId()))
                                .map(Player::getName)
                                .collect(Collectors.toList());
                    }
                    return Collections.emptyList();

                case "kick":
                    // Show players in the same faction as the command sender
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        FactionsPlugin kickPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
                        if (kickPlugin != null) {
                            return kickPlugin.getUtilityManager().getFactionMembers(player.getUniqueId()).stream()
                                    .filter(name -> !name.equals(player.getName())) // Don't show self
                                    .collect(Collectors.toList());
                        }
                    }
                    return Collections.emptyList();

                case "adminclaim":
                    // Only show if player has the required permissions
                    if (sender.isOp() || sender.hasPermission("factions.adminclaim")) {
                        return Arrays.asList("Spawn", "Warzone", "<FactionName>");
                    }
                    return Collections.emptyList();

                case "adminunclaimall":
                    // Only show if player has the required permissions
                    if (sender.isOp() || sender.hasPermission("factions.adminunclaimall")) {
                        return Arrays.asList("Spawn", "Warzone", "<FactionName>");
                    }
                    return Collections.emptyList();

                case "load":
                    // Only show if player has the required permissions
                    if (sender.isOp() || sender.hasPermission("factions.load")) {
                        return Bukkit.getWorlds().stream()
                                .map(org.bukkit.World::getName)
                                .collect(Collectors.toList());
                    }
                    return Collections.emptyList();

                case "adminjoin":
                    if (sender.isOp() || sender.hasPermission("factions.adminjoin")) {
                        // Show faction names
                        FactionsPlugin adminJoinPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
                        if (adminJoinPlugin != null) {
                            return adminJoinPlugin.getUtilityManager().getAllFactionNames();
                        }
                    }
                    return Collections.emptyList();
                case "join":
                    if (sender instanceof Player) {
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

                            return availableFactions;
                        }
                    }
                    return Collections.emptyList();

                case "privacy":
                    return Arrays.asList("public", "private");

                default:
                    return Collections.emptyList();
            }
        }

        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("adminjoin")) {
                if (sender.isOp() || sender.hasPermission("factions.adminjoin")) {
                    return Arrays.asList("<PlayerName>");
                }
            }

            return Collections.emptyList();
        }

        return Collections.emptyList();
    }
}