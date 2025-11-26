package com.project.tycoon.view.util;

import com.badlogic.gdx.math.Vector2;

/**
 * Pure mathematical conversions between Grid Space and World (Render) Space.
 * Does NOT handle picking logic or height offsets.
 */
public class IsoUtils {
    
    public static final float TILE_WIDTH = 16f;
    public static final float TILE_HEIGHT = 8f;
    public static final float HEIGHT_SCALE = 8f; // Pixels per height unit

    /**
     * Project Grid(x, z) to Cartesian(x, y) for rendering (Base position, H=0).
     */
    public static Vector2 gridToWorld(int x, int z) {
        float isoX = (x - z) * (TILE_WIDTH / 2f);
        float isoY = (x + z) * (TILE_HEIGHT / 2f);
        return new Vector2(isoX, isoY);
    }
    
    /**
     * Gets the visual Y offset for a given height.
     */
    public static float getHeightOffset(int height) {
        return height * HEIGHT_SCALE;
    }

    /**
     * Helper: Converts World(x, y) to Grid(x, z) assuming H=0.
     * Useful for finding the center of the search area.
     */
    public static Vector2 worldToFlatGrid(float worldX, float worldY) {
        float halfW = TILE_WIDTH / 2f;
        float halfH = TILE_HEIGHT / 2f;

        float x = (worldY / halfH + worldX / halfW) / 2f;
        float z = (worldY / halfH - worldX / halfW) / 2f;

        return new Vector2(x, z);
    }
}
