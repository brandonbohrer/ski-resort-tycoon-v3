package com.project.tycoon.ecs.systems.skier;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.components.LiftComponent;
import com.project.tycoon.ecs.components.TransformComponent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Detects when skiers are near lift bases for boarding.
 */
public class LiftProximityDetector {
    
    private static final float DETECTION_RADIUS = 8.0f; // Must match LiftSystem.QUEUE_DETECTION_RADIUS
    
    private final Engine engine;
    
    public LiftProximityDetector(Engine engine) {
        this.engine = engine;
    }
    
    /**
     * Check if a skier is near any lift base.
     * 
     * @param skierPos The skier's position
     * @return true if within detection radius of a lift base
     */
    public boolean isNearLiftBase(TransformComponent skierPos) {
        // Find all lift base entities (pylons with no incoming links)
        Set<UUID> hasIncoming = new HashSet<>();
        
        // First pass: identify all pylons that are pointed to
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, LiftComponent.class)) {
                LiftComponent lift = engine.getComponent(entity, LiftComponent.class);
                if (lift.nextPylonId != null) {
                    hasIncoming.add(lift.nextPylonId);
                }
            }
        }
        
        // Second pass: check if skier is near any base (pylon with no incoming link)
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, LiftComponent.class) &&
                    !hasIncoming.contains(entity.getId())) {
                
                TransformComponent liftPos = engine.getComponent(entity, TransformComponent.class);
                if (liftPos == null) {
                    continue;
                }
                
                float dx = liftPos.x - skierPos.x;
                float dz = liftPos.z - skierPos.z;
                float distance = (float) Math.sqrt(dx * dx + dz * dz);
                
                if (distance < DETECTION_RADIUS) {
                    return true;
                }
            }
        }
        
        return false;
    }
}

