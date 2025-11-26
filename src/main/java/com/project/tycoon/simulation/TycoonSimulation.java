package com.project.tycoon.simulation;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.ecs.components.VelocityComponent;
import com.project.tycoon.ecs.systems.PhysicsSystem;
import com.project.tycoon.ecs.systems.SkierBehaviorSystem;
import com.project.tycoon.world.model.TerrainGenerator;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;

import java.util.Random;

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
        ecsEngine.addSystem(new PhysicsSystem(ecsEngine));
        
        spawnTestSkiers();
    }
    
    private void spawnTestSkiers() {
        Random rand = new Random();
        // Spawn at the "Peak" area (roughly x=128, z=250)
        for (int i = 0; i < 50; i++) {
            Entity skier = ecsEngine.createEntity();
            
            int startX = 128 + rand.nextInt(40) - 20;
            int startZ = 240 + rand.nextInt(10);
            
            // Clamp to map
            if (startX < 0) startX = 0; if (startX >= 256) startX = 255;
            if (startZ < 0) startZ = 0; if (startZ >= 256) startZ = 255;
            
            Tile t = worldMap.getTile(startX, startZ);
            float h = (t != null) ? t.getHeight() : 0;
            
            ecsEngine.addComponent(skier, new TransformComponent(startX, h, startZ));
            ecsEngine.addComponent(skier, new VelocityComponent(0, 0, 0));
            ecsEngine.addComponent(skier, new SkierComponent());
        }
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

