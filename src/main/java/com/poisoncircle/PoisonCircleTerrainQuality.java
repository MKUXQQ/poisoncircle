package com.poisoncircle;

/** Terrain rules for choosing a safe, low and mostly flat next-zone center. */
final class PoisonCircleTerrainQuality {
    private static final int MAX_HEIGHT_VARIATION = 4;
    private static final int MAX_ABOVE_SEA_LEVEL = 32;

    private PoisonCircleTerrainQuality() { }

    static boolean accepts(int[] heights, int seaLevel, boolean hasFluid) {
        if (heights.length == 0 || hasFluid) return false;
        int min = heights[0];
        int max = heights[0];
        long total = 0;
        for (int height : heights) {
            min = Math.min(min, height);
            max = Math.max(max, height);
            total += height;
        }
        return max - min <= MAX_HEIGHT_VARIATION && total / heights.length <= seaLevel + MAX_ABOVE_SEA_LEVEL;
    }

    static int score(int[] heights, int seaLevel) {
        int min = heights[0];
        int max = heights[0];
        long total = 0;
        for (int height : heights) {
            min = Math.min(min, height);
            max = Math.max(max, height);
            total += height;
        }
        return (max - min) * 1_000 + Math.max(0, (int) (total / heights.length) - seaLevel);
    }
}
