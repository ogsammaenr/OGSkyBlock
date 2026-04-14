package me.ogsammaenr.skyblockApi.repository;

import me.ogsammaenr.skyblockApi.model.IslandTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Şablonların (.nbt) diskten okunması için soyutlama.
 * Sadece okuma (Read-Only) işlemi yapar.
 */
public interface TemplateRepository {

    /**
     * Klasördeki tüm .nbt dosyalarını tarar, ofsetleri hesaplar ve döndürür.
     */
    @NotNull
    CompletableFuture<List<IslandTemplate>> loadAllTemplates();
}