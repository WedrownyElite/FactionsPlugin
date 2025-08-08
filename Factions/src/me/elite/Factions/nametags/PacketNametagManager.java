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

            // Create a unique team name for this target player
            String teamName = TEAM_PREFIX + target.getName().toLowerCase();
            if (teamName.length() > 16) {
                teamName = teamName.substring(0, 16);
            }

            plugin.getLogger().info("Updating nametag: " + target.getName() + " -> " + viewer.getName() + " (Suffix: '" + suffix + "')");

            // First remove player from any existing team for this viewer
            removePlayerFromTeam(viewer, target);

            // Small delay before creating new team (important for packet ordering)
            String finalTeamName = teamName;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    // Create team and add player in one packet
                    sendCreateTeamPacket(viewer, target, finalTeamName, suffix);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to send team packet: " + e.getMessage());
                }
            }, 1L);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update nametag for " + target.getName() + " -> " + viewer.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Remove player from any existing team
     */
    private void removePlayerFromTeam(Player viewer, Player target) {
        try {
            String teamName = TEAM_PREFIX + target.getName().toLowerCase();
            if (teamName.length() > 16) {
                teamName = teamName.substring(0, 16);
            }

            Object packet = packetPlayOutScoreboardTeamClass.newInstance();

            // Set team name
            setField(packet, "a", teamName);

            // Set action to REMOVE_TEAM (1)
            setField(packet, "i", 1);

            sendPacket(viewer, packet);
        } catch (Exception e) {
            // Ignore removal errors - team might not exist
        }
    }

    /**
     * Send create team packet with player already included
     */
    private void sendCreateTeamPacket(Player viewer, Player target, String teamName, String suffix) throws Exception {
        plugin.getLogger().info("Creating team packet for " + target.getName() + " with suffix: '" + suffix + "'");

        Object packet = packetPlayOutScoreboardTeamClass.newInstance();

        // Set team name
        setField(packet, "a", teamName);

        // Set action to CREATE_TEAM (0)
        setField(packet, "i", 0);

        // Try to handle 1.16.5+ team info structure
        boolean useTeamInfo = true;
        try {
            // Look for team info inner class
            Class<?>[] innerClasses = packetPlayOutScoreboardTeamClass.getDeclaredClasses();
            Class<?> teamInfoClass = null;

            for (Class<?> innerClass : innerClasses) {
                String simpleName = innerClass.getSimpleName();
                if (simpleName.equals("a") || simpleName.contains("TeamInfo") || simpleName.contains("Parameters")) {
                    teamInfoClass = innerClass;
                    break;
                }
            }

            if (teamInfoClass != null) {
                // Find appropriate constructor
                Constructor<?> teamInfoConstructor = null;
                Constructor<?>[] constructors = teamInfoClass.getDeclaredConstructors();

                // Look for constructor with multiple parameters (display name, prefix, suffix, etc.)
                for (Constructor<?> constructor : constructors) {
                    if (constructor.getParameterCount() >= 6) {
                        teamInfoConstructor = constructor;
                        break;
                    }
                }

                if (teamInfoConstructor != null && chatComponentConstructor != null) {
                    teamInfoConstructor.setAccessible(true);

                    // Create chat components
                    Object displayNameComponent = chatComponentConstructor.newInstance("");
                    Object prefixComponent = chatComponentConstructor.newInstance("");
                    Object suffixComponent = chatComponentConstructor.newInstance(suffix);

                    // Get EnumChatFormat for color
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
                            0                     // friendly fire flags (0 = allow friendly fire)
                    );

                    // Set the team info in the packet
                    setField(packet, "b", teamInfo);

                    plugin.getLogger().info("✓ Created team info with suffix: '" + suffix + "'");
                } else {
                    useTeamInfo = false;
                }
            } else {
                useTeamInfo = false;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create team info, using fallback: " + e.getMessage());
            useTeamInfo = false;
        }

        // Fallback method if team info creation failed
        if (!useTeamInfo && chatComponentConstructor != null) {
            try {
                // Set individual fields directly
                setField(packet, "b", chatComponentConstructor.newInstance(""));     // display name
                setField(packet, "c", chatComponentConstructor.newInstance(""));     // prefix
                setField(packet, "d", chatComponentConstructor.newInstance(suffix)); // suffix
                setField(packet, "e", "always");                                     // visibility
                setField(packet, "f", "always");                                     // collision rule

                // Get color format
                Class<?> enumChatFormatClass = Class.forName("net.minecraft.server." + nmsVersion + ".EnumChatFormat");
                Object chatFormat = enumChatFormatClass.getField("RESET").get(null);
                setField(packet, "g", chatFormat);                                   // color

                setField(packet, "j", 0);                                            // friendly fire flags

                plugin.getLogger().info("✓ Used fallback field setting with suffix: '" + suffix + "'");
            } catch (Exception e) {
                plugin.getLogger().severe("Both team info and fallback methods failed: " + e.getMessage());
                throw e;
            }
        }

        // Set players to add to team (include player in creation packet)
        Collection<String> players = new ArrayList<>();
        players.add(target.getName());
        setField(packet, "h", players);

        sendPacket(viewer, packet);
        plugin.getLogger().info("✓ Sent create team packet with suffix: '" + suffix + "' for player: " + target.getName());
    }

    /**
     * Helper method to set fields safely with better error handling
     */
    private void setField(Object packet, String fieldName, Object value) throws Exception {
        try {
            Field field = packetPlayOutScoreboardTeamClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(packet, value);
        } catch (NoSuchFieldException e) {
            // Try to find field by type if name doesn't match
            Field[] fields = packetPlayOutScoreboardTeamClass.getDeclaredFields();
            boolean found = false;

            for (Field field : fields) {
                if (field.getType().equals(value.getClass()) ||
                        (value instanceof Collection && Collection.class.isAssignableFrom(field.getType()))) {
                    field.setAccessible(true);
                    field.set(packet, value);
                    found = true;
                    break;
                }
            }

            if (!found) {
                plugin.getLogger().warning("Could not find field " + fieldName + " for type " + value.getClass().getSimpleName());
                throw e;
            }
        }
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
        plugin.getLogger().info("Refreshing all nametags for " + Bukkit.getOnlinePlayers().size() + " players");
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            // Add small delay between players to prevent packet spam
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (viewer.isOnline()) {
                    updateAllNametagsFor(viewer);
                }
            }, Bukkit.getOnlinePlayers().size() % 20); // Spread over 1 second max
        }
    }

    /**
     * Handle player joining
     */
    public void onPlayerJoin(Player player) {
        plugin.getLogger().info("Player " + player.getName() + " joined, updating nametags");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updateAllNametagsFor(player);
                updateNametagForAll(player);
            }
        }, 60L); // Wait 3 seconds for full login
    }

    /**
     * Handle player leaving
     */
    public void onPlayerLeave(Player player) {
        // Clean up any teams for this player
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) {
                try {
                    removePlayerFromTeam(viewer, player);
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    /**
     * Handle faction changes
     */
    public void onFactionChange(Player player) {
        plugin.getLogger().info("Faction change for " + player.getName() + ", updating nametags");
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
        }, 5L);
    }

    /**
     * Force refresh all nametags (for testing/debugging)
     */
    public void forceRefreshAll() {
        plugin.getLogger().info("Force refreshing all nametags...");

        // Clear all existing teams first
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!viewer.equals(target)) {
                    try {
                        removePlayerFromTeam(viewer, target);
                    } catch (Exception e) {
                        // Ignore errors
                    }
                }
            }
        }

        // Wait a bit then recreate all teams
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (!viewer.equals(target)) {
                        updateNametagFor(target, viewer);
                    }
                }
            }
            plugin.getLogger().info("Force refresh completed.");
        }, 10L);
    }
}