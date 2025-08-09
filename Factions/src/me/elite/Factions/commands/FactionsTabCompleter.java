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
            commands.addAll(Arrays.asList("create", "claim", "pr...cy", "ally", "truce", "enemy", "neutral", "confirm", "cancel"));

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

            // Filter top-level command suggestions by what the player has started typing
            String partial = args[0].toLowerCase();
            return commands.stream()
                    .filter(c -> c.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        // For second+ args we will try to filter suggestions by the partial input typed
        final String partial = args.length > 1 ? args[1].toLowerCase() : "";

        switch (args[0].toLowerCase()) {
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

            case "invite":
                // Show online players who aren't already in a faction (filtered by typed partial)
                FactionsPlugin invitePlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
                if (invitePlugin != null) {
                    return Bukkit.getOnlinePlayers().stream()
                            .filter(p -> p.getName().toLowerCase().startsWith(partial))
                            .filter(p -> !invitePlugin.getUtilityManager().isPlayerInFaction(p.getUniqueId()))
                            .map(Player::getName)
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "kick":
                // Show players in the same faction as the command sender (filtered)
                if (sender instanceof Player) {
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

                        // Filter by typed partial
                        return availableFactions.stream()
                                .filter(f -> f.toLowerCase().startsWith(partial))
                                .collect(Collectors.toList());
                    }
                }
                return Collections.emptyList();

            case "ally":
            case "truce":
                FactionsPlugin allyPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
                if (allyPlugin != null) {
                    return allyPlugin.getUtilityManager().getAllFactionNames().stream()
                            .filter(name -> !name.equals(getPlayerFaction(sender)))
                            .filter(n -> n.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            case "enemy":
            case "neutral":
                FactionsPlugin enemyPlugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
                if (enemyPlugin != null) {
                    return enemyPlugin.getUtilityManager().getAllFactionNames().stream()
                            .filter(name -> !name.equals(getPlayerFaction(sender)))
                            .filter(n -> n.toLowerCase().startsWith(partial))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();

            // (Leave other cases as-is if they already returned contextual lists or empty list)
            default:
                return Collections.emptyList();
        }
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