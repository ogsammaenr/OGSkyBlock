package me.ogsammaenr.skyblockApi.model;

import me.ogsammaenr.skyblockApi.math.BlockCoordinate;
import me.ogsammaenr.skyblockApi.math.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * .nbt dosyasından okunarak RAM'e alınan salt okunur ada şablonu.
 */
public record IslandTemplate(
        @NotNull String id, // Dosya adı (Örn: "classic_island")
        @NotNull String displayName,
        @NotNull String iconMaterial,
        @NotNull BlockCoordinate bedrockOffset,
        @NotNull Location spawnOffset
) {}