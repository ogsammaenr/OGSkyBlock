package me.ogsammaenr.skyblockCore.service;

import me.ogsammaenr.skyblockApi.math.BlockCoordinate;
import me.ogsammaenr.skyblockApi.model.Island;
import me.ogsammaenr.skyblockApi.model.IslandID;
import me.ogsammaenr.skyblockApi.repository.IslandRepository;
import me.ogsammaenr.skyblockApi.service.IslandService;
import me.ogsammaenr.skyblockCore.registry.IslandRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ada yönetiminin ana iş mantığı. Tüm modüller (komutlar, menüler) bu sınıfı kullanır.
 */
public class IslandServiceImpl implements IslandService {

    private final IslandRepository repository;
    private final IslandRegistry registry;

    // Basit Grid (Izgara) hesaplaması için geçici sayaç (Gerçek sistemde DB'den son ID okunmalı)
    private final AtomicInteger islandCounter = new AtomicInteger(0);
    private static final int GRID_SPACING = 1000; // Adalar arası 1000 blok mesafe

    public IslandServiceImpl(IslandRepository repository, IslandRegistry registry) {
        this.repository = repository;
        this.registry = registry;
    }

    @Override
    public @NotNull Optional<Island> getLoadedIsland(@NotNull IslandID id) {
        return registry.getIsland(id);
    }

    @Override
    public @NotNull Optional<Island> getLoadedIslandByPlayer(@NotNull UUID playerId) {
        return registry.getIslandByPlayer(playerId);
    }

    @Override
    public @NotNull CompletableFuture<Island> createIsland(@NotNull UUID ownerId, @NotNull String templateId) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. O(1) Grid Koordinat Hesaplama (Spiral veya Lineer Algoritma)
            int count = islandCounter.getAndIncrement();
            int x = (count % 100) * GRID_SPACING;
            int z = (count / 100) * GRID_SPACING;
            BlockCoordinate center = new BlockCoordinate(x, 64, z);

            // 2. Domain Nesnesini Oluştur
            Island newIsland = new Island(IslandID.random(), ownerId, center);

            // 3. Veritabanına (SQL) Kaydet ve Önbelleğe (RAM) Al
            repository.saveIsland(newIsland);
            registry.registerToCache(newIsland);

            // 4. TODO: Burada Custom Event fırlatılmalı (Örn: IslandCreateEvent)
            // skyblock-core içindeki WorldPasting sistemi bu eventi dinleyip adayı fiziksel olarak dünyaya yapıştıracak.

            return newIsland;
        });
    }

    @Override
    public void deleteIsland(@NotNull IslandID id) {
        registry.unregisterFromCache(id);
        repository.deleteIsland(id);
        // TODO: Dünyadaki blokları havaya (AIR) dönüştürecek Event fırlatılmalı.
    }

    @Override
    public void addMember(@NotNull IslandID id, @NotNull UUID playerId, int roleWeight) {
        registry.getIsland(id).ifPresent(island -> {
            island.addMember(playerId, roleWeight);
            // Caffeine'e adanın güncellendiğini bildirmek için (gerekirse diske yazması için) dirty işaretledik
            island.markDirty();
        });
    }

    @Override
    public void removeMember(@NotNull IslandID id, @NotNull UUID playerId) {
        registry.getIsland(id).ifPresent(island -> {
            island.removeMember(playerId);
            island.markDirty();
        });
    }

    @Override
    public void changeMemberRole(@NotNull IslandID id, @NotNull UUID playerId, int newRoleWeight) {
        registry.getIsland(id).ifPresent(island -> {
            island.changeMemberRole(playerId, newRoleWeight);
            island.markDirty();
        });
    }

    @Override
    public void loadIslandToCache(@NotNull IslandID id) {
        if (registry.getIsland(id).isPresent()) return; // Zaten yüklü

        repository.loadIsland(id).thenAccept(optionalIsland ->
                optionalIsland.ifPresent(registry::registerToCache)
        ).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    @Override
    public void unloadIslandFromCache(@NotNull IslandID id) {
        registry.unregisterFromCache(id);
    }
}