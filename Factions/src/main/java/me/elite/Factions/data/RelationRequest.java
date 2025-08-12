package me.elite.Factions.data;

import me.elite.Factions.FactionsPlugin;
import java.util.UUID;

import org.bukkit.Bukkit;

public class RelationRequest {
    public final String fromFaction;
    public final String toFaction;
    public final Relation requestedRelation;
    public final UUID requestedBy; // Player who sent the request
    public final long timestamp; // When the request was sent

    public RelationRequest(String fromFaction, String toFaction, Relation requestedRelation, UUID requestedBy) {
        this.fromFaction = fromFaction;
        this.toFaction = toFaction;
        this.requestedRelation = requestedRelation;
        this.requestedBy = requestedBy;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Check if the request has expired (older than 7 days)
     */
    public boolean isExpired() {
        // Get expiration days from config
        FactionsPlugin plugin = (FactionsPlugin) Bukkit.getPluginManager().getPlugin("Factions");
        int expirationDays = plugin != null ? plugin.getConfigManager().getRequestExpirationDays() : 7;
        long expirationTime = expirationDays * 24 * 60 * 60 * 1000L; // Convert to milliseconds
        return System.currentTimeMillis() - timestamp > expirationTime;
    }

    /**
     * Get a unique identifier for this request
     */
    public String getRequestId() {
        return fromFaction + "->" + toFaction + ":" + requestedRelation.name();
    }
}