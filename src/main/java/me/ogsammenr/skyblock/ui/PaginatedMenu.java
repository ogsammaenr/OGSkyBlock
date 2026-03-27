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
    // Sayfa içeriğinin hangi slotlar arasında olacağını tanımla
    protected final int paginationStartSlot = 0;
    protected final int paginationEndSlot = 44; // 5. sıranın sonu

    public PaginatedMenu(ServerPlayer player, MenuData menuData) {
        super(player, menuData);
    }

    @Override
    public void open() {
        // build() içindeki item yerleşimi paginated menüler için geçerli değil.
        super.build(); // Arka planı ve sabit eşyaları çiz
        buildPage();
        super.open();
    }

    protected void buildPage() {
        if (menuData.pages == null || menuData.pages.isEmpty() || currentPage >= menuData.pages.size()) {
            addPaginationButtons(); // Butonları yine de göster (belki sadece geri butonu)
            return;
        }

        // Sayfa içeriği için ayrılan alanı temizle
        for (int i = paginationStartSlot; i <= paginationEndSlot; i++) {
            setSlot(i, null); // Önceki sayfadan kalanları temizle
        }

        MenuData.MenuPage pageData = menuData.pages.get(currentPage);
        if (pageData.items != null) {
            pageData.items.forEach((slotKey, item) -> {
                parseSlots(slotKey).forEach(slotIndex -> {
                    if (slotIndex >= paginationStartSlot && slotIndex <= paginationEndSlot) {
                        setSlot(slotIndex, createGuiElement(item));
                    }
                });
            });
        }

        addPaginationButtons();
    }

    protected void addPaginationButtons() {
        // Geri butonu
        if (currentPage > 0) {
            GuiElement backButton = new GuiElementBuilder(Items.ARROW)
                    .setName(Component.literal("§eGeri"))
                    .setCallback((index, type, action, gui) -> previousPage())
                    .build();
            setSlot(45, backButton);
        } else {
             setSlot(45, new ItemStack(Items.GRAY_STAINED_GLASS_PANE).setHoverName(Component.literal(" ")));
        }

        // İleri butonu
        if (menuData.pages != null && currentPage < menuData.pages.size() - 1) {
            GuiElement nextButton = new GuiElementBuilder(Items.ARROW)
                    .setName(Component.literal("§eİleri"))
                    .setCallback((index, type, action, gui) -> nextPage())
                    .build();
            setSlot(53, nextButton);
        } else {
             setSlot(53, new ItemStack(Items.GRAY_STAINED_GLASS_PANE).setHoverName(Component.literal(" ")));
        }
    }

    public void nextPage() {
        if (menuData.pages != null && currentPage < menuData.pages.size() - 1) {
            currentPage++;
            buildPage();
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            buildPage();
        }
    }
}
