package com.project.tycoon.ecs.systems.skier;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.components.LiftComponent;
import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.SkillLevel;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.world.SnapPointManager;
import com.project.tycoon.world.model.SnapPoint;
import com.project.tycoon.world.model.TrailDifficulty;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;

import java.util.*;

/**
 * Plans which lift a skier should target next based on their skill level
 * and available terrain.
 */
public class LiftPlanner {
    
    private final Engine engine;
    private final WorldMap worldMap;
    private final Random random = new Random();
    
    public LiftPlanner(Engine engine, SnapPointManager snapPointManager, WorldMap worldMap) {
        this.engine = engine;
        this.worldMap = worldMap;
        // snapPointManager kept for future use if needed
    }
    
    /**
     * Choose the next lift for a skier to target based on their skill level.
     * Called when a skier exits a lift and starts skiing.
     * 
     * @param skier The skier component
     * @param currentPos The skier's current position
     * @return UUID of the target lift base entity, or null if no suitable lift found
     */
    public UUID chooseNextLift(SkierComponent skier, TransformComponent currentPos) {
        // Find all available lift bases
        List<LiftCandidate> candidates = findLiftCandidates(currentPos);
        
        if (candidates.isEmpty()) {
            java.lang.System.out.println("📋 PLANNER: No lift candidates found");
            return null;
        }
        
        // Score each lift based on skier preferences
        LiftCandidate bestLift = null;
        float bestScore = -1f;
        
        for (LiftCandidate candidate : candidates) {
            float score = scoreLift(candidate, skier);
            java.lang.System.out.println("📋 PLANNER: Lift at (" + Math.round(candidate.position.x) + "," + 
                                         Math.round(candidate.position.z) + ") score=" + score);
            
            if (score > bestScore) {
                bestScore = score;
                bestLift = candidate;
            }
        }
        
        if (bestLift != null) {
            java.lang.System.out.println("🎯 PLANNER: Skier targeting lift at (" + 
                                         Math.round(bestLift.position.x) + "," + 
                                         Math.round(bestLift.position.z) + ")");
            return bestLift.liftEntityId;
        }
        
        return null;
    }
    
    /**
     * Find all lift bases that could be reached.
     */
    private List<LiftCandidate> findLiftCandidates(TransformComponent currentPos) {
        List<LiftCandidate> candidates = new ArrayList<>();
        
        // Find all lift base entities (first pylon in each chain - no other pylon points to them)
        Set<UUID> hasIncomingLink = new HashSet<>();
        
        // First pass: identify all pylons that have something pointing to them
        for (Entity entity : engine.getEntities()) {
            if (!engine.hasComponent(entity, LiftComponent.class)) {
                continue;
            }
            
            LiftComponent lift = engine.getComponent(entity, LiftComponent.class);
            if (lift.nextPylonId != null) {
                hasIncomingLink.add(lift.nextPylonId);
            }
        }
        
        // Second pass: find bases (pylons with no incoming links) and assess them
        for (Entity entity : engine.getEntities()) {
            if (!engine.hasComponent(entity, LiftComponent.class)) {
                continue;
            }
            
            // Only consider bases (no incoming link)
            if (hasIncomingLink.contains(entity.getId())) {
                continue;
            }
            
            TransformComponent liftPos = engine.getComponent(entity, TransformComponent.class);
            if (liftPos == null) {
                continue;
            }
            
            // Calculate terrain difficulty near this lift base
            TrailDifficulty nearbyDifficulty = assessNearbyTrailDifficulty(liftPos);
            
            candidates.add(new LiftCandidate(
                entity.getId(),
                liftPos,
                nearbyDifficulty,
                calculateDistance(currentPos, liftPos)
            ));
        }
        
        return candidates;
    }
    
    /**
     * Score a lift based on how well it matches the skier's preferences.
     */
    private float scoreLift(LiftCandidate candidate, SkierComponent skier) {
        float score = 0f;
        
        // 1. Trail difficulty match (most important)
        if (candidate.nearbyDifficulty != null) {
            float difficultyPreference = TrailPreferences.getPreference(
                skier.skillLevel, 
                candidate.nearbyDifficulty
            );
            score += difficultyPreference * 10f; // Weight heavily
        }
        
        // 2. Distance penalty (prefer closer lifts, but not too heavily)
        // Normalize distance so it doesn't dominate the score
        float distancePenalty = Math.min(candidate.distance / 100f, 1.0f); // Cap at 1.0
        score -= distancePenalty * 2f;
        
        // 3. Add some randomness for variety (small factor)
        score += random.nextFloat() * 0.5f;
        
        return score;
    }
    
    /**
     * Check what trail difficulty is near a lift base by sampling tiles.
     */
    private TrailDifficulty assessNearbyTrailDifficulty(TransformComponent pos) {
        int x = (int) Math.floor(pos.x);
        int z = (int) Math.floor(pos.z);
        
        // Count trail difficulties in a radius
        Map<TrailDifficulty, Integer> difficultyCounts = new HashMap<>();
        
        for (int dz = -15; dz <= 15; dz++) {
            for (int dx = -15; dx <= 15; dx++) {
                int nx = x + dx;
                int nz = z + dz;
                
                if (!worldMap.isValid(nx, nz)) {
                    continue;
                }
                
                Tile tile = worldMap.getTile(nx, nz);
                if (tile != null && tile.isTrail()) {
                    TrailDifficulty diff = tile.getTrailDifficulty();
                    difficultyCounts.put(diff, difficultyCounts.getOrDefault(diff, 0) + 1);
                }
            }
        }
        
        // Return the most common difficulty
        TrailDifficulty mostCommon = null;
        int maxCount = 0;
        for (Map.Entry<TrailDifficulty, Integer> entry : difficultyCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostCommon = entry.getKey();
            }
        }
        
        return mostCommon;
    }
    
    private float calculateDistance(TransformComponent a, TransformComponent b) {
        float dx = b.x - a.x;
        float dz = b.z - a.z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * Internal class to hold lift candidate information.
     */
    private static class LiftCandidate {
        UUID liftEntityId;
        TransformComponent position;
        TrailDifficulty nearbyDifficulty;
        float distance;
        
        LiftCandidate(UUID liftEntityId, TransformComponent position, 
                     TrailDifficulty nearbyDifficulty, float distance) {
            this.liftEntityId = liftEntityId;
            this.position = position;
            this.nearbyDifficulty = nearbyDifficulty;
            this.distance = distance;
        }
    }
}

