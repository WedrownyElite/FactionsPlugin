package me.elite.Factions.nametags;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Relation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class PacketNametagManager {
    private final FactionsPlugin plugin;
    private final Map<UUID, String> playerFactions;

    // Reflection objects for NMS
    private String nmsVersion;
    private Class<?> packetPlayOutScoreboardTeamClass;
    private Class<?> craftPlayerClass;
    private Class<?> scoreboardTeamClass;
    private Constructor<?> packetConstructor;
    private Method sendPacketMethod;
    private Method getHandleMethod;
    private Method getPlayerConnectionMethod;
    private Field playerConnectionField;

    // Team management
    private static final String TEAM_PREFIX = "fac_";

    public PacketNametagManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.playerFactions = plugin.getPlayerFactions();
        initializeReflection();
    }

    private void initializeReflection() {
        try {
            // Get NMS version
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            nmsVersion = packageName.substring(packageName.lastIndexOf('.') + 1);

            // Get classes
            packetPlayOutScoreboardTeamClass = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutScoreboardTeam");
            craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftPlayer");
            scoreboardTeamClass = Class.forName("net.minecraft.server." + nmsVersion + ".ScoreboardTeam");

            // Get constructor for creating team packet
            packetConstructor = packetPlayOutScoreboardTeamClass.getConstructor(scoreboardTeamClass, int.class);

            // Get methods for sending packets
            getHandleMethod = craftPlayerClass.getMethod("getHandle");
            Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + nmsVersion + ".EntityPlayer");
            playerConnectionField = entityPlayerClass.getField("playerConnection");
            Class<?> playerConnectionClass = Class.forName("net.minecraft.server." + nmsVersion + ".PlayerConnection");
            sendPacketMethod = playerConnectionClass.getMethod("sendPacket", Class.forName("net.minecraft.server." + nmsVersion + ".Packet"));

            plugin.getLogger().info("PacketNametagManager initialized successfully for " + nmsVersion);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize PacketNametagManager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update nametag color for a target player as seen by a viewer
     */
    public void updateNametagFor(Player target, Player viewer) {
        if (target == null || viewer == null || !target.isOnline() || !viewer.isOnline()) {
            return;
        }

        if (target.equals(viewer)) {
            return; // Don't change own nametag
        }

        try {
            // Get the appropriate color
            ChatColor color = getRelationColor(target, viewer);
            String teamName = TEAM_PREFIX + color.name().toLowerCase();

            // Create and send team packet
            sendTeamPacket(viewer, target, teamName, color);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update nametag for " + target.getName() + " -> " + viewer.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Send team packet to update nametag color
     */
    private void sendTeamPacket(Player viewer, Player target, String teamName, ChatColor color) throws Exception {
        // Create a fake scoreboard team for the packet
        Object fakeTeam = createFakeTeam(teamName, color);

        // Create packet to create/update team
        Object createPacket = packetConstructor.newInstance(fakeTeam, 0); // 0 = CREATE_TEAM

        // Set team members
        Field playersField = packetPlayOutScoreboardTeamClass.getDeclaredField("h"); // players field
        playersField.setAccessible(true);
        Collection<String> players = (Collection<String>) playersField.get(createPacket);
        players.add(target.getName());

        // Send packet to viewer
        sendPacket(viewer, createPacket);
    }

    /**
     * Create a fake scoreboard team object
     */
    private Object createFakeTeam(String teamName, ChatColor color) throws Exception {
        Class<?> scoreboardClass = Class.forName("net.minecraft.server." + nmsVersion + ".Scoreboard");
        Object fakeScoreboard = scoreboardClass.newInstance();

        Constructor<?> teamConstructor = scoreboardTeamClass.getConstructor(scoreboardClass, String.class);
        Object team = teamConstructor.newInstance(fakeScoreboard, teamName);

        // Set team color (prefix)
        Method setPrefixMethod = scoreboardTeamClass.getMethod("setPrefix", String.class);
        setPrefixMethod.invoke(team, color.toString());

        Method setSuffixMethod = scoreboardTeamClass.getMethod("setSuffix", String.class);
        setSuffixMethod.invoke(team, ChatColor.RESET.toString());

        // Set team visibility options - this is key to only affecting nametags
        try {
            Method setNameTagVisibilityMethod = scoreboardTeamClass.getMethod("setNameTagVisibility", Enum.class);
            Class<?> enumTeamPushClass = Class.forName("net.minecraft.server." + nmsVersion + ".ScoreboardTeamBase$EnumNameTagVisibility");
            Object alwaysVisible = Enum.valueOf((Class<Enum>) enumTeamPushClass, "ALWAYS");
            setNameTagVisibilityMethod.invoke(team, alwaysVisible);
        } catch (Exception e) {
            // Fallback for older versions
            plugin.getLogger().fine("Could not set name tag visibility (older MC version)");
        }

        return team;
    }

    /**
     * Send packet to player
     */
    private void sendPacket(Player player, Object packet) throws Exception {
        Object handle = getHandleMethod.invoke(player);
        Object playerConnection = playerConnectionField.get(handle);
        sendPacketMethod.invoke(playerConnection, packet);
    }

    /**
     * Update nametag for all players as seen by viewer
     */
    public void updateAllNametagsFor(Player viewer) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.equals(viewer)) {
                updateNametagFor(target, viewer);
            }
        }
    }

    /**
     * Update nametag for target as seen by all players
     */
    public void updateNametagForAll(Player target) {
        if (target == null || !target.isOnline()) {
            return;
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target)) {
                updateNametagFor(target, viewer);
            }
        }
    }

    /**
     * Refresh all nametags
     */
    public void refreshAllNametags() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            updateAllNametagsFor(viewer);
        }
    }

    /**
     * Get relation color between target and viewer
     */
    private ChatColor getRelationColor(Player target, Player viewer) {
        String targetFaction = playerFactions.get(target.getUniqueId());
        String viewerFaction = playerFactions.get(viewer.getUniqueId());

        // If viewer has no faction, show white
        if (viewerFaction == null) {
            return ChatColor.WHITE;
        }

        // If target has no faction, show white (neutral)
        if (targetFaction == null) {
            return ChatColor.WHITE;
        }

        // Same faction = GREEN
        if (targetFaction.equals(viewerFaction)) {
            return ChatColor.GREEN;
        }

        // Different factions - check relations
        Relation relation = plugin.getRelationManager().getRelation(viewerFaction, targetFaction);

        switch (relation) {
            case ALLY:
                return ChatColor.LIGHT_PURPLE; // Purple for allies
            case TRUCE:
                return ChatColor.BLUE; // Blue for truce
            case ENEMY:
                return ChatColor.RED; // Red for enemies
            case NEUTRAL:
            default:
                return ChatColor.WHITE; // White for neutral
        }
    }

    /**
     * Handle player joining
     */
    public void onPlayerJoin(Player player) {
        // Delay to ensure player is fully loaded
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updateAllNametagsFor(player);
            updateNametagForAll(player);
        }, 20L); // 1 second delay
    }

    /**
     * Handle player leaving
     */
    public void onPlayerLeave(Player player) {
        // Nothing special needed for packet-based approach
    }

    /**
     * Handle faction changes
     */
    public void onFactionChange(Player player) {
        // Update how this player sees everyone
        updateAllNametagsFor(player);
        // Update how everyone sees this player
        updateNametagForAll(player);
    }

    /**
     * Handle relation changes
     */
    public void onRelationChange(String faction1, String faction2) {
        // Update all players from both factions
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerFaction = playerFactions.get(player.getUniqueId());
            if (playerFaction != null && (playerFaction.equals(faction1) || playerFaction.equals(faction2))) {
                updateAllNametagsFor(player);
                updateNametagForAll(player);
            }
        }
    }
}