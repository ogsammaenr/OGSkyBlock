package me.ogsammenr.skyblock.manager;

import com.google.gson.Gson;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.ogsammenr.skyblock.model.MenuData;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.ogsammenr.skyblock.SkyblockMain.LOGGER;

public class MenuManager {
    private static final Gson gson = new Gson();
    private static final Map<String, MenuData> MENU_CACHE = new HashMap<>();

    public static void loadAllMenus(File menuFolder) {
        MENU_CACHE.clear();
        if (!menuFolder.exists()) {
            menuFolder.mkdirs();
            System.out.println("Menü klasörü oluşturuldu: " + menuFolder.getAbsolutePath());
            return;
        }

        File[] menuFiles = menuFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (menuFiles != null) {
            for (File file : menuFiles) {
                try (FileReader reader = new FileReader(file)) {
                    MenuData data = gson.fromJson(reader, MenuData.class);
                    String menuName = file.getName().replace(".json", "");
                    MENU_CACHE.put(menuName, data);
                    System.out.println("Menü yüklendi: \n" + data.toString());
                } catch (Exception e) {
                    System.err.println("Menü yüklenirken hata oluştu: " + file.getName() + "\n" + e.getMessage());
                }
            }
            System.out.println("===== Toplam " + MENU_CACHE.size() + " menü yüklendi =====");
        }
    }

