package com.project.tycoon.ecs.systems.skier;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.System;
import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.ecs.components.VelocityComponent;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;

/**
 * Applies slope-based physics to skiing entities.
 * Calculates terrain gradients and applies gravity-based acceleration downhill.
 */
public class SkierPhysicsSystem implements System {

    private final Engine engine;
    private final WorldMap map;

    private static final float GRAVITY = 5.0f;

    public SkierPhysicsSystem(Engine engine, WorldMap map) {
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

                // Only apply physics to skiing entities
                if (skier.state == SkierComponent.State.SKIING) {
                    TransformComponent pos = engine.getComponent(entity, TransformComponent.class);
                    VelocityComponent vel = engine.getComponent(entity, VelocityComponent.class);
                    applySlopePhysics(pos, vel, dt);
                }
            }
        }
    }

    /**
     * Apply slope-based physics to skier based on terrain gradient.
     */
    private void applySlopePhysics(TransformComponent pos, VelocityComponent vel, double dt) {
        int x = (int) pos.x;
        int z = (int) pos.z;

        if (!map.isValid(x, z))
            return;

        // Calculate Local Gradient
        // Compare height with neighbors to find "Downhill" direction
        Tile current = map.getTile(x, z);

        // Sample neighbors in ALL directions for accurate gradient (central difference)
        Tile left = map.getTile(x - 1, z);
        Tile right = map.getTile(x + 1, z);
        Tile up = map.getTile(x, z - 1);
        Tile down = map.getTile(x, z + 1);

        float h = current.getHeight();
        float hLeft = (left != null) ? left.getHeight() : h;
        float hRight = (right != null) ? right.getHeight() : h;
        float hUp = (up != null) ? up.getHeight() : h;
        float hDown = (down != null) ? down.getHeight() : h;

        // Central difference gradient (more accurate and bidirectional)
        // Positive gradient = uphill in that direction
        float gradientX = (hRight - hLeft) / 2.0f;
        float gradientZ = (hDown - hUp) / 2.0f;

        // Apply forces downhill (opposite of gradient direction)
        // Negative gradient = downhill = accelerate in that direction
        float delta = (float) dt;
        vel.dx -= gradientX * GRAVITY * delta;
        vel.dz -= gradientZ * GRAVITY * delta;

        // Snap Y to terrain height (simple collision)
        pos.y = h;
    }
}
