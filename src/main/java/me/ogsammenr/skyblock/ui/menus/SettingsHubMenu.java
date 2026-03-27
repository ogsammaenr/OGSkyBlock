package me.ogsammenr.skyblock.ui.menus;

import me.ogsammenr.skyblock.manager.MenuManager;
import me.ogsammenr.skyblock.model.MenuData;
import me.ogsammenr.skyblock.ui.BaseMenu;
import net.minecraft.server.level.ServerPlayer;

public class SettingsHubMenu extends BaseMenu {

    public SettingsHubMenu(ServerPlayer player, MenuData menuData) {
        super(player, menuData);
    }

    @Override
    protected void handleAction(String action) {

        // Genel Ayarlar ikonuna tıklandıysa
        if ("OPEN_GENERAL_SETTINGS".equalsIgnoreCase(action)) {

            // Yeni menüyü aç (SGUI eski menüyü otomatik olarak kapatıp yenisine geçiş yapar)
            MenuManager.openMenu(player, "island_settings_general_menu");

        }
        // Koruma ve İzinler ikonuna tıklandıysa
        else if ("OPEN_ACTION_SETTINGS".equalsIgnoreCase(action)) {

            MenuManager.openMenu(player, "island_settings_action_menu");

        }
    }
}