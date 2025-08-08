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
    private Class<?> scoreboardTeamClass;
    private Class<?> chatComponentTextClass;
    private Class<?> enumChatFormatClass;
    private Constructor<?> packetConstructor;
    private Constructor<?> chatComponentConstructor;
    private Method sendPacketMethod;
    private Method getHandleMethod;
    private Field playerConnectionField;

    // Team management
    private static final String TEAM_PREFIX = "fac_";

    // Debug flag
    private boolean debugPrinted = false;

    public PacketNametagManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        this.playerFactions = plugin.getPlayerFactions();
        initializeReflection();
    }

    /**
     * Convert ChatColor to NMS EnumChatFormat
     */
    private Object getEnumChatFormat(ChatColor color) {
        if (enumChatFormatClass == null) {
            plugin.getLogger().info("EnumChatFormat class is null");
            return null;
        }

        try {
            String enumName;
            switch (color) {
                case BLACK: enumName = "BLACK"; break;
                case DARK_BLUE: enumName = "DARK_BLUE"; break;
                case DARK_GREEN: enumName = "DARK_GREEN"; break;
                case DARK_AQUA: enumName = "DARK_AQUA"; break;
                case DARK_RED: enumName = "DARK_RED"; break;
                case DARK_PURPLE: enumName = "DARK_PURPLE"; break;
                case GOLD: enumName = "GOLD"; break;
                case GRAY: enumName = "GRAY"; break;
                case DARK_GRAY: enumName = "DARK_GRAY"; break;
                case BLUE: enumName = "BLUE"; break;
                case GREEN: enumName = "GREEN"; break;
                case AQUA: enumName = "AQUA"; break;
                case RED: enumName = "RED"; break;
                case LIGHT_PURPLE: enumName = "LIGHT_PURPLE"; break;
                case YELLOW: enumName = "YELLOW"; break;
                case WHITE: enumName = "WHITE"; break;
                default: enumName = "WHITE"; break;
            }

            // Try different methods to get the enum value
            try {
                Method valueOfMethod = enumChatFormatClass.getMethod("valueOf", String.class);
                Object result = valueOfMethod.invoke(null, enumName);
                plugin.getLogger().info("Successfully converted " + color.name() + " to " + result);
                return result;
            } catch (Exception e) {
                plugin.getLogger().info("valueOf failed: " + e.getMessage());

                // Try getting all enum constants
                Object[] constants = enumChatFormatClass.getEnumConstants();
                if (constants != null) {
                    for (Object constant : constants) {
                        if (constant.toString().equals(enumName)) {
                            plugin.getLogger().info("Found enum constant: " + constant);
                            return constant;
                        }
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().fine("Could not convert ChatColor to EnumChatFormat: " + e.getMessage());
        }

        plugin.getLogger().info("Failed to convert ChatColor " + color.name() + " to EnumChatFormat");
        return null;
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

            // For 1.16+, we need to handle chat components differently
            try {
                chatComponentTextClass = Class.forName("net.minecraft.server." + nmsVersion + ".ChatComponentText");
                chatComponentConstructor = chatComponentTextClass.getConstructor(String.class);
            } catch (ClassNotFoundException e) {
                // Fallback for older versions
                chatComponentTextClass = null;
                chatComponentConstructor = null;
            }

            try {
                enumChatFormatClass = Class.forName("net.minecraft.server." + nmsVersion + ".EnumChatFormat");
            } catch (ClassNotFoundException e) {
                enumChatFormatClass = null;
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

            // Create a short team name (max 16 chars)
            // Format: fac_R_123 where R is relation and 123 is hash
            String relationCode = getRelationCode(color);
            String hash = String.valueOf(Math.abs(target.getName().hashCode()) % 1000);
            String teamName = "fac_" + relationCode + "_" + hash;

            // Ensure it's under 16 characters
            if (teamName.length() > 16) {
                teamName = teamName.substring(0, 16);
            }

            plugin.getLogger().info("Updating nametag: " + target.getName() + " -> " + viewer.getName() + " (Color: " + color.name() + ", Team: " + teamName + ")");

            // Use the systematic method
            sendTeamPacketSystematic(viewer, target, teamName, color);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update nametag for " + target.getName() + " -> " + viewer.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get a short code for each relation type
     */
    private String getRelationCode(ChatColor color) {
        switch (color) {
            case GREEN: return "F"; // Faction/Friend
            case LIGHT_PURPLE: return "A"; // Ally
            case BLUE: return "T"; // Truce
            case RED: return "E"; // Enemy
            case WHITE:
            default: return "N"; // Neutral
        }
    }

    /**
     * Send team packet to update nametag color - Updated for 1.16+
     */
    private void sendTeamPacket(Player viewer, Player target, String teamName, ChatColor color) throws Exception {
        // First, remove player from any existing team
        sendRemovePlayerPacket(viewer, target);

        // Then add to new colored team
        sendCreateTeamPacket(viewer, target, teamName, color);
    }

    /**
     * Send packet to remove player from their current team
     */
    private void sendRemovePlayerPacket(Player viewer, Player target) throws Exception {
        try {
            Object packet = packetPlayOutScoreboardTeamClass.newInstance();

            // Set team name - use a generic name for removal
            Field teamNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("a");
            teamNameField.setAccessible(true);
            teamNameField.set(packet, "old_team");

            // Set action to REMOVE_PLAYERS (value 4)
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
        } catch (Exception e) {
            // Ignore errors for removal - player might not be on a team
        }
    }

    /**
     * Send packet to create team and add player
     */
    private void sendCreateTeamPacket(Player viewer, Player target, String teamName, ChatColor color) throws Exception {
        // Create the team first
        Object createPacket = packetPlayOutScoreboardTeamClass.newInstance();

        // Set team name
        Field teamNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("a");
        teamNameField.setAccessible(true);
        teamNameField.set(createPacket, teamName);

        // Set action to CREATE_TEAM (value 0)
        Field actionField = packetPlayOutScoreboardTeamClass.getDeclaredField("i");
        actionField.setAccessible(true);
        actionField.set(createPacket, 0);

        // Set team display name (same as team name)
        Field displayNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("b");
        displayNameField.setAccessible(true);
        if (chatComponentTextClass != null && chatComponentConstructor != null) {
            Object displayNameComponent = chatComponentConstructor.newInstance(teamName);
            displayNameField.set(createPacket, displayNameComponent);
        } else {
            displayNameField.set(createPacket, teamName);
        }

        // Set prefix (the color)
        Field prefixField = packetPlayOutScoreboardTeamClass.getDeclaredField("c");
        prefixField.setAccessible(true);
        if (chatComponentTextClass != null && chatComponentConstructor != null) {
            Object prefixComponent = chatComponentConstructor.newInstance(color.toString());
            prefixField.set(createPacket, prefixComponent);
        } else {
            prefixField.set(createPacket, color.toString());
        }

        // Set suffix (reset)
        Field suffixField = packetPlayOutScoreboardTeamClass.getDeclaredField("d");
        suffixField.setAccessible(true);
        if (chatComponentTextClass != null && chatComponentConstructor != null) {
            Object suffixComponent = chatComponentConstructor.newInstance(ChatColor.RESET.toString());
            suffixField.set(createPacket, suffixComponent);
        } else {
            suffixField.set(createPacket, ChatColor.RESET.toString());
        }

        // Set name tag visibility to ALWAYS
        Field nameTagVisibilityField = packetPlayOutScoreboardTeamClass.getDeclaredField("e");
        nameTagVisibilityField.setAccessible(true);
        nameTagVisibilityField.set(createPacket, "always");

        // Set collision rule to ALWAYS
        Field collisionRuleField = packetPlayOutScoreboardTeamClass.getDeclaredField("f");
        collisionRuleField.setAccessible(true);
        collisionRuleField.set(createPacket, "always");

        // Set team color using EnumChatFormat
        Field colorField = packetPlayOutScoreboardTeamClass.getDeclaredField("g");
        colorField.setAccessible(true);
        Object enumChatFormat = getEnumChatFormat(color);
        if (enumChatFormat != null) {
            colorField.set(createPacket, enumChatFormat);
        }

        // Set friendly fire (0 = allow, 1 = disallow, 2 = disallow for invisible)
        Field friendlyFireField = packetPlayOutScoreboardTeamClass.getDeclaredField("h");
        friendlyFireField.setAccessible(true);
        friendlyFireField.set(createPacket, 0);

        // Set players (empty for creation)
        Field playersCreateField = packetPlayOutScoreboardTeamClass.getDeclaredField("j");
        playersCreateField.setAccessible(true);
        playersCreateField.set(createPacket, new ArrayList<String>());

        // Send create team packet
        sendPacket(viewer, createPacket);

        // Now add player to the team
        Object addPacket = packetPlayOutScoreboardTeamClass.newInstance();

        // Set team name
        Field addTeamNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("a");
        addTeamNameField.setAccessible(true);
        addTeamNameField.set(addPacket, teamName);

        // Set action to ADD_PLAYERS (value 3)
        Field addActionField = packetPlayOutScoreboardTeamClass.getDeclaredField("i");
        addActionField.setAccessible(true);
        addActionField.set(addPacket, 3);

        // Set players to add
        Field addPlayersField = packetPlayOutScoreboardTeamClass.getDeclaredField("j");
        addPlayersField.setAccessible(true);
        Collection<String> playersToAdd = new ArrayList<>();
        playersToAdd.add(target.getName());
        addPlayersField.set(addPacket, playersToAdd);

        // Send add player packet
        sendPacket(viewer, addPacket);
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
            plugin.getLogger().info("Field " + i + ": " + field.getName() + " (Type: " + field.getType().getSimpleName() + ")");
        }
        plugin.getLogger().info("=== End Field Analysis ===");
    }

    /**
     * Systematic approach to sending team packets for 1.16
     */
    private void sendTeamPacketSystematic(Player viewer, Player target, String teamName, ChatColor color) throws Exception {
        // Debug: Print all fields on first run
        if (!debugPrinted) {
            debugPacketFields();
            debugPrinted = true;
        }

        // Create packet using the simpler CREATE_TEAM with players approach
        Object packet = packetPlayOutScoreboardTeamClass.newInstance();

        // Set team name (field a)
        Field teamNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("a");
        teamNameField.setAccessible(true);
        teamNameField.set(packet, teamName);
        plugin.getLogger().info("Set team name: " + teamName);

        // Set team display name (field b) - empty for now
        try {
            Field displayNameField = packetPlayOutScoreboardTeamClass.getDeclaredField("b");
            displayNameField.setAccessible(true);
            if (chatComponentConstructor != null) {
                Object displayComponent = chatComponentConstructor.newInstance("");
                displayNameField.set(packet, displayComponent);
            }
            plugin.getLogger().info("Set display name");
        } catch (Exception e) {
            plugin.getLogger().info("Could not set display name: " + e.getMessage());
        }

        // Set prefix with color (field c)
        try {
            Field prefixField = packetPlayOutScoreboardTeamClass.getDeclaredField("c");
            prefixField.setAccessible(true);
            if (chatComponentConstructor != null) {
                // Create prefix with the color code
                String colorCode = color.toString();
                Object prefixComponent = chatComponentConstructor.newInstance(colorCode);
                prefixField.set(packet, prefixComponent);
                plugin.getLogger().info("Set prefix: '" + colorCode + "' (" + color.name() + ")");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not set prefix: " + e.getMessage());
        }

        // Set suffix (field d)
        try {
            Field suffixField = packetPlayOutScoreboardTeamClass.getDeclaredField("d");
            suffixField.setAccessible(true);
            if (chatComponentConstructor != null) {
                Object suffixComponent = chatComponentConstructor.newInstance(ChatColor.RESET.toString());
                suffixField.set(packet, suffixComponent);
            }
            plugin.getLogger().info("Set suffix");
        } catch (Exception e) {
            plugin.getLogger().info("Could not set suffix: " + e.getMessage());
        }

        // Set name tag visibility (field e)
        try {
            Field visibilityField = packetPlayOutScoreboardTeamClass.getDeclaredField("e");
            visibilityField.setAccessible(true);
            visibilityField.set(packet, "always");
            plugin.getLogger().info("Set visibility: always");
        } catch (Exception e) {
            plugin.getLogger().info("Could not set visibility: " + e.getMessage());
        }

        // Set collision rule (field f)
        try {
            Field collisionField = packetPlayOutScoreboardTeamClass.getDeclaredField("f");
            collisionField.setAccessible(true);
            collisionField.set(packet, "always");
            plugin.getLogger().info("Set collision: always");
        } catch (Exception e) {
            plugin.getLogger().info("Could not set collision: " + e.getMessage());
        }

        // Set team color (field g)
        try {
            Field colorField = packetPlayOutScoreboardTeamClass.getDeclaredField("g");
            colorField.setAccessible(true);
            Object enumColor = getEnumChatFormat(color);
            if (enumColor != null) {
                colorField.set(packet, enumColor);
                plugin.getLogger().info("Set team color: " + enumColor);
            } else {
                plugin.getLogger().info("EnumChatFormat was null for color: " + color.name());
            }
        } catch (Exception e) {
            plugin.getLogger().info("Could not set team color: " + e.getMessage());
        }

        // Field h is a Collection (players), based on the error message
        try {
            Field playersField = packetPlayOutScoreboardTeamClass.getDeclaredField("h");
            playersField.setAccessible(true);
            Collection<String> players = new ArrayList<>();
            players.add(target.getName());
            playersField.set(packet, players);
            plugin.getLogger().info("Set players in field h: [" + target.getName() + "]");
        } catch (Exception e) {
            plugin.getLogger().warning("Could not set players in field h: " + e.getMessage());
        }

        // Field j is an int (probably action), based on the error message
        try {
            Field actionField = packetPlayOutScoreboardTeamClass.getDeclaredField("j");
            actionField.setAccessible(true);
            actionField.set(packet, 0); // 0 = CREATE_TEAM
            plugin.getLogger().info("Set action in field j: CREATE_TEAM (0)");
        } catch (Exception e) {
            plugin.getLogger().warning("Could not set action in field j: " + e.getMessage());
        }

        // Try to find the actual action field (int type)
        try {
            Field[] allFields = packetPlayOutScoreboardTeamClass.getDeclaredFields();
            for (Field field : allFields) {
                if (field.getType() == int.class && !field.getName().equals("j")) {
                    field.setAccessible(true);
                    field.set(packet, 0); // CREATE_TEAM
                    plugin.getLogger().info("Set action in field " + field.getName() + ": CREATE_TEAM (0)");
                    break;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().info("Could not find/set action field: " + e.getMessage());
        }

        // Send the packet
        sendPacket(viewer, packet);
        plugin.getLogger().info("Successfully sent packet for " + target.getName() + " to " + viewer.getName());
    }
    private void sendTeamPacketAlternative(Player viewer, Player target, String teamName, ChatColor color) throws Exception {
        // Debug: Print all fields on first run
        if (!debugPrinted) {
            debugPacketFields();
            debugPrinted = true;
        }

        // Try the simplified approach for 1.16
        Object packet = packetPlayOutScoreboardTeamClass.newInstance();

        Field[] fields = packetPlayOutScoreboardTeamClass.getDeclaredFields();

        try {
            // Field 0: Team name (String)
            if (fields.length > 0 && fields[0].getType() == String.class) {
                fields[0].setAccessible(true);
                fields[0].set(packet, teamName);
                plugin.getLogger().info("Set team name: " + teamName);
            }

            // Look for IChatBaseComponent fields for prefix/suffix
            for (int i = 1; i < Math.min(fields.length, 8); i++) {
                Field field = fields[i];
                field.setAccessible(true);

                if (field.getType().getSimpleName().contains("IChatBaseComponent") ||
                        field.getType().getSimpleName().contains("ChatComponent")) {

                    if (i == 2) { // Usually prefix
                        if (chatComponentConstructor != null) {
                            Object prefixComponent = chatComponentConstructor.newInstance(color.toString());
                            field.set(packet, prefixComponent);
                            plugin.getLogger().info("Set prefix component at field " + i);
                        }
                    } else if (i == 3) { // Usually suffix
                        if (chatComponentConstructor != null) {
                            Object suffixComponent = chatComponentConstructor.newInstance(ChatColor.RESET.toString());
                            field.set(packet, suffixComponent);
                            plugin.getLogger().info("Set suffix component at field " + i);
                        }
                    }
                }
            }

            // Look for action field (int)
            for (int i = 1; i < fields.length; i++) {
                Field field = fields[i];
                if (field.getType() == int.class) {
                    field.setAccessible(true);
                    field.set(packet, 0); // CREATE_TEAM
                    plugin.getLogger().info("Set action (CREATE_TEAM) at field " + i);
                    break;
                }
            }

            // Look for Collection field for players
            for (int i = 1; i < fields.length; i++) {
                Field field = fields[i];
                if (Collection.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Collection<String> players = new ArrayList<>();
                    players.add(target.getName());
                    field.set(packet, players);
                    plugin.getLogger().info("Set players collection at field " + i);
                    break;
                }
            }

            sendPacket(viewer, packet);
            plugin.getLogger().info("Sent packet for " + target.getName() + " to " + viewer.getName());

        } catch (Exception e) {
            plugin.getLogger().warning("Alternative packet method failed: " + e.getMessage());
            e.printStackTrace();
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

        plugin.getLogger().info("DEBUG: Getting relation color for " + target.getName() + " (faction: " + targetFaction + ") -> " + viewer.getName() + " (faction: " + viewerFaction + ")");

        // If viewer has no faction, show white
        if (viewerFaction == null) {
            plugin.getLogger().info("DEBUG: Viewer has no faction, returning WHITE");
            return ChatColor.WHITE;
        }

        // If target has no faction, show white (neutral)
        if (targetFaction == null) {
            plugin.getLogger().info("DEBUG: Target has no faction, returning WHITE");
            return ChatColor.WHITE;
        }

        // Same faction = GREEN
        if (targetFaction.equals(viewerFaction)) {
            plugin.getLogger().info("DEBUG: Same faction, returning GREEN");
            return ChatColor.GREEN;
        }

        // Different factions - check relations
        Relation relation = plugin.getRelationManager().getRelation(viewerFaction, targetFaction);
        plugin.getLogger().info("DEBUG: Relation between " + viewerFaction + " and " + targetFaction + " is: " + relation);

        ChatColor color;
        switch (relation) {
            case ALLY:
                color = ChatColor.LIGHT_PURPLE; // Purple for allies
                break;
            case TRUCE:
                color = ChatColor.BLUE; // Blue for truce
                break;
            case ENEMY:
                color = ChatColor.RED; // Red for enemies
                break;
            case NEUTRAL:
            default:
                color = ChatColor.WHITE; // White for neutral
                break;
        }

        plugin.getLogger().info("DEBUG: Returning color: " + color.name());
        return color;
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