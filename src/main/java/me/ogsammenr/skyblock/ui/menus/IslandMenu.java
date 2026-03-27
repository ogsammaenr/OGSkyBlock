package me.ogsammenr.skyblock.ui.menus;

import me.ogsammenr.skyblock.manager.IslandManager;
import me.ogsammenr.skyblock.model.MenuData;
import me.ogsammenr.skyblock.ui.BaseMenu;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class IslandMenu extends BaseMenu {

    public IslandMenu(ServerPlayerEntity player, MenuData menuData) {
        super(player, menuData);
    }

    @Override
    protected void handleAction(String action) {
        switch (action) {
            case "TELEPORT_ISLAND":
                close();
                player.sendMessage(Text.literal("§eAdana ışınlanıyorsun..."), false);
                // IslandManager.spawnPlayerToIsland(player); // Gerçek implementasyon
                break;
            case "OPEN_MEMBERS":
                close();
                // MenuManager.openMenu(player, "members"); // Diğer menüyü aç
                break;
            default:
                // Bu menünün tanımadığı diğer aksiyonlar için bir şey yapma
                break;
        }
    }
}
