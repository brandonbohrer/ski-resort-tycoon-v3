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
    private final DayTimeSystem dayTimeSystem;
    private final VisitorManager visitorManager;

    private boolean paused = false;
    private float timeScale = 1.0f; // 1x, 2x, or 3x speed

    public TycoonSimulation() {
        this.ecsEngine = new Engine();
        // Increased resolution: 256x256 (4x total area)
        this.worldMap = new WorldMap(256, 256);
        this.economyManager = new EconomyManager();

        // Generate Mountain Terrain
        TerrainGenerator.generateMountain(this.worldMap);

        // Initialize day/time system
        this.dayTimeSystem = new DayTimeSystem();
        this.visitorManager = new VisitorManager();

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
}
