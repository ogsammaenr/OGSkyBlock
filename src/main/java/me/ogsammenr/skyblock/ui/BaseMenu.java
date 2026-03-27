package me.ogsammenr.skyblock.ui;

import eu.pb4.sgui.api.gui.SimpleGui;
import me.ogsammenr.skyblock.model.MenuData;
import me.ogsammenr.skyblock.util.PlaceholderUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Objects;

public abstract class BaseMenu extends SimpleGui {
    protected final MenuData menuData;
    protected final ServerPlayerEntity player;

    public BaseMenu(ServerPlayerEntity player, MenuData menuData) {
        super(getScreenHandlerType(menuData.rows), player, false);
        this.player = player;
        this.menuData = menuData;
        this.setTitle(Text.of(PlaceholderUtil.parse(player, menuData.title)));
    }

    public void open() {
        build();
        super.open();
    }

    protected void build() {
        if (menuData.items == null) return;

        for (int i = 0; i < this.getSize(); i++) {
            this.setSlot(i, new ItemStack(Items.GRAY_STAINED_GLASS_PANE).setCustomName(Text.of(" ")));
        }

        menuData.items.forEach((slot, item) -> {
            int slotIndex = Integer.parseInt(slot);
            this.setSlot(slotIndex, createMenuItem(item));
        });
    }

    protected ItemStack createMenuItem(MenuData.MenuItem item) {
        ItemStack stack = new ItemStack(Items.BARRIER); // Varsayılan olarak bariyer
        // Gerçek eşya oluşturma mantığı (ID'den Item bulma, NBT, vb.) buraya eklenecek.
        // Şimdilik basit tutuyoruz.

        stack.setCustomName(Text.of(PlaceholderUtil.parse(player, item.name)));
        // Lore oluşturma mantığı buraya eklenecek.

        return stack;
    }

    protected void onMenuItemClick(MenuData.MenuItem item) {
        if (item.action != null && !item.action.isEmpty()) {
            handleAction(item.action);
        }
    }

    protected abstract void handleAction(String action);

    private static ScreenHandlerType<?> getScreenHandlerType(int rows) {
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };
    }
}
