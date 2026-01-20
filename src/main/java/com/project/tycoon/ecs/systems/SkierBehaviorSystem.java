package com.project.tycoon.ecs.systems;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.System;
import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.ecs.components.VelocityComponent;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;

/**
 * Applies Skiing logic (Gravity on slopes).
 */
public class SkierBehaviorSystem implements System {

    private final Engine engine;
    private final WorldMap map;

    private static final float GRAVITY = 5.0f;

    public SkierBehaviorSystem(Engine engine, WorldMap map) {
        this.engine = engine;
        this.map = map;
    }

    @Override
    public void update(double dt) {
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, SkierComponent.class) &&
                    engine.hasComponent(entity, TransformComponent.class) &&
                    engine.hasComponent(entity, VelocityComponent.class)) {

                SkierComponent skier = engine.getComponent(entity, SkierComponent.class);
                TransformComponent pos = engine.getComponent(entity, TransformComponent.class);
                VelocityComponent vel = engine.getComponent(entity, VelocityComponent.class);

                // Handle different states
                if (skier.state == SkierComponent.State.WAITING) {
                    handleWaitingState(skier, pos, vel);
                } else if (skier.state == SkierComponent.State.SKIING) {
                    applySlopePhysics(pos, vel);

                    // Check if reached bottom (finish line)
                    if (pos.z < 20) {
                        skier.state = SkierComponent.State.FINISHED;
                    }
                }
                // QUEUED and RIDING_LIFT states are handled by LiftSystem
            }
        }
    }

    /**
     * Handle WAITING state - find and move toward nearest lift.
     */
    private void handleWaitingState(SkierComponent skier, TransformComponent pos, VelocityComponent vel) {
        // Find nearest lift base
        Entity nearestLift = findNearestLiftBase(pos);

        if (nearestLift != null) {
            skier.targetLiftId = nearestLift.getId();
            moveTowardLift(pos, vel, nearestLift);
        } else {
            // No lift found, stop moving
            vel.dx = 0;
            vel.dz = 0;
        }
    }

    /**
     * Find the nearest lift base to the skier.
     */
    private Entity findNearestLiftBase(TransformComponent skierPos) {
        Entity nearest = null;
        float minDistance = Float.MAX_VALUE;

        // Find all lift base entities (pylons with no incoming links)
        java.util.Set<java.util.UUID> hasIncoming = new java.util.HashSet<>();

        // First pass: identify all pylons that are pointed to
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, com.project.tycoon.ecs.components.LiftComponent.class)) {
                com.project.tycoon.ecs.components.LiftComponent lift = engine.getComponent(entity,
                        com.project.tycoon.ecs.components.LiftComponent.class);
                if (lift.nextPylonId != null) {
                    hasIncoming.add(lift.nextPylonId);
                }
            }
        }

        // Second pass: find nearest base (pylon with no incoming link)
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, com.project.tycoon.ecs.components.LiftComponent.class) &&
                    !hasIncoming.contains(entity.getId())) {

                TransformComponent liftPos = engine.getComponent(entity, TransformComponent.class);
                if (liftPos == null)
                    continue;

                float dx = liftPos.x - skierPos.x;
                float dz = liftPos.z - skierPos.z;
                float distance = (float) Math.sqrt(dx * dx + dz * dz);

                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = entity;
                }
            }
        }

        return nearest;
    }

    /**
     * Move skier toward the target lift.
     */
    private void moveTowardLift(TransformComponent pos, VelocityComponent vel, Entity liftEntity) {
        TransformComponent liftPos = engine.getComponent(liftEntity, TransformComponent.class);
        if (liftPos == null)
            return;

        // Calculate direction to lift
        float dx = liftPos.x - pos.x;
        float dz = liftPos.z - pos.z;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);

        // If already close enough, stop moving
        if (distance < 2.0f) {
            vel.dx = 0;
            vel.dz = 0;
            return;
        }

        // Move toward lift at constant speed
        float speed = 3.0f;
        vel.dx = (dx / distance) * speed;
        vel.dz = (dz / distance) * speed;
    }

    private void applySlopePhysics(TransformComponent pos, VelocityComponent vel) {
        int x = (int) pos.x;
        int z = (int) pos.z;

        if (!map.isValid(x, z))
            return;

        // Calculate Local Gradient
        // Compare height with neighbors to find "Downhill" direction
        Tile current = map.getTile(x, z);

        // Sample neighbors
        // We look at X+1 and Z+1 to get a rough gradient vector
        Tile right = map.getTile(x + 1, z);
        Tile down = map.getTile(x, z + 1);

        float h = current.getHeight();
        float hRight = (right != null) ? right.getHeight() : h;
        float hDown = (down != null) ? down.getHeight() : h;

        // Gradient: Negative means downhill
        float dx = hRight - h;
        float dz = hDown - h;

        // Apply forces downhill
        // If dx is negative (right is lower), we accelerate positive X
        // Force is proportional to steepness

        if (dx < 0)
            vel.dx += (-dx * GRAVITY * 0.016); // Accelerate Right
        else if (dx > 0)
            vel.dx -= (dx * GRAVITY * 0.016); // Accelerate Left (slide back)

        if (dz < 0)
            vel.dz += (-dz * GRAVITY * 0.016); // Accelerate Down
        else if (dz > 0)
            vel.dz -= (dz * GRAVITY * 0.016); // Accelerate Up

        // Snap Y to terrain height (simple collision)
        pos.y = h;
    }
}
