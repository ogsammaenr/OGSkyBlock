package me.ogsammenr.skyblock.ui;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.ogsammenr.skyblock.model.MenuData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class PaginatedMenu {

    protected final MenuData data;
    protected final ServerPlayer player;
    protected final SimpleGui gui;
    protected int currentPage = 0;

    public PaginatedMenu(MenuData data, ServerPlayer player) {
        this.data = data;
        this.player = player;

        int safeRows = (data.rows >= 1 && data.rows <= 6) ? data.rows : 3;
        MenuType<?> menuType = getMenuType(safeRows);
        this.gui = new SimpleGui(menuType, player, false);

        String safeTitle = data.title != null ? data.title : "Paginated Menu";
        gui.setTitle(Component.literal(safeTitle.replace("&", "§")));
    }

    public void open() {
        renderPage();
        gui.open();
    }

    public void renderPage() {
        gui.clear(); // Clear all slots before rendering

        // Render global items that are present on all pages
        if (data.items != null) {
            renderItems(data.items);
        }

        // Render items for the current page
        if (data.pages != null && currentPage >= 0 && currentPage < data.pages.size()) {
            MenuData.MenuPage page = data.pages.get(currentPage);
            if (page.items != null) {
                renderItems(page.items);
            }
        }
    }

    private void renderItems(Map<String, MenuData.MenuItem> items) {
        PlaceholderContext context = PlaceholderContext.of(player);

        for (Map.Entry<String, MenuData.MenuItem> entry : items.entrySet()) {
            String slotKey = entry.getKey();
            MenuData.MenuItem itemData = entry.getValue();

            String itemId = itemData.id != null ? itemData.id : "minecraft:barrier";
            ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
            Item mcItem = Items.BARRIER;

            if (resourceLocation != null) {
                mcItem = BuiltInRegistries.ITEM.getOptional(resourceLocation).orElse(Items.BARRIER);
            }
            if (mcItem == Items.AIR) {
                mcItem = Items.BARRIER;
            }

            GuiElementBuilder builder = new GuiElementBuilder(mcItem);

            int amount = (itemData.amount > 0) ? itemData.amount : 1;
            builder.setCount(amount);

            if (itemData.name != null) {
                Component coloredName = Component.literal(itemData.name.replace("&", "§"));
                builder.setName(itemData.hasPlaceholder ? Placeholders.parseText(coloredName, context) : coloredName);
            }

            if (itemData.lore != null) {
                for (String line : itemData.lore) {
                    Component coloredLine = Component.literal(line.replace("&", "§"));
                    builder.addLoreLine(itemData.hasPlaceholder ? Placeholders.parseText(coloredLine, context) : coloredLine);
                }
            }

            builder.setCallback((index, clickType, actionType) -> {
                handleAction(itemData);
            });

            List<Integer> targetSlots = parseSlots(slotKey);
            for (int slot : targetSlots) {
                if (slot >= 0 && slot < gui.getSize()) {
                    gui.setSlot(slot, builder);
                }
            }
        }
    }

    private void handleAction(MenuData.MenuItem item) {
        if (item.action == null) return;

        switch (item.action) {
            case "next_page":
                if (currentPage < data.pages.size() - 1) {
                    currentPage++;
                    renderPage();
                }
                break;
            case "prev_page":
                if (currentPage > 0) {
                    currentPage--;
                    renderPage();
                }
                break;
            default:
                handleCustomAction(item.action, item);
                break;
        }
    }

    private List<Integer> parseSlots(String slotKey) {
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
            // Log error for invalid slot format
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
            case 6 -> MenuType.GENERIC_9x6;
            default -> MenuType.GENERIC_9x3; // Default fallback
        };
    }

    public abstract void handleCustomAction(String action, MenuData.MenuItem item);
}
