package me.elite.Factions.data;

public enum Relation {
    NEUTRAL("Neutral"),
    TRUCE("Truce"),
    ALLY("Ally"),
    ENEMY("Enemy");

    private final String displayName;

    Relation(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}