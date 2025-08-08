package me.elite.Factions.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Faction {
    public String name;
    public String description = "";
    public UUID owner;
    public Map<UUID, Rank> members = new HashMap<>();
    public boolean isPublic = false;

    public Faction(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
    }
}