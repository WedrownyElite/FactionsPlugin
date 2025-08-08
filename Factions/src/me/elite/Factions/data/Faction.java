package me.elite.Factions.data;

import java.util.EnumSet;
import java.util.*;

public class Faction {
    public String name;
    public String description = "";
    public UUID owner;
    public Map<UUID, Rank> members = new HashMap<>();
    public boolean isPublic = false;

    // Permission system
    public Map<Rank, Set<FactionPermission>> rankPermissions = new HashMap<>();
    public Map<Relation, Set<RelationPermission>> relationPermissions = new HashMap<>();

    public Faction(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        initializeDefaultPermissions();
    }

    private void initializeDefaultPermissions() {
        // Initialize rank permissions with defaults
        rankPermissions.put(Rank.RECRUIT, getDefaultRecruitPermissions());
        rankPermissions.put(Rank.MEMBER, getDefaultMemberPermissions());
        rankPermissions.put(Rank.MOD, getDefaultModPermissions());
        rankPermissions.put(Rank.ADMIN, getDefaultAdminPermissions());
        rankPermissions.put(Rank.OWNER, getDefaultOwnerPermissions());

        // Initialize relation permissions with defaults
        relationPermissions.put(Relation.NEUTRAL, getDefaultNeutralPermissions());
        relationPermissions.put(Relation.TRUCE, getDefaultTrucePermissions());
        relationPermissions.put(Relation.ALLY, getDefaultAllyPermissions());
        relationPermissions.put(Relation.ENEMY, getDefaultEnemyPermissions());
    }

    private Set<FactionPermission> getDefaultRecruitPermissions() {
        return EnumSet.of(
                FactionPermission.BREAK_BLOCKS,
                FactionPermission.PLACE_BLOCKS,
                FactionPermission.INTERACT,
                FactionPermission.CONTAINER_ACCESS,
                FactionPermission.USE_HOME,
                FactionPermission.WARPS_ACCESS,
                FactionPermission.VIEW_DISCORD,
                FactionPermission.BANK_DEPOSIT
        );
    }

    private Set<FactionPermission> getDefaultMemberPermissions() {
        Set<FactionPermission> perms = EnumSet.copyOf(getDefaultRecruitPermissions());
        perms.addAll(EnumSet.of(
                FactionPermission.ENDER_CHEST_ACCESS,
                FactionPermission.SET_HOME,
                FactionPermission.FACTION_CHEST_ACCESS,
                FactionPermission.BANK_WITHDRAW
        ));
        return perms;
    }

    private Set<FactionPermission> getDefaultModPermissions() {
        Set<FactionPermission> perms = EnumSet.copyOf(getDefaultMemberPermissions());
        perms.addAll(EnumSet.of(
                FactionPermission.PLACE_SPAWNERS,
                FactionPermission.BREAK_SPAWNERS,
                FactionPermission.INVITE_MEMBERS,
                FactionPermission.KICK_MEMBERS,
                FactionPermission.PROMOTE_MEMBERS,
                FactionPermission.DEMOTE_MEMBERS,
                FactionPermission.MANAGE_WARPS,
                FactionPermission.CLAIM_LAND,
                FactionPermission.UNCLAIM_LAND,
                FactionPermission.FLY,
                FactionPermission.FACTION_CHEST_LOGS
        ));
        return perms;
    }

    private Set<FactionPermission> getDefaultAdminPermissions() {
        Set<FactionPermission> perms = EnumSet.copyOf(getDefaultModPermissions());
        perms.addAll(EnumSet.of(
                FactionPermission.SET_TITLES,
                FactionPermission.BANK_LOGS,
                FactionPermission.SET_RELATIONS,
                FactionPermission.UNCLAIM_ALL,
                FactionPermission.CHANGE_DESCRIPTION,
                FactionPermission.SET_DISCORD,
                FactionPermission.SET_ANNOUNCEMENTS,
                FactionPermission.OPEN_CLOSE
        ));
        return perms;
    }

    private Set<FactionPermission> getDefaultOwnerPermissions() {
        // Owner gets all permissions
        return EnumSet.allOf(FactionPermission.class);
    }

    private Set<RelationPermission> getDefaultNeutralPermissions() {
        return EnumSet.noneOf(RelationPermission.class); // No permissions for neutrals
    }

    private Set<RelationPermission> getDefaultTrucePermissions() {
        return EnumSet.of(
                RelationPermission.INTERACT
        );
    }

    private Set<RelationPermission> getDefaultAllyPermissions() {
        return EnumSet.of(
                RelationPermission.BREAK_BLOCKS,
                RelationPermission.PLACE_BLOCKS,
                RelationPermission.INTERACT,
                RelationPermission.CONTAINER_ACCESS,
                RelationPermission.FLY
        );
    }

    private Set<RelationPermission> getDefaultEnemyPermissions() {
        return EnumSet.noneOf(RelationPermission.class); // No permissions for enemies
    }

    public boolean hasPermission(Rank rank, FactionPermission permission) {
        Set<FactionPermission> perms = rankPermissions.get(rank);
        return perms != null && perms.contains(permission);
    }

    public boolean hasRelationPermission(Relation relation, RelationPermission permission) {
        Set<RelationPermission> perms = relationPermissions.get(relation);
        return perms != null && perms.contains(permission);
    }

    public void togglePermission(Rank rank, FactionPermission permission) {
        Set<FactionPermission> perms = rankPermissions.computeIfAbsent(rank, k -> EnumSet.noneOf(FactionPermission.class));
        if (perms.contains(permission)) {
            perms.remove(permission);
        } else {
            perms.add(permission);
        }
    }

    public void toggleRelationPermission(Relation relation, RelationPermission permission) {
        Set<RelationPermission> perms = relationPermissions.computeIfAbsent(relation, k -> EnumSet.noneOf(RelationPermission.class));
        if (perms.contains(permission)) {
            perms.remove(permission);
        } else {
            perms.add(permission);
        }
    }
}