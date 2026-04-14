package me.ogsammaenr.skyblockCore.registry;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import me.ogsammaenr.skyblockApi.model.Island;
import me.ogsammaenr.skyblockApi.model.IslandID;
import me.ogsammaenr.skyblockApi.repository.IslandRepository;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class IslandRegistry {

    private final Cache<IslandID, Island> islandCache;
    private final Map<UUID, IslandID> playerIslandIndex = new ConcurrentHashMap<>();
    private final IslandRepository repository;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public IslandRegistry(@NotNull IslandRepository repository) {
        this.repository = repository;

        this.islandCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES) // 30 dk işlem görmeyen adayı RAM'den at
                .maximumSize(1000)
                .removalListener(this::handleIslandEviction)
                .build();

        // Write-Behind: Periyodik toplu kayıt
        startWriteBehindTask();
    }

    private void startWriteBehindTask() {
        scheduler.scheduleAtFixedRate(() -> {
            List<Island> dirtyIslands = getDirtyIslands();

            if (!dirtyIslands.isEmpty()) {
                // Batch save işlemi
                repository.saveAll(dirtyIslands);

                // Island modeline setDirty(boolean flag) veya clearDirty() adında bir metod eklemelisin.
                // markDirty() genelde true yapar, yazıldıktan sonra false yapmamız gerek.
                dirtyIslands.forEach(Island::clearDirty);
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    private void handleIslandEviction(IslandID id, Island island, RemovalCause cause) {
        if (island == null) return;

        // Cache'den atılma sebebi ne olursa olsun eğer veri kirliyse diske yaz.
        if (island.isDirty()) {
            // Sadece saveAll tanımlıysa, singleton list kullanarak tekli kaydı batch yapısına uyarlarız.
            repository.saveAll(Collections.singletonList(island));
        }

        // Bellek sızıntısını önlemek için hızlı indeksten de temizliyoruz
        playerIslandIndex.remove(island.getOwnerId());
    }

    public void registerToCache(@NotNull Island island) {
        islandCache.put(island.getIslandID(), island);
        playerIslandIndex.put(island.getOwnerId(), island.getIslandID());
    }

    public void unregisterFromCache(@NotNull IslandID id) {
        islandCache.invalidate(id); // Bu işlem otomatik olarak removalListener'ı (handleIslandEviction) tetikler
    }

    @NotNull
    public Optional<Island> getIsland(@NotNull IslandID id) {
        return Optional.ofNullable(islandCache.getIfPresent(id));
    }

    @NotNull
    public Optional<Island> getIslandByPlayer(@NotNull UUID playerId) {
        IslandID islandId = playerIslandIndex.get(playerId);

        // Endekste varsa adayı getir (Bu işlem ada süresini SIFIRLAR), yoksa boş dön.
        return islandId != null ? getIsland(islandId) : Optional.empty();
    }

    @NotNull
    public List<Island> getDirtyIslands() {
        return islandCache.asMap().values().stream()
                .filter(Island::isDirty)
                .collect(Collectors.toList());
    }

    @NotNull
    public Collection<Island> getAllCachedIslands() {
        return islandCache.asMap().values();
    }
}