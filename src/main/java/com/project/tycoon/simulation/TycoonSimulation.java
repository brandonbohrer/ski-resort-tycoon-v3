package com.project.tycoon.simulation;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.components.BaseCampComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.ecs.systems.core.PhysicsSystem;
import com.project.tycoon.ecs.systems.lift.LiftSystem;
import com.project.tycoon.ecs.systems.skier.SkierBehaviorSystem;
import com.project.tycoon.ecs.systems.skier.SkierPhysicsSystem;
import com.project.tycoon.ecs.systems.skier.SkierSpawnerSystem;
import com.project.tycoon.economy.EconomyManager;
import com.project.tycoon.world.SnapPointManager;
import com.project.tycoon.world.model.BaseCampLocation;
import com.project.tycoon.world.model.SnapPoint;
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
    private final DayTimeSystem dayTimeSystem;
    private final VisitorManager visitorManager;
    private final SnapPointManager snapPointManager;
    private final BaseCampLocation baseCampLocation;

    private boolean paused = false;
    private float timeScale = 1.0f; // 1x, 2x, or 3x speed

    public TycoonSimulation() {
        this.ecsEngine = new Engine();
        // Increased resolution: 256x256 (4x total area)
        this.worldMap = new WorldMap(256, 256);
        this.economyManager = new EconomyManager();

        // Generate Mountain Terrain with random seed
        long seed = System.currentTimeMillis();
        System.out.println("Generating map with seed: " + seed);
        this.baseCampLocation = TerrainGenerator.generateMountain(this.worldMap, seed);

        // Initialize managers
        this.dayTimeSystem = new DayTimeSystem();
        this.visitorManager = new VisitorManager();
        this.snapPointManager = new SnapPointManager();

        // Create base camp at optimal location
        createBaseCamp();

        // Setup day transition listeners
        dayTimeSystem.setDayTransitionListener(new DayTimeSystem.DayTransitionListener() {
            @Override
            public void onDayEnd(int dayNumber) {
                // Apply end-of-day revenue
                float revenue = economyManager.endOfDay();
                System.out.println("Day " + dayNumber + " ended. Revenue: $" + String.format("%.2f", revenue));

                // Update visitor stats
                visitorManager.endOfDay();
            }

            @Override
            public void onDayStart(int dayNumber) {
                visitorManager.startNewDay();
            }
        });

        // Register Systems
        ecsEngine.addSystem(new PhysicsSystem(ecsEngine, worldMap)); // General physics
        ecsEngine.addSystem(new SkierPhysicsSystem(ecsEngine, worldMap)); // Skiing slope physics
        ecsEngine.addSystem(new com.project.tycoon.ecs.systems.skier.SkierNavigationSystem(ecsEngine, snapPointManager)); // High-level navigation
        ecsEngine.addSystem(new SkierBehaviorSystem(ecsEngine, worldMap)); // Skier AI/behavior
        ecsEngine.addSystem(new LiftSystem(ecsEngine, economyManager)); // Lift operations

        SkierSpawnerSystem spawnerSystem = new SkierSpawnerSystem(ecsEngine, worldMap);
        spawnerSystem.setVisitorManager(visitorManager); // Inject visitor manager
        ecsEngine.addSystem(spawnerSystem);
    }

    @Override
    public void tick(long tickNumber) {
        // Skip updates when paused
        if (paused) {
            return;
        }

        // Fixed time step with speed scaling
        double fixedDt = 1.0 / 60.0;
        double scaledDt = fixedDt * timeScale;

        // Update day/time system
        dayTimeSystem.update(scaledDt);

        ecsEngine.update(scaledDt);
        economyManager.update(scaledDt);
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

    public void setTimeScale(float scale) {
        this.timeScale = scale;
        System.out.println("Speed set to " + scale + "x");
    }

    public DayTimeSystem getDayTimeSystem() {
        return dayTimeSystem;
    }

    public VisitorManager getVisitorManager() {
        return visitorManager;
    }

    public SnapPointManager getSnapPointManager() {
        return snapPointManager;
    }

    /**
     * Create the base camp building at the optimal location found by terrain
     * generator.
     */
    private void createBaseCamp() {
        // Use location determined by terrain generator (flattest spot in base zone)
        int baseCampX = baseCampLocation.x;
        int baseCampZ = baseCampLocation.z;
        float baseCampHeight = baseCampLocation.height;

        // Create base camp entity
        Entity baseCamp = ecsEngine.createEntity();
        ecsEngine.addComponent(baseCamp, new BaseCampComponent());
        ecsEngine.addComponent(baseCamp, new TransformComponent(baseCampX, baseCampHeight, baseCampZ));

        // Register base camp snap point
        SnapPoint baseCampSnapPoint = new SnapPoint(
                baseCampX, baseCampZ,
                SnapPoint.SnapPointType.BASE_CAMP,
                baseCamp.getId());
        snapPointManager.registerSnapPoint(baseCampSnapPoint);

        System.out.println("Base camp created at (" + baseCampX + ", " + baseCampZ + ")");
    }
}
