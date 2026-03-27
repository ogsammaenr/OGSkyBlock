package me.ogsammenr.skyblock.ui.menus;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.ogsammenr.skyblock.manager.BlockValueManager;
import me.ogsammenr.skyblock.model.MenuData;
import me.ogsammenr.skyblock.ui.BaseMenu;
import me.ogsammenr.skyblock.util.ItemModifierUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class ValuesMenu extends BaseMenu {

    public ValuesMenu(ServerPlayer player, MenuData menuData) {
        super(player, menuData);
    }

    @Override
    protected void handleAction(String action) {

    }

    @Override
    protected GuiElementBuilder customizeItem(GuiElementBuilder builder, MenuData.MenuItem item) {

        // Sadece "BLOCK_VALUE" aksiyonu atanmış eşyaları filtrele
        if ("BLOCK_VALUE".equalsIgnoreCase(item.action)) {

            Identifier blockId = Identifier.tryParse(item.id);
            if (blockId != null) {
                Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(Blocks.AIR);

                if (block != Blocks.AIR) {
                    // BlockValueManager'dan bloğun adaya verdiği puanı çekiyoruz (O(1) hızında)
                    int value = BlockValueManager.getValue(block);

                    if (value > 0) {
                        // 1. İSMİ DİNAMİK OLARAK DEĞİŞTİR:
                        // JSON'da yazan ismin yanına puanını ekliyoruz. (Örn: "Elmas Blok (150 Puan)")
                        String originalName = item.name != null ? item.name.replace("&", "§") : "Blok";
                        builder.setName(Component.literal(originalName + " §8(§e" + value + " Puan§8)"));

                        // 2. LORE (AÇIKLAMA) EKLE:
                        // JSON'a yazmana gerek kalmadan doğrudan Java'dan açıklama satırı ekleyebilirsin
                        builder.addLoreLine(Component.literal(""));
                        builder.addLoreLine(Component.literal("§7Ada Seviyesi Katkısı:"));
                        builder.addLoreLine(Component.literal("§e⭐ +" + value + " Puan"));
                    }
                }
            }
        }

        return builder; // Değiştirdiğimiz Builder'ı BaseMenu'ye geri gönderiyoruz
    }
}
