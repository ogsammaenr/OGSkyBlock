package me.ogsammenr.skyblock.ui.menus;

import me.ogsammenr.skyblock.model.MenuData;
import me.ogsammenr.skyblock.ui.PaginatedMenu;
import net.minecraft.server.level.ServerPlayer;

public class TestPaginationMenu extends PaginatedMenu {

    public TestPaginationMenu(ServerPlayer player, MenuData data) {
        // Not: Parametre sırasını super() çağrısına uygun olarak (player, data) şeklinde düzelttim.
        // PaginatedMenu constructor'ı büyük ihtimalle (ServerPlayer, MenuData) bekliyor.
        super(player, data);
    }

    @Override
    public void handleCustomAction(String action) {
        // BaseMenu'deki abstract metodu birebir aynı isim ve parametre ile eziyoruz (override)
        System.out.println("[TestPaginationMenu] Custom action triggered: " + action);

        // Örnek kullanım:
        if ("TEST_ACTION".equals(action)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aTest aksiyonu çalıştı!"));
            this.close();
        }
    }
}
