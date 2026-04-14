package me.ogsammaenr.skyblockApi.math;
/**
 * Blok tabanlı koordinatları temsil eder (int).
 */
public record BlockCoordinate(int x, int y, int z) {
    public BlockCoordinate add(int dx, int dy, int dz) {
        return new BlockCoordinate(this.x + dx, this.y + dy, this.z + dz);
    }

    public BlockCoordinate subtract(BlockCoordinate other) {
        return new BlockCoordinate(this.x - other.x, this.y - other.y, this.z - other.z);
    }
}