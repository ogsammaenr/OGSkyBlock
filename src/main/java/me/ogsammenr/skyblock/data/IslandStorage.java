package me.ogsammenr.skyblock.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.ogsammenr.skyblock.model.Island;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IslandStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Sadece global verileri ve Oyuncu->Ada eşleştirmesini tutan meta sınıfı
    private static class MetaData {
        int islandCount;
        int currentGridX;
        int currentGridZ;
        Map<UUID, UUID> playerIslands;

        public MetaData() { }

        MetaData(int islandCount, int currentGridX, int currentGridZ, Map<UUID, UUID> playerIslands) {
            this.currentGridX = currentGridX;
            this.currentGridZ = currentGridZ;
            this.islandCount = islandCount;
            this.playerIslands = playerIslands;
        }
    }

    private static final String BASE_FOLDER = "skyblock_data";

    private static Path getMetaFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(BASE_FOLDER + "/skyblock_meta.json");
    }

    private static Path getIslandsDirectory(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(BASE_FOLDER + "/islands");
    }

    public static void save(MinecraftServer server) {
        Path metaFile = getMetaFile(server);
        Path islandsDir = getIslandsDirectory(server);

        try {
            Files.createDirectories(metaFile.getParent());
            Files.createDirectories(islandsDir);

            // 1. Adalar klasörünün var olduğundan emin ol (Yoksa oluştur)
            Files.createDirectories(islandsDir);

            // 2. Global Meta Verilerini (skyblock_meta.json) Kaydet
            MetaData metaData = new MetaData(
                    IslandRegistry.getIslandCount(),
                    IslandRegistry.getCurrentGridX(),
                    IslandRegistry.getCurrentGridZ(),
                    IslandRegistry.getPlayerIslands()
            );

            try (Writer writer = Files.newBufferedWriter(metaFile)) {
                GSON.toJson(metaData, writer);
            }

            // 3. Her bir adayı kendi özel dosyasına kaydet
            Map<UUID, Island> allIslands = IslandRegistry.getIslands();
            for (Island island : allIslands.values()) {
                Path islandFile = islandsDir.resolve(island.getIslandUUID() + ".json");
                try (Writer writer = Files.newBufferedWriter(islandFile)) {
                    GSON.toJson(island, writer);
                } catch (Exception e) {
                    System.err.println("[Skyblock] Ada kaydedilirken hata oluştu (UUID: " + island.getIslandUUID() + "): " + e.getMessage());
                }
            }

            System.out.println("[Skyblock] " + allIslands.size() + " adet ada başarıyla kaydedildi.");

        } catch (Exception e) {
            System.err.println("[Skyblock] Genel kayıt işlemi sırasında kritik bir hata oluştu:\n " + e.getMessage());
        }
    }

    public static void load(MinecraftServer server) {
        Path metaFile = getMetaFile(server);
        Path islandsDir = getIslandsDirectory(server);

        if (!Files.exists(metaFile)) {
            System.out.println("[Skyblock] Kayıt dosyası bulunamadı, sistem sıfırdan başlatılıyor.");
            return;
        }

        try {
            // 1. Global Meta Verilerini Oku
            MetaData metaData;
            try (Reader reader = Files.newBufferedReader(metaFile)) {
                metaData = GSON.fromJson(reader, MetaData.class);
            }

            if (metaData == null) {
                System.err.println("[Skyblock] Meta verisi bozuk, yükleme iptal edildi.");
                return;
            }

            // 2. Klasördeki Ada Dosyalarını Tek Tek Oku
            Map<UUID, Island> loadedIslands = new HashMap<>();
            if (Files.exists(islandsDir)) {
                // Sadece .json uzantılı dosyaları okuyarak güvenliği artırıyoruz
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(islandsDir, "*.json")) {
                    for (Path entry : stream) {
                        try (Reader reader = Files.newBufferedReader(entry)) {
                            Island island = GSON.fromJson(reader, Island.class);
                            if (island != null && island.getIslandUUID() != null) {
                                loadedIslands.put(island.getIslandUUID(), island);
                            }
                        } catch (Exception e) {
                            // HATA İZOLASYONU: Eğer bir adanın JSON'u bozuksa, sadece o ada yüklenmez.
                            // Sunucu çökmekten kurtulur ve diğer 499 ada sorunsuz yüklenir!
                            System.err.println("[Skyblock] Şu ada dosyası bozuk ve atlandı: " + entry.getFileName());
                        }
                    }
                }
            }

            // 3. Verileri IslandRegistry'e Aktar (Mevcut loadData metodunla tam uyumlu)
            IslandRegistry.loadData(
                    loadedIslands,
                    metaData.playerIslands != null ? metaData.playerIslands : new HashMap<>(),
                    metaData.islandCount,
                    metaData.currentGridX,
                    metaData.currentGridZ
            );

            System.out.println("[Skyblock] " + loadedIslands.size() + " adet ada başarıyla yüklendi.");

        } catch (Exception e) {
            System.err.println("[Skyblock] Adalar yüklenirken kritik bir hata oluştu:\n " + e.getMessage());
        }
    }
}
