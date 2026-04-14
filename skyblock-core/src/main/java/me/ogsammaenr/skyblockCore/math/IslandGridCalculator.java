package me.ogsammaenr.skyblockCore.math;

import me.ogsammaenr.skyblockApi.math.BlockCoordinate;

public final class IslandGridCalculator {

    private static final int DISTANCE_BETWEEN_ISLANDS = 500;

    private IslandGridCalculator() {}

    /**
     * Verilen ada indeksine (sunucudaki toplam ada sayısı vb.) göre
     * kare sarmal (square spiral) algoritması ile sıradaki X, Z koordinatını hesaplar.
     */
    public static BlockCoordinate calculateNextLocation(int index) {
        if (index <= 0) return new BlockCoordinate(0, 0, 0);

        // Matematiksel Spiral Algoritması
        int k = (int) Math.ceil((Math.sqrt(index) - 1) / 2.0);
        int t = 2 * k;
        int m = (t + 1) * (t + 1);

        int x = 0;
        int z = 0;

        if (index >= m - t) {
            x = k - (m - index);
            z = -k;
        } else {
            m -= t;
            if (index >= m - t) {
                x = -k;
                z = -k + (m - index);
            } else {
                m -= t;
                if (index >= m - t) {
                    x = -k + (m - index);
                    z = k;
                } else {
                    x = k;
                    z = k - (m - index - t);
                }
            }
        }

        return new BlockCoordinate(x * DISTANCE_BETWEEN_ISLANDS, 0, z * DISTANCE_BETWEEN_ISLANDS);
    }
}