package com.project.tycoon.view;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.project.tycoon.view.util.IsoUtils;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;

/**
 * Robust Mouse Picking System.
 * Solves the "Isometric Height Problem" by performing a hit-test against the actual terrain geometry.
 */
public class MousePicker {

    private final WorldMap map;
    
    // Search radius (in tiles) to check around the "flat" mouse position.
    // Higher mountains need a larger radius.
    // At 8px per height unit, a height of 30 (max mountain) shifts the tile 240px up.
    // 240px / 8px (tile height) = 30 tiles shift.
    private static final int SEARCH_RADIUS = 40; 

    public MousePicker(WorldMap map) {
        this.map = map;
    }

    /**
     * Returns the Grid Coordinates (x, z) of the tile under the world position (mx, my).
     * Returns null if no tile is hit.
     */
    public Vector2 pickTile(float mx, float my) {
        // 1. Get "Flat" guess (H=0)
        Vector2 flatGuess = IsoUtils.worldToFlatGrid(mx, my);
        int guessX = (int) flatGuess.x;
        int guessZ = (int) flatGuess.y;

        // 2. Define Search Window
        // We scan a square around the guess.
        // Optimization: In isometric, height shifts things "Up" (Positive Y).
        // So we mostly need to check tiles "South" (Lower Y in World, Higher X+Z in Grid) of the guess?
        // Actually, scanning a simple box is safer and cheap enough for 256x256 maps if radius is sane.
        
        int startX = Math.max(0, guessX - SEARCH_RADIUS);
        int endX = Math.min(map.getWidth() - 1, guessX + SEARCH_RADIUS);
        int startZ = Math.max(0, guessZ - SEARCH_RADIUS);
        int endZ = Math.min(map.getDepth() - 1, guessZ + SEARCH_RADIUS);

        Vector2 bestMatch = null;
        float bestDepth = -Float.MAX_VALUE; // Standard Z-Buffer logic (Painter's Algo reverse)

        // 3. Iterate Candidates
        // We want the tile that is "closest" to the camera (visually on top).
        // In standard Iso rendering (Painter's), we draw:
        // for Z: for X: draw()
        // Later drawn tiles cover earlier ones.
        // So we want the LAST drawn tile that contains the point.
        
        // Iterate in render order:
        for (int z = startZ; z <= endZ; z++) {
            for (int x = startX; x <= endX; x++) {
                Tile tile = map.getTile(x, z);
                if (tile == null) continue;

                if (isPointInTile(mx, my, x, z, tile.getHeight())) {
                    // Since we iterate in render order, every match we find OVERWRITES the previous one.
                    // This naturally handles occlusion (e.g. a hill blocking a valley).
                    if (bestMatch == null) bestMatch = new Vector2();
                    bestMatch.set(x, z);
                }
            }
        }

        return bestMatch;
    }

    private boolean isPointInTile(float px, float py, int tx, int tz, int height) {
        Vector2 basePos = IsoUtils.gridToWorld(tx, tz);
        float hOffset = IsoUtils.getHeightOffset(height);
        
        // Tile Center (Visual)
        float cx = basePos.x + IsoUtils.TILE_WIDTH/2f; // Center X
        float cy = basePos.y + hOffset; // Bottom tip + Height?
        
        // Note on TextureFactory coordinates:
        // The texture is drawn at (isoX, isoY + hOffset).
        // Texture size is TILE_WIDTH x TILE_HEIGHT.
        // The diamond is inside this texture.
        
        // Polygon vertices relative to draw position (0,0)
        // Top: (W/2, H) -> wait, TextureFactory draws Bottom at 0?
        // Let's check TextureFactory again.
        // TextureFactory:
        // Top: (W/2, 0) (Wait, Pixmap Y is usually Down? LibGDX Texture coords are Y-Up?)
        // In LibGDX SpriteBatch, (0,0) is Bottom-Left.
        // TextureFactory Pixmap creation:
        // pixmap.fillTriangle(TEX_W/2, 0, 0, TEX_H/2, TEX_W, TEX_H/2); -> Top Triangle
        // Pixmap coords: 0,0 is Top-Left.
        // So (W/2, 0) is Top Middle.
        // (0, H/2) is Left Middle.
        // (W, H/2) is Right Middle.
        // This forms the Top Triangle.
        // This implies the "Diamond" top is at Pixmap Y=0.
        // But Texture drawing flips Y?
        
        // SIMPLIFICATION:
        // Let's define the diamond in World Coordinates mathematically.
        // Center of Diamond Base (H=0): (isoX + W/2, isoY + H/2)
        // With Height: Shift Y by hOffset.
        
        float left = basePos.x;
        float right = basePos.x + IsoUtils.TILE_WIDTH;
        float bottom = basePos.y + hOffset;
        float top = basePos.y + IsoUtils.TILE_HEIGHT + hOffset;
        
        float centerX = basePos.x + IsoUtils.TILE_WIDTH/2f;
        float centerY = basePos.y + IsoUtils.TILE_HEIGHT/2f + hOffset;
        
        // Detailed Hit Test (Diamond Shape)
        // |dx| / (W/2) + |dy| / (H/2) <= 1
        float dx = Math.abs(px - centerX);
        float dy = Math.abs(py - centerY);
        
        return (dx / (IsoUtils.TILE_WIDTH/2f) + dy / (IsoUtils.TILE_HEIGHT/2f)) <= 1.0f;
    }
}

