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

            // Create a unique team name for this target-viewer pair
            String baseName = target.getName().toLowerCase();
            String teamName = TEAM_PREFIX + (baseName.length() > 12 ? baseName.substring(0, 12) : baseName);

            plugin.getLogger().info("Updating nametag: " + target.getName() + " -> " + viewer.getName() + " (Suffix: '" + suffix + "', Team: " + teamName + ")");

            // First remove player from any existing team
            removePlayerFromTeam(viewer, target);

            // Small delay before creating new team
            final String finalTeamName = teamName;
            final String finalSuffix = suffix;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    sendModernTeamPacket(viewer, target, finalTeamName, finalSuffix);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to send team packet: " + e.getMessage());
                }
            }, 1L); // 1 tick delay

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update nametag for " + target.getName() + " -> " + viewer.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Remove player from any existing team
     */
    private void removePlayerFromTeam(Player viewer, Player target) {
        try {
            Object packet = packetPlayOutScoreboardTeamClass.newInstance();

            // Generic team name for removal
            Field teamNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("a");
            teamNameField.setAccessible(true);
            teamNameField.set(packet, TEAM_PREFIX + target.getName().toLowerCase());

            // Set action to REMOVE_PLAYERS (4)
            Field actionField = packetPlayOutScoreboardTeamClass.getDeclaredField("i");
            actionField.setAccessible(true);
            actionField.set(packet, 4);

            // Set players to remove
            Field playersField = packetPlayOutScoreboardTeamClass.getDeclaredField("h");
            playersField.setAccessible(true);
            Collection<String> players = new ArrayList<>();
            players.add(target.getName());
            playersField.set(packet, players);

            sendPacket(viewer, packet);
            plugin.getLogger().fine("✓ Removed " + target.getName() + " from team");
        } catch (Exception e) {
            // Ignore removal errors
            plugin.getLogger().fine("Remove player failed (expected): " + e.getMessage());
        }
    }

    /**
     * Alternative team packet approach
     */
    private void sendAlternativeTeamPacket(Player viewer, Player target, String teamName, String suffix) throws Exception {
        plugin.getLogger().info("Trying alternative packet approach...");

        // First remove player from any existing team
        sendRemovePlayerPacket(viewer, target);

        // Then create new team with suffix
        sendCreateTeamPacket(viewer, target, teamName, suffix);
    }

    /**
     * Send remove player packet
     */
    private void sendRemovePlayerPacket(Player viewer, Player target) throws Exception {
        try {
            Object packet = packetPlayOutScoreboardTeamClass.newInstance();

            // Set a generic team name for removal
            Field teamNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("a");
            teamNameField.setAccessible(true);
            teamNameField.set(packet, "remove_team");

            // Set action to REMOVE_PLAYERS (4)
            Field[] fields = packetPlayOutScoreboardTeamClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType() == int.class) {
                    field.setAccessible(true);
                    field.set(packet, 4); // REMOVE_PLAYERS
                    break;
                }
            }

            // Set players to remove
            for (Field field : fields) {
                if (Collection.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Collection<String> players = new ArrayList<>();
                    players.add(target.getName());
                    field.set(packet, players);
                    break;
                }
            }

            sendPacket(viewer, packet);
            plugin.getLogger().info("✓ Sent remove player packet");
        } catch (Exception e) {
            // Ignore removal errors - player might not be on a team
            plugin.getLogger().fine("Remove player packet failed (expected): " + e.getMessage());
        }
    }

    /**
     * Send create team packet
     */
    private void sendCreateTeamPacket(Player viewer, Player target, String teamName, String suffix) throws Exception {
        Object packet = packetPlayOutScoreboardTeamClass.newInstance();

        Field[] fields = packetPlayOutScoreboardTeamClass.getDeclaredFields();

        plugin.getLogger().info("Creating team with suffix: '" + suffix + "'");

        // Set team name (usually field 'a' or index 0)
        for (Field field : fields) {
            if (field.getType() == String.class) {
                field.setAccessible(true);
                field.set(packet, teamName);
                plugin.getLogger().info("✓ Set team name in field " + field.getName());
                break;
            }
        }

        // Set action to CREATE_TEAM (0)
        for (Field field : fields) {
            if (field.getType() == int.class) {
                field.setAccessible(true);
                field.set(packet, 0);
                plugin.getLogger().info("✓ Set action to CREATE_TEAM in field " + field.getName());
                break;
            }
        }

        // Try to set chat component fields systematically
        int componentFieldCount = 0;
        for (Field field : fields) {
            field.setAccessible(true);

            // Check if it's a chat component field
            if (chatComponentTextClass != null &&
                    (field.getType().equals(chatComponentTextClass) ||
                            field.getType().getSimpleName().contains("IChatBaseComponent") ||
                            field.getType().getSimpleName().contains("ChatComponent"))) {

                componentFieldCount++;

                if (componentFieldCount == 1) {
                    // First component field - display name (empty)
                    Object displayComponent = chatComponentConstructor.newInstance("");
                    field.set(packet, displayComponent);
                    plugin.getLogger().info("✓ Set display name in field " + field.getName());
                } else if (componentFieldCount == 2) {
                    // Second component field - prefix (empty)
                    Object prefixComponent = chatComponentConstructor.newInstance("");
                    field.set(packet, prefixComponent);
                    plugin.getLogger().info("✓ Set prefix in field " + field.getName());
                } else if (componentFieldCount == 3) {
                    // Third component field - suffix (our relation indicator)
                    Object suffixComponent = chatComponentConstructor.newInstance(suffix);
                    field.set(packet, suffixComponent);
                    plugin.getLogger().info("✓ Set suffix '" + suffix + "' in field " + field.getName());
                }
            }
            // For string fields after team name
            else if (field.getType() == String.class && componentFieldCount == 0) {
                // These might be visibility/collision settings
                String fieldName = field.getName();
                if (fieldName.contains("visibility") || fieldName.equals("e")) {
                    field.set(packet, "always");
                    plugin.getLogger().info("✓ Set visibility to 'always' in field " + fieldName);
                } else if (fieldName.contains("collision") || fieldName.equals("f")) {
                    field.set(packet, "always");
                    plugin.getLogger().info("✓ Set collision to 'always' in field " + fieldName);
                }
            }
        }

        // Set players
        for (Field field : fields) {
            if (Collection.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                Collection<String> players = new ArrayList<>();
                players.add(target.getName());
                field.set(packet, players);
                plugin.getLogger().info("✓ Set players [" + target.getName() + "] in field " + field.getName());
                break;
            }
        }

        sendPacket(viewer, packet);
        plugin.getLogger().info("✓ Sent create team packet with suffix: '" + suffix + "'");
    }

    /**
     * Send team packet to update nametag suffix
     */
    private void sendTeamPacket(Player viewer, Player target, String teamName, String suffix) throws Exception {
        plugin.getLogger().info("Creating team packet for " + target.getName() + " with suffix: '" + suffix + "'");

        // Try the modern approach first (for 1.16+)
        try {
            sendModernTeamPacket(viewer, target, teamName, suffix);
        } catch (Exception e) {
            plugin.getLogger().warning("Modern approach failed, trying legacy: " + e.getMessage());
            sendLegacyTeamPacket(viewer, target, teamName, suffix);
        }
    }

    /**
     * Modern team packet for 1.16+ (Create team with players included)
     */
    private void sendModernTeamPacket(Player viewer, Player target, String teamName, String suffix) throws Exception {
        Object packet = packetPlayOutScoreboardTeamClass.newInstance();

        // Field a: Team name
        Field teamNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("a");
        teamNameField.setAccessible(true);
        teamNameField.set(packet, teamName);

        // Field b: Display name (IChatBaseComponent)
        Field displayNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("b");
        displayNameField.setAccessible(true);
        Object displayComponent = chatComponentConstructor.newInstance("");
        displayNameField.set(packet, displayComponent);

        // Field c: Prefix (IChatBaseComponent)
        Field prefixField = packetPlayOutScoreboardTeamClass.getDeclaredField("c");
        prefixField.setAccessible(true);
        Object prefixComponent = chatComponentConstructor.newInstance("");
        prefixField.set(packet, prefixComponent);

        // Field d: Suffix (IChatBaseComponent) - THE IMPORTANT ONE
        Field suffixField = packetPlayOutScoreboardTeamClass.getDeclaredField("d");
        suffixField.setAccessible(true);
        Object suffixComponent = chatComponentConstructor.newInstance(suffix);
        suffixField.set(packet, suffixComponent);

        // Field e: Name tag visibility
        Field visibilityField = packetPlayOutScoreboardTeamClass.getDeclaredField("e");
        visibilityField.setAccessible(true);
        visibilityField.set(packet, "always");

        // Field f: Collision rule
        Field collisionField = packetPlayOutScoreboardTeamClass.getDeclaredField("f");
        collisionField.setAccessible(true);
        collisionField.set(packet, "always");

        // Field g: Team color (EnumChatFormat) - try setting this to reset
        try {
            Field colorField = packetPlayOutScoreboardTeamClass.getDeclaredField("g");
            colorField.setAccessible(true);
            // Try to get RESET enum value
            Class<?> enumChatFormatClass = Class.forName("net.minecraft.server." + nmsVersion + ".EnumChatFormat");
            Object resetColor = enumChatFormatClass.getField("RESET").get(null);
            colorField.set(packet, resetColor);
        } catch (Exception e) {
            plugin.getLogger().fine("Could not set team color: " + e.getMessage());
        }

        // Field h: Players collection
        Field playersField = packetPlayOutScoreboardTeamClass.getDeclaredField("h");
        playersField.setAccessible(true);
        Collection<String> players = new ArrayList<>();
        players.add(target.getName());
        playersField.set(packet, players);

        // Field i: Action (0 = CREATE_TEAM_WITH_PLAYERS)
        Field actionField = packetPlayOutScoreboardTeamClass.getDeclaredField("i");
        actionField.setAccessible(true);
        actionField.set(packet, 0);

        // Field j: Friendly fire flags
        Field friendlyFireField = packetPlayOutScoreboardTeamClass.getDeclaredField("j");
        friendlyFireField.setAccessible(true);
        friendlyFireField.set(packet, 3);

        sendPacket(viewer, packet);
        plugin.getLogger().info("✓ Sent team packet with suffix: '" + suffix + "'");
    }

    /**
     * Legacy team packet approach (separate create and add)
     */
    private void sendLegacyTeamPacket(Player viewer, Player target, String teamName, String suffix) throws Exception {
        plugin.getLogger().info("Using legacy approach...");

        // First: Create the team (action 0)
        Object createPacket = packetPlayOutScoreboardTeamClass.newInstance();

        Field teamNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("a");
        teamNameField.setAccessible(true);
        teamNameField.set(createPacket, teamName);

        Field displayNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("b");
        displayNameField.setAccessible(true);
        Object displayComponent = chatComponentConstructor.newInstance("");
        displayNameField.set(createPacket, displayComponent);

        Field prefixField = packetPlayOutScoreboardTeamClass.getDeclaredField("c");
        prefixField.setAccessible(true);
        Object prefixComponent = chatComponentConstructor.newInstance("");
        prefixField.set(createPacket, prefixComponent);

        Field suffixField = packetPlayOutScoreboardTeamClass.getDeclaredField("d");
        suffixField.setAccessible(true);
        Object suffixComponent = chatComponentConstructor.newInstance(suffix);
        suffixField.set(createPacket, suffixComponent);

        Field visibilityField = packetPlayOutScoreboardTeamClass.getDeclaredField("e");
        visibilityField.setAccessible(true);
        visibilityField.set(createPacket, "always");

        Field collisionField = packetPlayOutScoreboardTeamClass.getDeclaredField("f");
        collisionField.setAccessible(true);
        collisionField.set(createPacket, "always");

        Field playersField = packetPlayOutScoreboardTeamClass.getDeclaredField("h");
        playersField.setAccessible(true);
        playersField.set(createPacket, new ArrayList<String>());

        Field actionField = packetPlayOutScoreboardTeamClass.getDeclaredField("i");
        actionField.setAccessible(true);
        actionField.set(createPacket, 0); // CREATE_TEAM

        Field friendlyFireField = packetPlayOutScoreboardTeamClass.getDeclaredField("j");
        friendlyFireField.setAccessible(true);
        friendlyFireField.set(createPacket, 3);

        sendPacket(viewer, createPacket);
        plugin.getLogger().info("✓ Sent team creation packet");

        // Second: Add player to team (action 3)
        Object addPacket = packetPlayOutScoreboardTeamClass.newInstance();

        Field addTeamNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("a");
        addTeamNameField.setAccessible(true);
        addTeamNameField.set(addPacket, teamName);

        Field addPlayersField = packetPlayOutScoreboardTeamClass.getDeclaredField("h");
        addPlayersField.setAccessible(true);
        Collection<String> playersToAdd = new ArrayList<>();
        playersToAdd.add(target.getName());
        addPlayersField.set(addPacket, playersToAdd);

        Field addActionField = packetPlayOutScoreboardTeamClass.getDeclaredField("i");
        addActionField.setAccessible(true);
        addActionField.set(addPacket, 3); // ADD_PLAYERS

        sendPacket(viewer, addPacket);
        plugin.getLogger().info("✓ Sent add player packet");
    }

    /**
     * Debug method to print packet field information
     */
    private void debugPacketFields() {
        if (packetPlayOutScoreboardTeamClass == null) return;

        plugin.getLogger().info("=== PacketPlayOutScoreboardTeam Field Analysis ===");
        Field[] fields = packetPlayOutScoreboardTeamClass.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            plugin.getLogger().info("Field " + field.getName() + " (Type: " + field.getType().getSimpleName() + ")");
        }
        plugin.getLogger().info("=== End Field Analysis ===");
    }

    /**
     * Find the int field (action field)
     */
    private Field findIntField(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == int.class) {
                return field;
            }
        }
        return null;
    }

    /**
     * Find the Collection field (players field)
     */
    private Field findCollectionField(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (Collection.class.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        return null;
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

        // Same faction = green F (with space)
        if (targetFaction.equals(viewerFaction)) {
            return " " + ChatColor.GREEN + "F";
        }

        // Different factions - check relations
        Relation relation = plugin.getRelationManager().getRelation(viewerFaction, targetFaction);

        switch (relation) {
            case ALLY:
                return " " + ChatColor.LIGHT_PURPLE + "A"; // Purple A for ally
            case TRUCE:
                return " " + ChatColor.BLUE + "T"; // Blue T for truce
            case ENEMY:
                return " " + ChatColor.RED + "E"; // Red E for enemy
            case NEUTRAL:
            default:
                return ""; // Nothing for neutral
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
        // Small delay to ensure faction data is updated
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Update how this player sees everyone
            updateAllNametagsFor(player);
            // Update how everyone sees this player
            updateNametagForAll(player);
        }, 5L); // 5 tick delay
    }

    /**
     * Handle relation changes
     */
    public void onRelationChange(String faction1, String faction2) {
        // Small delay to ensure relation data is updated
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Update all players from both factions
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerFaction = playerFactions.get(player.getUniqueId());
                if (playerFaction != null && (playerFaction.equals(faction1) || playerFaction.equals(faction2))) {
                    updateAllNametagsFor(player);
                    updateNametagForAll(player);
                }
            }
        }, 5L); // 5 tick delay
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