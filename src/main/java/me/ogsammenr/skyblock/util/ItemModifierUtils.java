package me.ogsammenr.skyblock.util;

import me.ogsammenr.skyblock.manager.BlockValueManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class ItemModifierUtils {

    public static ItemStack injectBlockValueLore(ItemStack stack, String blockId) {
        Identifier identifier = Identifier.tryParse(blockId);
        if (identifier == null) return stack;

        var blockHolder = BuiltInRegistries.BLOCK.getOptional(identifier);
        if (blockHolder.isEmpty()) return stack;

        Block block = blockHolder.get();
        if (block == Blocks.AIR) return stack;

        int value = BlockValueManager.getValue(block);
        if (value <= 0) return stack;

        // 1.21 Lore Komponentini al
        ItemLore existingLore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        List<Component> newLoreLines = new ArrayList<>(existingLore.lines());

        // Dinamik lore satırlarını ekle
        newLoreLines.add(Component.literal(""));
        newLoreLines.add(Component.literal("§7Ada Seviyesi Katkısı:"));
        newLoreLines.add(Component.literal("§e⭐ +" + value + " Puan"));

        // Eşyaya yeni Lore'u set et
        stack.set(DataComponents.LORE, new ItemLore(newLoreLines));

        return stack;
    }
}
