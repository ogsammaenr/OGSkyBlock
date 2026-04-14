package me.ogsammaenr.skyblockApi.model;

import me.ogsammaenr.skyblockApi.math.BlockCoordinate;
import me.ogsammaenr.skyblockApi.math.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Island {
    private final IslandID id;
    private final UUID ownerId;
    private final BlockCoordinate center;
    private Location spawnPoint;

    private final Map<UUID, Integer> members = new ConcurrentHashMap<>();
    private int radius = 50;
    private transient boolean isDirty;

    public Island(@NotNull IslandID id, @NotNull UUID ownerId, @NotNull BlockCoordinate center) {
        this.id = id;
        this.ownerId = ownerId;
        this.center = center;
        this.spawnPoint = Location.at(center.x() + 0.5, center.y() + 1.0, center.z() + 0.5);
        this.members.put(ownerId, 4); // OWNER
        this.isDirty = true;
    }


    public boolean isWithinBounds(BlockCoordinate pos) {
        return Math.abs(pos.x() - center.x()) <= radius &&
                Math.abs(pos.z() - center.z()) <= radius;
    }

    public int getPlayerRoleWeight(UUID playerId) {
        return members.getOrDefault(playerId, 0); // 0 = VISITOR
    }

    public void addMember(UUID playerUUID, int roleWeight) {
        this.members.put(playerUUID, roleWeight);
        this.markDirty();
    }

    public void removeMember(UUID playerUUID) {
        this.members.remove(playerUUID);
        this.markDirty();
    }

    public void changeMemberRole(UUID playerUUID, int newRoleWeight) {
        if (members.containsKey(playerUUID)) {
            this.members.put(playerUUID, newRoleWeight);
            this.markDirty();
        }
    }

    public void markDirty(){
        this.isDirty = true;
    }

    public boolean isDirty(){
        return this.isDirty;
    }

    public void clearDirty() {
        this.isDirty = false;
    }

    // Getters & Setters
    public Location getSpawnPoint() { return spawnPoint; }

    @NotNull
    public IslandID getIslandID() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setSpawnPoint(Location spawnPoint) {
        this.spawnPoint = spawnPoint;
        this.isDirty = true;
    }
    public BlockCoordinate getCenter() { return center; }

    public int getRadius() { return radius; }

    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }
}
