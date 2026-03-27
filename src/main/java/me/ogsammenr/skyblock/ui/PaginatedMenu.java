package me.ogsammenr.skyblock.ui;

import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.ogsammenr.skyblock.model.MenuData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public abstract class PaginatedMenu extends BaseMenu {
    protected int currentPage = 0;

    public PaginatedMenu(ServerPlayer player, MenuData menuData) {
        super(player, menuData);
    }

    @Override
    protected void build() {
        super.build(); // BaseMenu'ye git: Arka plan camlarını koy (items null olduğu için global eşya koymayacak)
        buildPage();   // Sonra bizim sayfa eşyalarımızı camların üzerine yerleştir
    }

    protected void buildPage() {
        if (menuData.pages == null || menuData.pages.isEmpty() || currentPage >= menuData.pages.size()) {
            return;
        }

        MenuData.MenuPage pageData = menuData.pages.get(currentPage);

        if (pageData.items != null) {
            pageData.items.forEach((slotKey, item) -> {
                parseSlots(slotKey).forEach(slotIndex -> {
                    setSlot(slotIndex, createGuiElement(item));
                });
            });
        }
    }

    // BaseMenu'den gelen aksiyon işleyiciyi (Router) burada eziyoruz (Override)
    @Override
    public void handleAction(String action) {
        if ("NEXT_PAGE".equalsIgnoreCase(action)) {
            nextPage();
        } else if ("PREVIOUS_PAGE".equalsIgnoreCase(action)) {
            previousPage();
        } else {
            // Eğer aksiyon sayfa değiştirmek değilse, alt sınıflara devret
            handleCustomAction(action);
        }
    }

    public abstract void handleCustomAction (String action);

    public void nextPage() {
        if (menuData.pages != null && currentPage < menuData.pages.size() - 1) {
            currentPage++;
            refreshGui();
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            refreshGui();
        }
    }

    private void refreshGui() {
        // Sayfa değiştiğinde eski sayfanın itemlarını temizle
        // (getSize() metodu SGUI'nin GUI boyutunu döndürür, BaseMenu'de tanımlı değilse menuData.rows * 9 kullanabilirsin)
        int totalSlots = menuData.rows * 9;
        for (int i = 0; i < totalSlots; i++) {
            setSlot(i, ItemStack.EMPTY);
        }

        // Önce arka planı, sonra yeni sayfayı tekrar çiz
        super.build();
        buildPage();
    }
}
