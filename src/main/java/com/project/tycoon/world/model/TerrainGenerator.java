package com.project.tycoon.world.model;

import java.util.Random;

/**
 * Procedural terrain generator using Perlin noise for realistic mountains.
 * Guarantees flat base area and finds optimal base camp location.
 */
public class TerrainGenerator {

    /**
     * Generate a random mountain with guaranteed flat base.
     * 
     * @param map  WorldMap to populate
     * @param seed Random seed for reproducible generation
     * @return Optimal base camp location
     */
    public static BaseCampLocation generateMountain(WorldMap map, long seed) {
        int width = map.getWidth();
        int depth = map.getDepth();

        Random random = new Random(seed);
        PerlinNoise perlin = new PerlinNoise(seed);

        // Mountain parameters (randomized per seed)
        float peakOffsetX = random.nextFloat() * 0.4f - 0.2f; // -20% to +20%
        float peakHeight = 60.0f + random.nextFloat() * 40.0f; // 60-100
        float baseZoneRatio = 0.28f; // Bottom 28% is flat

        // Generate height map
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                Tile tile = map.getTile(x, z);

                // Always snow terrain
                tile.setType(TerrainType.SNOW);

                // Normalized coordinates
                float normZ = 1.0f - (float) z / depth; // 0 = front (base), 1 = back (peak)
                float normX = (float) x / width; // 0 = left, 1 = right

                float height;

                if (normZ < baseZoneRatio) {
                    // FLAT BASE ZONE - guaranteed flat area for base camp
                    double noise = perlin.octaveNoise(x * 0.05, z * 0.05, 2, 0.5);
                    height = (float) (noise * 1.5); // Just tiny bumps
                } else {
                    // MOUNTAIN ZONE - use Perlin noise

                    // Large scale: overall mountain shape
                    double largeNoise = perlin.octaveNoise(x * 0.01, z * 0.01, 1, 0.5);

                    // Medium scale: ridges and valleys
                    double mediumNoise = perlin.octaveNoise(x * 0.03, z * 0.03, 3, 0.5);

                    // Small scale: surface detail
                    double smallNoise = perlin.octaveNoise(x * 0.1, z * 0.1, 2, 0.6);

                    // Altitude based on distance from base
                    float t = (normZ - baseZoneRatio) / (1.0f - baseZoneRatio); // 0-1 in mountain zone
                    float altitude = (float) Math.pow(t, 1.8) * peakHeight; // Exponential rise

                    // X-axis tapering (mountain ridge)
                    float peakX = 0.5f + peakOffsetX; // Peak position
                    float distX = Math.abs(normX - peakX) / 0.5f; // 0 at peak, 1 at edge
                    float xMask = 1.0f - (float) Math.pow(distX, 1.2); // Smooth falloff

                    // Combine all noise layers
                    height = altitude * xMask +
                            (float) (largeNoise * 12.0 + mediumNoise * 8.0 + smallNoise * 3.0);

                    // Add random sub-peaks
                    double subPeakNoise = perlin.octaveNoise(x * 0.02, z * 0.02, 2, 0.5);
                    if (subPeakNoise > 0.3) {
                        height += (float) ((subPeakNoise - 0.3) * 15.0);
                    }
                }

                int finalHeight = (int) Math.max(0, height);
                tile.setHeight(finalHeight);

                // Decorations (trees and rocks)
                placeDecorations(tile, finalHeight, random);
            }
        }

        // Find optimal base camp location
        return findBestBaseCampLocation(map, baseZoneRatio);
    }

    /**
     * Find the flattest area in the base zone for base camp placement.
     */
    private static BaseCampLocation findBestBaseCampLocation(WorldMap map, float baseZoneRatio) {
        int width = map.getWidth();
        int depth = map.getDepth();

        int bestX = width / 2;
        int bestZ = (int) (depth * 0.85); // Default: 85% down
        float bestFlatness = Float.MAX_VALUE;

        // Search area (bottom portion of base zone)
        int searchZStart = (int) (depth * baseZoneRatio * 0.6); // Start of flat zone
        int searchZEnd = depth - 15; // Stay away from edge

        int checkRadius = 8; // Check 8x8 area around each point

        for (int z = searchZStart; z < searchZEnd; z += 3) { // Sample every 3rd tile
            for (int x = 25; x < width - 25; x += 3) { // Stay away from edges
                float flatness = calculateFlatness(map, x, z, checkRadius);

                // Prefer center-ish locations (slight bias)
                float centerDist = Math.abs(x - width / 2f) / (width / 2f);
                float score = flatness * (1.0f + centerDist * 0.2f);

                if (score < bestFlatness) {
                    bestFlatness = score;
                    bestX = x;
                    bestZ = z;
                }
            }
        }

        float height = map.getTile(bestX, bestZ).getHeight();
        return new BaseCampLocation(bestX, bestZ, height);
    }

    /**
     * Calculate flatness metric for area around point.
     * Lower value = flatter terrain.
     */
    private static float calculateFlatness(WorldMap map, int centerX, int centerZ, int radius) {
        float minHeight = Float.MAX_VALUE;
        float maxHeight = Float.MIN_VALUE;
        int count = 0;

        for (int z = centerZ - radius; z <= centerZ + radius; z++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                if (x >= 0 && x < map.getWidth() && z >= 0 && z < map.getDepth()) {
                    Tile tile = map.getTile(x, z);
                    if (tile != null) {
                        float h = tile.getHeight();
                        minHeight = Math.min(minHeight, h);
                        maxHeight = Math.max(maxHeight, h);
                        count++;
                    }
                }
            }
        }

        // Flatness = height variance
        return (count > 0) ? (maxHeight - minHeight) : Float.MAX_VALUE;
    }

    /**
     * Place trees and rocks based on height and randomness.
     */
    private static void placeDecorations(Tile tile, int height, Random random) {
        tile.setDecoration(Decoration.NONE);

        float r = random.nextFloat();

        if (height < 40 && height > 2) {
            // Tree zone (lower elevations)
            if (r < 0.035f) { // 3.5% chance
                tile.setDecoration(Decoration.TREE);
            } else if (r < 0.045f) { // 1% chance
                tile.setDecoration(Decoration.ROCK);
            }
        } else if (height >= 40) {
            // Alpine zone (higher elevations, rocks only)
            if (r < 0.06f) { // 6% chance
                tile.setDecoration(Decoration.ROCK);
            }
        }
    }
}
