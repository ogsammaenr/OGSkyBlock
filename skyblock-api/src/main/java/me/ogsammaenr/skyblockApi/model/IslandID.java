package me.ogsammaenr.skyblockApi.model;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Ada kimliğini temsil eden tip-güvenli sarmalayıcı.
 */
public record IslandID(@NotNull UUID value) {

    public static IslandID random() {
        return new IslandID(UUID.randomUUID());
    }

    public static IslandID fromString(String suite) {
        return new IslandID(UUID.fromString(suite));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}