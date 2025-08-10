package me.elite.Factions.commands;

import com.mojang.brigadier.Message;
import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.permissions.PermissionManager;
import me.elite.Factions.territory.ChunkCoord;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.Rank;
import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Relation;
import me.elite.Factions.data.FactionPermission;
import me.elite.Factions.data.RelationRequest;
import me.elite.Factions.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.Set;
import java.util.HashSet;

import java.util.*;

public class CommandManager implements CommandExecutor {
    private final FactionsPlugin plugin;
    private final Map<String, Faction> factions;
    private final Map<UUID, String> playerFactions;
    private final Map<String, Map<ChunkCoord, String>> worldClaims;
    private final Map<UUID, Set<String>> playerInvitations;
    private final Map<UUID, String> pendingOwnershipTransfers = new HashMap<>();

    public CommandManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
        this.worldClaims = plugin.getWorldClaims();
        this.playerInvitations = plugin.getPlayerInvitations();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        if (args.length == 0) {
            // If no args provided, show menu
            return handleMenu(player, new String[]{"menu"});
        }

        switch (args[0].toLowerCase()) {
            case "create":
                return handleWithUsage(args[0].toLowerCase(), "handleCreate", player, args);
            case "promote":
            case "demote":
                return handleWithUsage(args[0].toLowerCase(), "handlePromoteDemote", player, args);
            case "desc":
                return handleWithUsage(args[0].toLowerCase(), "handleDescription", player, args);
            case "claim":
                return handleWithUsage(args[0].toLowerCase(), "handleClaim", player, args);
            case "map":
                return handleWithUsage(args[0].toLowerCase(), "handleMap", player, args);
            case "adminclaim":
                return handleWithUsage(args[0].toLowerCase(), "handleAdminClaim", player, args);
            case "unclaim":
                return handleWithUsage(args[0].toLowerCase(), "handleUnclaim", player, args);
            case "adminunclaim":
                return handleWithUsage(args[0].toLowerCase(), "handleAdminUnclaim", player, args);
            case "unclaimall":
                return handleWithUsage(args[0].toLowerCase(), "handleUnclaimAll", player, args);
            case "adminunclaimall":
                return handleWithUsage(args[0].toLowerCase(), "handleAdminUnclaimAll", player, args);
            case "loadall":
                return handleWithUsage(args[0].toLowerCase(), "handleLoadAll", player, args);
            case "load":
                return handleWithUsage(args[0].toLowerCase(), "handleLoad", player, args);
            case "invite":
                return handleWithUsage(args[0].toLowerCase(), "handleInvite", player, args);
            case "kick":
                return handleWithUsage(args[0].toLowerCase(), "handleKick", player, args);
            case "menu":
                return handleWithUsage(args[0].toLowerCase(), "handleMenu", player, args);
            case "adminjoin":
                return handleWithUsage(args[0].toLowerCase(), "handleAdminJoin", player, args);
            case "join":
                return handleWithUsage(args[0].toLowerCase(), "handleJoin", player, args);
            case "leave":
                return handleWithUsage(args[0].toLowerCase(), "handleLeave", player, args);
            case "disband":
                return handleWithUsage(args[0].toLowerCase(), "handleDisband", player, args);
            case "invitations":
            case "invites":
                return handleWithUsage(args[0].toLowerCase(), "handleViewInvitations", player, args);
            case "privacy":
                return handleWithUsage(args[0].toLowerCase(), "handlePrivacy", player, args);
            case "createtestfactions":
                return handleWithUsage(args[0].toLowerCase(), "handleCreateTestFactions", player, args);
            case "removetestfactions":
                return handleWithUsage(args[0].toLowerCase(), "handleRemoveTestFactions", player, args);
            case "enemy":
                return handleWithUsage(args[0].toLowerCase(), "handleEnemy", player, args);
            case "neutral":
                return handleWithUsage(args[0].toLowerCase(), "handleNeutral", player, args);
            case "ally":
                return handleWithUsage(args[0].toLowerCase(), "handleAlly", player, args);
            case "truce":
                return handleWithUsage(args[0].toLowerCase(), "handleTruce", player, args);
            case "confirm":
                return handleWithUsage(args[0].toLowerCase(), "handleConfirmOwnership", player, args);
            case "cancel":
                return handleWithUsage(args[0].toLowerCase(), "handleCancelOwnership", player, args);
            case "debugnametags":
                return handleWithUsage(args[0].toLowerCase(), "handleDebugNametags", player, args);
            case "power":
                return handleWithUsage(args[0].toLowerCase(), "handlePower", player, args);
            case "debuginfo":
                return handleWithUsage(args[0].toLowerCase(), "handleDebugInfo", player, args);
            default:
                return false;
        }
    }

    // Helper that returns a usage string for a given subcommand (modify/add entries as you expand commands)
    private String getUsageFor(String subCommand) {
        Map<String, String> usages = new HashMap<>();

        usages.put("create", "Usage: /f create <name> - Create a faction.");
        usages.put("promote", "Usage: /f promote <player> - Promote a faction member.");
        usages.put("demote", "Usage: /f demote <player> - Demote a faction member.");
        usages.put("desc", "Usage: /f desc <description> - Set the faction description.");
        usages.put("claim", "Usage: /f claim - Claim the chunk you're standing in.");
        usages.put("unclaim", "Usage: /f unclaim - Unclaim the chunk you're standing in.");
        usages.put("adminclaim", "Usage: /f adminclaim <faction> - Admin claim land for a faction.");
        usages.put("map", "Usage: /f map - Show the faction territory map.");
        usages.put("invite", "Usage: /f invite <player> - Invite a player to your faction.");
        usages.put("kick", "Usage: /f kick <player> - Kick a player from your faction.");
        usages.put("join", "Usage: /f join <faction> - Join a faction (public or invited).");
        usages.put("leave", "Usage: /f leave - Leave your current faction.");
        usages.put("disband", "Usage: /f disband - Disband your faction.");
        usages.put("invitations", "Usage: /f invites - View your faction invitations.");
        usages.put("privacy", "Usage: /f privacy <open|invite|closed> - Change faction privacy.");
        usages.put("createtestfactions", "Usage: /f createtestfactions <count> - Create test factions.");
        usages.put("removetestfactions", "Usage: /f removetestfactions - Remove test factions.");
        usages.put("enemy", "Usage: /f enemy <faction> - Set enemy relation.");
        usages.put("neutral", "Usage: /f neutral <faction> - Set neutral relation.");
        usages.put("ally", "Usage: /f ally <faction> - Set ally relation.");
        usages.put("truce", "Usage: /f truce <faction> - Set truce relation.");
        usages.put("confirm", "Usage: /f confirm - Confirm pending ownership/claim.");
        usages.put("cancel", "Usage: /f cancel - Cancel pending ownership/claim.");
        usages.put("debugnametags", "Usage: /f debugnametags - Show nametag debug info.");
        usages.put("load", "Usage: /f load <faction> - Load a specific faction's data.");
        usages.put("loadall", "Usage: /f loadall - Load all factions (admin).");
        usages.put("adminunclaim", "Usage: /f adminunclaim <faction> - Admin unclaim.");
        usages.put("unclaimall", "Usage: /f unclaimall - Unclaim all land for your faction.");
        usages.put("adminunclaimall", "Usage: /f adminunclaimall - Admin unclaim all.");
        usages.put("adminjoin", "Usage: /f adminjoin <player> <faction> - Force join a player to a faction.");
        usages.put("power", "Usage: /f power - View your faction's power information.");
        usages.put("debuginfo", "Usage: /f debuginfo - Debug: Show detailed power breakdown.");

        String key = subCommand == null ? "" : subCommand.toLowerCase();
        if (usages.containsKey(key)) return usages.get(key);
        return "Usage: /f " + key + " [args] - Invalid or missing arguments.";
    }

    private boolean handleDebugInfo(Player player, String[] args) {
        if (!player.isOp()) {
            MessageManager.sendError(player, "You must be an operator to use debug commands.");
            return true;
        }

        MessageManager.sendBasicMessage(player, "§6§l=== DEBUG POWER INFO ===");

        // Personal power info
        UUID uuid = player.getUniqueId();
        int currentPower = plugin.getPowerManager().getPlayerPower(uuid);
        int maxPower = plugin.getPowerManager().getPlayerMaxPower(uuid);
        MessageManager.sendBasicMessage(player, "§eYour Power Details:");
        MessageManager.sendBasicMessage(player, "  §7Current: §f" + currentPower + " §7Max: §f" + maxPower);

        // Faction power breakdown
        String factionName = playerFactions.get(uuid);
        if (factionName != null) {
            MessageManager.sendBasicMessage(player, "§eFaction Power Breakdown:");
            MessageManager.sendBasicMessage(player, "  §7Faction: §f" + factionName);

            int totalPower = plugin.getPowerManager().getFactionEffectivePower(factionName);
            int usedPower = plugin.getPowerManager().getFactionUsedPower(factionName);
            int availablePower = plugin.getPowerManager().getFactionAvailablePower(factionName);

            MessageManager.sendBasicMessage(player, "  §7Total Faction Power: §f" + totalPower);
            MessageManager.sendBasicMessage(player, "  §7Power Used: §c" + usedPower);
            MessageManager.sendBasicMessage(player, "  §7Available Power: §a" + availablePower);

            // Calculate claims info
            int currentClaims = usedPower / 2; // Each claim costs 2 power
            int maxPossibleClaims = totalPower / 2;
            MessageManager.sendBasicMessage(player, "  §7Current Claims: §f" + currentClaims + " chunks");
            MessageManager.sendBasicMessage(player, "  §7Max Possible Claims: §f" + maxPossibleClaims + " chunks");
        }

        MessageManager.sendBasicMessage(player, "§6§l======================");
        return true;
    }

    private boolean handlePower(Player player, String[] args) {
        plugin.getPowerManager().sendPowerInfo(player);
        return true;
    }

    private boolean handleWithUsage(String subCommand, String handlerMethodName, Player player, String[] args) {
        try {
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod(handlerMethodName, Player.class, String[].class);
            m.setAccessible(true);
            Object ret = m.invoke(this, new Object[]{player, args});
            boolean ok = true;
            if (ret instanceof Boolean) ok = (Boolean) ret;

            // If the handler returns false -> wrong usage, show helpful usage message (custom per subcommand)
            if (!ok) {
                String usage = getUsageFor(subCommand);
                MessageManager.sendInfo(player, usage);
                return true; // we handled the error message here
            }

            return true;
        } catch (java.lang.reflect.InvocationTargetException ite) {
            ite.printStackTrace();
            MessageManager.sendError(player, "An error occurred while running that command.");
            return true;
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException e) {
            e.printStackTrace();
            MessageManager.sendError(player, "An error occurred while running that command.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            MessageManager.sendError(player, "An unexpected error occurred.");
            return true;
        }
    }

    private boolean handleDebugNametags(Player player, String[] args) {
        if (!player.isOp()) {
            MessageManager.sendError(player, "You must be an operator to use this command.");
            return true;
        }

        if (args.length < 2) {
            MessageManager.sendInfo(player, "Usage: /f debugnametags <refresh|info|test>");
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "refresh":
                MessageManager.sendInfo(player, "Refreshing all nametags...");
                plugin.getNametagManager().forceRefreshAll();
                MessageManager.sendSuccess(player, "Nametag refresh complete!");
                break;

            case "info":
                UUID uuid = player.getUniqueId();
                String factionName = playerFactions.get(uuid);
                MessageManager.sendInfo(player, "=== NAMETAG DEBUG INFO ===");
                MessageManager.sendInfo(player, "Your faction: " + (factionName != null ? factionName : "None"));

                // Show what suffixes you should see for other players
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (!other.equals(player)) {
                        String otherFaction = playerFactions.get(other.getUniqueId());
                        String suffix = getDebugSuffix(player, other);
                        MessageManager.sendInfo(player, other.getName() + " (" +
                                (otherFaction != null ? otherFaction : "No faction") + "): " + suffix);
                    }
                }
                break;

            case "test":
                // Force update nametags for the player
                plugin.getNametagManager().onFactionChange(player);
                MessageManager.sendSuccess(player, "Triggered nametag update for you!");
                break;

            default:
                MessageManager.sendError(player, "Unknown debug option. Use: refresh, info, or test");
                break;
        }

        return true;
    }

    private boolean handleEnemy(Player player, String[] args) {
        return handleDirectRelationCommand(player, args, Relation.ENEMY);
    }

    private boolean handleNeutral(Player player, String[] args) {
        return handleDirectRelationCommand(player, args, Relation.NEUTRAL);
    }

    private boolean handleDirectRelationCommand(Player player, String[] args, Relation relation) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return true;
        }

        if (args.length < 2) {
            MessageManager.sendError(player,"Usage: /f " + relation.name().toLowerCase() + " <FactionName>");
            return true;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        // Check permission
        if (!faction.hasPermission(playerRank, FactionPermission.SET_RELATIONS)) {
            MessageManager.sendError(player, "You lack permission to set faction relations.");
            return true;
        }

        String targetFaction = args[1];

        if (!factions.containsKey(targetFaction)) {
            MessageManager.sendError(player, "Faction '" + targetFaction + "' does not exist.");
            return true;
        }

        if (targetFaction.equals(factionName)) {
            MessageManager.sendError(player, "You cannot set relations with your own faction.");
            return true;
        }

        boolean success = plugin.getRelationManager().setDirectRelation(factionName, targetFaction, relation, uuid);
        if (success) {
            MessageManager.sendRelationSet(player, targetFaction, 
                    plugin.getRelationManager().getRelationColor(relation) + relation.getDisplayName());

            // Save data
            plugin.getDataManager().saveFactionData();
        } else {
            MessageManager.sendError(player, "Failed to set relation.");
        }

        return true;
    }

    private boolean handleAlly(Player player, String[] args) {
        return handleRelationRequest(player, args, Relation.ALLY);
    }

    private boolean handleTruce(Player player, String[] args) {
        return handleRelationRequest(player, args, Relation.TRUCE);
    }

    private boolean handleRelationRequest(Player player, String[] args, Relation relation) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return true;
        }

        if (args.length < 2) {
            MessageManager.sendError(player, "Usage: /f " + relation.name().toLowerCase() + " <FactionName>");
            return true;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        // Check permission
        if (!faction.hasPermission(playerRank, FactionPermission.SET_RELATIONS)) {
            MessageManager.sendError(player, "You lack permission to set faction relations.");
            return true;
        }

        String targetFaction = args[1];

        if (!factions.containsKey(targetFaction)) {
            MessageManager.sendError(player, "Faction '" + targetFaction + "' does not exist.");
            return true;
        }

        if (targetFaction.equals(factionName)) {
            MessageManager.sendError(player, "You cannot set relations with your own faction.");
            return true;
        }

        boolean success = plugin.getRelationManager().sendRelationRequest(factionName, targetFaction, relation, uuid);
        if (success) {
            MessageManager.sendSuccess(player, "Sent " +
                    plugin.getRelationManager().getRelationColor(relation) + relation.getDisplayName() +
                    ChatColor.GREEN + " request to " + targetFaction + "!");

            // Save data
            plugin.getDataManager().saveFactionData();
        } else {
            MessageManager.sendError(player, "Failed to send relation request. A request may already exist.");
        }

        return true;
    }

    private boolean handleCreateTestFactions(Player player, String[] args) {
        if (!player.isOp()) {
            MessageManager.sendError(player, "You must be an operator to use this command.");
            return true;
        }

        int count = 80; // Changed from 40 to 80
        if (args.length >= 2) {
            try {
                count = Integer.parseInt(args[1]);
                if (count <= 0 || count > 100) {
                    MessageManager.sendError(player, "Count must be between 1 and 100.");
                    return true;
                }
            } catch (NumberFormatException e) {
                MessageManager.sendError(player, "Invalid number: " + args[1]);
                return true;
            }
        }

        // Faction name templates
        String[] templates = {
                "Dragons", "Warriors", "Legends", "Knights", "Titans", "Phoenix", "Storm", "Shadow",
                "Thunder", "Lightning", "Fire", "Ice", "Steel", "Diamond", "Emerald", "Ruby",
                "Sapphire", "Golden", "Silver", "Bronze", "Elite", "Supreme", "Ultimate", "Prime",
                "Alpha", "Beta", "Gamma", "Delta", "Omega", "Nova", "Cosmic", "Stellar",
                "Royal", "Imperial", "Majestic", "Noble", "Divine", "Sacred", "Ancient", "Mystic",
                "Dark", "Light", "Blood", "Soul", "Spirit", "Ghost", "Demon", "Angel",
                "Wild", "Fierce", "Savage", "Brutal", "Gentle", "Swift", "Strong", "Mighty"
        };

        String[] suffixes = {
                "Clan", "Guild", "Order", "Legion", "Brotherhood", "Alliance", "Empire", "Kingdom",
                "Tribe", "Nation", "Republic", "Dynasty", "Society", "Council", "Union", "Federation",
                "Assembly", "Collective", "Syndicate", "Coalition", "Confederation", "Consortium"
        };

        Random random = new Random();
        int created = 0;
        int publicCount = 0;
        int privateCount = 0;
        UUID playerUUID = player.getUniqueId();

        MessageManager.sendInfo(player, "Creating " + count + " test factions...");

        for (int i = 0; i < count; i++) {
            // Generate unique faction name
            String factionName;
            int attempts = 0;
            do {
                String template = templates[random.nextInt(templates.length)];
                String suffix = suffixes[random.nextInt(suffixes.length)];
                factionName = template + suffix + (random.nextInt(999) + 1);
                attempts++;
            } while (factions.containsKey(factionName) && attempts < 50);

            if (factions.containsKey(factionName)) {
                continue; // Skip if we couldn't generate a unique name
            }

            // Create fake owner UUID
            UUID fakeOwnerUUID = UUID.nameUUIDFromBytes(("TestFaction:" + factionName).getBytes());

            // Create faction
            Faction faction = new Faction(factionName, fakeOwnerUUID);

            // Add fake owner as OWNER
            faction.members.put(fakeOwnerUUID, Rank.OWNER);

            // Add some fake members (2-8 members per faction)
            int memberCount = random.nextInt(7) + 2; // 2-8 members
            for (int j = 1; j < memberCount; j++) {
                UUID fakeMemberUUID = UUID.nameUUIDFromBytes(("TestMember:" + factionName + ":" + j).getBytes());
                Rank[] ranks = {Rank.RECRUIT, Rank.MEMBER, Rank.ADMIN};
                Rank memberRank = ranks[random.nextInt(ranks.length)];
                faction.members.put(fakeMemberUUID, memberRank);
            }

            // Set random description
            String[] descriptions = {
                    "A powerful faction ready for battle!",
                    "Join us for epic adventures and glory!",
                    "United we stand, divided we fall.",
                    "Strength through unity and honor.",
                    "Where legends are born and heroes rise.",
                    "Building an empire one block at a time.",
                    "Defenders of justice and freedom.",
                    "Masters of strategy and warfare.",
                    "Welcome to our growing community!",
                    "Together we conquer all challenges."
            };
            faction.description = descriptions[random.nextInt(descriptions.length)];

            // 70% chance to be public, 30% private
            boolean isPublic = random.nextDouble() < 0.7;
            faction.isPublic = isPublic;

            if (isPublic) {
                publicCount++;
            } else {
                privateCount++;
                // Add invitation to the command sender for private factions
                playerInvitations.putIfAbsent(playerUUID, new HashSet<>());
                playerInvitations.get(playerUUID).add(factionName);
            }

            // Add faction to the map
            factions.put(factionName, faction);
            created++;
        }

        // Send summary
        MessageManager.sendSuccess(player, "Successfully created " + created + " test factions!");
        MessageManager.sendInfo(player, "• Public factions: " + ChatColor.GREEN + publicCount);
        MessageManager.sendInfo(player, "• Private factions: " + ChatColor.BLUE + privateCount + ChatColor.GRAY + " (you've been invited to all private factions)");
        MessageManager.sendInfo(player, "Use /f browsefactions to test the browser, or /f removetestfactions to clean up.");

        // Save data
        plugin.getDataManager().saveFactionData();

        return true;
    }

    private boolean handleRemoveTestFactions(Player player, String[] args) {
        if (!player.isOp()) {
            MessageManager.sendError(player, "You must be an operator to use this command.");
            return true;
        }

        MessageManager.sendInfo(player, "Removing all test factions...");

        int removed = 0;
        Iterator<Map.Entry<String, Faction>> iterator = factions.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Faction> entry = iterator.next();
            String factionName = entry.getKey();
            Faction faction = entry.getValue();

            // Check if this is a test faction by checking if owner UUID is fake
            String ownerUUIDString = faction.owner.toString();
            if (ownerUUIDString.contains("TestFaction:")) {
                // Remove from player factions map
                Iterator<Map.Entry<UUID, String>> playerIterator = playerFactions.entrySet().iterator();
                while (playerIterator.hasNext()) {
                    Map.Entry<UUID, String> playerEntry = playerIterator.next();
                    if (playerEntry.getValue().equals(factionName)) {
                        playerIterator.remove();
                    }
                }

                // Remove from claims
                for (Map<ChunkCoord, String> worldClaim : worldClaims.values()) {
                    worldClaim.entrySet().removeIf(claim -> claim.getValue().equals(factionName));
                }

                // Remove from invitations
                for (Set<String> invites : playerInvitations.values()) {
                    invites.remove(factionName);
                }

                // Remove the faction
                iterator.remove();
                removed++;
            }
        }

        // Clean up empty invitation sets
        playerInvitations.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        MessageManager.sendSuccess(player, "Successfully removed " + removed + " test factions!");

        if (removed > 0) {
            // Save data
            plugin.getDataManager().saveFactionData();
        }

        return true;
    }

    private boolean handlePrivacy(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);
        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return true;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);
        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.OPEN_CLOSE)) {
            MessageManager.sendError(player, "You lack permission to change faction privacy settings.");
            return true;
        }

        if (args.length < 2) {
            // Show current status
            MessageManager.sendInfo(player, "Faction Privacy: " +
                    (faction.isPublic ? ChatColor.GREEN + "Public" : ChatColor.RED + "Private"));
            MessageManager.sendInfo(player, "Usage: /f privacy <public|private>");
            return true;
        }

        String setting = args[1].toLowerCase();
        if (setting.equals("public")) {
            if (faction.isPublic) {
                MessageManager.sendInfo(player, "Faction is already public.");
                return true;
            }
            faction.isPublic = true;
            MessageManager.sendFactionPrivacyToggle(player, true);

            // Notify other online faction members
            for (UUID memberUUID : faction.members.keySet()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && !member.equals(player)) {
                    MessageManager.sendMemberPrivacyToggle(player, member.getName(), "public");
                }
            }

        } else if (setting.equals("private")) {
            if (!faction.isPublic) {
                MessageManager.sendInfo(player, "Faction is already private.");
                return true;
            }
            faction.isPublic = false;
            MessageManager.sendFactionPrivacyToggle(player, false);

            // Notify other online faction members
            for (UUID memberUUID : faction.members.keySet()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && !member.equals(player)) {
                    MessageManager.sendMemberPrivacyToggle(player, member.getName(), "private");
                }
            }

        } else {
            MessageManager.sendError(player, "Invalid option. Use 'public' or 'private'.");
            MessageManager.sendInfo(player, "Usage: /f privacy <public|private>");
            return true;
        }

        // Save data
        plugin.getDataManager().saveFactionData();
        return true;
    }

    private boolean handleMenu(Player player, String[] args) {
        plugin.openFactionsMenu(player);
        return true;
    }

    private boolean handleAdminJoin(Player player, String[] args) {
        if (!(player.isOp() || player.hasPermission("factions.adminjoin"))) {
            MessageManager.sendError(player, "You lack permission to use this command.");
            return true;
        }
        if (args.length < 3) {
            MessageManager.sendError(player, "Usage: /f adminjoin <FactionName> <PlayerName>");
            return true;
        }

        String factionName = args[1];
        String playerName = args[2];

        if (!factions.containsKey(factionName)) {
            MessageManager.sendError(player, "Faction '" + factionName + "' does not exist.");
            return true;
        }

        // Create a fake offline player UUID (for testing)
        UUID fakeUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());

        if (playerFactions.containsKey(fakeUUID)) {
            MessageManager.sendError(player, "Player " + playerName + " is already in a faction.");
            return true;
        }

        Faction f = factions.get(factionName);
        f.members.put(fakeUUID, Rank.RECRUIT);
        playerFactions.put(fakeUUID, factionName);

        MessageManager.sendSuccess(player, "Successfully added fake player " + playerName + " to faction " + factionName + "!");
        return true;
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) return false;
        UUID uuid = player.getUniqueId();

        if (playerFactions.containsKey(uuid)) {
            MessageManager.sendError(player,"You are already in a faction.");
            return true;
        }

        String name = args[1];
        if (factions.containsKey(name)) {
            MessageManager.sendError(player,"A faction with that name already exists.");
            return true;
        }

        Faction f = new Faction(name, uuid);
        f.members.put(uuid, Rank.OWNER);
        factions.put(name, f);
        playerFactions.put(uuid, name);
        MessageManager.sendFactionCreated(player, name);

        plugin.getEventListener().onPlayerJoinFaction(player);

        return true;
    }

    private boolean handlePromoteDemote(Player player, String[] args) {
        if (args.length < 2) return false;
        UUID uuid = player.getUniqueId();
        boolean isPromotion = args[0].equalsIgnoreCase("promote");

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            MessageManager.sendError(player, "Player not found or not online.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        String factionName = playerFactions.get(uuid);

        // Check if both players are in the same faction
        if (factionName == null || !factionName.equals(playerFactions.get(targetUUID))) {
            MessageManager.sendError(player, "You must be in the same faction as " + targetName + ".");
            return true;
        }

        // Can't promote/demote yourself
        if (uuid.equals(targetUUID)) {
            MessageManager.sendError(player, "You cannot " + (isPromotion ? "promote" : "demote") + " yourself.");
            return true;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);
        Rank targetRank = faction.members.getOrDefault(targetUUID, Rank.RECRUIT);

        // Check if player has the required permission
        FactionPermission requiredPermission = isPromotion ? FactionPermission.PROMOTE_MEMBERS : FactionPermission.DEMOTE_MEMBERS;

        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, requiredPermission)) {
            MessageManager.sendError(player, "You lack permission to " + (isPromotion ? "promote" : "demote") + " members.");
            return true;
        }

        // Special case: Owner trying to promote ADMIN to OWNER (ownership transfer)
        if (playerRank == Rank.OWNER && isPromotion && targetRank == Rank.ADMIN) {
            Rank newRank = targetRank.promote(); // This would be OWNER
            if (newRank == Rank.OWNER) {
                // Initiate ownership transfer confirmation
                pendingOwnershipTransfers.put(uuid, targetName);

                MessageManager.sendOwnerTransferWarning(player, targetName, factionName);

                // Schedule expiration of the confirmation
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (pendingOwnershipTransfers.containsKey(uuid)) {
                        pendingOwnershipTransfers.remove(uuid);
                        if (player.isOnline()) {
                            MessageManager.sendError(player, "Ownership transfer confirmation expired.");
                        }
                    }
                }, 20L * 30); // 30 seconds

                return true;
            }
        }

        // Check rank hierarchy rules (updated to prevent promoting to same rank)
        if (!canModifyRank(playerRank, targetRank, isPromotion)) {
            if (isPromotion) {
                if (targetRank.ordinal() >= playerRank.ordinal()) {
                    MessageManager.sendError(player, "You cannot promote " + targetName + " to or above your own rank (" +
                            playerRank.name() + ").");
                } else {
                    // Calculate what rank they would be promoted to
                    Rank wouldBe = targetRank.promote();
                    if (wouldBe.ordinal() >= playerRank.ordinal()) {
                        MessageManager.sendError(player, "You cannot promote " + targetName + " to " + wouldBe.name() +
                                " as it would be equal to your rank.");
                    } else {
                        MessageManager.sendError(player, "You cannot promote " + targetName + " beyond " +
                                getRankBelow(playerRank).name() + ".");
                    }
                }
            } else {
                if (targetRank.ordinal() >= playerRank.ordinal()) {
                    MessageManager.sendError(player, "You cannot demote " + targetName + " as they are the same rank or higher than you.");
                } else {
                    MessageManager.sendError(player, "You cannot demote " + targetName + ".");
                }
            }
            return true;
        }

        // Calculate the new rank
        Rank newRank;
        if (isPromotion) {
            newRank = targetRank.promote();
        } else {
            newRank = targetRank.demote();
        }

        // Check if the rank actually changed (prevent unnecessary operations)
        if (newRank == targetRank) {
            if (isPromotion) {
                MessageManager.sendInfo(player, targetName + " is already at the highest rank they can be promoted to.");
            } else {
                MessageManager.sendInfo(player, targetName + " is already at the lowest rank.");
            }
            return true;
        }

        // Apply the rank change
        faction.members.put(targetUUID, newRank);

        // For the target player
        if (isPromotion) {
            MessageManager.sendPromoted(target, newRank.name(), player.getName());
        } else {
            MessageManager.sendDemoted(target, newRank.name(), player.getName());
        }

        // For the promoter/demoter
        if (isPromotion) {
            MessageManager.sendPromotionSuccess(player, targetName, targetRank.name(), newRank.name());
        } else {
            MessageManager.sendDemotionSuccess(player, targetName, targetRank.name(), newRank.name());
        }

        // Notify other online faction members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(player) && !member.equals(target)) {
                if (isPromotion) {
                    MessageManager.sendPromoted(member, targetName, player.getName());
                }
                else {
                    MessageManager.sendDemoted(member, targetName, player.getName());
                }
            }
        }

        // Save data
        plugin.getDataManager().saveFactionData();

        return true;
    }

    /**
     * Handle ownership transfer cancellation
     */
    private boolean handleCancelOwnership(Player player, String[] args) {
        UUID uuid = player.getUniqueId();

        // Check for GUI context first
        String guiTargetName = plugin.getMenuHandler().getPendingGUIOwnershipTransfer(uuid);
        if (guiTargetName != null) {
            plugin.getMenuHandler().clearPendingOwnershipTransfer(uuid);
            MessageManager.sendInfo(player, "Ownership transfer to " + guiTargetName + " has been cancelled.");
            return true;
        }

        // Original command-based logic
        String targetName = pendingOwnershipTransfers.get(uuid);
        if (targetName == null) {
            MessageManager.sendError(player, "No pending ownership transfer found.");
            return true;
        }

        // Remove the pending transfer
        pendingOwnershipTransfers.remove(uuid);

        MessageManager.sendInfo(player, "Ownership transfer to " + targetName + " has been cancelled.");

        return true;
    }

    /**
     * Handle ownership transfer confirmation
     */
    private boolean handleConfirmOwnership(Player player, String[] args) {
        if (args.length < 2) {
            MessageManager.sendError(player, "Usage: /f confirm <FactionName>");
            return true;
        }

        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);
        String providedFactionName = args[1];

        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return true;
        }

        if (!factionName.equals(providedFactionName)) {
            MessageManager.sendError(player, "You must type your exact faction name: " + ChatColor.WHITE + factionName);
            return true;
        }

        // First check if this is a GUI-initiated ownership transfer
        String guiTargetName = plugin.getMenuHandler().getPendingGUIOwnershipTransfer(uuid);
        if (guiTargetName != null) {
            // Handle GUI ownership transfer
            boolean success = plugin.getMenuHandler().handleGUIOwnershipTransfer(player, guiTargetName, factionName);
            if (success) {
                // Optionally reopen the members menu
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            plugin.getMenuHandler().openMembersMenu(player, factionName);
                        }
                    }
                }.runTaskLater(plugin, 20L); // 1 second delay
            }
            return true;
        }

        // Original command-based ownership transfer logic
        String targetName = pendingOwnershipTransfers.get(uuid);
        if (targetName == null) {
            MessageManager.sendError(player, "No pending ownership transfer found or confirmation expired.");
            return true;
        }

        // Remove the pending transfer
        pendingOwnershipTransfers.remove(uuid);

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            MessageManager.sendError(player, "Target player " + targetName + " is no longer online.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();

        // Verify target is still in the faction and is ADMIN
        if (!factionName.equals(playerFactions.get(targetUUID))) {
            MessageManager.sendError(player, targetName + " is no longer in your faction.");
            return true;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);
        Rank targetRank = faction.members.get(targetUUID);

        if (playerRank != Rank.OWNER) {
            MessageManager.sendError(player, "You are no longer the faction owner.");
            return true;
        }

        if (targetRank != Rank.ADMIN) {
            MessageManager.sendError(player, targetName + " is no longer an Admin.");
            return true;
        }

        // Perform the ownership transfer
        faction.members.put(targetUUID, Rank.OWNER); // Promote target to OWNER
        faction.members.put(uuid, Rank.ADMIN);       // Demote current owner to ADMIN
        faction.owner = targetUUID;                  // Update faction owner field

        // Send messages
        MessageManager.sendOwnerTransferSuccess(player, target, factionName);

        // Notify all other faction members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(player) && !member.equals(target)) {
                MessageManager.sendMemberOwnerTransferSuccess(member, player, targetName, factionName);
            }
        }

        // Save data
        plugin.getDataManager().saveFactionData();

        return true;
    }

    /**
     * Check if a player can modify (promote/demote) another player's rank
     * Updated to prevent promoting to same rank unless you're owner doing ownership transfer
     */
    private boolean canModifyRank(Rank playerRank, Rank targetRank, boolean isPromotion) {
        // Owners can do anything except the special ownership transfer case (handled separately)
        if (playerRank == Rank.OWNER) {
            return true;
        }

        if (isPromotion) {
            // Can only promote players below your rank
            if (targetRank.ordinal() >= playerRank.ordinal()) {
                return false;
            }

            // UPDATED: Cannot promote someone to your same rank
            Rank wouldBeRank = targetRank.promote();
            if (wouldBeRank.ordinal() >= playerRank.ordinal()) {
                return false;
            }

            return true;
        } else {
            // Can only demote players below your rank (same rank or higher cannot be demoted)
            return targetRank.ordinal() < playerRank.ordinal();
        }
    }

    /**
     * Get the rank that is one level below the given rank
     */
    private Rank getRankBelow(Rank rank) {
        if (rank.ordinal() == 0) {
            return Rank.RECRUIT; // Already at the bottom
        }
        return Rank.values()[rank.ordinal() - 1];
    }

    private boolean handleDescription(Player player, String[] args) {
        if (args.length < 2) return false;
        UUID uuid = player.getUniqueId();

        String factionName = playerFactions.get(uuid);
        if (factionName == null) {
            MessageManager.sendError(player,"You are not in a faction.");
            return true;
        }

        Faction f = factions.get(factionName);
        Rank rank = f.members.get(uuid);
        if (rank != Rank.OWNER && !f.hasPermission(rank, FactionPermission.CHANGE_DESCRIPTION)) {
            MessageManager.sendError(player,"You lack permission.");
            return true;
        }

        f.description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        MessageManager.sendSuccess(player,"Faction description updated.");
        return true;
    }

    private boolean handleClaim(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);
        if (factionName == null) {
            MessageManager.sendError(player,"You are not in a faction.");
            return true;
        }

        Faction f = factions.get(factionName);
        Rank rank = f.members.get(uuid);
        if (rank != Rank.OWNER && rank != Rank.ADMIN) {
            MessageManager.sendError(player,"You lack permission.");
            return true;
        }

        return plugin.getClaimManager().claimChunk(player, factionName);
    }

    private boolean handleMap(Player player, String[] args) {
        plugin.getClaimManager().displayFactionMap(player);
        return true;
    }

    private boolean handleAdminClaim(Player player, String[] args) {
        if (!(player.isOp() || player.hasPermission("factions.adminclaim"))) {
            MessageManager.sendError(player, "You lack permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            MessageManager.sendError(player, "Usage: /f adminclaim <FactionName>");
            return true;
        }

        String factionName = args[1];
        return plugin.getClaimManager().adminClaim(player, factionName);
    }

    private boolean handleUnclaim(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);
        if (factionName == null) {
            MessageManager.sendError(player,"You are not in a faction.");
            return true;
        }

        Faction f = factions.get(factionName);
        Rank rank = f.members.get(uuid);
        if (rank != Rank.OWNER && rank != Rank.ADMIN) {
            MessageManager.sendError(player,"You lack permission.");
            return true;
        }

        return plugin.getClaimManager().unclaimChunk(player, factionName);
    }

    private boolean handleAdminUnclaim(Player player, String[] args) {
        if (!(player.isOp() || player.hasPermission("factions.adminunclaim"))) {
            MessageManager.sendError(player, "You lack permission to use this command.");
            return true;
        }

        return plugin.getClaimManager().adminUnclaim(player);
    }

    private boolean handleUnclaimAll(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);
        if (factionName == null) {
            MessageManager.sendError(player,"You are not in a faction.");
            return true;
        }

        Faction f = factions.get(factionName);
        Rank rank = f.members.get(uuid);
        if (rank != Rank.OWNER && rank != Rank.ADMIN) {
            MessageManager.sendError(player,"You lack permission.");
            return true;
        }

        plugin.getClaimManager().unclaimAll(player, factionName);
        return true;
    }

    private boolean handleAdminUnclaimAll(Player player, String[] args) {
        if (!(player.isOp() || player.hasPermission("factions.adminunclaimall"))) {
            MessageManager.sendError(player, "You lack permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            MessageManager.sendError(player, "Usage: /f adminunclaimall <FactionName>");
            return true;
        }

        String targetFaction = args[1];
        plugin.getClaimManager().adminUnclaimAll(player, targetFaction);
        return true;
    }

    private boolean handleLoadAll(Player player, String[] args) {
        if (!(player.isOp() || player.hasPermission("factions.loadall"))) {
            MessageManager.sendError(player, "You lack permission to use this command.");
            return true;
        }

        MessageManager.sendInfo(player, "Starting wilderness initialization for ALL worlds...");
        MessageManager.sendInfo(player, "This may take several minutes. Check console for progress.");

        plugin.getClaimManager().initializeAllWorlds(player);
        return true;
    }

    private boolean handleLoad(Player player, String[] args) {
        if (!(player.isOp() || player.hasPermission("factions.load"))) {
            MessageManager.sendError(player, "You lack permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            MessageManager.sendError(player, "Usage: /f load <WorldName>");
            return true;
        }

        String worldName = args[1];
        World targetWorld = Bukkit.getWorld(worldName);
        if (targetWorld == null) {
            MessageManager.sendError(player, "World '" + worldName + "' not found!");
            return true;
        }

        MessageManager.sendInfo(player, "Starting wilderness initialization for world: " + worldName);
        MessageManager.sendInfo(player, "This may take several minutes. Check console for progress.");

        plugin.getClaimManager().initializeSingleWorld(targetWorld, player);
        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);
        if (factionName == null) {
            MessageManager.sendError(player,"You are not in a faction.");
            return true;
        }
        if (args.length < 2) {
            MessageManager.sendError(player, "Usage: /f invite <PlayerName>");
            return true;
        }

        Faction f = factions.get(factionName);
        Rank rank = f.members.get(uuid);
        if (rank != Rank.OWNER && rank != Rank.ADMIN) {
            MessageManager.sendError(player,"You lack permission to invite players.");
            return true;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            MessageManager.sendError(player,"Player '" + targetName + "' not found or not online.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        if (playerFactions.containsKey(targetUUID)) {
            MessageManager.sendError(player,"Player " + targetName + " is already in a faction.");
            return true;
        }

        // Add invitation
        playerInvitations.putIfAbsent(targetUUID, new HashSet<>());
        Set<String> invites = playerInvitations.get(targetUUID);

        if (invites.contains(factionName)) {
            MessageManager.sendInfo(player, targetName + " has already been invited to " + factionName + ".");
            return true;
        }

        invites.add(factionName);

        // Notify both players
        MessageManager.sendInvitationSent(player, targetName, factionName);
        MessageManager.sendInvitationReceived(target, player.getName(), factionName);

        return true;
    }

    private boolean handleJoin(Player player, String[] args) {
        UUID uuid = player.getUniqueId();

        if (playerFactions.containsKey(uuid)) {
            MessageManager.sendError(player, "You are already in a faction. Leave your current faction first.");
            return true;
        }

        if (args.length < 2) {
            MessageManager.sendError(player, "Usage: /f join <FactionName>");
            return true;
        }

        String factionName = args[1];
        Faction faction = factions.get(factionName);
        if (faction == null) {
            MessageManager.sendError(player, "Faction '" + factionName + "' does not exist.");
            return true;
        }

        // Check if faction is public or player has invitation
        boolean canJoin = faction.isPublic;
        Set<String> invites = playerInvitations.get(uuid);
        boolean hasInvitation = invites != null && invites.contains(factionName);

        if (!canJoin && !hasInvitation) {
            MessageManager.sendError(player, "You cannot join " + factionName + ". This faction is private and you haven't been invited.");
            return true;
        }

        // Remove the invitation regardless of how they're joining (public or invited)
        if (hasInvitation) {
            invites.remove(factionName);
            if (invites.isEmpty()) {
                playerInvitations.remove(uuid);
            }
        }

        // Join the faction
        faction.members.put(uuid, Rank.RECRUIT);
        playerFactions.put(uuid, factionName);

        MessageManager.sendFactionJoined(player, factionName);

        // UPDATE NAMETAGS when player joins faction
        plugin.getEventListener().onPlayerJoinFaction(player);

        // Notify other online faction members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(player)) {
                MessageManager.sendMemberJoined(member, player.getName());
            }
        }

        return true;
    }

    private boolean handleLeave(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return true;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        if (playerRank == Rank.OWNER) {
            MessageManager.sendError(player, "You cannot leave as the faction owner. Use /f disband to disband the faction.");
            return true;
        }

        // Remove player from faction
        faction.members.remove(uuid);
        playerFactions.remove(uuid);

        MessageManager.sendFactionLeft(player, factionName);

        // UPDATE NAMETAGS when player leaves faction
        plugin.getEventListener().onPlayerLeaveFaction(player);

        // Notify other online faction members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null) {
                MessageManager.sendMemberLeft(member, player.getDisplayName());
            }
        }

        return true;
    }

    private boolean handleDisband(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return true;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        if (playerRank != Rank.OWNER) {
            MessageManager.sendError(player, "Only the faction owner can disband the faction.");
            return true;
        }

        // Confirm disbanding
        MessageManager.sendError(player, "" + ChatColor.BOLD + "WARNING: " + ChatColor.RED + "Are you sure you want to disband " + factionName + "?");
        MessageManager.sendInfo(player, "Type " + ChatColor.RED + "/f disband confirm" + ChatColor.YELLOW + " to confirm.");

        if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
            // Actually disband the faction
            disbandFaction(factionName, player);
        }

        return true;
    }

    private boolean handleViewInvitations(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        Set<String> invites = playerInvitations.get(uuid);

        if (invites == null || invites.isEmpty()) {
            MessageManager.sendInfo(player, "You have no pending faction invitations.");
            return true;
        }

        MessageManager.sendSuccess(player, "Your faction invitations:");
        for (String factionName : invites) {
            MessageManager.sendInfo(player, "• " + factionName + ChatColor.GRAY + " - Use /f join " + factionName);
        }

        return true;
    }

    private void disbandFaction(String factionName, Player disbander) {
        Faction faction = factions.get(factionName);
        if (faction == null) return;

        // Notify all members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null) {
                MessageManager.sendMemberFactionDisbanded(member, factionName, disbander.getDisplayName());
            }
            playerFactions.remove(memberUUID);
        }

        // Remove all claims
        for (Map<ChunkCoord, String> worldClaim : worldClaims.values()) {
            worldClaim.entrySet().removeIf(entry -> entry.getValue().equals(factionName));
        }

        // Remove faction from invitations
        for (Set<String> invites : playerInvitations.values()) {
            invites.remove(factionName);
        }
        playerInvitations.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        // Remove faction
        factions.remove(factionName);

        MessageManager.sendDisbandSuccess(disbander, factionName);
    }

    private boolean handleKick(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);
        if (factionName == null) {
            MessageManager.sendError(player, "You are not in a faction.");
            return true;
        }
        if (args.length < 2) {
            MessageManager.sendError(player, "Usage: /f kick <PlayerName>");
            return true;
        }

        Faction f = factions.get(factionName);
        Rank playerRank = f.members.get(uuid);
        if (playerRank != Rank.OWNER || !f.hasPermission(playerRank, FactionPermission.KICK_MEMBERS)) {
            MessageManager.sendError(player, "You lack permission to kick players.");
            return true;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        UUID targetUUID;

        if (target != null) {
            targetUUID = target.getUniqueId();
        } else {
            // Try to find offline player by name
            targetUUID = null;
            for (Map.Entry<UUID, String> entry : playerFactions.entrySet()) {
                if (entry.getValue().equals(factionName)) {
                    String storedName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    if (storedName != null && storedName.equalsIgnoreCase(targetName)) {
                        targetUUID = entry.getKey();
                        break;
                    }
                }
            }
            if (targetUUID == null) {
                MessageManager.sendError(player,"Player '" + targetName + "' not found or not in your faction.");
                return true;
            }
        }

        // Check if target is in the same faction
        String targetFactionName = playerFactions.get(targetUUID);
        if (!factionName.equals(targetFactionName)) {
            MessageManager.sendError(player,"Player " + targetName + " is not in your faction.");
            return true;
        }

        // Check if trying to kick yourself
        if (targetUUID.equals(uuid)) {
            MessageManager.sendError(player,"You cannot kick yourself from the faction.");
            return true;
        }

        // Check rank permissions
        Rank targetRank = f.members.get(targetUUID);
        if (playerRank == Rank.ADMIN && (targetRank == Rank.ADMIN || targetRank == Rank.OWNER)) {
            MessageManager.sendError(player,"You cannot kick a player of equal or higher rank.");
            return true;
        }
        if (targetRank == Rank.OWNER) {
            MessageManager.sendError(player,"You cannot kick the faction owner.");
            return true;
        }

        // Remove player from faction
        f.members.remove(targetUUID);
        playerFactions.remove(targetUUID);

        // Update nametags
        if (target.isOnline()) {
            plugin.getEventListener().onPlayerLeaveFaction((Player) target);
        }

        // Notify both players
        MessageManager.sendKickSuccess(player, targetName, factionName);
        if (target != null) {
            MessageManager.sendKicked(target, factionName, player.getName());

            // UPDATE NAMETAGS when player is kicked
            plugin.getEventListener().onPlayerLeaveFaction(target);
        }

        return true;
    }

    private String getDebugSuffix(Player viewer, Player target) {
        String viewerFaction = playerFactions.get(viewer.getUniqueId());
        String targetFaction = playerFactions.get(target.getUniqueId());

        if (viewerFaction == null) return "None (no faction)";
        if (targetFaction == null) return "None (target no faction)";
        if (viewerFaction.equals(targetFaction)) return ChatColor.GREEN + "Green F (same faction)";

        me.elite.Factions.data.Relation relation = plugin.getRelationManager().getRelation(viewerFaction, targetFaction);
        switch (relation) {
            case ALLY: return ChatColor.LIGHT_PURPLE + "Purple A (ally)";
            case TRUCE: return ChatColor.BLUE + "Blue T (truce)";
            case ENEMY: return ChatColor.RED + "Red E (enemy)";
            default: return "None (neutral)";
        }
    }
}