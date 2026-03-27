package me.ogsammenr.skyblock.util;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.stream.Collectors;

public class PlaceholderUtil {

    /**
     * Metni PlaceholderAPI ile işler ve String olarak geri döndürür.
     */
    public static String parse(ServerPlayer player, String text) {
        if (text == null || player == null) {
            return text;
        }
        // 1. String'i Component'e çevir
        // 2. Parse et
        // 3. Çıkan Component'i tekrar String'e çevir
        Component parsedComponent = Placeholders.parseText(Component.literal(text), PlaceholderContext.of(player));
        return parsedComponent.getString();
    }

    /**
     * Lore listesini PlaceholderAPI ile işler ve String listesi olarak geri döndürür.
     */
    public static List<String> parse(ServerPlayer player, List<String> lore) {
        if (lore == null || player == null) {
            return lore;
        }
        return lore.stream()
                .map(line -> {
                    Component parsedComponent = Placeholders.parseText(Component.literal(line), PlaceholderContext.of(player));
                    return parsedComponent.getString();
                })
                .collect(Collectors.toList());
    }

    /**
     * Metni PlaceholderAPI ile işler ve Component olarak döndürür.
     * (SGUI eşyalarının ismi ve lore'u için en güvenli ve önerilen yöntem budur).
     */
    public static Component parseToComponent(ServerPlayer player, String text) {
        if (text == null || player == null) {
            return Component.literal(text != null ? text : "");
        }
        return Placeholders.parseText(Component.literal(text), PlaceholderContext.of(player));
    }

    /**
     * Lore listesini PlaceholderAPI ile işler ve Component listesi olarak döndürür.
     */
    public static List<Component> parseToComponents(ServerPlayer player, List<String> lore) {
        if (lore == null || player == null) {
            return List.of();
        }
        return lore.stream()
                .map(line -> Placeholders.parseText(Component.literal(line), PlaceholderContext.of(player)))
                .collect(Collectors.toList());
    }
}
