package me.ogsammaenr.skyblockCore.registry;

import me.ogsammaenr.skyblockApi.model.IslandTemplate;
import me.ogsammaenr.skyblockApi.repository.TemplateRepository;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Ada şablonlarının hem mantıksal ofsetlerini (API Modeli) 
 * hem de fiziksel blok verilerini (StructureTemplate) RAM üzerinde tutan Registry.
 */
public class TemplateRegistry {

    private final TemplateRepository repository;
    private final Path templatesDir;

    // API için ofset ve metadata önbelleği (Örn: /is menüsünde göstermek için)
    private final Map<String, IslandTemplate> templates = new ConcurrentHashMap<>();

    // Fiziksel yapıştırma (WorldPastingEngine) işlemi için blok verisi önbelleği
    private final Map<String, StructureTemplate> structureCache = new ConcurrentHashMap<>();

    public TemplateRegistry(TemplateRepository repository) {
        this.repository = repository;
        this.templatesDir = FabricLoader.getInstance().getConfigDir().resolve("OGSkyBlock").resolve("templates");
    }

    /**
     * 1. AŞAMA: Sadece ofsetleri ve isimleri yükler (Sunucu başlarken çalışabilir)
     */
    public void loadLogicalTemplates() {
        repository.loadAllTemplates().thenAccept(loadedTemplates -> {
            templates.clear();
            loadedTemplates.forEach(t -> templates.put(t.id(), t));
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    /**
     * 2. AŞAMA: Fiziksel StructureTemplate nesnelerini RAM'e doldurur.
     * DİKKAT: Bu metot sadece Minecraft dünyası/kayıtları hazır olduğunda 
     * (Örn: ServerLifecycleEvents.SERVER_STARTED) çağrılmalıdır.
     * * @param server MinecraftServer instance'ı
     */
    public void loadPhysicalStructures(MinecraftServer server) {
        structureCache.clear();

        try (Stream<Path> paths = Files.list(templatesDir)) {
            paths.filter(p -> p.toString().endsWith(".nbt")).forEach(path -> {
                try {
                    String fileName = path.getFileName().toString();
                    String id = fileName.substring(0, fileName.lastIndexOf('.'));

                    // NBT Dosyasını Diskten Oku
                    CompoundTag nbtData = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());

                    // Minecraft StructureTemplate Nesnesini Oluştur ve Yükle
                    StructureTemplate structure = new StructureTemplate();

                    // Fabric 1.21.2+ için HolderLookup Provider gereklidir. Server'dan alıyoruz.
                    structure.load(server.registryAccess().lookupOrThrow(Registries.BLOCK), nbtData);

                    // Cache'e Ekle
                    structureCache.put(id, structure);

                } catch (Exception e) {
                    System.err.println("[OGSkyBlock] StructureTemplate yüklenemedi: " + path.getFileName());
                    e.printStackTrace();
                }
            });
            System.out.println("[OGSkyBlock] Başarıyla " + structureCache.size() + " adet StructureTemplate RAM'e alındı.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- GETTERS ---

    @NotNull
    public Optional<IslandTemplate> getTemplate(@NotNull String id) {
        return Optional.ofNullable(templates.get(id));
    }

    @NotNull
    public Collection<IslandTemplate> getAllTemplates() {
        return templates.values();
    }

    /**
     * Yapıştırma motoru (WorldPastingEngine) için fiziksel şablonu döndürür.
     */
    @NotNull
    public Optional<StructureTemplate> getStructureTemplate(@NotNull String id) {
        return Optional.ofNullable(structureCache.get(id));
    }
}