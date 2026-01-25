package com.project.tycoon.ecs.systems.skier;

import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.ecs.components.VelocityComponent;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.TrailDifficulty;
import com.project.tycoon.world.model.WorldMap;

/**
 * Finds nearby trails and steers skiers toward them.
 */
public class TrailSeeker {
    
    private static final int SEARCH_RADIUS = 10;
    private static final float SEEK_SPEED = 3.5f;
    private static final float TURN_LERP = 0.18f;
    
    private final WorldMap map;
    
    public TrailSeeker(WorldMap map) {
        this.map = map;
    }
    
    /**
     * Seek trails matching the skier's preferred difficulty.
     * 
     * @return true if found preferred trail, false otherwise
     */
    public boolean seekPreferredTrail(TransformComponent pos, VelocityComponent vel, SkierComponent skier) {
        int x = (int) Math.floor(pos.x);
        int z = (int) Math.floor(pos.z);
        
        int bestX = -1;
        int bestZ = -1;
        float bestScore = -1;
        
        // Search for trails matching preferred difficulty
        for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                
                int nx = x + dx;
                int nz = z + dz;
                
                if (!map.isValid(nx, nz)) {
                    continue;
                }
                
                Tile tile = map.getTile(nx, nz);
                if (tile == null || !tile.isTrail()) {
                    continue;
                }
                
                // Calculate score based on preference and distance
                float preference = TrailPreferences.getPreference(skier.skillLevel, tile.getTrailDifficulty());
                float distSq = dx * dx + dz * dz;
                float score = preference / (1 + distSq * 0.1f); // Prefer close trails
                
                if (score > bestScore) {
                    bestScore = score;
                    bestX = nx;
                    bestZ = nz;
                }
            }
        }
        
        if (bestX == -1) {
            return false;
        }
        
        steerTowardTile(pos, vel, bestX, bestZ);
        return true;
    }
    
    /**
     * Steer toward the nearest trail tile (any difficulty).
     * 
     * @return true if trail found, false otherwise
     */
    public boolean seekNearestTrail(TransformComponent pos, VelocityComponent vel) {
        int x = (int) Math.floor(pos.x);
        int z = (int) Math.floor(pos.z);
        
        int bestX = -1;
        int bestZ = -1;
        float bestDistSq = Float.MAX_VALUE;
        
        for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                
                int nx = x + dx;
                int nz = z + dz;
                
                if (!map.isValid(nx, nz)) {
                    continue;
                }
                
                Tile tile = map.getTile(nx, nz);
                if (tile == null || !tile.isTrail()) {
                    continue;
                }
                
                float distSq = dx * dx + dz * dz;
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    bestX = nx;
                    bestZ = nz;
                }
            }
        }
        
        if (bestX == -1) {
            return false;
        }
        
        steerTowardTile(pos, vel, bestX, bestZ);
        return true;
    }
    
    /**
     * Steer toward a specific tile with smooth lerping.
     */
    private void steerTowardTile(TransformComponent pos, VelocityComponent vel, int tileX, int tileZ) {
        float targetX = tileX + 0.5f;
        float targetZ = tileZ + 0.5f;
        
        float dx = targetX - pos.x;
        float dz = targetZ - pos.z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        
        if (dist < 0.05f) {
            return;
        }
        
        float desiredDx = (dx / dist) * SEEK_SPEED;
        float desiredDz = (dz / dist) * SEEK_SPEED;
        
        vel.dx = lerp(vel.dx, desiredDx, TURN_LERP);
        vel.dz = lerp(vel.dz, desiredDz, TURN_LERP);
    }
    
    private float lerp(float from, float to, float alpha) {
        return from + (to - from) * alpha;
    }
}

