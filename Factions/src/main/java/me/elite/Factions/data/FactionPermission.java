package me.elite.Factions.data;

public enum FactionPermission {
    // Member permissions
    BREAK_BLOCKS("Break Blocks"),
    PLACE_BLOCKS("Place Blocks"),
    PLACE_SPAWNERS("Place Spawners"),
    BREAK_SPAWNERS("Break Spawners"),
    INTERACT("Interact (Levers, Pressure Plates, etc)"),
    CONTAINER_ACCESS("Container Access"),
    ENDER_CHEST_ACCESS("Ender Chest Access"),
    KICK_MEMBERS("Kick Members"),
    INVITE_MEMBERS("Invite Members"),
    PROMOTE_MEMBERS("Promote Members"),
    DEMOTE_MEMBERS("Demote Members"),
    WARPS_ACCESS("Warps Access"),
    MANAGE_WARPS("Manage Warps"),
    SET_HOME("Set Home"),
    USE_HOME("Use Home"),
    CHANGE_NAME("Change Name"),
    BANK_DEPOSIT("Bank Deposit"),
    BANK_WITHDRAW("Bank Withdraw"),
    BANK_LOGS("Bank Logs"),
    MANAGE_PERMISSIONS("Manage Permissions"),
    SET_RELATIONS("Set Relations"),
    CLAIM_LAND("Claim Land"),
    UNCLAIM_LAND("Unclaim Land"),
    UNCLAIM_ALL("Unclaim All"),
    CHANGE_DESCRIPTION("Change Description"),
    FACTION_CHEST_ACCESS("Faction Chest Access"),
    FACTION_CHEST_LOGS("Faction Chest Logs"),
    FLY("Fly in faction claims"),
    SET_DISCORD("Set Discord"),
    VIEW_DISCORD("View Discord"),
    SET_ANNOUNCEMENTS("Set Announcements"),
    OPEN_CLOSE("Open & Close (Public/Private)");

    private final String displayName;

    FactionPermission(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}