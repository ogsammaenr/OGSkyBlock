package me.ogsammenr.skyblock.data;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.ogsammenr.skyblock.model.Island;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class IslandStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static class SaveData {
        int islandCount;
        int currentGridX;
        int currentGridZ;
        Map<UUID, Island> islands;
        Map<UUID, UUID> playerIslands;

        public SaveData(){ }

        SaveData(int islandCount, int currentGridX, int currentGridZ, Map<UUID, Island> islands, Map<UUID, UUID> playerIslands) {
            this.currentGridX = currentGridX;
            this.currentGridZ = currentGridZ;
            this.islandCount = islandCount;
            this.playerIslands = playerIslands;
            this.islands = islands;
        }
    }

    private static Path getSaveFile(MinecraftServer server ) {
        return server.getWorldPath(LevelResource.ROOT).resolve("skyblock_islands_data.json");
    }

    public static void save (MinecraftServer server) {
        Path filePath = getSaveFile(server);
        try {
            SaveData data = new SaveData(
                    IslandRegistry.getIslandCount(),
                    IslandRegistry.getCurrentGridX(),
                    IslandRegistry.getCurrentGridZ(),
                    IslandRegistry.getIslands(),
                    IslandRegistry.getPlayerIslands()
                    );
            try (Writer writer = Files.newBufferedWriter(filePath)) {
                GSON.toJson(data, writer);
            }
            System.out.println("Adalar başarıyla kaydedildi.");
        }catch (Exception e) {
            System.err.println("Adalar kaydedilirken hata oluştu:\n " + e.getMessage());
        }

    }

    public static void load (MinecraftServer server) {
        Path filePath = getSaveFile(server);
        if (!Files.exists(filePath)) {
            System.out.println("Kayıt dosyası bulunamadı, yeni bir kayıt oluşturulacak.");
            return;
        }
        try (Reader reader = Files.newBufferedReader(filePath)){
            SaveData data = GSON.fromJson(reader, SaveData.class);
            if (data != null  && data.islands != null) {
                IslandRegistry.loadData(
                        data.islands,
                        data.playerIslands,
                        data.islandCount,
                        data.currentGridX,
                        data.currentGridZ
                );
                System.out.println(data.islandCount + "adet ada yüklendi.");
            }
        }catch (Exception e) {
            System.err.println("Adalar yüklenirken hata oluştu:\n " + e.getMessage());
        }
    }

}
