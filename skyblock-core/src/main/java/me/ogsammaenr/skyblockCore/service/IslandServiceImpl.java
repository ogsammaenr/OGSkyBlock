package me.ogsammaenr.skyblockCore.service;

import me.ogsammaenr.skyblockApi.math.BlockCoordinate;
import me.ogsammaenr.skyblockApi.math.Location;
import me.ogsammaenr.skyblockApi.model.Island;
import me.ogsammaenr.skyblockApi.model.IslandID;
import me.ogsammaenr.skyblockApi.model.IslandTemplate;
import me.ogsammaenr.skyblockApi.repository.IslandRepository;
import me.ogsammaenr.skyblockApi.service.IslandService;
import me.ogsammaenr.skyblockCore.math.IslandGridCalculator;
import me.ogsammaenr.skyblockCore.registry.IslandRegistry;
import me.ogsammaenr.skyblockCore.registry.IslandTemplateRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ada yönetiminin ana iş mantığı. Tüm modüller (komutlar, menüler) bu sınıfı kullanır.
 */
public class IslandServiceImpl implements IslandService {

    private final IslandRegistry islandRegistry;
    private final IslandTemplateRegistry templateRegistry;
    private final IslandRepository repository; // EKSİKTİ: Veritabanı işlemleri için eklendi

    // Veritabanındaki toplam ada sayısını startup'ta çekip buna eşitlemelisin
    private final AtomicInteger totalIslandCount = new AtomicInteger(0);

    public IslandServiceImpl(IslandRegistry islandRegistry, IslandTemplateRegistry templateRegistry, IslandRepository repository) {
        this.islandRegistry = islandRegistry;
        this.templateRegistry = templateRegistry;
        this.repository = repository;
    }

    @Override
    public @NotNull Optional<Island> getLoadedIsland(@NotNull IslandID id) {
        return islandRegistry.getIsland(id);
    }

    @Override
    public @NotNull Optional<Island> getLoadedIslandByPlayer(@NotNull UUID playerId) {
        return islandRegistry.getIslandByPlayer(playerId);
    }

    @Override
    @NotNull
    public CompletableFuture<Island> createIsland(@NotNull UUID ownerId, @NotNull String templateId) {
        if (islandRegistry.getIslandByPlayer(ownerId).isPresent()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Oyuncunun zaten bir adası var."));
        }

        IslandTemplate template = templateRegistry.getTemplate(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Böyle bir şablon bulunamadı: " + templateId));

        return CompletableFuture.supplyAsync(() -> {
            int nextIndex = totalIslandCount.getAndIncrement();
            BlockCoordinate centerCoords = IslandGridCalculator.calculateNextLocation(nextIndex);

            Island newIsland = new Island(
                    new IslandID(UUID.randomUUID()),
                    ownerId,
                    centerCoords
            );

            Location offset = template.spawnOffset();
            Location actualSpawn = Location.at(
                    offset.x() + centerCoords.x(),
                    offset.y() + centerCoords.y(),
                    offset.z() + centerCoords.z()
                    // Eğer Location class'ında yaw/pitch varsa buraya eklemelisin
            );
            newIsland.setSpawnPoint(actualSpawn);
            newIsland.markDirty();

            islandRegistry.registerToCache(newIsland);

            // EVENT FIRLAT: Örn: EventManager.call(new IslandCreateEvent(newIsland, template));

            return newIsland;
        });
    }

    @Override
    public void deleteIsland(@NotNull IslandID id) {
        islandRegistry.unregisterFromCache(id);

        // Veritabanından asenkron olarak sil (Repository'de delete(IslandID) metodu olmalı)
        CompletableFuture.runAsync(() -> repository.deleteIsland(id));

        // TODO: Dünyadaki blokları havaya (AIR) dönüştürecek Event fırlatılmalı.
    }

    @Override
    public void addMember(@NotNull IslandID id, @NotNull UUID playerId, int roleWeight) {
        islandRegistry.getIsland(id).ifPresent(island -> {
            island.addMember(playerId, roleWeight); // Island modeline göre düzeltildi
            island.markDirty();
        });
    }

    @Override
    public void removeMember(@NotNull IslandID id, @NotNull UUID playerId) {
        islandRegistry.getIsland(id).ifPresent(island -> {
            island.removeMember(playerId);
            island.markDirty();
        });
    }

    @Override
    public void changeMemberRole(@NotNull IslandID id, @NotNull UUID playerId, int newRoleWeight) {
        islandRegistry.getIsland(id).ifPresent(island -> {
            if (island.isMember(playerId)) {
                island.changeMemberRole(playerId, newRoleWeight);
                island.markDirty();
            }
        });
    }

    @Override
    public void loadIslandToCache(@NotNull IslandID id) {
        if (islandRegistry.getIsland(id).isPresent()) return;

        // DÜZELTME: Exception tip uyuşmazlığı giderildi
        repository.loadIsland(id).thenAccept(optionalIsland -> {
            if (optionalIsland != null && optionalIsland.isPresent()) {
                islandRegistry.registerToCache(optionalIsland.get());
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    @Override
    public void unloadIslandFromCache(@NotNull IslandID id) {
        islandRegistry.unregisterFromCache(id);
    }
}