package me.ogsammaenr.skyblockApi.repository;
import me.ogsammaenr.skyblockApi.model.Island;
import me.ogsammaenr.skyblockApi.model.IslandID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Veritabanı işlemleri (JSON/SQLite/SQL) için soyutlama katmanı.
 * Hiçbir metot Main Thread'i kilitlememesi için asenkron (CompletableFuture) çalışır.
 */
public interface IslandRepository {

    // --- TEMEL CRUD İŞLEMLERİ ---

    /**
     * Veritabanından adayı okur. I/O işlemi olduğu için asenkrondur.
     */
    @NotNull
    CompletableFuture<Optional<Island>> loadIsland(@NotNull IslandID id);

    /**
     * Adanın mevcut durumunu diske yazar. Bu işlem "Fire-and-Forget"
     * mantığıyla arka planda çalışacağı için void dönmesi yeterlidir.
     */
    void saveIsland(@NotNull Island island);

    /**
     * Adanın verilerini diskten siler.
     */
    void deleteIsland(@NotNull IslandID id);

    // --- GELİŞMİŞ SORGULAR ---

    /**
     * Kurucu UUID'sine göre adanın ID'sini bulur.
     */
    @NotNull
    CompletableFuture<Optional<IslandID>> getIslandIdByOwner(@NotNull UUID ownerId);

    /**
     * Bir oyuncunun kurucu veya üye olduğu TÜM adaları getirir.
     * (Örn: /is profile menüsü için)
     */
    @NotNull
    CompletableFuture<List<Island>> getIslandsByMember(@NotNull UUID playerId);

       /**
     * Veritabanındaki tüm ada kimliklerini çeker.
     * Sunucu açılışında Grid sınırlarını hesaplamak için kullanılır.
     */
    @NotNull
    CompletableFuture<List<IslandID>> getAllIslandIds();

    /**
     * Toplu kayıt (Batch Save). Sunucu kapanırken veya periyodik Auto-Save
     * sisteminde I/O darboğazını önlemek için kullanılır.
     */
    void saveAll(@NotNull Collection<Island> islands);
}