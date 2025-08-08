package me.elite.Factions.data;

import java.util.UUID;

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
        long sevenDays = 7 * 24 * 60 * 60 * 1000L; // 7 days in milliseconds
        return System.currentTimeMillis() - timestamp > sevenDays;
    }

    /**
     * Get a unique identifier for this request
     */
    public String getRequestId() {
        return fromFaction + "->" + toFaction + ":" + requestedRelation.name();
    }
}