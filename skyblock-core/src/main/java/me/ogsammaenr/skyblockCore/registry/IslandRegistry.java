package me.ogsammaenr.skyblockCore.registry;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import me.ogsammaenr.skyblockApi.model.Island;
import me.ogsammaenr.skyblockApi.model.IslandID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class IslandRegistry {

    // Caffeine Cache: Belirli bir süre inaktif kalan adaları otomatik siler (Time-To-Idle)
    private final Cache<IslandID, Island> activeIslands;

    // Hızlı Erişim İndeksi
    private final Map<UUID, IslandID> playerIslandIndex = new ConcurrentHashMap<>();

    public IslandRegistry() {
        this.activeIslands = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES) // Adaya 30 dk kimse girmezse RAM'den uçur
                .maximumSize(5000) // Maksimum yüklü ada sayısı (OOM koruması)
                .removalListener((IslandID key, Island island, RemovalCause cause) -> {
                    if (island != null) {
                        handleIslandEviction(island, cause);
                    }
                })
                .build();
    }

    private void handleIslandEviction(Island island, RemovalCause cause) {
        // Not: Eğer ada "dirty" ise, RAM'den silinmeden hemen önce diske yazılması için
        // Repository'nin save metoduna veya bir Event'e gönderilmesi gerekir.
        if (island.isDirty()) {
            System.out.println("[OGSkyBlock] Ada RAM'den siliniyor, diske yazılmalı: " + island.getIslandID());
            // TODO: SqliteIslandRepository.saveIsland(island) tetiklenmeli
        }

        // Bellek sızıntısını önlemek için hızlı indeksten de temizliyoruz
        playerIslandIndex.remove(island.getOwnerId());
    }

    public void registerToCache(@NotNull Island island) {
        activeIslands.put(island.getIslandID(), island);
        playerIslandIndex.put(island.getOwnerId(), island.getIslandID());
    }

    public void unregisterFromCache(@NotNull IslandID id) {
        activeIslands.invalidate(id); // Bu işlem otomatik olarak removalListener'ı tetikler
    }

    @NotNull
    public Optional<Island> getIsland(@NotNull IslandID id) {
        return Optional.ofNullable(activeIslands.getIfPresent(id));
    }

    @NotNull
    public Optional<Island> getIslandByPlayer(@NotNull UUID playerId) {
        IslandID islandId = playerIslandIndex.get(playerId);
        if (islandId != null) {
            return getIsland(islandId);
        }

        return activeIslands.asMap().values().stream()
                .filter(island -> island.getPlayerRoleWeight(playerId) > 0)
                .findFirst();
    }

    @NotNull
    public Collection<Island> getAllCachedIslands() {
        return activeIslands.asMap().values();
    }

    @NotNull
    public Collection<Island> getDirtyIslands() {
        return activeIslands.asMap().values().stream()
                .filter(Island::isDirty)
                .collect(Collectors.toList());
    }
}