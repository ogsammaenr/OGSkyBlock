package me.ogsammenr.skyblock.level;

import me.ogsammenr.skyblock.level.snapshot.ChunkSnapshot;
import me.ogsammenr.skyblock.level.snapshot.IslandSnapshot;
import me.ogsammenr.skyblock.manager.BlockValueManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncLevelCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger("Skyblock-core | AsyncWorker");

    private static final ExecutorService WORKER_POOL = Executors.newSingleThreadExecutor();

    public static void calculateIslandLevel(MinecraftServer server, UUID islandId, IslandSnapshot snapshot, ServerPlayer player) {

        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            long[] totalPoints = {0};
            int[] blocksScanned = {0};

            // --- 1. AŞAMA: ANINDA ALINAN (YÜKLÜ) CHUNKLARI TARA ---
            for (ChunkSnapshot chunkSnap : snapshot.getChunkSnapshots()) {
                scanSnapshot(chunkSnap, snapshot, totalPoints, blocksScanned);
            }

            // --- 2. AŞAMA: YÜKLÜ OLMAYAN CHUNKLARI GÜVENLİCE (PİNG-PONG) YÜKLE VE TARA ---
            ServerLevel world = player.level();

            for (ChunkPos pos : snapshot.getUnloadedChunks()) {

                // Asenkron iplik ile Main Thread arasında bir haberleşme köprüsü kuruyoruz
                CompletableFuture<ChunkSnapshot> chunkFuture = new CompletableFuture<>();

                server.execute(() -> {
                    // MAIN THREAD İÇİNDEYİZ: Burada 'true' diyerek chunk'ı diskten güvenle okuyoruz.
                    // Bu işlem server.execute içinde olduğu için sunucu müsait oldukça (tick başına 1-2 adet) işlenir.
                    LevelChunk loadedChunk = world.getChunkSource().getChunk(pos.x, pos.z, true);

                    if (loadedChunk != null) {
                        ChunkSnapshot lateSnap = new ChunkSnapshot(pos.x, pos.z);
                        LevelChunkSection[] sections = loadedChunk.getSections();
                        for (int i = 0; i < sections.length; i++) {
                            if (sections[i] != null && !sections[i].hasOnlyAir()) {
                                lateSnap.addSection(i, sections[i].getStates().copy());
                            }
                        }
                        chunkFuture.complete(lateSnap); // Kopyayı çıkarttık, köprüden Asenkron ipliğe yolladık!
                    } else {
                        chunkFuture.complete(null);
                    }
                });

                // ASYNC İPLİĞİ DURDUR! Main Thread işini bitirene kadar (1-2 milisaniye) bekle.
                // Bu satır sayesinde tüm chunklar aynı anda yüklenmez, doğal bir Tick-Slicer (zaman dilimleyici) oluşur!
                ChunkSnapshot lateSnapshot = chunkFuture.join();

                // Gelen kopyayı tara
                if (lateSnapshot != null) {
                    scanSnapshot(lateSnapshot, snapshot, totalPoints, blocksScanned);
                }
            }

            long timeTaken = System.currentTimeMillis() - startTime;
            long finalPoints = totalPoints[0];
            long finalLevel = finalPoints / 100L;
            int finalScanned = blocksScanned[0];

            server.execute(() -> {
                LOGGER.info("Ada ID : {} seviyesi hesaplandi! Puan: {} | Taranan Hedef Blok: {} | Süre : {}ms",
                        islandId, finalPoints,finalScanned, timeTaken);

                if (player != null && !player.hasDisconnected()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§a§lAda Seviyesi Hesaplandı!\n" +
                                    "§7Toplam Puan: §e" + finalPoints + "\n" +
                                    "§7Ada Seviyesi: §e" + finalLevel + "§8(Her 100 puan = 1 lvl)\n" +
                                    "§7Taranan Blok: §b" + finalScanned + "\n" +
                                    "§7Hesaplama Süresi: §8" + timeTaken + "ms"
                    ));
                }

            });

        }, WORKER_POOL).exceptionally(ex -> {
            LOGGER.error("IslandID: {} Error in calculating island level", islandId, ex);
            return null;
        });
    }


    private static void scanSnapshot(ChunkSnapshot chunkSnap, IslandSnapshot bounds, long[] points, int[] scanned) {
        int chunkBaseX = chunkSnap.getChunkX() << 4;
        int chunkBaseZ = chunkSnap.getChunkZ() << 4;

        for (PalettedContainer<BlockState> sectionPalette : chunkSnap.getSections().values()) {
            for (int i = 0; i < 4096; i++) {
                int localX = i & 15;
                int localZ = (i >> 4) & 15;
                int localY = (i >> 8) & 15;

                int globalX = chunkBaseX + localX;
                int globalZ = chunkBaseZ + localZ;

                if (globalX >= bounds.minX && globalX <= bounds.maxX &&
                        globalZ >= bounds.minZ && globalZ <= bounds.maxZ) {

                    BlockState state = sectionPalette.get(localX, localY, localZ);
                    if (!state.isAir()) {
                        scanned[0]++;
                        int value = BlockValueManager.getValue(state.getBlock());
                        if (value > 0) {
                            points[0] += value;
                        }
                    }
                }
            }
        }
    }
}
