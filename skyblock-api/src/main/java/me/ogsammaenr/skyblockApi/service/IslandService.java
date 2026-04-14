package me.ogsammaenr.skyblockApi.service;

import me.ogsammaenr.skyblockApi.model.Island;
import me.ogsammaenr.skyblockApi.model.IslandID;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Ada yönetiminin merkezi iş mantığı (Business Logic) sözleşmesi.
 * Diğer modüllerin (presentation, protection) ada sistemine açılan tek kapısıdır.
 */
public interface IslandService {

    // --- RAM (CACHE) ERİŞİM KONTROLLERİ ---

    /**
     * Adanın bellekte (Cache) olup olmadığını kontrol eder ve varsa döndürür.
     * TPS dostudur, O(1) zaman karmaşıklığıyla çalışır.
     */
    @NotNull
    Optional<Island> getLoadedIsland(@NotNull IslandID id);

    @NotNull
    Optional<Island> getLoadedIslandByPlayer(@NotNull UUID playerId);

    // --- TEMEL İŞ MANTIKLARI (DOMAIN LOGIC) ---

    /**
     * Grid algoritmasını tetikleyerek dünyada fiziksel olarak yeni bir ada inşa eder.
     * Blok kopyalama (Pasting) işlemleri içerdiğinden asla Main Thread'i kilitlemez, Future döner.
     */
    @NotNull
    CompletableFuture<Island> createIsland(@NotNull UUID ownerId, @NotNull String templateId);

    void deleteIsland(@NotNull IslandID id);

    // --- ÜYE VE YETKİ YÖNETİMİ ---

    /**
     * Adaya yeni bir üye ekler. Bu işlem sonrası Island nesnesi 'dirty' olarak
     * işaretlenir ve asenkron Write-Behind döngüsü (Auto-Save) ile veritabanına yansıtılır.
     * İşlem başarılı olursa 'IslandMemberAddEvent' custom eventi fırlatılmalıdır.
     */
    void addMember(@NotNull IslandID id, @NotNull UUID playerId, int roleWeight);

    void removeMember(@NotNull IslandID id, @NotNull UUID playerId);

    void changeMemberRole(@NotNull IslandID id, @NotNull UUID playerId, int newRoleWeight);

    // --- YAŞAM DÖNGÜSÜ YÖNETİMİ ---

    /**
     * Oyuncu sunucuya girdiğinde veya ada sınırlarına yaklaştığında
     * veritabanından diski okuyarak adayı önbelleğe (Cache) alır.
     */
    void loadIslandToCache(@NotNull IslandID id);

    /**
     * Adadaki tüm oyuncular çıktığında veya belirli bir süre inaktif kalındığında
     * RAM'i rahatlatmak için adayı önbellekten siler (Silmeden önce diske son halini yazar).
     */
    void unloadIslandFromCache(@NotNull IslandID id);
}