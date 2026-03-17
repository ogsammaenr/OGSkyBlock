package me.ogsammenr.skyblock.model;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Island {
    private final UUID islandUUID;
    private final BlockPos center;
    private BlockPos spawnPoint;

    // PlayerUUID -> IslandRole
    private final Map<UUID, IslandRole> members = new HashMap<>();
    // IslandAction -> Minimum Required Role
    private final Map<IslandAction, IslandRole> permissions = new HashMap<>();

    /**
     * create new island with default permissions
     *
     * @param islandUUID islands unique identifier
     * @param ownerUUID island owner unique identifier
     * @param center island center position (bedrock coordinates)
     */
    public Island(UUID islandUUID, UUID ownerUUID, BlockPos center) {
        this.islandUUID = islandUUID;
        this.center = center;
        this.spawnPoint = center.above();

        this.members.put(ownerUUID, IslandRole.OWNER);

        setDefaultPermissions();
    }

    /**
     * @param playerUUID the UUID of the player whose role we want to check
     * @return the role of the player on the island, or VISITOR if they are not a member
     */
    public IslandRole getPlayerRole(UUID playerUUID) {
        return members.getOrDefault(playerUUID, IslandRole.VISITOR);
    }

    /**
     * Check if a player can perform a specific action based on their role and the island's permissions.
     *
     * @param playerUUID the UUID of the player attempting the action
     * @param action the action the player is trying to perform
     * @return true if the player has the required role to perform the action, false otherwise
     */
    public boolean canPerformAction(UUID playerUUID, IslandAction action) {
        IslandRole playerRole = getPlayerRole(playerUUID);
        IslandRole requiredRole = permissions.getOrDefault(action, IslandRole.OWNER);
        return playerRole.isAtLeast(requiredRole);
    }

    public boolean isWithinBounds(BlockPos pos) {
        return Math.abs(pos.getX() - center.getX())<= getProtectionRadius() &&
                Math.abs(pos.getZ() - center.getZ()) <= getProtectionRadius();
    }

    /**
     * @return the center position of the island (bedrock coordinates)
     */
    public BlockPos getCenter() {
        return center;
    }

    /**
     * @return the spawn point of the island
     */
    public BlockPos getSpawnPoint() {
        return spawnPoint;
    }
    /**
     * Set the spawn point of the island.
     *
     * @param spawnPoint the new spawn point to set
     */
    public void setSpawnPoint(BlockPos spawnPoint) {
        this.spawnPoint = spawnPoint;
    }
    /**
     * @return the unique identifier of the island
     */
    public UUID getIslandUUID() {
        return islandUUID;
    }

    public int getProtectionRadius() {
        return 50; // Sabit bir koruma yarıçapı, istenirse dinamik hale getirilebilir
    }



    private void setDefaultPermissions() {
        permissions.put(IslandAction.BREAK_BLOCK, IslandRole.MEMBER);
        permissions.put(IslandAction.PLACE_BLOCK, IslandRole.MEMBER);
        permissions.put(IslandAction.OPEN_CONTAINER, IslandRole.TRUSTED);
        permissions.put(IslandAction.INTERACT_DOOR, IslandRole.TRUSTED);
        permissions.put(IslandAction.KILL_MOB, IslandRole.COOP);
        permissions.put(IslandAction.DROP_ITEM, IslandRole.MEMBER);
        permissions.put(IslandAction.PICKUP_ITEM, IslandRole.MEMBER);
    }


}
