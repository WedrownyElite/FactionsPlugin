package me.elite.Factions.data;

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
                    factions.put(name, f);
                }
            }

            // Load playerFactions
            if (data.has("playerFactions")) {
                JSONObject pfMap = data.getJSONObject("playerFactions");
                for (String uuidStr : pfMap.keySet()) {
                    playerFactions.put(UUID.fromString(uuidStr), pfMap.getString(uuidStr));
                }
            }

            // Load claims
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

            // Load player invitations
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

            plugin.getLogger().info("Successfully loaded faction data: " + factions.size() + " factions, " +
                    playerFactions.size() + " player mappings");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load faction data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}