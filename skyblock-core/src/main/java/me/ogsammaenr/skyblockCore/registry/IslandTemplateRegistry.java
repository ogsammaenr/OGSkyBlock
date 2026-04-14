// Dosya: skyblock-core/src/main/java/me/ogsammaenr/skyblockCore/registry/IslandTemplateRegistry.java
package me.ogsammaenr.skyblockCore.registry;

import me.ogsammaenr.skyblockApi.model.IslandTemplate;
import me.ogsammaenr.skyblockApi.repository.TemplateRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SADECE mantıksal ofsetleri ve API Modellerini önbellekte tutar. (Core Modülü)
 */
public class IslandTemplateRegistry {

    private final TemplateRepository repository;
    private final Map<String, IslandTemplate> templates = new ConcurrentHashMap<>();

    public IslandTemplateRegistry(TemplateRepository repository) {
        this.repository = repository;
    }

    public void reloadTemplates() {
        repository.loadLogicalTemplates().thenAccept(loadedTemplates -> {
            templates.clear();
            loadedTemplates.forEach(t -> templates.put(t.id(), t));
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    @NotNull
    public Optional<IslandTemplate> getTemplate(@NotNull String id) {
        return Optional.ofNullable(templates.get(id));
    }

    @NotNull
    public Collection<IslandTemplate> getAllTemplates() {
        return templates.values();
    }
}