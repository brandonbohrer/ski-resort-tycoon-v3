package com.project.tycoon.ecs.systems.skier;

import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;

/**
 * Calculates a flow field for trails using BFS from the base area.
 * This provides downhill navigation for skiers.
 */
public class TrailFlowFieldCalculator {
    
    private final WorldMap map;
    
    private int[][] trailDistance;
    private int[][] trailNextX;
    private int[][] trailNextZ;
    private boolean flowReady = false;
    
    public TrailFlowFieldCalculator(WorldMap map) {
        this.map = map;
    }
    
    /**
     * Rebuild the flow field if the map has changed.
     */
    public void update() {
        if (flowReady && !map.isDirty()) {
            return;
        }
        
        calculateFlowField();
    }
    
    /**
     * Get the next step in the flow field from a given position.
     * 
     * @return TrailStep with next coordinates and height drop, or null if no path
     */
    public TrailStep getFlowStep(int x, int z, int currentHeight) {
        if (!flowReady || trailNextX == null || trailNextZ == null) {
            return null;
        }
        if (!map.isValid(x, z)) {
            return null;
        }
        
        int nx = trailNextX[x][z];
        int nz = trailNextZ[x][z];
        
        if (nx < 0 || nz < 0) {
            return null;
        }
        
        Tile next = map.getTile(nx, nz);
        if (next == null) {
            return null;
        }
        
        int heightDrop = currentHeight - next.getHeight();
        return new TrailStep(nx, nz, heightDrop);
    }
    
    public boolean isReady() {
        return flowReady;
    }
    
    /**
     * Calculate the flow field using BFS from base area.
     */
    private void calculateFlowField() {
        int width = map.getWidth();
        int depth = map.getDepth();
        
        trailDistance = new int[width][depth];
        trailNextX = new int[width][depth];
        trailNextZ = new int[width][depth];
        
        // Initialize arrays
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                trailDistance[x][z] = -1;
                trailNextX[x][z] = -1;
                trailNextZ[x][z] = -1;
            }
        }
        
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        int maxTrailZ = -1;
        boolean hasTrail = false;
        
        // Find max trail Z (lowest on mountain)
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                Tile tile = map.getTile(x, z);
                if (tile != null && tile.isTrail()) {
                    hasTrail = true;
                    if (z > maxTrailZ) {
                        maxTrailZ = z;
                    }
                }
            }
        }
        
        if (!hasTrail) {
            flowReady = false;
            return;
        }
        
        // Seed BFS from base area trails
        boolean seeded = false;
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                Tile tile = map.getTile(x, z);
                if (tile == null || !tile.isTrail()) {
                    continue;
                }
                if (z >= SkierSpawnerSystem.BASE_Z - 2) {
                    trailDistance[x][z] = 0;
                    queue.add(new int[] { x, z });
                    seeded = true;
                }
            }
        }
        
        // Fallback: seed from max Z if no base trails found
        if (!seeded && maxTrailZ >= 0) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    Tile tile = map.getTile(x, z);
                    if (tile == null || !tile.isTrail()) {
                        continue;
                    }
                    if (z == maxTrailZ) {
                        trailDistance[x][z] = 0;
                        queue.add(new int[] { x, z });
                    }
                }
            }
        }
        
        // BFS to calculate distances
        int[] dxs = { -1, 0, 1, -1, 1, -1, 0, 1 };
        int[] dzs = { -1, -1, -1, 0, 0, 1, 1, 1 };
        
        while (!queue.isEmpty()) {
            int[] cur = queue.removeFirst();
            int cx = cur[0];
            int cz = cur[1];
            int dist = trailDistance[cx][cz];
            
            for (int i = 0; i < dxs.length; i++) {
                int nx = cx + dxs[i];
                int nz = cz + dzs[i];
                
                if (!map.isValid(nx, nz)) {
                    continue;
                }
                if (trailDistance[nx][nz] != -1) {
                    continue;
                }
                
                Tile neighbor = map.getTile(nx, nz);
                if (neighbor == null || !neighbor.isTrail()) {
                    continue;
                }
                
                trailDistance[nx][nz] = dist + 1;
                queue.add(new int[] { nx, nz });
            }
        }
        
        // Calculate next steps (gradient descent)
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                if (trailDistance[x][z] <= 0) {
                    continue;
                }
                
                int bestNx = -1;
                int bestNz = -1;
                int bestDist = trailDistance[x][z];
                
                for (int i = 0; i < dxs.length; i++) {
                    int nx = x + dxs[i];
                    int nz = z + dzs[i];
                    
                    if (!map.isValid(nx, nz)) {
                        continue;
                    }
                    
                    int nDist = trailDistance[nx][nz];
                    if (nDist >= 0 && nDist < bestDist) {
                        bestDist = nDist;
                        bestNx = nx;
                        bestNz = nz;
                    }
                }
                
                if (bestNx != -1) {
                    trailNextX[x][z] = bestNx;
                    trailNextZ[x][z] = bestNz;
                }
            }
        }
        
        flowReady = true;
    }
    
    /**
     * Represents a single step in the flow field.
     */
    public static class TrailStep {
        public final int x;
        public final int z;
        public final int heightDrop;
        
        public TrailStep(int x, int z, int heightDrop) {
            this.x = x;
            this.z = z;
            this.heightDrop = heightDrop;
        }
    }
}

