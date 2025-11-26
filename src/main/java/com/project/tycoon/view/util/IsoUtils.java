package com.project.tycoon.view.util;

import com.badlogic.gdx.math.Vector2;

public class IsoUtils {
    
    // Reduced tile size for higher resolution look (16x8)
    public static final float TILE_WIDTH = 16f;
    public static final float TILE_HEIGHT = 8f;

    /**
     * Converts grid coordinates (integer x, z) to world coordinates (rendering position).
     */
    public static Vector2 gridToWorld(int x, int z) {
        float isoX = (x - z) * (TILE_WIDTH / 2f);
        float isoY = (x + z) * (TILE_HEIGHT / 2f);
        return new Vector2(isoX, isoY);
    }

    /**
     * Converts world coordinates (from unprojected mouse position) to grid coordinates.
     * Note: This assumes Flat terrain (y=0). Height complicates picking significantly.
     * For a tycoon game, we typically pick at "base height" or raycast against bounding boxes.
     * 
     * Simplification: We will pick based on the flat plane.
     */
    public static Vector2 worldToGrid(float worldX, float worldY) {
        // Reverse the formulas:
        // x = (worldY / (H/2) + worldX / (W/2)) / 2
        // z = (worldY / (H/2) - worldX / (W/2)) / 2
        
        float halfW = TILE_WIDTH / 2f;
        float halfH = TILE_HEIGHT / 2f;

        float x = (worldY / halfH + worldX / halfW) / 2f;
        float z = (worldY / halfH - worldX / halfW) / 2f;

        return new Vector2(x, z);
    }
}

