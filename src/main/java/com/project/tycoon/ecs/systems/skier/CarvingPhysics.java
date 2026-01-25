package com.project.tycoon.ecs.systems.skier;

import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.ecs.components.VelocityComponent;
import com.project.tycoon.world.model.WorldMap;

/**
 * Handles realistic carving skiing physics with S-turns.
 */
public class CarvingPhysics {
    
    private static final float BASE_SKI_SPEED = 4.0f;
    private static final float MAX_SKI_SPEED = 10.0f;
    private static final float TURN_LERP = 0.18f;
    
    private final WorldMap map;
    private final TrailFlowFieldCalculator flowField;
    private final TrailScanner trailScanner;
    private final TrailSeeker trailSeeker;
    
    public CarvingPhysics(WorldMap map, TrailFlowFieldCalculator flowField, 
                          TrailScanner trailScanner, TrailSeeker trailSeeker) {
        this.map = map;
        this.flowField = flowField;
        this.trailScanner = trailScanner;
        this.trailSeeker = trailSeeker;
    }
    
    /**
     * Apply carving physics to a skier on a trail.
     */
    public void applyCarving(SkierComponent skier, TransformComponent pos, VelocityComponent vel, double dt) {
        // Update carving phase (advance through turn cycle)
        skier.carvingPhase += (float) dt * skier.carvingSpeed;
        
        // Calculate current carving direction (sine wave for smooth S-turns)
        skier.carvingDirection = (float) Math.sin(skier.carvingPhase);
        
        // Get trail info at current position
        TrailScanner.TrailInfo trailInfo = trailScanner.scan(pos.x, pos.z);
        
        if (!trailInfo.onTrail) {
            // Lost trail, seek back
            trailSeeker.seekNearestTrail(pos, vel);
            return;
        }
        
        // Get downhill direction from flow field
        int x = (int) Math.floor(pos.x);
        int z = (int) Math.floor(pos.z);
        TrailFlowFieldCalculator.TrailStep next = flowField.getFlowStep(x, z, map.getTile(x, z).getHeight());
        
        if (next == null) {
            // No flow, seek trail
            trailSeeker.seekNearestTrail(pos, vel);
            return;
        }
        
        // Calculate lateral offset from centerline based on carving direction
        float lateralShift = skier.carvingDirection * (trailInfo.trailWidth * 0.35f);
        
        // Target position combines downhill + lateral movement
        float targetX = trailInfo.centerlineX + lateralShift;
        float targetZ = next.z + 0.5f;
        
        // Keep within trail boundaries
        targetX = Math.max(trailInfo.leftEdge + 0.5f, Math.min(trailInfo.rightEdge - 0.5f, targetX));
        
        // Steer toward target
        float dx = targetX - pos.x;
        float dz = targetZ - pos.z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        
        if (dist < 0.05f) {
            return;
        }
        
        // Calculate speed based on skill and turn sharpness
        float turnSharpness = Math.abs(skier.carvingDirection);
        float speedMultiplier = 1.0f - (turnSharpness * 0.3f); // Slow down 30% in sharp turns
        
        float skillMultiplier = skier.skillLevel.ordinal() * 0.8f;
        float baseSpeed = BASE_SKI_SPEED + skillMultiplier;
        float speed = baseSpeed * speedMultiplier;
        speed = Math.min(speed, MAX_SKI_SPEED);
        
        // Apply velocity
        float desiredDx = (dx / dist) * speed;
        float desiredDz = (dz / dist) * speed;
        
        vel.dx = lerp(vel.dx, desiredDx, TURN_LERP);
        vel.dz = lerp(vel.dz, desiredDz, TURN_LERP);
    }
    
    private float lerp(float from, float to, float alpha) {
        return from + (to - from) * alpha;
    }
}

