package me.elite.Factions.data;

public enum RelationPermission {
    BREAK_BLOCKS("Break Blocks"),
    PLACE_BLOCKS("Place Blocks"),
    PLACE_SPAWNERS("Place Spawners"),
    BREAK_SPAWNERS("Break Spawners"),
    INTERACT("Interact"),
    CONTAINER_ACCESS("Container Access"),
    ENDER_CHEST_ACCESS("Ender Chest Access"),
    FLY("Fly (Ability to fly in faction land)");

    private final String displayName;

    RelationPermission(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}