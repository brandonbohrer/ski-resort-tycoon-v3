package com.project.tycoon.ecs.systems.skier;

import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;

/**
 * Scans trail boundaries to find width and centerline information.
 */
public class TrailScanner {
    
    private static final int MAX_SCAN_DISTANCE = 20;
    
    private final WorldMap map;
    
    public TrailScanner(WorldMap map) {
        this.map = map;
    }
    
    /**
     * Get trail width and centerline information at a position.
     */
    public TrailInfo scan(float x, float z) {
        int ix = (int) x;
        int iz = (int) z;
        
        // Scan left to find edge
        int leftEdge = scanLeftEdge(ix, iz);
        
        // Scan right to find edge
        int rightEdge = scanRightEdge(ix, iz);
        
        float centerX = (leftEdge + rightEdge) / 2.0f;
        float width = rightEdge - leftEdge;
        boolean onTrail = (ix >= leftEdge && ix <= rightEdge);
        
        return new TrailInfo(centerX, width, leftEdge, rightEdge, onTrail);
    }
    
    private int scanLeftEdge(int ix, int iz) {
        int leftEdge = ix;
        
        for (int dx = 0; dx < MAX_SCAN_DISTANCE; dx++) {
            int checkX = ix - dx;
            
            if (!map.isValid(checkX, iz)) {
                break;
            }
            
            Tile t = map.getTile(checkX, iz);
            if (t == null || !t.isTrail()) {
                leftEdge = checkX + 1;
                break;
            }
            
            if (dx == MAX_SCAN_DISTANCE - 1) {
                leftEdge = checkX; // Hit scan limit
            }
        }
        
        return leftEdge;
    }
    
    private int scanRightEdge(int ix, int iz) {
        int rightEdge = ix;
        
        for (int dx = 0; dx < MAX_SCAN_DISTANCE; dx++) {
            int checkX = ix + dx;
            
            if (!map.isValid(checkX, iz)) {
                break;
            }
            
            Tile t = map.getTile(checkX, iz);
            if (t == null || !t.isTrail()) {
                rightEdge = checkX - 1;
                break;
            }
            
            if (dx == MAX_SCAN_DISTANCE - 1) {
                rightEdge = checkX; // Hit scan limit
            }
        }
        
        return rightEdge;
    }
    
    /**
     * Trail width information at a position.
     */
    public static class TrailInfo {
        public final float centerlineX;
        public final float trailWidth;
        public final float leftEdge;
        public final float rightEdge;
        public final boolean onTrail;
        
        public TrailInfo(float centerX, float width, float left, float right, boolean onTrail) {
            this.centerlineX = centerX;
            this.trailWidth = width;
            this.leftEdge = left;
            this.rightEdge = right;
            this.onTrail = onTrail;
        }
    }
}

