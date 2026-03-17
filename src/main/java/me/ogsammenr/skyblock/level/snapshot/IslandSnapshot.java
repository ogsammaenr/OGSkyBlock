package me.ogsammenr.skyblock.level.snapshot;

import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public class IslandSnapshot {
    public final int minX, minZ, maxX, maxZ;
    private final List<ChunkSnapshot> chunkSnapshots = new ArrayList<>();

    private final List<ChunkPos> unloadedChunks = new ArrayList<>();
    public IslandSnapshot(int minX, int minZ, int maxX, int maxZ) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }

    public void addChunkSnapshot(ChunkSnapshot snapshot){
        this.chunkSnapshots.add(snapshot);
    }
    public void addUnloadedChunk(ChunkPos chunk){
        this.unloadedChunks.add(chunk);
    }

    public List<ChunkSnapshot> getChunkSnapshots() {
        return chunkSnapshots;
    }
    public List<ChunkPos> getUnloadedChunks() {
        return unloadedChunks;
    }
}
