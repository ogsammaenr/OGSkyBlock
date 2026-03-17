package me.ogsammenr.skyblock.level.snapshot;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;

import java.util.HashMap;
import java.util.Map;

public class ChunkSnapshot {
    private final int chunkX;
    private final int chunkZ;

    private final Map<Integer, PalettedContainer<BlockState>> sections = new HashMap<>();

    public ChunkSnapshot(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public void addSection(int sectionIndex, PalettedContainer<BlockState> states ){
        this.sections.put(sectionIndex, states);
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public Map<Integer, PalettedContainer<BlockState>> getSections() {
        return sections;
    }

}
