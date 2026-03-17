package me.ogsammenr.skyblock.manager;

import me.lucko.fabric.api.permissions.v0.Permissions;
import me.ogsammenr.skyblock.model.Island;
import me.ogsammenr.skyblock.model.IslandAction;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import static me.ogsammenr.skyblock.SkyblockMain.*;

public class IslandProtection {
    /**
     * Checks if the player can perform the given action at the specified position based on island ownership and permissions.
     * @param player The player attempting the action
     * @param pos The block position where the action is being attempted
     * @param action The type of action being attempted (e.g., breaking a block, interacting with a door)
     * @return true if the player can perform the action, false otherwise
     */
    public static boolean canPerformAction(ServerPlayer player, BlockPos pos, IslandAction action) {

        boolean hasBypassPermission = switch (action) {
            case BREAK_BLOCK, PLACE_BLOCK -> Permissions.check(player, BREAK_BYPASS_PERMISSION,  4 );
            case INTERACT_DOOR, OPEN_CONTAINER -> Permissions.check(player, INTERACT_BYPASS_PERMISSION, 4);
            default -> false;
        };

        if (hasBypassPermission) {
            System.out.println("Player " + player.getName().getString() + " has bypass permission for action " + action);
            return true;
        }

        if (!player.level().dimension().equals(SKYBLOCK_WORLD_KEY)) {
            return true;
        }

        Island targetIsland = IslandRegistry.getIslandAt(pos);

        if (targetIsland == null) {
            return false;
        }
        return targetIsland.canPerformAction(player.getUUID(), action);
    }

}
