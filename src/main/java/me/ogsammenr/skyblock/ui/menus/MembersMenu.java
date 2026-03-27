package me.ogsammenr.skyblock.ui.menus;

import me.ogsammenr.skyblock.model.MenuData;
import me.ogsammenr.skyblock.ui.BaseMenu;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class MembersMenu extends BaseMenu {

    public MembersMenu(ServerPlayerEntity player, MenuData menuData) {
        super(player, menuData);
    }

    @Override
    protected void handleAction(String action) {
        switch (action) {
            case "KICK_PLAYER":
                // Oyuncu kickleme mantığı
                player.sendMessage(Text.literal("§cOyuncu atıldı (simülasyon)."), false);
                close();
                break;
            case "PROMOTE_PLAYER":
                // Oyuncu terfi ettirme mantığı
                player.sendMessage(Text.literal("§aOyuncu terfi etti (simülasyon)."), false);
                break;
            default:
                // Bilinmeyen aksiyon
                break;
        }
    }
}
