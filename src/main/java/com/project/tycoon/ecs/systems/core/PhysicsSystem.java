package com.project.tycoon.ecs.systems.core;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.System;
import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.ecs.components.VelocityComponent;
import com.project.tycoon.world.model.WorldMap;

/**
 * Updates entity positions based on velocity.
 * Note: Friction is NOT applied to skiers, as SkierBehaviorSystem manages their
 * physics.
 */
public class PhysicsSystem implements System {

    private final Engine engine;
    private final WorldMap worldMap;

    public PhysicsSystem(Engine engine, WorldMap worldMap) {
        this.engine = engine;
        this.worldMap = worldMap;
    }

    @Override
    public void update(double dt) {
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, TransformComponent.class) &&
                    engine.hasComponent(entity, VelocityComponent.class)) {

                TransformComponent pos = engine.getComponent(entity, TransformComponent.class);
                VelocityComponent vel = engine.getComponent(entity, VelocityComponent.class);

                // Update Position
                pos.x += vel.dx * dt;
                pos.y += vel.dy * dt;
                pos.z += vel.dz * dt;

                // Clamp position to map bounds to prevent out-of-bounds errors
                pos.x = Math.max(0, Math.min(pos.x, worldMap.getWidth() - 1));
                pos.z = Math.max(0, Math.min(pos.z, worldMap.getDepth() - 1));
                // Y is clamped to terrain height by other systems

                // Apply Ground Friction (simplified)
                // Skiers have their own physics in SkierBehaviorSystem, so skip friction for
                // them
                if (!engine.hasComponent(entity, SkierComponent.class)) {
                    vel.dx *= 0.98f;
                    vel.dz *= 0.98f;
                    vel.dy *= 0.98f; // Vertical damping (though we usually snap to ground)
                }
            }
        }
    }
}
