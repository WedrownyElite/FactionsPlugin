package me.elite.Factions.data;

public enum Rank {
    RECRUIT, MEMBER, MOD, ADMIN, OWNER;

    public Rank promote() {
        return values()[Math.min(values().length - 1, this.ordinal() + 1)];
    }

    public Rank demote() {
        return values()[Math.max(0, this.ordinal() - 1)];
    }
}