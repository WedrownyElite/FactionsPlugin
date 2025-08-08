package me.elite.Factions.Relations;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class RelationManager {
    private final FactionsPlugin plugin;
    private final Map<String, Faction> factions;
    private final Map<UUID, String> playerFactions;

    // Relations between factions: faction1 -> (faction2 -> relation)
    private final Map<String, Map<String, Relation>> factionRelations = new HashMap<>();

    // Pending relation requests: targetFaction -> list of requests
    private final Map<String, List<RelationRequest>> pendingRequests = new HashMap<>();

    public RelationManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
    }

    // =================================================================
    // RELATION MANAGEMENT
    // =================================================================

    /**
     * Get the relation between two factions
     */
    public Relation getRelation(String faction1, String faction2) {
        if (faction1 == null || faction2 == null) return Relation.NEUTRAL;
        if (faction1.equals(faction2)) return Relation.NEUTRAL; // Same faction

        Map<String, Relation> faction1Relations = factionRelations.get(faction1);
        if (faction1Relations != null && faction1Relations.containsKey(faction2)) {
            return faction1Relations.get(faction2);
        }

        return Relation.NEUTRAL; // Default relation
    }

    /**
     * Check if a player can perform an action in territory based on their relation
     */
    public boolean canPerformAction(Player player, String territoryFaction, RelationPermission permission) {
        String playerFaction = playerFactions.get(player.getUniqueId());

        // If no player faction, they're neutral
        if (playerFaction == null) {
            return hasRelationPermission(player, territoryFaction, permission);
        }

        // If same faction, always allow (faction rank permissions handled elsewhere)
        if (playerFaction.equals(territoryFaction)) {
            return true;
        }

        // Check relation permissions
        return hasRelationPermission(player, territoryFaction, permission);
    }

    /**
     * Set a direct relation (neutral/enemy) - private relations
     */
    public boolean setDirectRelation(String fromFaction, String toFaction, Relation relation, UUID playerUUID) {
        if (!factions.containsKey(fromFaction) || !factions.containsKey(toFaction)) {
            return false;
        }

        if (fromFaction.equals(toFaction)) {
            return false; // Can't set relation to self
        }

        // Only NEUTRAL and ENEMY can be set directly (private relations)
        if (relation != Relation.NEUTRAL && relation != Relation.ENEMY) {
            return false;
        }

        // Remove any existing mutual relations (ally/truce) if setting enemy/neutral
        Map<String, Relation> fromFactionRelations = factionRelations.get(fromFaction);
        Map<String, Relation> toFactionRelations = factionRelations.get(toFaction);

        if (fromFactionRelations != null && fromFactionRelations.containsKey(toFaction)) {
            Relation currentRelation = fromFactionRelations.get(toFaction);
            if (currentRelation == Relation.ALLY || currentRelation == Relation.TRUCE) {
                // Remove mutual relation
                fromFactionRelations.remove(toFaction);
                if (toFactionRelations != null) {
                    toFactionRelations.remove(fromFaction);
                }
            }
        }

        // Set the private relation (only affects fromFaction's view of toFaction)
        factionRelations.computeIfAbsent(fromFaction, k -> new HashMap<>()).put(toFaction, relation);

        // Notify the faction that set the relation
        notifyFactionMembers(fromFaction, ChatColor.YELLOW + "Your faction's relation with " +
                ChatColor.WHITE + toFaction + ChatColor.YELLOW + " is now: " +
                getRelationColor(relation) + relation.getDisplayName());

        plugin.getEventListener().onFactionRelationChange(fromFaction, toFaction);

        return true;
    }

    /**
     * Send a relation request for ALLY or TRUCE
     */
    public boolean sendRelationRequest(String fromFaction, String toFaction, Relation relation, UUID playerUUID) {
        if (!factions.containsKey(fromFaction) || !factions.containsKey(toFaction)) {
            return false;
        }

        if (fromFaction.equals(toFaction)) {
            return false; // Can't send request to self
        }

        // Only ALLY and TRUCE require requests
        if (relation != Relation.ALLY && relation != Relation.TRUCE) {
            return false;
        }

        // NEW: Check if the relation already exists
        Relation currentRelation = getRelation(fromFaction, toFaction);
        if (currentRelation == relation) {
            // Notify the player that the relation already exists
            Player sender = Bukkit.getPlayer(playerUUID);
            if (sender != null) {
                sender.sendMessage(ChatColor.YELLOW + "You already have a " +
                        getRelationColor(relation) + relation.getDisplayName() +
                        ChatColor.YELLOW + " relation with " + toFaction + "!");
            }
            return false;
        }

        // NEW: Check if there's already a pending request for this relation
        List<RelationRequest> existingRequests = pendingRequests.get(toFaction);
        if (existingRequests != null) {
            for (RelationRequest existingRequest : existingRequests) {
                if (existingRequest.fromFaction.equals(fromFaction) &&
                        existingRequest.requestedRelation == relation) {
                    // Notify the player that a request is already pending
                    Player sender = Bukkit.getPlayer(playerUUID);
                    if (sender != null) {
                        sender.sendMessage(ChatColor.YELLOW + "You already have a pending " +
                                getRelationColor(relation) + relation.getDisplayName() +
                                ChatColor.YELLOW + " request to " + toFaction + "!");
                    }
                    return false;
                }
            }
        }

        // Remove any existing requests from this faction to the target faction (for different relations)
        if (existingRequests != null) {
            existingRequests.removeIf(existingRequest -> existingRequest.fromFaction.equals(fromFaction));
            if (existingRequests.isEmpty()) {
                pendingRequests.remove(toFaction);
            }
        }

        // Create and add the new request
        RelationRequest request = new RelationRequest(fromFaction, toFaction, relation, playerUUID);
        pendingRequests.computeIfAbsent(toFaction, k -> new ArrayList<>()).add(request);

        // Notify both factions
        Player sender = Bukkit.getPlayer(playerUUID);
        String senderName = sender != null ? sender.getName() : "Unknown";

        notifyFactionMembers(fromFaction, ChatColor.YELLOW + "Sent " +
                getRelationColor(relation) + relation.getDisplayName() + ChatColor.YELLOW +
                " request to " + ChatColor.WHITE + toFaction);

        notifyFactionMembers(toFaction, ChatColor.YELLOW + "Received " +
                getRelationColor(relation) + relation.getDisplayName() + ChatColor.YELLOW +
                " request from " + ChatColor.WHITE + fromFaction + ChatColor.GRAY + " (by " + senderName + ")");

        return true;
    }

    /**
     * Accept a relation request - creates mutual relations
     */
    public boolean acceptRelationRequest(String toFaction, String fromFaction, Relation relation, UUID playerUUID) {
        List<RelationRequest> requests = pendingRequests.get(toFaction);
        if (requests == null) return false;

        // Find and remove the request
        RelationRequest targetRequest = null;
        for (RelationRequest request : requests) {
            if (request.fromFaction.equals(fromFaction) && request.requestedRelation == relation) {
                targetRequest = request;
                break;
            }
        }

        if (targetRequest == null) return false;

        requests.remove(targetRequest);
        if (requests.isEmpty()) {
            pendingRequests.remove(toFaction);
        }

        // Remove any existing private relations between these factions
        Map<String, Relation> fromFactionRelations = factionRelations.get(fromFaction);
        Map<String, Relation> toFactionRelations = factionRelations.get(toFaction);

        if (fromFactionRelations != null) {
            fromFactionRelations.remove(toFaction);
        }
        if (toFactionRelations != null) {
            toFactionRelations.remove(fromFaction);
        }

        // Set the mutual relation (public relation)
        factionRelations.computeIfAbsent(fromFaction, k -> new HashMap<>()).put(toFaction, relation);
        factionRelations.computeIfAbsent(toFaction, k -> new HashMap<>()).put(fromFaction, relation);

        // Notify both factions
        Player accepter = Bukkit.getPlayer(playerUUID);
        String accepterName = accepter != null ? accepter.getName() : "Unknown";

        notifyFactionMembers(fromFaction, ChatColor.GREEN + "Your " +
                getRelationColor(relation) + relation.getDisplayName() + ChatColor.GREEN +
                " request to " + ChatColor.WHITE + toFaction + ChatColor.GREEN + " was accepted!");

        notifyFactionMembers(toFaction, ChatColor.GREEN + "Accepted " +
                getRelationColor(relation) + relation.getDisplayName() + ChatColor.GREEN +
                " request from " + ChatColor.WHITE + fromFaction + ChatColor.GRAY + " (by " + accepterName + ")");

        plugin.getEventListener().onFactionRelationChange(fromFaction, toFaction);

        return true;
    }

    /**
     * Reject a relation request
     */
    public boolean rejectRelationRequest(String toFaction, String fromFaction, Relation relation, UUID playerUUID) {
        List<RelationRequest> requests = pendingRequests.get(toFaction);
        if (requests == null) return false;

        // Find and remove the request
        RelationRequest targetRequest = null;
        for (RelationRequest request : requests) {
            if (request.fromFaction.equals(fromFaction) && request.requestedRelation == relation) {
                targetRequest = request;
                break;
            }
        }

        if (targetRequest == null) return false;

        requests.remove(targetRequest);
        if (requests.isEmpty()) {
            pendingRequests.remove(toFaction);
        }

        // Notify both factions
        Player rejecter = Bukkit.getPlayer(playerUUID);
        String rejecterName = rejecter != null ? rejecter.getName() : "Unknown";

        notifyFactionMembers(fromFaction, ChatColor.RED + "Your " +
                getRelationColor(relation) + relation.getDisplayName() + ChatColor.RED +
                " request to " + ChatColor.WHITE + toFaction + ChatColor.RED + " was rejected.");

        notifyFactionMembers(toFaction, ChatColor.YELLOW + "Rejected " +
                getRelationColor(relation) + relation.getDisplayName() + ChatColor.YELLOW +
                " request from " + ChatColor.WHITE + fromFaction + ChatColor.GRAY + " (by " + rejecterName + ")");

        return true;
    }

    /**
     * Remove a relation between two factions
     */
    public boolean removeRelation(String fromFaction, String toFaction, UUID playerUUID) {
        if (!factions.containsKey(fromFaction) || !factions.containsKey(toFaction)) {
            return false;
        }

        if (fromFaction.equals(toFaction)) {
            return false;
        }

        Map<String, Relation> fromFactionRelations = factionRelations.get(fromFaction);
        Map<String, Relation> toFactionRelations = factionRelations.get(toFaction);

        if (fromFactionRelations == null || !fromFactionRelations.containsKey(toFaction)) {
            return false; // No relation exists
        }

        Relation currentRelation = fromFactionRelations.get(toFaction);

        // Remove the relation
        fromFactionRelations.remove(toFaction);
        if (fromFactionRelations.isEmpty()) {
            factionRelations.remove(fromFaction);
        }

        // If it was a mutual relation (ally/truce), remove from both sides
        if ((currentRelation == Relation.ALLY || currentRelation == Relation.TRUCE) && toFactionRelations != null) {
            toFactionRelations.remove(fromFaction);
            if (toFactionRelations.isEmpty()) {
                factionRelations.remove(toFaction);
            }

            // Notify both factions for mutual relations
            Player remover = Bukkit.getPlayer(playerUUID);
            String removerName = remover != null ? remover.getName() : "Unknown";

            notifyFactionMembers(toFaction, ChatColor.RED + "Your " +
                    getRelationColor(currentRelation) + currentRelation.getDisplayName() + ChatColor.RED +
                    " relation with " + ChatColor.WHITE + fromFaction + ChatColor.RED + " has been removed by " + removerName + ".");
        }

        // Notify the faction that removed the relation
        notifyFactionMembers(fromFaction, ChatColor.YELLOW + "Removed relation with " +
                ChatColor.WHITE + toFaction + ChatColor.YELLOW + ". They are now " +
                ChatColor.WHITE + "Neutral" + ChatColor.YELLOW + ".");

        plugin.getEventListener().onFactionRelationChange(fromFaction, toFaction);

        return true;
    }

    // =================================================================
    // UTILITY METHODS
    // =================================================================

    /**
     * Get pending requests for a faction
     */
    public List<RelationRequest> getPendingRequests(String factionName) {
        return pendingRequests.getOrDefault(factionName, new ArrayList<>());
    }

    /**
     * Get the color associated with a relation
     */
    public static ChatColor getRelationColor(Relation relation) {
        switch (relation) {
            case ALLY: return ChatColor.GREEN;
            case TRUCE: return ChatColor.YELLOW;
            case ENEMY: return ChatColor.RED;
            case NEUTRAL:
            default: return ChatColor.WHITE;
        }
    }

    /**
     * Clean up expired requests
     */
    public void cleanupExpiredRequests() {
        pendingRequests.values().forEach(requests ->
                requests.removeIf(RelationRequest::isExpired));
        pendingRequests.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Notify all online members of a faction
     */
    private void notifyFactionMembers(String factionName, String message) {
        Faction faction = factions.get(factionName);
        if (faction == null) return;

        for (UUID memberUUID : faction.members.keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(message);
            }
        }
    }

    // =================================================================
    // PERMISSION CHECKING
    // =================================================================

    /**
     * Check if a player has permission to perform an action in faction territory
     * based on their relation to the faction
     */
    public boolean hasRelationPermission(Player player, String territoryFaction, RelationPermission permission) {
        if (player == null || territoryFaction == null) return false;

        // If player is in the same faction, check faction rank permissions instead
        String playerFaction = playerFactions.get(player.getUniqueId());
        if (playerFaction != null && playerFaction.equals(territoryFaction)) {
            return true; // Handle faction member permissions elsewhere
        }

        // Get the relation from the territory faction's perspective
        Relation relation = Relation.NEUTRAL; // Default for factionless players
        if (playerFaction != null) {
            relation = getRelation(territoryFaction, playerFaction); // Note: territory faction first
        }

        // Check if the territory faction allows this relation to perform the action
        Faction faction = factions.get(territoryFaction);
        if (faction == null) return false;

        return faction.hasRelationPermission(relation, permission);
    }

    /**
     * Get all relations for a faction
     */
    public Map<String, Relation> getFactionRelations(String factionName) {
        Map<String, Relation> allRelations = factionRelations.getOrDefault(factionName, new HashMap<>());
        Map<String, Relation> filteredRelations = new HashMap<>();

        for (Map.Entry<String, Relation> entry : allRelations.entrySet()) {
            if (entry.getValue() != Relation.NEUTRAL) {
                filteredRelations.put(entry.getKey(), entry.getValue());
            }
        }

        return filteredRelations;
    }

    // =================================================================
    // DATA PERSISTENCE SUPPORT
    // =================================================================

    public Map<String, Map<String, Relation>> getAllRelations() {
        return factionRelations;
    }

    public Map<String, List<RelationRequest>> getAllPendingRequests() {
        return pendingRequests;
    }

    public void loadRelations(Map<String, Map<String, Relation>> relations) {
        factionRelations.clear();
        factionRelations.putAll(relations);
    }

    public void loadPendingRequests(Map<String, List<RelationRequest>> requests) {
        pendingRequests.clear();
        pendingRequests.putAll(requests);
    }
}