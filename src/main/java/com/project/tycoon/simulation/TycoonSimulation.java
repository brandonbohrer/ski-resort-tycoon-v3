package com.project.tycoon.simulation;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.systems.LiftSystem;
import com.project.tycoon.ecs.systems.PhysicsSystem;
import com.project.tycoon.ecs.systems.SkierBehaviorSystem;
import com.project.tycoon.ecs.systems.SkierSpawnerSystem;
import com.project.tycoon.world.model.TerrainGenerator;
import com.project.tycoon.world.model.WorldMap;

/**
 * The concrete simulation implementation for the Ski Resort Tycoon.
 * It holds the ECS engine and delegates ticks to it.
 */
public class TycoonSimulation implements Simulation {

    private final Engine ecsEngine;
    private final WorldMap worldMap;

    public TycoonSimulation() {
        this.ecsEngine = new Engine();
        // Increased resolution: 256x256 (4x total area)
        this.worldMap = new WorldMap(256, 256);

        // Generate Mountain Terrain
        TerrainGenerator.generateMountain(this.worldMap);

        // Register Systems
        ecsEngine.addSystem(new SkierBehaviorSystem(ecsEngine, worldMap));
        ecsEngine.addSystem(new LiftSystem(ecsEngine));
        ecsEngine.addSystem(new PhysicsSystem(ecsEngine));
        ecsEngine.addSystem(new SkierSpawnerSystem(ecsEngine, worldMap)); // Dynamic spawning
    }

    @Override
    public void tick(long tickNumber) {
        // For now, we assume 1 tick = fixed time step (e.g. 1/60s)
        // In a more advanced setup, we might pass the actual dt if variable
        double fixedDt = 1.0 / 60.0;
        ecsEngine.update(fixedDt);
    }

    public Engine getEcsEngine() {
        return ecsEngine;
    }

    public WorldMap getWorldMap() {
        return worldMap;
    }
}
