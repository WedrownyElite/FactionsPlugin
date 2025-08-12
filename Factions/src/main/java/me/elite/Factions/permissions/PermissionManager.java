package me.elite.Factions.permissions;

import me.elite.Factions.FactionsPlugin;
import org.bukkit.entity.Player;

public class PermissionManager {
    private final FactionsPlugin plugin;

    public PermissionManager(FactionsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if a player has permission to bypass faction protections
     * Permission format: factions.bypass.<faction>.<action>
     *
     * @param player The player to check
     * @param faction The faction name (e.g., "Spawn", "Warzone", or faction name)
     * @param action The action type (e.g., "break", "place", "interact", "pvp", "damage_mobs")
     * @return true if player has bypass permission
     */
    public boolean hasPermissionBypass(Player player, String faction, String action) {
        // Global bypass permission (works for all factions and actions)
        if (player.hasPermission("factions.bypass.*")) {
            return true;
        }

        // Faction-specific bypass permission (works for all actions in a specific faction)
        if (player.hasPermission("factions.bypass." + faction.toLowerCase() + ".*")) {
            return true;
        }

        // Action-specific bypass permission (works for specific action in all factions)
        if (player.hasPermission("factions.bypass.*." + action.toLowerCase())) {
            return true;
        }

        // Specific bypass permission (faction + action)
        if (player.hasPermission("factions.bypass." + faction.toLowerCase() + "." + action.toLowerCase())) {
            return true;
        }

        // Admin bypass (op players can bypass everything)
        if (player.isOp()) {
            return true;
        }

        return false;
    }
}