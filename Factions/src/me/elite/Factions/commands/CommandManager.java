package me.elite.Factions.commands;

import me.elite.Factions.territory.ChunkCoord;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.Rank;
import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Relation;
import me.elite.Factions.data.FactionPermission;
import me.elite.Factions.data.RelationRequest;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Set;
import java.util.HashSet;

import java.util.*;

public class CommandManager implements CommandExecutor {
    private final FactionsPlugin plugin;
    private final Map<String, Faction> factions;
    private final Map<UUID, String> playerFactions;
    private final Map<String, Map<ChunkCoord, String>> worldClaims;
    private final Map<UUID, Set<String>> playerInvitations;

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
                return handleCreate(player, args);
            case "promote":
            case "demote":
                return handlePromoteDemote(player, args);
            case "desc":
                return handleDescription(player, args);
            case "claim":
                return handleClaim(player, args);
            case "map":
                return handleMap(player, args);
            case "adminclaim":
                return handleAdminClaim(player, args);
            case "unclaim":
                return handleUnclaim(player, args);
            case "adminunclaim":
                return handleAdminUnclaim(player, args);
            case "unclaimall":
                return handleUnclaimAll(player, args);
            case "adminunclaimall":
                return handleAdminUnclaimAll(player, args);
            case "loadall":
                return handleLoadAll(player, args);
            case "load":
                return handleLoad(player, args);
            case "invite":
                return handleInvite(player, args);
            case "kick":
                return handleKick(player, args);
            case "menu":
                return handleMenu(player, args);
            case "adminjoin":
                return handleAdminJoin(player, args);
            case "join":
                return handleJoin(player, args);
            case "leave":
                return handleLeave(player, args);
            case "disband":
                return handleDisband(player, args);
            case "invitations":
            case "invites":
                return handleViewInvitations(player, args);
            case "privacy":
                return handlePrivacy(player, args);
            case "createtestfactions":
                return handleCreateTestFactions(player, args);
            case "removetestfactions":
                return handleRemoveTestFactions(player, args);
            case "enemy":
                return handleEnemy(player, args);
            case "neutral":
                return handleNeutral(player, args);
            case "ally":
                return handleAlly(player, args);
            case "truce":
                return handleTruce(player, args);
            default:
                return false;
        }
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
            player.sendMessage(ChatColor.RED + "You are not in a faction.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /f " + relation.name().toLowerCase() + " <FactionName>");
            return true;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        // Check permission
        if (!faction.hasPermission(playerRank, FactionPermission.SET_RELATIONS)) {
            player.sendMessage(ChatColor.RED + "You lack permission to set faction relations.");
            return true;
        }

        String targetFaction = args[1];

        if (!factions.containsKey(targetFaction)) {
            player.sendMessage(ChatColor.RED + "Faction '" + targetFaction + "' does not exist.");
            return true;
        }

        if (targetFaction.equals(factionName)) {
            player.sendMessage(ChatColor.RED + "You cannot set relations with your own faction.");
            return true;
        }

        boolean success = plugin.getRelationManager().setDirectRelation(factionName, targetFaction, relation, uuid);
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Relation with " + targetFaction + " set to: " +
                    plugin.getRelationManager().getRelationColor(relation) + relation.getDisplayName());

            // Save data
            plugin.getDataManager().saveFactionData();
        } else {
            player.sendMessage(ChatColor.RED + "Failed to set relation.");
        }

        return true;
    }

    private boolean handleRelationsInfo(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            player.sendMessage(ChatColor.RED + "You are not in a faction.");
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "═══════════════════════════════════");
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + factionName + " RELATIONS");
        player.sendMessage(ChatColor.YELLOW + "═══════════════════════════════════");

        // Show current relations
        Map<String, Relation> relations = plugin.getRelationManager().getFactionRelations(factionName);

        if (relations.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No special relations with other factions.");
            player.sendMessage(ChatColor.GRAY + "All other factions are " + ChatColor.WHITE + "Neutral" + ChatColor.GRAY + " by default.");
        } else {
            // Group by relation type
            Map<Relation, List<String>> grouped = new HashMap<>();
            for (Map.Entry<String, Relation> entry : relations.entrySet()) {
                grouped.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
            }

            // Display each relation type
            for (Relation relationType : Arrays.asList(Relation.ALLY, Relation.TRUCE, Relation.ENEMY, Relation.NEUTRAL)) {
                List<String> factionsWithRelation = grouped.get(relationType);
                if (factionsWithRelation == null || factionsWithRelation.isEmpty()) continue;

                ChatColor color = plugin.getRelationManager().getRelationColor(relationType);
                player.sendMessage(color + "" + ChatColor.BOLD + relationType.getDisplayName() + " (" + factionsWithRelation.size() + "):");

                for (String targetFaction : factionsWithRelation) {
                    Faction f = factions.get(targetFaction);
                    int memberCount = f != null ? f.members.size() : 0;
                    player.sendMessage(ChatColor.GRAY + "  • " + color + targetFaction + ChatColor.GRAY + " (" + memberCount + " members)");
                }
            }
        }

        // Show pending outgoing requests
        List<RelationRequest> outgoingRequests = new ArrayList<>();
        for (List<RelationRequest> requests : plugin.getRelationManager().getAllPendingRequests().values()) {
            for (RelationRequest request : requests) {
                if (request.fromFaction.equals(factionName)) {
                    outgoingRequests.add(request);
                }
            }
        }

        if (!outgoingRequests.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "OUTGOING REQUESTS:");
            for (RelationRequest request : outgoingRequests) {
                ChatColor color = plugin.getRelationManager().getRelationColor(request.requestedRelation);
                long timeSince = System.currentTimeMillis() - request.timestamp;
                String timeString = formatTimeString(timeSince);
                player.sendMessage(ChatColor.GRAY + "  • " + color + request.requestedRelation.getDisplayName() +
                        ChatColor.GRAY + " to " + ChatColor.WHITE + request.toFaction +
                        ChatColor.GRAY + " (" + timeString + " ago)");
            }
        }

        // Show pending incoming requests
        List<RelationRequest> incomingRequests = plugin.getRelationManager().getPendingRequests(factionName);
        if (!incomingRequests.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "INCOMING REQUESTS:");
            for (RelationRequest request : incomingRequests) {
                ChatColor color = plugin.getRelationManager().getRelationColor(request.requestedRelation);
                long timeSince = System.currentTimeMillis() - request.timestamp;
                String timeString = formatTimeString(timeSince);
                Player requester = Bukkit.getOfflinePlayer(request.requestedBy).getPlayer();
                String requesterName = requester != null ? requester.getName() : "Unknown";
                player.sendMessage(ChatColor.GRAY + "  • " + color + request.requestedRelation.getDisplayName() +
                        ChatColor.GRAY + " from " + ChatColor.WHITE + request.fromFaction +
                        ChatColor.GRAY + " by " + requesterName + " (" + timeString + " ago)");
            }

            Faction faction = factions.get(factionName);
            Rank playerRank = faction.members.get(uuid);
            if (faction.hasPermission(playerRank, FactionPermission.SET_RELATIONS)) {
                player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/f menu" + ChatColor.GRAY + " to manage incoming requests.");
            }
        }

        player.sendMessage(ChatColor.YELLOW + "═══════════════════════════════════");
        return true;
    }

    private boolean handleRelation(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            player.sendMessage(ChatColor.RED + "You are not in a faction.");
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /f relation <FactionName> <neutral|enemy>");
            return true;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        // Check permission
        if (!faction.hasPermission(playerRank, FactionPermission.SET_RELATIONS)) {
            player.sendMessage(ChatColor.RED + "You lack permission to set faction relations.");
            return true;
        }

        String targetFaction = args[1];
        String relationStr = args[2].toLowerCase();

        if (!factions.containsKey(targetFaction)) {
            player.sendMessage(ChatColor.RED + "Faction '" + targetFaction + "' does not exist.");
            return true;
        }

        if (targetFaction.equals(factionName)) {
            player.sendMessage(ChatColor.RED + "You cannot set relations with your own faction.");
            return true;
        }

        Relation relation;
        try {
            relation = Relation.valueOf(relationStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid relation. Use: neutral, enemy");
            return true;
        }

        // Only allow NEUTRAL and ENEMY for direct setting
        if (relation != Relation.NEUTRAL && relation != Relation.ENEMY) {
            player.sendMessage(ChatColor.RED + "Use /f ally or /f truce for those relations.");
            return true;
        }

        boolean success = plugin.getRelationManager().setDirectRelation(factionName, targetFaction, relation, uuid);
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Relation with " + targetFaction + " set to: " +
                    plugin.getRelationManager().getRelationColor(relation) + relation.getDisplayName());

            // Save data
            plugin.getDataManager().saveFactionData();
        } else {
            player.sendMessage(ChatColor.RED + "Failed to set relation.");
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
            player.sendMessage(ChatColor.RED + "You are not in a faction.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /f " + relation.name().toLowerCase() + " <FactionName>");
            return true;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        // Check permission
        if (!faction.hasPermission(playerRank, FactionPermission.SET_RELATIONS)) {
            player.sendMessage(ChatColor.RED + "You lack permission to set faction relations.");
            return true;
        }

        String targetFaction = args[1];

        if (!factions.containsKey(targetFaction)) {
            player.sendMessage(ChatColor.RED + "Faction '" + targetFaction + "' does not exist.");
            return true;
        }

        if (targetFaction.equals(factionName)) {
            player.sendMessage(ChatColor.RED + "You cannot set relations with your own faction.");
            return true;
        }

        boolean success = plugin.getRelationManager().sendRelationRequest(factionName, targetFaction, relation, uuid);
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Sent " +
                    plugin.getRelationManager().getRelationColor(relation) + relation.getDisplayName() +
                    ChatColor.GREEN + " request to " + targetFaction + "!");

            // Save data
            plugin.getDataManager().saveFactionData();
        } else {
            player.sendMessage(ChatColor.RED + "Failed to send relation request. A request may already exist.");
        }

        return true;
    }

    private boolean handleCreateTestFactions(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "You must be an operator to use this command.");
            return true;
        }

        int count = 80; // Changed from 40 to 80
        if (args.length >= 2) {
            try {
                count = Integer.parseInt(args[1]);
                if (count <= 0 || count > 100) {
                    player.sendMessage(ChatColor.RED + "Count must be between 1 and 100.");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid number: " + args[1]);
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

        player.sendMessage(ChatColor.YELLOW + "Creating " + count + " test factions...");

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
        player.sendMessage(ChatColor.GREEN + "Successfully created " + created + " test factions!");
        player.sendMessage(ChatColor.YELLOW + "• Public factions: " + ChatColor.GREEN + publicCount);
        player.sendMessage(ChatColor.YELLOW + "• Private factions: " + ChatColor.BLUE + privateCount + ChatColor.GRAY + " (you've been invited to all private factions)");
        player.sendMessage(ChatColor.GRAY + "Use /f browsefactions to test the browser, or /f removetestfactions to clean up.");

        // Save data
        plugin.getDataManager().saveFactionData();

        return true;
    }

    private boolean handleRemoveTestFactions(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "You must be an operator to use this command.");
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Removing all test factions...");

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

        player.sendMessage(ChatColor.GREEN + "Successfully removed " + removed + " test factions!");

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
            player.sendMessage(ChatColor.RED + "You are not in a faction.");
            return true;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);
        if (playerRank != Rank.OWNER && playerRank != Rank.ADMIN) {
            player.sendMessage(ChatColor.RED + "You lack permission to change faction privacy settings.");
            return true;
        }

        if (args.length < 2) {
            // Show current status
            player.sendMessage(ChatColor.YELLOW + "Faction Privacy: " +
                    (faction.isPublic ? ChatColor.GREEN + "Public" : ChatColor.RED + "Private"));
            player.sendMessage(ChatColor.GRAY + "Usage: /f privacy <public|private>");
            return true;
        }

        String setting = args[1].toLowerCase();
        if (setting.equals("public")) {
            if (faction.isPublic) {
                player.sendMessage(ChatColor.YELLOW + "Faction is already public.");
                return true;
            }
            faction.isPublic = true;
            player.sendMessage(ChatColor.GREEN + "Faction is now public! Anyone can join without an invitation.");

            // Notify other online faction members
            for (UUID memberUUID : faction.members.keySet()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && !member.equals(player)) {
                    member.sendMessage(ChatColor.GREEN + player.getName() + " made the faction public!");
                }
            }

        } else if (setting.equals("private")) {
            if (!faction.isPublic) {
                player.sendMessage(ChatColor.YELLOW + "Faction is already private.");
                return true;
            }
            faction.isPublic = false;
            player.sendMessage(ChatColor.GREEN + "Faction is now private! Only invited players can join.");

            // Notify other online faction members
            for (UUID memberUUID : faction.members.keySet()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && !member.equals(player)) {
                    member.sendMessage(ChatColor.YELLOW + player.getName() + " made the faction private!");
                }
            }

        } else {
            player.sendMessage(ChatColor.RED + "Invalid option. Use 'public' or 'private'.");
            player.sendMessage(ChatColor.GRAY + "Usage: /f privacy <public|private>");
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
            player.sendMessage(ChatColor.RED + "You lack permission to use this command.");
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /f adminjoin <FactionName> <PlayerName>");
            return true;
        }

        String factionName = args[1];
        String playerName = args[2];

        if (!factions.containsKey(factionName)) {
            player.sendMessage(ChatColor.RED + "Faction '" + factionName + "' does not exist.");
            return true;
        }

        // Create a fake offline player UUID (for testing)
        UUID fakeUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());

        if (playerFactions.containsKey(fakeUUID)) {
            player.sendMessage(ChatColor.RED + "Player " + playerName + " is already in a faction.");
            return true;
        }

        Faction f = factions.get(factionName);
        f.members.put(fakeUUID, Rank.RECRUIT);
        playerFactions.put(fakeUUID, factionName);

        player.sendMessage(ChatColor.GREEN + "Successfully added fake player " + playerName + " to faction " + factionName + "!");
        return true;
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) return false;
        UUID uuid = player.getUniqueId();

        if (playerFactions.containsKey(uuid)) {
            player.sendMessage("You are already in a faction.");
            return true;
        }

        String name = args[1];
        if (factions.containsKey(name)) {
            player.sendMessage("A faction with that name already exists.");
            return true;
        }

        Faction f = new Faction(name, uuid);
        f.members.put(uuid, Rank.OWNER);
        factions.put(name, f);
        playerFactions.put(uuid, name);
        player.sendMessage("Faction created: " + name);
        return true;
    }

    private boolean handlePromoteDemote(Player player, String[] args) {
        if (args.length < 2) return false;
        UUID uuid = player.getUniqueId();

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage("Player not found.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        String factionName = playerFactions.get(uuid);
        if (factionName == null || !factionName.equals(playerFactions.get(targetUUID))) {
            player.sendMessage("You must be in the same faction.");
            return true;
        }

        Faction f = factions.get(factionName);
        Rank playerRank = f.members.get(uuid);
        if (playerRank != Rank.OWNER && playerRank != Rank.ADMIN) {
            player.sendMessage("You lack permission.");
            return true;
        }

        Rank current = f.members.getOrDefault(targetUUID, Rank.RECRUIT);
        Rank updated = args[0].equalsIgnoreCase("promote") ? current.promote() : current.demote();
        f.members.put(targetUUID, updated);
        player.sendMessage("Updated " + targetName + " to " + updated.name());
        return true;
    }

    private boolean handleDescription(Player player, String[] args) {
        if (args.length < 2) return false;
        UUID uuid = player.getUniqueId();

        String factionName = playerFactions.get(uuid);
        if (factionName == null) {
            player.sendMessage("You are not in a faction.");
            return true;
        }

        Faction f = factions.get(factionName);
        Rank rank = f.members.get(uuid);
        if (rank != Rank.OWNER && rank != Rank.ADMIN) {
            player.sendMessage("You lack permission.");
            return true;
        }

        f.description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        player.sendMessage("Faction description updated.");
        return true;
    }

    private boolean handleClaim(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);
        if (factionName == null) {
            player.sendMessage("You are not in a faction.");
            return true;
        }

        Faction f = factions.get(factionName);
        Rank rank = f.members.get(uuid);
        if (rank != Rank.OWNER && rank != Rank.ADMIN) {
            player.sendMessage("You lack permission.");
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
            player.sendMessage(ChatColor.RED + "You lack permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /f adminclaim <FactionName>");
            return true;
        }

        String factionName = args[1];
        return plugin.getClaimManager().adminClaim(player, factionName);
    }

    private boolean handleUnclaim(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);
        if (factionName == null) {
            player.sendMessage("You are not in a faction.");
            return true;
        }

        Faction f = factions.get(factionName);
        Rank rank = f.members.get(uuid);
        if (rank != Rank.OWNER && rank != Rank.ADMIN) {
            player.sendMessage("You lack permission.");
            return true;
        }

        return plugin.getClaimManager().unclaimChunk(player, factionName);
    }

    private boolean handleAdminUnclaim(Player player, String[] args) {
        if (!(player.isOp() || player.hasPermission("factions.adminunclaim"))) {
            player.sendMessage(ChatColor.RED + "You lack permission to use this command.");
            return true;
        }

        return plugin.getClaimManager().adminUnclaim(player);
    }

    private boolean handleUnclaimAll(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);
        if (factionName == null) {
            player.sendMessage("You are not in a faction.");
            return true;
        }

        Faction f = factions.get(factionName);
        Rank rank = f.members.get(uuid);
        if (rank != Rank.OWNER && rank != Rank.ADMIN) {
            player.sendMessage("You lack permission.");
            return true;
        }

        plugin.getClaimManager().unclaimAll(player, factionName);
        return true;
    }

    private boolean handleAdminUnclaimAll(Player player, String[] args) {
        if (!(player.isOp() || player.hasPermission("factions.adminunclaimall"))) {
            player.sendMessage(ChatColor.RED + "You lack permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /f adminunclaimall <FactionName>");
            return true;
        }

        String targetFaction = args[1];
        plugin.getClaimManager().adminUnclaimAll(player, targetFaction);
        return true;
    }

    private boolean handleLoadAll(Player player, String[] args) {
        if (!(player.isOp() || player.hasPermission("factions.loadall"))) {
            player.sendMessage(ChatColor.RED + "You lack permission to use this command.");
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Starting wilderness initialization for ALL worlds...");
        player.sendMessage(ChatColor.YELLOW + "This may take several minutes. Check console for progress.");

        plugin.getClaimManager().initializeAllWorlds(player);
        return true;
    }

    private boolean handleLoad(Player player, String[] args) {
        if (!(player.isOp() || player.hasPermission("factions.load"))) {
            player.sendMessage(ChatColor.RED + "You lack permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /f load <WorldName>");
            return true;
        }

        String worldName = args[1];
        World targetWorld = Bukkit.getWorld(worldName);
        if (targetWorld == null) {
            player.sendMessage(ChatColor.RED + "World '" + worldName + "' not found!");
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Starting wilderness initialization for world: " + worldName);
        player.sendMessage(ChatColor.YELLOW + "This may take several minutes. Check console for progress.");

        plugin.getClaimManager().initializeSingleWorld(targetWorld, player);
        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);
        if (factionName == null) {
            player.sendMessage("You are not in a faction.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /f invite <PlayerName>");
            return true;
        }

        Faction f = factions.get(factionName);
        Rank rank = f.members.get(uuid);
        if (rank != Rank.OWNER && rank != Rank.ADMIN) {
            player.sendMessage("You lack permission to invite players.");
            return true;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage("Player '" + targetName + "' not found or not online.");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        if (playerFactions.containsKey(targetUUID)) {
            player.sendMessage("Player " + targetName + " is already in a faction.");
            return true;
        }

        // Add invitation
        playerInvitations.putIfAbsent(targetUUID, new HashSet<>());
        Set<String> invites = playerInvitations.get(targetUUID);

        if (invites.contains(factionName)) {
            player.sendMessage(ChatColor.YELLOW + targetName + " has already been invited to " + factionName + ".");
            return true;
        }

        invites.add(factionName);

        // Notify both players
        player.sendMessage(ChatColor.GREEN + "Successfully invited " + targetName + " to " + factionName + "!");
        target.sendMessage(ChatColor.GREEN + "You have been invited to faction " + factionName + " by " + player.getName() + "!");
        target.sendMessage(ChatColor.YELLOW + "Use /f join " + factionName + " to join, or /f invites to see all invitations.");

        return true;
    }

    private boolean handleJoin(Player player, String[] args) {
        UUID uuid = player.getUniqueId();

        if (playerFactions.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "You are already in a faction. Leave your current faction first.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /f join <FactionName>");
            return true;
        }

        String factionName = args[1];
        Faction faction = factions.get(factionName);
        if (faction == null) {
            player.sendMessage(ChatColor.RED + "Faction '" + factionName + "' does not exist.");
            return true;
        }

        // Check if faction is public or player has invitation
        boolean canJoin = faction.isPublic;
        Set<String> invites = playerInvitations.get(uuid);
        if (!canJoin && invites != null && invites.contains(factionName)) {
            canJoin = true;
            // Remove the invitation since they're joining
            invites.remove(factionName);
            if (invites.isEmpty()) {
                playerInvitations.remove(uuid);
            }
        }

        if (!canJoin) {
            player.sendMessage(ChatColor.RED + "You cannot join " + factionName + ". This faction is private and you haven't been invited.");
            return true;
        }

        // Join the faction
        faction.members.put(uuid, Rank.RECRUIT);
        playerFactions.put(uuid, factionName);

        player.sendMessage(ChatColor.GREEN + "Successfully joined faction " + factionName + "!");

        // Notify other online faction members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(player)) {
                member.sendMessage(ChatColor.GREEN + player.getName() + " has joined the faction!");
            }
        }

        return true;
    }

    private boolean handleLeave(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            player.sendMessage(ChatColor.RED + "You are not in a faction.");
            return true;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        if (playerRank == Rank.OWNER) {
            player.sendMessage(ChatColor.RED + "You cannot leave as the faction owner. Use /f disband to disband the faction.");
            return true;
        }

        // Remove player from faction
        faction.members.remove(uuid);
        playerFactions.remove(uuid);

        player.sendMessage(ChatColor.GREEN + "You have left faction " + factionName + ".");

        // Notify other online faction members
        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null) {
                member.sendMessage(ChatColor.YELLOW + player.getName() + " has left the faction.");
            }
        }

        return true;
    }

    private boolean handleDisband(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);

        if (factionName == null) {
            player.sendMessage(ChatColor.RED + "You are not in a faction.");
            return true;
        }

        Faction faction = factions.get(factionName);
        Rank playerRank = faction.members.get(uuid);

        if (playerRank != Rank.OWNER) {
            player.sendMessage(ChatColor.RED + "Only the faction owner can disband the faction.");
            return true;
        }

        // Confirm disbanding
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "WARNING: " + ChatColor.RED + "Are you sure you want to disband " + factionName + "?");
        player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.RED + "/f disband confirm" + ChatColor.YELLOW + " to confirm.");

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
            player.sendMessage(ChatColor.YELLOW + "You have no pending faction invitations.");
            return true;
        }

        player.sendMessage(ChatColor.GREEN + "Your faction invitations:");
        for (String factionName : invites) {
            player.sendMessage(ChatColor.YELLOW + "• " + factionName + ChatColor.GRAY + " - Use /f join " + factionName);
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
                member.sendMessage(ChatColor.RED + "Faction " + factionName + " has been disbanded by " + disbander.getName() + "!");
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

        disbander.sendMessage(ChatColor.GREEN + "Faction " + factionName + " has been disbanded.");
    }

    private boolean handleKick(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        String factionName = playerFactions.get(uuid);
        if (factionName == null) {
            player.sendMessage("You are not in a faction.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /f kick <PlayerName>");
            return true;
        }

        Faction f = factions.get(factionName);
        Rank playerRank = f.members.get(uuid);
        if (playerRank != Rank.OWNER && playerRank != Rank.ADMIN) {
            player.sendMessage("You lack permission to kick players.");
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
                player.sendMessage("Player '" + targetName + "' not found or not in your faction.");
                return true;
            }
        }

        // Check if target is in the same faction
        String targetFactionName = playerFactions.get(targetUUID);
        if (!factionName.equals(targetFactionName)) {
            player.sendMessage("Player " + targetName + " is not in your faction.");
            return true;
        }

        // Check if trying to kick yourself
        if (targetUUID.equals(uuid)) {
            player.sendMessage("You cannot kick yourself from the faction.");
            return true;
        }

        // Check rank permissions
        Rank targetRank = f.members.get(targetUUID);
        if (playerRank == Rank.ADMIN && (targetRank == Rank.ADMIN || targetRank == Rank.OWNER)) {
            player.sendMessage("You cannot kick a player of equal or higher rank.");
            return true;
        }
        if (targetRank == Rank.OWNER) {
            player.sendMessage("You cannot kick the faction owner.");
            return true;
        }

        // Remove player from faction
        f.members.remove(targetUUID);
        playerFactions.remove(targetUUID);

        // Notify both players
        player.sendMessage(ChatColor.GREEN + "Successfully kicked " + targetName + " from " + factionName + "!");
        if (target != null) {
            target.sendMessage(ChatColor.RED + "You have been kicked from faction " + factionName + " by " + player.getName() + "!");
        }

        return true;
    }

    // Add this helper method to CommandManager:

    private String formatTimeString(long milliseconds) {
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
}