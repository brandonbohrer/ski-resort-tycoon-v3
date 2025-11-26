package com.project.tycoon.ecs.systems;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.System;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.ecs.components.VelocityComponent;

/**
 * Updates entity positions based on velocity.
 */
public class PhysicsSystem implements System {

    private final Engine engine;

    public PhysicsSystem(Engine engine) {
        this.engine = engine;
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
                
                // Apply Ground Friction (simplified)
                // Reduce velocity slightly every frame
                vel.dx *= 0.98f;
                vel.dz *= 0.98f;
                vel.dy *= 0.98f; // Vertical damping (though we usually snap to ground)
            }
        }
    }
}

