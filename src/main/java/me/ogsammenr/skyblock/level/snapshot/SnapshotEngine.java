package me.ogsammenr.skyblock.level.snapshot;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

public class SnapshotEngine {


    public static IslandSnapshot takeSnapshot(ServerLevel level, int minX, int minZ, int maxX, int maxZ) {
        IslandSnapshot snapshot = new IslandSnapshot(minX, minZ, maxX, maxZ);
        // x >> 4 == x / 16
        int cMinX = minX >> 4;
        int cMaxX = maxX >> 4;
        int cMinZ = minZ >> 4;
        int cMaxZ = maxZ >> 4;

        for (int cx = cMinX; cx <= cMaxX; cx++) {
            for (int cz = cMinZ; cz <= cMaxZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);

                if (chunk == null) {
                    snapshot.addUnloadedChunk(new ChunkPos(cx, cz));
                    continue;
                }

                ChunkSnapshot chunkSnap = new ChunkSnapshot(cx, cz);
                LevelChunkSection[] sections = chunk.getSections();

                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];

                    if (section == null) continue;

                    PalettedContainer<BlockState> stateCopy = section.getStates().copy();
                    chunkSnap.addSection(i, stateCopy);
                }

                snapshot.addChunkSnapshot(chunkSnap);
            }
        }

        return snapshot;
    }




}
