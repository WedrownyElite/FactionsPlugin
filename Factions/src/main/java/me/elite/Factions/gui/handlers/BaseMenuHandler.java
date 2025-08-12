package me.elite.Factions.gui.handlers;

import me.elite.Factions.FactionsPlugin;
import me.elite.Factions.data.Faction;
import me.elite.Factions.data.FactionPermission;
import me.elite.Factions.data.Rank;
import me.elite.Factions.data.Relation;
import me.elite.Factions.data.RelationPermission;
import me.elite.Factions.gui.MenuHandler;
import me.elite.Factions.gui.components.MenuComponents;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public abstract class BaseMenuHandler {
    protected final FactionsPlugin plugin;
    protected final MenuHandler parentHandler;
    protected final Map<String, Faction> factions;
    protected final Map<UUID, String> playerFactions;
    protected final Map<UUID, Set<String>> playerInvitations;

    // Shared tracking variables
    protected Rank targetCurrentRank;
    protected Rank currentManagerRank;
    protected final Map<UUID, String> pendingOwnershipTransfersGUI = new HashMap<>();
    protected final Map<UUID, UUID> pendingKicks = new HashMap<>();

    public BaseMenuHandler(FactionsPlugin plugin, MenuHandler parentHandler) {
        this.plugin = plugin;
        this.parentHandler = parentHandler;
        this.factions = plugin.getFactions();
        this.playerFactions = plugin.getPlayerFactions();
        this.playerInvitations = plugin.getPlayerInvitations();
    }

    /**
     * Create a back button using the shared component
     */
    protected ItemStack createBackButton() {
        return MenuComponents.createBackButton();
    }

    /**
     * Create a custom head using the shared component
     */
    protected ItemStack createCustomHead(String texture) {
        return MenuComponents.createCustomHead(texture);
    }

    /**
     * Helper methods for checking permissions without sending error messages (for GUI display)
     */
    protected boolean canModifyRankPermissionsCheck(Rank playerRank, Rank targetRank, Faction faction) {
        if (playerRank != Rank.OWNER && !faction.hasPermission(playerRank, FactionPermission.MANAGE_PERMISSIONS)) {
            return false;
        }
        return playerRank == Rank.OWNER || targetRank.ordinal() < playerRank.ordinal();
    }

    protected boolean canModifyRelationPermissionsCheck(Rank playerRank, Faction faction) {
        return playerRank == Rank.OWNER || faction.hasPermission(playerRank, FactionPermission.MANAGE_PERMISSIONS);
    }

    protected boolean canTogglePermissionCheck(Rank playerRank, Rank targetRank, FactionPermission permission, Faction faction) {
        if (playerRank == Rank.OWNER) return true;
        return faction.hasPermission(playerRank, permission);
    }

    protected boolean canToggleRelationPermissionCheck(Rank playerRank, Relation targetRelation, RelationPermission permission, Faction faction) {
        if (playerRank == Rank.OWNER) return true;

        FactionPermission equivalentPermission = getEquivalentFactionPermission(permission);
        if (equivalentPermission != null) {
            return faction.hasPermission(playerRank, equivalentPermission);
        }
        return true;
    }

    /**
     * Get the equivalent faction permission for a relation permission
     */
    protected FactionPermission getEquivalentFactionPermission(RelationPermission relationPermission) {
        switch (relationPermission) {
            case BREAK_BLOCKS:
                return FactionPermission.BREAK_BLOCKS;
            case PLACE_BLOCKS:
                return FactionPermission.PLACE_BLOCKS;
            case PLACE_SPAWNERS:
                return FactionPermission.PLACE_SPAWNERS;
            case BREAK_SPAWNERS:
                return FactionPermission.BREAK_SPAWNERS;
            case INTERACT:
                return FactionPermission.INTERACT;
            case CONTAINER_ACCESS:
                return FactionPermission.CONTAINER_ACCESS;
            case ENDER_CHEST_ACCESS:
                return FactionPermission.ENDER_CHEST_ACCESS;
            case FLY:
                return FactionPermission.FLY;
            default:
                return null; // No direct equivalent
        }
    }

    // Getters for shared state
    protected Rank getTargetCurrentRank() {
        return targetCurrentRank;
    }

    protected Rank getCurrentManagerRank() {
        return currentManagerRank;
    }

    // Cleanup methods
    public void clearPendingKick(UUID kickerUUID) {
        pendingKicks.remove(kickerUUID);
    }

    public void clearPendingOwnershipTransfer(UUID playerUUID) {
        pendingOwnershipTransfersGUI.remove(playerUUID);
    }

    public String getPendingGUIOwnershipTransfer(UUID playerUUID) {
        return pendingOwnershipTransfersGUI.get(playerUUID);
    }
}