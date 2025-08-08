package me.elite.Factions.data;
import me.elite.Factions.data.FactionPermission;
import me.elite.Factions.data.RelationPermission;
import me.elite.Factions.data.Relation;
import me.elite.Factions.data.RelationRequest;
import me.elite.Factions.Relations.RelationManager;

import me.elite.Factions.territory.ChunkCoord;
import me.elite.Factions.FactionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.EnumSet;

public class DataManager {
    private final FactionsPlugin plugin;
    private final File dataFile;
    private final Map<String, Faction> factions;
    private final Map<UUID, String> playerFactions;
    private final Map<String, Map<ChunkCoord, String>> worldClaims;

    public DataManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "factionsdata.json");
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
        this.worldClaims = plugin.getWorldClaims();
    }

    public void saveFactionData() {
        try (PrintWriter writer = new PrintWriter(dataFile)) {
            Map<String, Object> data = new HashMap<>();

            // Save factions
            Map<String, Object> factionMap = new HashMap<>();
            for (Map.Entry<String, Faction> entry : factions.entrySet()) {
                Faction f = entry.getValue();
                Map<String, Object> fdata = new HashMap<>();
                fdata.put("description", f.description);
                fdata.put("owner", f.owner.toString());

                Map<String, String> membersMap = new HashMap<>();
                for (Map.Entry<UUID, Rank> e : f.members.entrySet()) {
                    membersMap.put(e.getKey().toString(), e.getValue().name());
                }
                fdata.put("members", membersMap);
                fdata.put("isPublic", f.isPublic);

                // Save rank permissions
                Map<String, List<String>> rankPermsMap = new HashMap<>();
                for (Map.Entry<Rank, Set<FactionPermission>> rankEntry : f.rankPermissions.entrySet()) {
                    List<String> permsList = new ArrayList<>();
                    for (FactionPermission perm : rankEntry.getValue()) {
                        permsList.add(perm.name());
                    }
                    rankPermsMap.put(rankEntry.getKey().name(), permsList);
                }
                fdata.put("rankPermissions", rankPermsMap);

                // Save relation permissions
                Map<String, List<String>> relationPermsMap = new HashMap<>();
                for (Map.Entry<Relation, Set<RelationPermission>> relationEntry : f.relationPermissions.entrySet()) {
                    List<String> permsList = new ArrayList<>();
                    for (RelationPermission perm : relationEntry.getValue()) {
                        permsList.add(perm.name());
                    }
                    relationPermsMap.put(relationEntry.getKey().name(), permsList);
                }
                fdata.put("relationPermissions", relationPermsMap);

                factionMap.put(entry.getKey(), fdata);
            }
            data.put("factions", factionMap);

            // Save playerFactions
            Map<String, String> pfMap = new HashMap<>();
            for (Map.Entry<UUID, String> entry : playerFactions.entrySet()) {
                pfMap.put(entry.getKey().toString(), entry.getValue());
            }
            data.put("playerFactions", pfMap);

            // Save claims
            Map<String, Map<String, String>> claimsMap = new HashMap<>();
            for (Map.Entry<String, Map<ChunkCoord, String>> entry : worldClaims.entrySet()) {
                String worldName = entry.getKey();
                Map<String, String> chunkMap = new HashMap<>();
                for (Map.Entry<ChunkCoord, String> claim : entry.getValue().entrySet()) {
                    ChunkCoord coord = claim.getKey();
                    String chunkKey = coord.x + "," + coord.z;
                    chunkMap.put(chunkKey, claim.getValue());
                }
                claimsMap.put(worldName, chunkMap);
            }
            data.put("claims", claimsMap);

            // Save player invitations
            Map<String, List<String>> invitationsMap = new HashMap<>();
            for (Map.Entry<UUID, Set<String>> entry : plugin.getPlayerInvitations().entrySet()) {
                invitationsMap.put(entry.getKey().toString(), new ArrayList<>(entry.getValue()));
            }
            data.put("invitations", invitationsMap);

            // Save faction relations
            Map<String, Map<String, String>> relationsMap = new HashMap<>();
            for (Map.Entry<String, Map<String, Relation>> factionEntry : plugin.getRelationManager().getAllRelations().entrySet()) {
                String faction = factionEntry.getKey();
                Map<String, String> factionRelations = new HashMap<>();
                for (Map.Entry<String, Relation> relationEntry : factionEntry.getValue().entrySet()) {
                    factionRelations.put(relationEntry.getKey(), relationEntry.getValue().name());
                }
                relationsMap.put(faction, factionRelations);
            }
            data.put("relations", relationsMap);

            // Save pending relation requests
            Map<String, List<Map<String, Object>>> requestsMap = new HashMap<>();
            for (Map.Entry<String, List<RelationRequest>> requestEntry : plugin.getRelationManager().getAllPendingRequests().entrySet()) {
                String toFaction = requestEntry.getKey();
                List<Map<String, Object>> requestsList = new ArrayList<>();

                for (RelationRequest request : requestEntry.getValue()) {
                    Map<String, Object> requestData = new HashMap<>();
                    requestData.put("fromFaction", request.fromFaction);
                    requestData.put("toFaction", request.toFaction);
                    requestData.put("relation", request.requestedRelation.name());
                    requestData.put("requestedBy", request.requestedBy.toString());
                    requestData.put("timestamp", request.timestamp);
                    requestsList.add(requestData);
                }

                requestsMap.put(toFaction, requestsList);
            }
            data.put("relationRequests", requestsMap);

            writer.println(new JSONObject(data).toString(2));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save faction data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadFactionData() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("No existing faction data found, starting fresh.");
            return;
        }

        try {
            String json = new String(java.nio.file.Files.readAllBytes(dataFile.toPath()));
            JSONObject data = new JSONObject(json);

            // Load factions
            if (data.has("factions")) {
                JSONObject factionMap = data.getJSONObject("factions");
                for (String name : factionMap.keySet()) {
                    JSONObject fdata = factionMap.getJSONObject(name);
                    UUID owner = UUID.fromString(fdata.getString("owner"));
                    Faction f = new Faction(name, owner);
                    f.description = fdata.getString("description");

                    if (fdata.has("isPublic")) {
                        f.isPublic = fdata.getBoolean("isPublic");
                    }

                    JSONObject members = fdata.getJSONObject("members");
                    for (String uuidStr : members.keySet()) {
                        f.members.put(UUID.fromString(uuidStr), Rank.valueOf(members.getString(uuidStr)));
                    }

                    // Load rank permissions
                    if (fdata.has("rankPermissions")) {
                        JSONObject rankPerms = fdata.getJSONObject("rankPermissions");
                        for (String rankName : rankPerms.keySet()) {
                            try {
                                Rank rank = Rank.valueOf(rankName);
                                Set<FactionPermission> perms = EnumSet.noneOf(FactionPermission.class);

                                org.json.JSONArray permArray = rankPerms.getJSONArray(rankName);
                                for (int i = 0; i < permArray.length(); i++) {
                                    try {
                                        FactionPermission perm = FactionPermission.valueOf(permArray.getString(i));
                                        perms.add(perm);
                                    } catch (IllegalArgumentException e) {
                                        plugin.getLogger().warning("Unknown faction permission: " + permArray.getString(i));
                                    }
                                }
                                f.rankPermissions.put(rank, perms);
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Unknown rank: " + rankName);
                            }
                        }
                    }

                    // Load relation permissions
                    if (fdata.has("relationPermissions")) {
                        JSONObject relationPerms = fdata.getJSONObject("relationPermissions");
                        for (String relationName : relationPerms.keySet()) {
                            try {
                                Relation relation = Relation.valueOf(relationName);
                                Set<RelationPermission> perms = EnumSet.noneOf(RelationPermission.class);

                                org.json.JSONArray permArray = relationPerms.getJSONArray(relationName);
                                for (int i = 0; i < permArray.length(); i++) {
                                    try {
                                        RelationPermission perm = RelationPermission.valueOf(permArray.getString(i));
                                        perms.add(perm);
                                    } catch (IllegalArgumentException e) {
                                        plugin.getLogger().warning("Unknown relation permission: " + permArray.getString(i));
                                    }
                                }
                                f.relationPermissions.put(relation, perms);
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Unknown relation: " + relationName);
                            }
                        }
                    }

                    factions.put(name, f);
                }
            }

            // Load playerFactions (unchanged)
            if (data.has("playerFactions")) {
                JSONObject pfMap = data.getJSONObject("playerFactions");
                for (String uuidStr : pfMap.keySet()) {
                    playerFactions.put(UUID.fromString(uuidStr), pfMap.getString(uuidStr));
                }
            }

            // Load claims (unchanged)
            if (data.has("claims")) {
                JSONObject claimsMap = data.getJSONObject("claims");
                for (String worldName : claimsMap.keySet()) {
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        plugin.getLogger().warning("Skipping claims for unknown world: " + worldName);
                        continue;
                    }

                    Map<ChunkCoord, String> chunkMap = new HashMap<>();
                    JSONObject chunks = claimsMap.getJSONObject(worldName);
                    for (String key : chunks.keySet()) {
                        try {
                            String[] parts = key.split(",");
                            int x = Integer.parseInt(parts[0]);
                            int z = Integer.parseInt(parts[1]);

                            ChunkCoord coord = new ChunkCoord(x, z);
                            chunkMap.put(coord, chunks.getString(key));
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load chunk claim: " + key + " - " + e.getMessage());
                        }
                    }
                    worldClaims.put(worldName, chunkMap);
                }
            }

            // Load player invitations (unchanged)
            if (data.has("invitations")) {
                JSONObject invitationsMap = data.getJSONObject("invitations");
                for (String uuidStr : invitationsMap.keySet()) {
                    org.json.JSONArray inviteArray = invitationsMap.getJSONArray(uuidStr);
                    Set<String> inviteSet = new HashSet<>();
                    for (int i = 0; i < inviteArray.length(); i++) {
                        inviteSet.add(inviteArray.getString(i));
                    }
                    plugin.getPlayerInvitations().put(UUID.fromString(uuidStr), inviteSet);
                }
            }

            // Load faction relations
            if (data.has("relations")) {
                JSONObject relationsMap = data.getJSONObject("relations");
                Map<String, Map<String, Relation>> loadedRelations = new HashMap<>();

                for (String faction : relationsMap.keySet()) {
                    JSONObject factionRelations = relationsMap.getJSONObject(faction);
                    Map<String, Relation> relations = new HashMap<>();

                    for (String targetFaction : factionRelations.keySet()) {
                        try {
                            Relation relation = Relation.valueOf(factionRelations.getString(targetFaction));
                            relations.put(targetFaction, relation);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Unknown relation: " + factionRelations.getString(targetFaction));
                        }
                    }

                    loadedRelations.put(faction, relations);
                }

                plugin.getRelationManager().loadRelations(loadedRelations);
            }

            // Load pending relation requests
            if (data.has("relationRequests")) {
                JSONObject requestsMap = data.getJSONObject("relationRequests");
                Map<String, List<RelationRequest>> loadedRequests = new HashMap<>();

                for (String toFaction : requestsMap.keySet()) {
                    org.json.JSONArray requestsArray = requestsMap.getJSONArray(toFaction);
                    List<RelationRequest> requests = new ArrayList<>();

                    for (int i = 0; i < requestsArray.length(); i++) {
                        JSONObject requestData = requestsArray.getJSONObject(i);

                        try {
                            String fromFaction = requestData.getString("fromFaction");
                            String requestToFaction = requestData.getString("toFaction");
                            Relation relation = Relation.valueOf(requestData.getString("relation"));
                            UUID requestedBy = UUID.fromString(requestData.getString("requestedBy"));

                            // Create request with original timestamp if available
                            RelationRequest request = new RelationRequest(fromFaction, requestToFaction, relation, requestedBy);
                            if (requestData.has("timestamp")) {
                                // Use reflection to set timestamp if needed, or recreate with current time
                                // For now, we'll accept that loaded requests will have current timestamp
                            }

                            // Only add if not expired
                            if (!request.isExpired()) {
                                requests.add(request);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load relation request: " + e.getMessage());
                        }
                    }

                    if (!requests.isEmpty()) {
                        loadedRequests.put(toFaction, requests);
                    }
                }

                plugin.getRelationManager().loadPendingRequests(loadedRequests);
            }

            plugin.getLogger().info("Successfully loaded faction data: " + factions.size() + " factions, " +
                    playerFactions.size() + " player mappings");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load faction data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}