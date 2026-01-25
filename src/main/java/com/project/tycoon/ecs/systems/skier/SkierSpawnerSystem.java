package com.project.tycoon.ecs.systems.skier;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.System;
import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.SkillLevel;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.ecs.components.VelocityComponent;
import com.project.tycoon.simulation.VisitorManager;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages skier population - spawns skiers at the base and despawns them when
 * finished.
 */
public class SkierSpawnerSystem implements System {

    private final Engine engine;
    private final WorldMap worldMap;
    private VisitorManager visitorManager; // Injected after construction

    // Spawning configuration
    private static final int TARGET_POPULATION = 75;
    private static final int SPAWN_X = 128; // Center of map
    public static final int BASE_Z = 250; // Bottom of map (base lodge)
    private static final float SPAWN_INTERVAL = 2.0f; // seconds between spawns
    private static final int SPAWN_SPREAD = 5; // Random spread around spawn point

    private float timeSinceLastSpawn = 0.0f;

    public SkierSpawnerSystem(Engine engine, WorldMap worldMap) {
        this.engine = engine;
        this.worldMap = worldMap;
    }

    /**
     * Set the visitor manager (called by TycoonSimulation after construction).
     */
    public void setVisitorManager(VisitorManager visitorManager) {
        this.visitorManager = visitorManager;
    }

    @Override
    public void update(double dt) {
        // Count current skiers
        int skierCount = countSkiers();

        // Despawn finished skiers
        despawnFinishedSkiers();

        // Spawn new skiers if below target AND within visitor cap
        timeSinceLastSpawn += dt;
        if (skierCount < TARGET_POPULATION && timeSinceLastSpawn >= SPAWN_INTERVAL) {
            // Check if we can spawn more visitors today
            if (visitorManager != null && visitorManager.canSpawnVisitor()) {
                spawnSkier();
                visitorManager.recordVisitorSpawned();
                timeSinceLastSpawn = 0.0f;
            }
        }
    }

    private int countSkiers() {
        int count = 0;
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, SkierComponent.class)) {
                count++;
            }
        }
        return count;
    }

    private void despawnFinishedSkiers() {
        List<Entity> toRemove = new ArrayList<>();

        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, SkierComponent.class)) {
                SkierComponent skier = engine.getComponent(entity, SkierComponent.class);
                if (skier.state == SkierComponent.State.FINISHED) {
                    toRemove.add(entity);
                }
            }
        }

        // Remove entities (avoid concurrent modification)
        for (Entity entity : toRemove) {
            engine.removeEntity(entity);
        }
    }

    private void spawnSkier() {
        // Random position around spawn point
        java.util.Random rand = new java.util.Random();
        int spawnX = SPAWN_X + rand.nextInt(SPAWN_SPREAD * 2) - SPAWN_SPREAD;
        int spawnZ = BASE_Z + rand.nextInt(SPAWN_SPREAD * 2) - SPAWN_SPREAD;

        // Clamp to map bounds
        spawnX = Math.max(0, Math.min(spawnX, worldMap.getWidth() - 1));
        spawnZ = Math.max(0, Math.min(spawnZ, worldMap.getDepth() - 1));

        // Get terrain height at spawn
        Tile tile = worldMap.getTile(spawnX, spawnZ);
        float height = (tile != null) ? tile.getHeight() : 0;

        // Create skier entity
        Entity skier = engine.createEntity();
        engine.addComponent(skier, new TransformComponent(spawnX, height, spawnZ));
        engine.addComponent(skier, new VelocityComponent(0, 0, 0));

        SkierComponent skierComp = new SkierComponent();
        skierComp.state = SkierComponent.State.WAITING;
        skierComp.skillLevel = SkillLevel.randomSkill(rand); // Use realistic distribution
        skierComp.satisfaction = 50.0f; // Start neutral
        engine.addComponent(skier, skierComp);
    }
}
