package com.project.tycoon.ecs.systems.skier;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.System;
import com.project.tycoon.ecs.components.LiftComponent;
import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.ecs.components.VelocityComponent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Simplified navigation system for skiers at base area.
 * Handles walking from spawn/base to lift bases.
 */
public class SkierNavigationSystem implements System {

    private final Engine engine;
    private static final float NAVIGATION_SPEED = 3.0f;
    private static final float LIFT_DETECTION_RADIUS = 15.0f;

    public SkierNavigationSystem(Engine engine, com.project.tycoon.world.SnapPointManager snapPointManager) {
        this.engine = engine;
        // snapPointManager kept for API compatibility but not used
    }

    @Override
    public void update(double dt) {
        for (Entity entity : engine.getEntities()) {
            if (!engine.hasComponent(entity, SkierComponent.class)) {
                continue;
            }

            SkierComponent skier = engine.getComponent(entity, SkierComponent.class);
            TransformComponent pos = engine.getComponent(entity, TransformComponent.class);
            VelocityComponent vel = engine.getComponent(entity, VelocityComponent.class);

            if (pos == null || vel == null) {
                continue;
            }

            // Only handle WAITING state - walking to lifts at base
            if (skier.state == SkierComponent.State.WAITING) {
                updateWaitingNavigation(skier, pos, vel);
            }
        }
    }

    /**
     * Handle navigation for skiers in WAITING state.
     * Simply walk directly toward target lift if at base, or force back to skiing if mid-mountain.
     */
    private void updateWaitingNavigation(SkierComponent skier, TransformComponent pos, VelocityComponent vel) {
        // If skier is already near a lift, let LiftSystem handle them
        if (isNearAnyLift(pos)) {
            vel.dx = 0;
            vel.dz = 0;
            return;
        }
        
        // Only allow walking navigation from BASE area
        if (pos.z < SkierSpawnerSystem.BASE_Z - 20) {
            // Mid-mountain! Force back to skiing
            vel.dx = 0;
            vel.dz = 0;
            skier.state = SkierComponent.State.SKIING;
            return;
        }
        
        // Walk directly toward target lift
        if (skier.targetLiftId != null) {
            Entity targetLift = findLiftById(skier.targetLiftId);
            if (targetLift != null) {
                TransformComponent liftPos = engine.getComponent(targetLift, TransformComponent.class);
                if (liftPos != null) {
                    // Walk toward target lift
                    float dx = liftPos.x - pos.x;
                    float dz = liftPos.z - pos.z;
                    float distance = (float) Math.sqrt(dx * dx + dz * dz);
                    
                    if (distance > 0.5f) {
                        vel.dx = (dx / distance) * NAVIGATION_SPEED;
                        vel.dz = (dz / distance) * NAVIGATION_SPEED;
                    } else {
                        vel.dx = 0;
                        vel.dz = 0;
                    }
                    return;
                }
            }
        }
        
        // No target lift - walk toward nearest lift
        Entity nearestLift = findNearestLift(pos);
        if (nearestLift != null) {
            TransformComponent liftPos = engine.getComponent(nearestLift, TransformComponent.class);
            if (liftPos != null) {
                float dx = liftPos.x - pos.x;
                float dz = liftPos.z - pos.z;
                float distance = (float) Math.sqrt(dx * dx + dz * dz);
                
                if (distance > 0.5f) {
                    vel.dx = (dx / distance) * NAVIGATION_SPEED;
                    vel.dz = (dz / distance) * NAVIGATION_SPEED;
                } else {
                    vel.dx = 0;
                    vel.dz = 0;
                }
                return;
            }
        }
        
        // No lifts found - stop
        vel.dx = 0;
        vel.dz = 0;
    }

    private boolean isNearAnyLift(TransformComponent skierPos) {
        Set<UUID> hasIncoming = new HashSet<>();
        
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, LiftComponent.class)) {
                LiftComponent lift = engine.getComponent(entity, LiftComponent.class);
                if (lift.nextPylonId != null) {
                    hasIncoming.add(lift.nextPylonId);
                }
            }
        }
        
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, LiftComponent.class) &&
                    !hasIncoming.contains(entity.getId())) {
                
                TransformComponent liftPos = engine.getComponent(entity, TransformComponent.class);
                if (liftPos == null) continue;
                
                float dx = liftPos.x - skierPos.x;
                float dz = liftPos.z - skierPos.z;
                float distance = (float) Math.sqrt(dx * dx + dz * dz);
                
                if (distance < LIFT_DETECTION_RADIUS) {
                    return true;
                }
            }
        }
        
        return false;
    }

    private Entity findLiftById(UUID liftId) {
        for (Entity entity : engine.getEntities()) {
            if (entity.getId().equals(liftId)) {
                return entity;
            }
        }
        return null;
    }

    private Entity findNearestLift(TransformComponent pos) {
        Set<UUID> hasIncoming = new HashSet<>();
        
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, LiftComponent.class)) {
                LiftComponent lift = engine.getComponent(entity, LiftComponent.class);
                if (lift.nextPylonId != null) {
                    hasIncoming.add(lift.nextPylonId);
                }
            }
        }
        
        Entity nearest = null;
        float nearestDist = Float.MAX_VALUE;
        
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, LiftComponent.class) &&
                    !hasIncoming.contains(entity.getId())) {
                
                TransformComponent liftPos = engine.getComponent(entity, TransformComponent.class);
                if (liftPos == null) continue;
                
                float dx = liftPos.x - pos.x;
                float dz = liftPos.z - pos.z;
                float distance = (float) Math.sqrt(dx * dx + dz * dz);
                
                if (distance < nearestDist) {
                    nearestDist = distance;
                    nearest = entity;
                }
            }
        }
        
        return nearest;
    }
}
