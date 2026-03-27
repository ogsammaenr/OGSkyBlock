package me.ogsammenr.skyblock.ui.menus;

import me.ogsammenr.skyblock.model.MenuData;
import me.ogsammenr.skyblock.ui.BaseMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MembersMenu extends BaseMenu {

    public MembersMenu(ServerPlayer player, MenuData menuData) {
        super(player, menuData);
    }

    @Override
    protected void handleAction(String action) {
        switch (action) {
            case "KICK_PLAYER":
                // Oyuncu kickleme mantığı
                player.sendSystemMessage(Component.literal("§cOyuncu atıldı (simülasyon)."), false);
                close();
                break;
            case "PROMOTE_PLAYER":
                // Oyuncu terfi ettirme mantığı
                player.sendSystemMessage(Component.literal("§aOyuncu terfi etti (simülasyon)."), false);
                break;
            default:
                // Bilinmeyen aksiyon
                break;
        }
    }
}
