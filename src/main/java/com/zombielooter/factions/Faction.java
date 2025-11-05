package com.zombielooter.factions;

import cn.nukkit.level.Location;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Faction {
    private final String name;
    private final UUID leader;
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> pendingInvites = new HashSet<>(); // NEW
    private Location home;
    private int power = 10;

    public Faction(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        this.members.add(leader);
    }

    public String getName() { return name; }
    public UUID getLeader() { return leader; }
    public Set<UUID> getMembers() { return members; }
    public boolean isMember(UUID id) { return members.contains(id); }
    public void addMember(UUID id) { members.add(id); }
    public void removeMember(UUID id) { members.remove(id); }

    public Location getHome() { return home; }
    public void setHome(Location home) { this.home = home; }

    public int getPower() { return power; }
    public void setPower(int power) { this.power = Math.max(0, power); }
    public void addPower(int delta) { setPower(this.power + delta); }

    // NEW: Invitation management
    public void invite(UUID playerId) {
        if (!members.contains(playerId)) {
            pendingInvites.add(playerId);
        }
    }

    public void revokeInvite(UUID playerId) {
        pendingInvites.remove(playerId);
    }

    public boolean hasInvite(UUID playerId) {
        return pendingInvites.contains(playerId);
    }

    public boolean acceptInvite(UUID playerId) {
        if (pendingInvites.remove(playerId)) {
            members.add(playerId);
            return true;
        }
        return false;
    }

    public Set<UUID> getPendingInvites() {
        return new HashSet<>(pendingInvites); // Return copy for safety
    }

    public void clearExpiredInvites() {
        // Can be called periodically to clean up
        pendingInvites.clear();
    }
}
