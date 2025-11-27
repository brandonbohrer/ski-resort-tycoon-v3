package com.project.tycoon.world.model;

import java.util.Random;

public class TerrainGenerator {

    public static void generateMountain(WorldMap map) {
        int width = map.getWidth();
        int depth = map.getDepth();
        Random random = new Random(12345); // Fixed seed

        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                Tile tile = map.getTile(x, z);
                
                // 1. Terrain Type: ALWAYS SNOW (as requested)
                tile.setType(TerrainType.SNOW);
                
                // 2. Height Generation
                // Z=0 is Back (Peak), Z=Depth is Front (Base)
                float normZ = 1.0f - (float)z / depth; 
                
                // Flat base logic: Flat for the first ~30% (Front), then rises
                float mountainRise = 0.0f;
                if (normZ > 0.25f) {
                    // Rescale range [0.25, 1.0] to [0.0, 1.0]
                    float t = (normZ - 0.25f) / 0.75f;
                    // Exponential rise creates a steep peak effect
                    mountainRise = (float)Math.pow(t, 2.0f) * 80.0f; 
                }
                
                // Subpeaks / Hilly Noise
                // Combining sines to create larger lumps (subpeaks)
                float largeHills = (float)(Math.sin(x * 0.03f) + Math.cos(z * 0.04f)) * 8.0f;
                
                // Detail Noise
                float detail = (float)(Math.sin(x * 0.1f + z * 0.15f)) * 2.0f;
                
                // Center Bias: Make the mountain taper off to the sides (X axis)
                float distX = Math.abs(x - width/2f) / (width/2f); // 0 center, 1 edge
                float xMask = 1.0f - (float)Math.pow(distX, 1.5f); // Curve to keep center high
                
                float combinedHeight = (mountainRise + largeHills + detail) * xMask;
                
                // Ensure base is flat-ish but allows small variation
                if (normZ <= 0.25f) {
                    combinedHeight = detail * 0.5f; // Just little bumps at base
                }
                
                int finalHeight = (int) Math.max(0, combinedHeight);
                tile.setHeight(finalHeight);
                
                // 3. Vegetation & Rocks
                // Trees: Randomly placed below 'Treeline' (height < 50)
                // Rocks: Randomly placed anywhere, more common higher up
                
                tile.setDecoration(Decoration.NONE); // Reset
                
                // Probability checks
                float r = random.nextFloat();
                
                if (finalHeight < 50 && finalHeight > 1) {
                    // Tree Zone
                    if (r < 0.03f) { // 3% chance
                        tile.setDecoration(Decoration.TREE);
                    } else if (r < 0.04f) { // 1% chance (overlapping range)
                        tile.setDecoration(Decoration.ROCK);
                    }
                } else if (finalHeight >= 50) {
                    // Alpine Zone (Rocks only)
                    if (r < 0.05f) {
                        tile.setDecoration(Decoration.ROCK);
                    }
                }
            }
        }
    }
}
