package me.ogsammenr.skyblock.ui;

import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.ogsammenr.skyblock.model.MenuData;
import me.ogsammenr.skyblock.util.PlaceholderUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore; // Required for DataComponents.LORE

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class BaseMenu extends SimpleGui {
    protected final MenuData menuData;
    protected final ServerPlayer player;

    public BaseMenu(ServerPlayer player, MenuData menuData) {
        super(getMenuType(menuData.rows > 0 ? menuData.rows : 3), player, false);
        this.player = player;
        this.menuData = menuData;
        this.setTitle(PlaceholderUtil.parseToComponent(player, menuData.title != null ? menuData.title : "Menu"));
    }

    @Override
    public boolean open() { // Corrected return type
        build();
        return super.open(); // Return super.open()
    }

    protected void build() {
        ItemStack background = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        background.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 0; i < this.getSize(); i++) { // Corrected method call
            this.setSlot(i, background);
        }

        if (menuData.items == null) return;

        menuData.items.forEach((slotKey, item) -> {
            GuiElement guiElement = createGuiElement(item);
            parseSlots(slotKey).forEach(slotIndex -> {
                if (slotIndex >= 0 && slotIndex < this.getSize()) { // Corrected method call
                    this.setSlot(slotIndex, guiElement);
                }
            });
        });
    }

    protected GuiElement createGuiElement(MenuData.MenuItem item) {
        // 1. Güvenli Item oluşturma (Aynı bıraktık)
        Item mcItem = BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(item.id))
                .orElse(Items.BARRIER);
        if (mcItem == Items.AIR) {
            mcItem = Items.BARRIER;
        }

        // 2. SGUI GuiElementBuilder'ı başlatıyoruz (Manuel ItemStack yerine)
        GuiElementBuilder builder = new GuiElementBuilder(mcItem)
                .setCount(item.amount > 0 ? item.amount : 1);

        // 3. İsim ve Lore için PlaceholderUtil kullanarak Builder'a ekleme yapıyoruz
        if (item.name != null && !item.name.isEmpty()) {
            builder.setName(PlaceholderUtil.parseToComponent(player, item.name));
        }

        if (item.lore != null && !item.lore.isEmpty()) {
            // SGUI Builder, lore satırlarını tek tek eklememizi sağlar.
            // DataComponents.LORE karmaşasından bizi kurtarır.
            for (Component loreLine : PlaceholderUtil.parseToComponents(player, item.lore)) {
                builder.addLoreLine(loreLine);
            }
        }

        // 4. Tıklama olayını (Callback) Builder'a ekliyoruz
        builder.setCallback((index, type, action, gui) -> onMenuItemClick(item));

        // 5. --- KANCA (HOOK) BURADA DEVREYE GİRİYOR ---
        // Eğer alt sınıflar (Örn: ValuesMenu) eşyaya özel bir isim/lore eklemek isterse,
        // builder nesnesini onlara gönderiyoruz. Onlar değiştirip geri yolluyor.
        builder = customizeItem(builder, item);
        // ----------------------------------------------

        // 6. En son Builder'ı derleyip (build) GuiElement olarak döndürüyoruz.
        return builder.build();
    }

    /**
     * Alt sınıfların eşya oluşturulmadan hemen önce müdahale edebilmesi için kanca metodu.
     * Varsayılan olarak builder üzerinde hiçbir değişiklik yapmaz.
     */
    protected GuiElementBuilder customizeItem(GuiElementBuilder builder, MenuData.MenuItem item) {
        return builder;
    }

    protected void onMenuItemClick(MenuData.MenuItem item) {
        if (item.action != null && !item.action.isEmpty()) {
            handleAction(item.action);
        }
    }

    protected abstract void handleAction(String action);

    protected List<Integer> parseSlots(String slotKey) {
        List<Integer> slots = new ArrayList<>();
        try {
            if (slotKey.contains("-")) {
                String[] parts = slotKey.split("-");
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());
                for (int i = start; i <= end; i++) {
                    slots.add(i);
                }
            } else {
                slots.add(Integer.parseInt(slotKey.trim()));
            }
        } catch (NumberFormatException e) {
            // Log error
        }
        return slots;
    }

    private static MenuType<?> getMenuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }
}
