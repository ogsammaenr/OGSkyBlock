package me.ogsammenr.skyblock.manager;

import me.ogsammenr.skyblock.model.Island;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static me.ogsammenr.skyblock.SkyblockMain.SKYBLOCK_WORLD_KEY;

public class IslandManager {
    public static final Path ISLANDS_DIR = FabricLoader.getInstance().getConfigDir().resolve("skyblock_core/islands");

    public static void spawnPlayerToIsland(ServerPlayer player) {
        ServerLevel skyblockWorld = player.level().getServer().getLevel(SKYBLOCK_WORLD_KEY);

        if (skyblockWorld != null) {
            UUID playerUuid = player.getUUID();
            boolean isNewIsland = !IslandRegistry.hasIsland(playerUuid);

            Island island = IslandRegistry.getOrCreateIsland(playerUuid);

            if (isNewIsland) {
                generateSmartIsland(skyblockWorld, island, "classic_island");
            }

            BlockPos spawnPos = island.getSpawnPoint();

            TeleportTransition transition = new TeleportTransition(
                    skyblockWorld,
                    new Vec3(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5),
                    Vec3.ZERO,
                    0.0f, 0.0f,
                    TeleportTransition.DO_NOTHING
            );

            player.teleport(transition);
            player.sendSystemMessage(Component.literal("§aSkyblock adana ışınlandın!"));
        }
    }


    public static void generateSmartIsland(ServerLevel level, Island island, String islandFileName) {
        Path templatePath = ISLANDS_DIR.resolve(islandFileName + ".nbt");

        if (!Files.exists(templatePath)) {
            System.err.println("HATA: Ada şablonu bulunamadı!");
            return;
        }

        try {
            CompoundTag nbtData = NbtIo.readCompressed(templatePath, NbtAccounter.unlimitedHeap());
            StructureTemplate template = new StructureTemplate();
            template.load(level.holderLookup(Registries.BLOCK), nbtData);

            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .setMirror(Mirror.NONE)
                    .setRotation(Rotation.NONE)
                    .setIgnoreEntities(false);

            // 1. ADIM: Bedrock'u Bul ve Adayı Hizala (Anchor Sistemi)
            List<StructureTemplate.StructureBlockInfo> bedrocks = template.filterBlocks(BlockPos.ZERO, settings, Blocks.BEDROCK);

            BlockPos bedrockOffset;
            if (!bedrocks.isEmpty()) {
                bedrockOffset = bedrocks.get(0).pos();
            } else {
                bedrockOffset = new BlockPos(template.getSize().getX() / 2, 0, template.getSize().getZ() / 2);
                System.out.println("UYARI: Şablonda Bedrock bulunamadı, varsayılan merkez kullanılıyor.");
            }

            // Dünyada yerleştirileceği başlangıç noktasını hesapla
            BlockPos placePos = island.getCenter().subtract(bedrockOffset);

            template.placeInWorld(level, placePos, placePos, settings, level.getRandom(), 2);

            // 2. ADIM: [SPAWN] Tabelasını Bul ve Doğuş Noktasını Ayarla
            Vec3i size = template.getSize();
            boolean spawnFound = false;

            for (BlockPos p : BlockPos.betweenClosed(placePos, placePos.offset(size))) {
                BlockEntity be = level.getBlockEntity(p);

                if (be instanceof SignBlockEntity sign) {
                    String line1 = sign.getFrontText().getMessage(0, false).getString();

                    if (line1.contains("[SPAWN]")) {
                        island.setSpawnPoint(p.immutable());
                        level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                        spawnFound = true;
                        break;
                    }
                }
            }

            // Eğer tabela bulunamadıysa, güvenli bir yer olarak Bedrock'un üstünü ayarla
            if (!spawnFound) {
                island.setSpawnPoint(island.getCenter().above());
                System.out.println("UYARI: [SPAWN] tabelası bulunamadı, doğuş noktası Bedrock'un üstü olarak ayarlandı.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
