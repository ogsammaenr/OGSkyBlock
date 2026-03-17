package me.ogsammenr.skyblock.world;

import me.ogsammenr.skyblock.model.Island;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IslandRegistry {
    // islandUUID -> Island
    private static final Map<UUID, Island> islands = new HashMap<>();
    // playerUUID -> islandUUID
    private static final Map<UUID, UUID> playerIslands = new HashMap<>();

    private static int islandCount = 0;
    private static int currentGridX = 0;
    private static int currentGridZ = 0;
    private static final int GRID_SPACING = 300; // Adalar arasındaki mesafe

    /**
     * @param playerUUID Player UUID
     * @return Players island if exists, otherwise returns null
     */
    public static Island getIslandByPlayer(UUID playerUUID) {
        UUID islandUUID = playerIslands.get(playerUUID);
        if (islandUUID != null) {
            return islands.get(islandUUID);
        }
        return null;
    }

    /**
     * @param pos Block position to check
     * @return Island that contains the given position, or null if no island contains it
     */
    public static Island getIslandAt(BlockPos pos) {
        for(Island island : islands.values()) {
            if( island.isWithinBounds(pos)){
                return island;
            }
        }
        return null;
    }

    /**
     * @param playerUUID Player UUID
     * @return True if player has an island
     */
    public static boolean hasIsland(UUID playerUUID) {
        return playerIslands.containsKey(playerUUID);
    }

    /**
     * Get the player island if exists, otherwise create a new island for the player and return it
     * New islands are generated in a spiral pattern to optimize space and minimize travel distance between islands
     *
     * @param playerUUID Player UUID
     * @return Player's island
     */
    public static Island getOrCreateIsland(UUID playerUUID) {
        Island existingIsland = getIslandByPlayer(playerUUID);
        if (existingIsland != null) {
            return existingIsland;
        }

        int x = currentGridX * GRID_SPACING;
        int z = currentGridZ * GRID_SPACING;
        BlockPos newCenter = new BlockPos(x, 100, z);

        // sarmal düzeni için koordinatları güncelle
        if (Math.abs(currentGridX) <= Math.abs(currentGridZ) && (currentGridX != currentGridZ || currentGridX >= 0)) {
            currentGridX += (currentGridZ >= 0) ? 1 : -1;
        } else {
            currentGridZ += (currentGridX >= 0) ? 1 : -1;
        }

        UUID newIslandUUID = UUID.randomUUID();
        Island newIsland = new Island(newIslandUUID, playerUUID, newCenter);
        islands.put(newIslandUUID, newIsland);
        playerIslands.put(playerUUID, newIslandUUID);
        islandCount++;

        return newIsland;
    }

    /**
     * IslandUUID -> Island
     * @return map of islandUUID to Island
     */
    public static Map<UUID, Island> getIslands() {
        return islands;
    }

    /**
     * PlayerUUID -> IslandUUID
     * @return map of playerUUID to islandUUID
     */
    public static Map<UUID, UUID> getPlayerIslands() {
        return playerIslands;
    }

    /**
     * @return total number of islands created
     */
    public static int getIslandCount() {
        return islandCount;
    }

    public static int getCurrentGridX() {
        return currentGridX;
    }

    public static int getCurrentGridZ() {
        return currentGridZ;
    }

    /**
     * Load islands and player-island associations from saved data *
     * @param loadedIslands all islands loaded from storage
     * @param loadedPlayers all player-island associations loaded from storage
     * @param count total island count loaded from storage
     */
    public static void loadData (Map<UUID, Island> loadedIslands, Map<UUID, UUID> loadedPlayers, int count, int gridX, int gridZ) {
        islands.clear();
        if (loadedIslands != null) {
            islands.putAll(loadedIslands);
        }
        playerIslands.clear();
        if (loadedPlayers != null) {
            playerIslands.putAll(loadedPlayers);
        }
        islandCount = count;
        currentGridX = gridX;
        currentGridZ = gridZ;

    }
}
