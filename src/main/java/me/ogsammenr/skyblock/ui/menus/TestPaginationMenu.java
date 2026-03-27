package me.ogsammenr.skyblock.ui.menus;

import me.ogsammenr.skyblock.model.MenuData;
import me.ogsammenr.skyblock.ui.PaginatedMenu;
import net.minecraft.server.level.ServerPlayer;

public class TestPaginationMenu extends PaginatedMenu {

    public TestPaginationMenu(MenuData data, ServerPlayer player) {
        super(data, player);
    }

    @Override
    public void handleCustomAction(String action, MenuData.MenuItem item) {
        // For now, we can just log the action to the console for debugging.
        System.out.println("[TestPaginationMenu] Custom action triggered: " + action);
    }
}
