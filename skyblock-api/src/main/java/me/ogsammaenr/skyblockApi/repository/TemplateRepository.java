package me.ogsammaenr.skyblockApi.repository;

import me.ogsammaenr.skyblockApi.model.IslandTemplate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TemplateRepository {
    /**
     * Sadece şablonların mantıksal verilerini (DTO) okur ve Core için döndürür.
     */
    CompletableFuture<List<IslandTemplate>> loadLogicalTemplates();
}