package me.ogsammaenr.skyblockApi.math;

/**
 * Hassas dünyadaki konumları temsil eder (double + rotation).
 */
public record Location(double x, double y, double z, float yaw, float pitch) {
    public static Location at(double x, double y, double z) {
        return new Location(x, y, z, 0.0f, 0.0f);
    }
}