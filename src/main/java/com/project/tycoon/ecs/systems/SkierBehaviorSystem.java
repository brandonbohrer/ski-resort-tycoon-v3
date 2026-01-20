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
                if (skier.state == SkierComponent.State.SKIING) {
                    applySlopePhysics(pos, vel);

                    // Check if reached bottom (finish line)
                    if (pos.z < 20) {
                        skier.state = SkierComponent.State.FINISHED;
                    }
                }
                // TODO: Handle other states (WAITING, QUEUED, RIDING_LIFT) in future phases
            }
        }
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
