package com.project.tycoon.simulation;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.systems.core.PhysicsSystem;
import com.project.tycoon.ecs.systems.lift.LiftSystem;
import com.project.tycoon.ecs.systems.skier.SkierBehaviorSystem;
import com.project.tycoon.ecs.systems.skier.SkierPhysicsSystem;
import com.project.tycoon.ecs.systems.skier.SkierSpawnerSystem;
import com.project.tycoon.economy.EconomyManager;
import com.project.tycoon.world.model.TerrainGenerator;
import com.project.tycoon.world.model.WorldMap;

/**
 * The concrete simulation implementation for the Ski Resort Tycoon.
 * It holds the ECS engine and delegates ticks to it.
 */
public class TycoonSimulation implements Simulation {

    private final Engine ecsEngine;
    private final WorldMap worldMap;
    private final EconomyManager economyManager;
    private boolean paused = false;

    public TycoonSimulation() {
        this.ecsEngine = new Engine();
        // Increased resolution: 256x256 (4x total area)
        this.worldMap = new WorldMap(256, 256);
        this.economyManager = new EconomyManager();

        // Generate Mountain Terrain
        TerrainGenerator.generateMountain(this.worldMap);

        // Register Systems
        ecsEngine.addSystem(new PhysicsSystem(ecsEngine, worldMap)); // General physics
        ecsEngine.addSystem(new SkierPhysicsSystem(ecsEngine, worldMap)); // Skiing slope physics
        ecsEngine.addSystem(new SkierBehaviorSystem(ecsEngine, worldMap)); // Skier AI/behavior
        ecsEngine.addSystem(new LiftSystem(ecsEngine, economyManager)); // Lift operations
        ecsEngine.addSystem(new SkierSpawnerSystem(ecsEngine, worldMap)); // Dynamic spawning
    }

    @Override
    public void tick(long tickNumber) {
        // Skip updates when paused
        if (paused) {
            return;
        }

        // For now, we assume 1 tick = fixed time step (e.g. 1/60s)
        // In a more advanced setup, we might pass the actual dt if variable
        double fixedDt = 1.0 / 60.0;
        ecsEngine.update(fixedDt);
        economyManager.update(fixedDt);
    }

    public Engine getEcsEngine() {
        return ecsEngine;
    }

    public WorldMap getWorldMap() {
        return worldMap;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        System.out.println("Simulation " + (paused ? "paused" : "unpaused"));
    }

    public boolean isPaused() {
        return paused;
    }
}
