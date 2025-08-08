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
import java.util.ArrayList;

public class PacketNametagManager {
    private final FactionsPlugin plugin;
    private final Map<UUID, String> playerFactions;

    // Reflection objects for NMS
    private String nmsVersion;
    private Class<?> packetPlayOutScoreboardTeamClass;
    private Class<?> craftPlayerClass;
    private Class<?> chatComponentTextClass;
    private Constructor<?> chatComponentConstructor;
    private Method sendPacketMethod;
    private Method getHandleMethod;
    private Field playerConnectionField;

    // Team management
    private static final String TEAM_PREFIX = "f_";

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

            plugin.getLogger().info("Detected NMS version: " + nmsVersion);

            // Get classes
            packetPlayOutScoreboardTeamClass = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutScoreboardTeam");
            craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftPlayer");

            // For 1.16+, we need to handle chat components
            try {
                chatComponentTextClass = Class.forName("net.minecraft.server." + nmsVersion + ".ChatComponentText");
                chatComponentConstructor = chatComponentTextClass.getConstructor(String.class);
            } catch (ClassNotFoundException e) {
                // Fallback for older versions
                chatComponentTextClass = null;
                chatComponentConstructor = null;
            }

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
     * Update nametag suffix for a target player as seen by a viewer
     */
    public void updateNametagFor(Player target, Player viewer) {
        if (target == null || viewer == null || !target.isOnline() || !viewer.isOnline()) {
            return;
        }

        if (target.equals(viewer)) {
            return; // Don't change own nametag
        }

        try {
            // Get the appropriate suffix
            String suffix = getRelationSuffix(target, viewer);

            // Create a unique team name for this target-viewer pair
            String baseName = target.getName().toLowerCase();
            String teamName = TEAM_PREFIX + baseName;
            if (teamName.length() > 16) {
                teamName = teamName.substring(0, 16);
            }

            plugin.getLogger().info("Updating nametag: " + target.getName() + " -> " + viewer.getName() + " (Suffix: '" + suffix + "')");

            // First remove player from any existing team
            removePlayerFromTeam(viewer, target);

            // Small delay before creating new team
            final String finalTeamName = teamName;
            final String finalSuffix = suffix;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    sendTeamPacket(viewer, target, finalTeamName, finalSuffix);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to send team packet: " + e.getMessage());
                }
            }, 3L);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update nametag for " + target.getName() + " -> " + viewer.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Remove player from any existing team
     */
    private void removePlayerFromTeam(Player viewer, Player target) {
        try {
            Object packet = packetPlayOutScoreboardTeamClass.newInstance();

            // Set team name
            setField(packet, "a", TEAM_PREFIX + target.getName().toLowerCase());

            // Set action to REMOVE_PLAYERS (4)
            setField(packet, "i", 4);

            // Set players to remove
            Collection<String> players = new ArrayList<>();
            players.add(target.getName());
            setField(packet, "h", players);

            sendPacket(viewer, packet);
        } catch (Exception e) {
            // Ignore removal errors
        }
    }

    /**
     * Send team packet with improved 1.16.5 compatibility
     */
    private void sendTeamPacket(Player viewer, Player target, String teamName, String suffix) throws Exception {
        plugin.getLogger().info("Creating team packet for " + target.getName() + " with suffix: '" + suffix + "'");

        // Create the packet
        Object packet = packetPlayOutScoreboardTeamClass.newInstance();

        // Method 1: Try direct field setting (works for most 1.16.5)
        try {
            sendDirectTeamPacket(viewer, target, teamName, suffix);
            return;
        } catch (Exception e) {
            plugin.getLogger().warning("Direct method failed, trying alternative: " + e.getMessage());
        }

        // Method 2: Try alternative approach
        try {
            sendAlternativeTeamPacket(viewer, target, teamName, suffix);
        } catch (Exception e) {
            plugin.getLogger().warning("Alternative method also failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Direct team packet method (primary approach for 1.16.5)
     */
    private void sendDirectTeamPacket(Player viewer, Player target, String teamName, String suffix) throws Exception {
        Object packet = packetPlayOutScoreboardTeamClass.newInstance();

        // Set team name
        setField(packet, "a", teamName);

        // Set action to CREATE_TEAM (0)
        setField(packet, "i", 0);

        // For 1.16.5, we need to create a team info object
        // Try to find and create the team info
        try {
            // Look for inner class ScoreboardTeamBase$a or similar
            Class<?>[] innerClasses = packetPlayOutScoreboardTeamClass.getDeclaredClasses();
            Class<?> teamInfoClass = null;

            for (Class<?> innerClass : innerClasses) {
                if (innerClass.getSimpleName().equals("a") || innerClass.getSimpleName().contains("TeamInfo")) {
                    teamInfoClass = innerClass;
                    break;
                }
            }

            if (teamInfoClass != null) {
                // Create team info with suffix
                Constructor<?> teamInfoConstructor = null;
                Constructor<?>[] constructors = teamInfoClass.getDeclaredConstructors();

                for (Constructor<?> constructor : constructors) {
                    if (constructor.getParameterCount() >= 6) { // Looking for the main constructor
                        teamInfoConstructor = constructor;
                        break;
                    }
                }

                if (teamInfoConstructor != null) {
                    teamInfoConstructor.setAccessible(true);

                    // Create chat components
                    Object displayNameComponent = chatComponentConstructor.newInstance("");
                    Object prefixComponent = chatComponentConstructor.newInstance("");
                    Object suffixComponent = chatComponentConstructor.newInstance(suffix);

                    // Get EnumChatFormat.RESET
                    Class<?> enumChatFormatClass = Class.forName("net.minecraft.server." + nmsVersion + ".EnumChatFormat");
                    Object chatFormat = enumChatFormatClass.getField("RESET").get(null);

                    // Create team info object
                    Object teamInfo = teamInfoConstructor.newInstance(
                            displayNameComponent,  // display name
                            prefixComponent,       // prefix
                            suffixComponent,       // suffix
                            "always",             // name tag visibility
                            "always",             // collision rule
                            chatFormat,           // color
                            3                     // friendly fire flags
                    );

                    // Set the team info in the packet
                    setField(packet, "b", teamInfo);

                    plugin.getLogger().info("✓ Created team info with suffix: '" + suffix + "'");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create team info, using fallback: " + e.getMessage());

            // Fallback: Set individual fields
            setField(packet, "b", chatComponentConstructor.newInstance(""));     // display name
            setField(packet, "c", chatComponentConstructor.newInstance(""));     // prefix
            setField(packet, "d", chatComponentConstructor.newInstance(suffix)); // suffix
            setField(packet, "e", "always");                                     // visibility
            setField(packet, "f", "always");                                     // collision
        }

        // Set players
        Collection<String> players = new ArrayList<>();
        players.add(target.getName());
        setField(packet, "h", players);

        sendPacket(viewer, packet);
        plugin.getLogger().info("✓ Sent direct team packet with suffix: '" + suffix + "'");
    }

    /**
     * Alternative team packet method
     */
    private void sendAlternativeTeamPacket(Player viewer, Player target, String teamName, String suffix) throws Exception {
        // Create team first
        Object createPacket = packetPlayOutScoreboardTeamClass.newInstance();

        setField(createPacket, "a", teamName);
        setField(createPacket, "i", 0); // CREATE_TEAM
        setField(createPacket, "b", chatComponentConstructor.newInstance(""));
        setField(createPacket, "c", chatComponentConstructor.newInstance(""));
        setField(createPacket, "d", chatComponentConstructor.newInstance(suffix));
        setField(createPacket, "e", "always");
        setField(createPacket, "f", "always");

        Collection<String> emptyPlayers = new ArrayList<>();
        setField(createPacket, "h", emptyPlayers);

        sendPacket(viewer, createPacket);

        // Add player to team
        Object addPacket = packetPlayOutScoreboardTeamClass.newInstance();

        setField(addPacket, "a", teamName);
        setField(addPacket, "i", 3); // ADD_PLAYERS

        Collection<String> playersToAdd = new ArrayList<>();
        playersToAdd.add(target.getName());
        setField(addPacket, "h", playersToAdd);

        sendPacket(viewer, addPacket);
        plugin.getLogger().info("✓ Sent alternative team packet with suffix: '" + suffix + "'");
    }

    /**
     * Helper method to set fields safely
     */
    private void setField(Object packet, String fieldName, Object value) throws Exception {
        Field field = packetPlayOutScoreboardTeamClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(packet, value);
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
     * Get relation suffix between target and viewer
     */
    private String getRelationSuffix(Player target, Player viewer) {
        String targetFaction = playerFactions.get(target.getUniqueId());
        String viewerFaction = playerFactions.get(viewer.getUniqueId());

        // If viewer has no faction, show nothing
        if (viewerFaction == null) {
            return "";
        }

        // If target has no faction, show nothing (neutral)
        if (targetFaction == null) {
            return "";
        }

        // Same faction = green F (with space before)
        if (targetFaction.equals(viewerFaction)) {
            return " " + ChatColor.GREEN + "F";
        }

        // Different factions - check relations
        Relation relation = plugin.getRelationManager().getRelation(viewerFaction, targetFaction);

        switch (relation) {
            case ALLY:
                return " " + ChatColor.LIGHT_PURPLE + "A";
            case TRUCE:
                return " " + ChatColor.BLUE + "T";
            case ENEMY:
                return " " + ChatColor.RED + "E";
            case NEUTRAL:
            default:
                return "";
        }
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
     * Handle player joining
     */
    public void onPlayerJoin(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updateAllNametagsFor(player);
            updateNametagForAll(player);
        }, 40L);
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
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updateAllNametagsFor(player);
                updateNametagForAll(player);
            }
        }, 10L);
    }

    /**
     * Handle relation changes
     */
    public void onRelationChange(String faction1, String faction2) {
        plugin.getLogger().info("Relation change detected between " + faction1 + " and " + faction2);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerFaction = playerFactions.get(player.getUniqueId());
                if (playerFaction != null && (playerFaction.equals(faction1) || playerFaction.equals(faction2))) {
                    updateAllNametagsFor(player);
                    updateNametagForAll(player);
                }
            }
        }, 10L);
    }

    /**
     * Force refresh all nametags (for testing/debugging)
     */
    public void forceRefreshAll() {
        plugin.getLogger().info("Force refreshing all nametags...");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (!viewer.equals(target)) {
                        updateNametagFor(target, viewer);
                    }
                }
            }
            plugin.getLogger().info("Force refresh completed.");
        }, 1L);
    }
}