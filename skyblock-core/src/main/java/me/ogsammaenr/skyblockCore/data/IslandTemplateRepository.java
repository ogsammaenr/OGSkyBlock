package me.ogsammaenr.skyblockCore.data;

import me.ogsammaenr.skyblockApi.math.BlockCoordinate;
import me.ogsammaenr.skyblockApi.math.Location;
import me.ogsammaenr.skyblockApi.model.IslandTemplate;
import me.ogsammaenr.skyblockApi.repository.TemplateRepository;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class IslandTemplateRepository implements TemplateRepository {

    private final Path templatesDir;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "NBT-Template-Scanner"));

    public IslandTemplateRepository() {
        this.templatesDir = FabricLoader.getInstance().getConfigDir().resolve("OGSkyBlock").resolve("templates");
        try {
            if (!Files.exists(templatesDir)) {
                Files.createDirectories(templatesDir);
            }
        } catch (Exception e) {
            throw new RuntimeException("Template klasörü oluşturulamadı!", e);
        }
    }

    @Override
    public @NotNull CompletableFuture<List<IslandTemplate>> loadAllTemplates() {
        return CompletableFuture.supplyAsync(() -> {
            List<IslandTemplate> templates = new ArrayList<>();
            try (Stream<Path> paths = Files.list(templatesDir)) {
                paths.filter(p -> p.toString().endsWith(".nbt")).forEach(path -> {
                    try {
                        String fileName = path.getFileName().toString();
                        String id = fileName.substring(0, fileName.lastIndexOf('.'));
                        String displayName = id.substring(0, 1).toUpperCase() + id.substring(1).replace("_", " ");

                        IslandTemplate template = parseNbtFile(path, id, displayName);
                        templates.add(template);
                    } catch (Exception e) {
                        System.err.println("[OGSkyBlock] Şablon okunamadı: " + path.getFileName());
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return templates;
        }, ioExecutor);
    }

    private IslandTemplate parseNbtFile(Path path, String id, String displayName) throws Exception {

        // DÜZELTME: InputStream sarmalayıcısına gerek yok. Monolitik yapındaki çalışan
        // orijinal satırı (Path + NbtAccounter) doğrudan kullanıyoruz.
        CompoundTag nbtData = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());

        ListTag palette = nbtData.getList("palette").orElseThrow();
        ListTag blocks = nbtData.getList("blocks").orElseThrow();

        int bedrockStateId = -1;
        int signStateId = -1;

        // 1. Palette'den Bedrock ve Tabela indekslerini bul
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag state = palette.getCompound(i).orElseThrow();
            String blockName = state.getString("Name").orElse("");

            if (blockName.equals("minecraft:bedrock")) bedrockStateId = i;
            if (blockName.contains("_sign")) signStateId = i;
        }

        BlockCoordinate bedrockOffset = null;
        Location spawnOffset = null;

        // 2. Blokları tara ve koordinatları yakala
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = blocks.getCompound(i).orElseThrow();
            int state = block.getInt("state").orElse(0);

            ListTag posTag = block.getList("pos").orElseThrow();
            int x = posTag.getInt(0).orElse(0);
            int y = posTag.getInt(1).orElse(0);
            int z = posTag.getInt(2).orElse(0);

            if (state == bedrockStateId && bedrockOffset == null) {
                bedrockOffset = new BlockCoordinate(x, y, z);
            }
            else if (state == signStateId) {
                CompoundTag blockEntityNbt = block.getCompound("nbt").orElse(null);
                if (blockEntityNbt != null) {
                    String text = blockEntityNbt.toString();
                    if (text.contains("[SPAWN]")) {
                        spawnOffset = new Location(x + 0.5, y, z + 0.5, 0f, 0f);
                    }
                }
            }

            if (bedrockOffset != null && spawnOffset != null) break;
        }

        if (bedrockOffset == null) bedrockOffset = new BlockCoordinate(0, 0, 0);
        if (spawnOffset == null) spawnOffset = Location.at(bedrockOffset.x() + 0.5, bedrockOffset.y() + 1.0, bedrockOffset.z() + 0.5);

        return new IslandTemplate(id, displayName, "minecraft:grass_block", bedrockOffset, spawnOffset);
    }
}