    public static void openMenu(ServerPlayer player, String menuName) {
        System.out.println("[DEBUG] /is menu komutu tetiklendi. Hedef menü: " + menuName);
        try {
            MenuData data = MENU_CACHE.get(menuName);
            if (data == null) {
                System.out.println("[DEBUG] HATA: " + menuName + " isimli menü RAM'de bulunamadı!");
                player.sendSystemMessage(Component.literal("§cHata: '" + menuName + "' isimli menü bulunamadı!"));
                return;
            }
            System.out.println("[DEBUG] Menü bulundu. Satır sayısı: " + data.rows);

            // 1. ZIRH: Satır (Row) Koruması (Eksikse veya 0 ise çökmek yerine 3 satır yap)
            int safeRows = (data.rows >= 1 && data.rows <= 6) ? data.rows : 3;
            MenuType<?> menuType = getMenuType(safeRows);
            SimpleGui gui = new SimpleGui(menuType, player, false);

            // 2. ZIRH: Başlık Koruması (JSON'da başlık unutulursa çökmek yerine "Menü" yaz)
            String safeTitle = data.title != null ? data.title : "Menü";
            gui.setTitle(Component.literal(safeTitle.replace("&", "§")));

            PlaceholderContext context = PlaceholderContext.of(player);

            if (data.items != null) {
                for (Map.Entry<String, MenuData.MenuItem> entry : data.items.entrySet()) {
                    String slotKey = entry.getKey();
                    MenuData.MenuItem itemData = entry.getValue();

                    // 3. ZIRH: Eşya ID Koruması (ID yazılmazsa veya uyuşmazsa bariyer koy)
                    String itemId = itemData.id != null ? itemData.id : "minecraft:barrier";
                    Identifier resourceLocation = Identifier.tryParse(itemId);
                    Item mcItem = Items.BARRIER;

                    if (resourceLocation != null) {
                        mcItem = BuiltInRegistries.ITEM.getOptional(resourceLocation).orElse(Items.BARRIER);
                    }
                    if (mcItem == Items.AIR) {
                        mcItem = Items.BARRIER;
                    }

                    GuiElementBuilder builder = new GuiElementBuilder(mcItem);

                    // 4. ZIRH: Miktar (Amount) Koruması
                    int amount = (itemData.amount > 0) ? itemData.amount : 1;
                    builder.setCount(amount);


                    // İsim
                    if (itemData.name != null) {
                        Component coloredName = Component.literal(itemData.name.replace("&", "§"));
                        if (itemData.hasPlaceholder) {
                            builder.setName(Placeholders.parseText(coloredName, context));
                        } else {
                            builder.setName(coloredName);
                        }
                    }

                    // Açıklama (Lore)
                    if (itemData.lore != null) {
                        for (String line : itemData.lore) {
                            Component coloredLine = Component.literal(line.replace("&", "§"));
                            if (itemData.hasPlaceholder) {
                                builder.addLoreLine(Placeholders.parseText(coloredLine, context));
                            } else {
                                builder.addLoreLine(coloredLine);
                            }
                        }
                    }

                    if ("BLOCK_VALUE".equalsIgnoreCase(itemData.action)) {
                        try {
                            // itemId'yi bloğa çeviriyoruz (Örn: "minecraft:diamond_block")
                            Identifier blockId = Identifier.tryParse(itemId);
                            if (blockId != null) {
                                var blockHolder = BuiltInRegistries.BLOCK.getOptional(blockId);
                                if (blockHolder.isPresent() && blockHolder.get() != Blocks.AIR) {

                                    // BlockValueManager'dan değeri çek (O(1) hızında)
                                    int blockValue = me.ogsammenr.skyblock.manager.BlockValueManager.getValue(blockHolder.get());

                                    // Sadece değeri 0'dan büyük olanları yazdır
                                    if (blockValue > 0) {
                                        builder.addLoreLine(Component.literal("")); // Boşluk
                                        builder.addLoreLine(Component.literal("§7Ada Seviyesi Katkısı:"));
                                        builder.addLoreLine(Component.literal("§e⭐ +" + blockValue + " Puan"));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("[DEBUG] Lore enjekte edilirken hata (Göz ardı edildi): " + e.getMessage());
                        }
                    }

                    // Tıklama Olayı (Action)
                    if (itemData.action != null && !itemData.action.isEmpty()) {
                        builder.setCallback((index, clickType, actionType, guiCallBack) -> {
                            handleAction(player, itemData.action, guiCallBack);
                        });
                    }

                    // Eşyayı Slota Yerleştir
                    List<Integer> targetSlots = parseSlots(slotKey);
                    for (int slot : targetSlots) {
                        if (slot >= 0 && slot < safeRows * 9) {
                            gui.setSlot(slot, builder);
                        }
                    }
                }
            }

            gui.open();

        } catch (Exception e) {
            // 5. ZIRH: Son çare! Eğer kod yine de çökerse, oyuncuya hata atmaz.
            // Sadece konsola kırmızı hatayı yazar ve sunucunun çalışmasına devam etmesini sağlar.
            player.sendSystemMessage(Component.literal("§cMenü açılırken sistemsel bir hata oluştu! Lütfen kurucuya bildirin."));
            System.err.println("[Skyblock] Menü çizilirken hata oluştu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== Helper Methods =====

    /**
     * Parses a slot key which can be a single number or a range and returns a list of slot indices.
     *
     * @param slotKey the slot key to parse (e.g. "0" or "0-8")
     * @return a list of integer slot indices
     */
    private static List<Integer> parseSlots(String slotKey) {
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
            System.err.println("Geçersiz slot tanımı: " + slotKey);
        }
        return slots;
    }

    /**
     * Handles the action associated with a menu item when clicked.
     * @param player the player who clicked the item
     * @param action the action string defined in the menu JSON (e.g. "TELEPORT_ISLAND")
     * @param gui the GUI instance to allow for closing or updating the menu if needed
     */
    private static void handleAction(ServerPlayer player, String action, SlotGuiInterface gui) {
        switch (action) {
            case "TELEPORT_ISLAND" :
                gui.close();
                player.sendSystemMessage(Component.literal("§eAdana ışınlanıyorsun..."));
                IslandManager.spawnPlayerToIsland(player);
                break;
            case "OPEN_MEMBERS" :
                gui.close();
                player.sendSystemMessage(Component.literal("§eAda üyeleri menüsü açılıyor..."));
                // TODO: Ada üyeleri menüsü açma işlemi
                break;
            case "BLOCK_VALUE" :
                player.sendSystemMessage(Component.literal("§aBlock seviyesi"));
                break;

            default:
                break;

        }
    }

    private static MenuType<?> getMenuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> throw new IllegalArgumentException("Geçersiz satır sayısı: " + rows);
        };
    }
}
