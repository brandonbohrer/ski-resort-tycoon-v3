package com.project.tycoon.world.model;

import java.util.Random;

public class TerrainGenerator {

    // Simple Pseudo-Random Noise
    // In a real engine, we'd use SimplexNoise or PerlinNoise
    
    public static void generateMountain(WorldMap map) {
        int width = map.getWidth();
        int depth = map.getDepth();
        Random random = new Random(12345); // Fixed seed for reproducibility

        // Base Slope: Orient so "Top" of screen (High Z + High X in iso projection?)
        // Isometric Z is depth. 
        // Let's make the center-back (High Z) the peak.
        
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                
                // Peak at Center-Back (x=Width/2, z=Depth)
                // Base at Front (z=0)
                
                // Normalized Z (0 to 1)
                // We invert Z so 0 (Front) is Low, Depth (Back) is High.
                // Wait, in Isometric +Z is usually South (Foreground). 
                // Camera is at +Z looking at -Z.
                // So if we want Mountain at Top (Background, -Z), we want High Height at Low Z.
                // So normZ should be 1.0 at z=0, and 0.0 at z=depth.
                float normZ = 1.0f - (float)z / depth; 
                
                // Distance from center X
                float distX = Math.abs(x - width/2f) / (width/2f); // 0 at center, 1 at edges
                
                // Mountain Shape: Height increases with Z, decreases with distance from center X
                float baseHeight = (normZ * 40f) * (1.0f - distX * 0.5f);
                
                // 2. Noise (The "Roughness")
                // Simple smooth noise simulation using sin waves
                float noise = (float) (Math.sin(x * 0.05f) + Math.cos(z * 0.05f)) * 4.0f;
                float detail = (float) (Math.sin(x * 0.15f + z * 0.1f)) * 1.5f;
                
                int finalHeight = (int) (baseHeight + noise + detail);
                if (finalHeight < 0) finalHeight = 0;
                
                Tile tile = map.getTile(x, z);
                tile.setHeight(finalHeight);
                
                // 3. Biomes (Terrain Painting)
                // Adjusted thresholds for new height scale
                if (finalHeight >= 30) {
                    tile.setType(TerrainType.SNOW);
                } else if (finalHeight >= 20) {
                    if (random.nextBoolean()) tile.setType(TerrainType.ROCK);
                    else tile.setType(TerrainType.SNOW);
                } else if (finalHeight >= 10) {
                    tile.setType(TerrainType.GRASS);
                } else {
                    tile.setType(TerrainType.DIRT);
                }
            }
        }
    }
}

