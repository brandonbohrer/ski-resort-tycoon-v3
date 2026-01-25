package com.project.tycoon.ecs.systems.skier;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.System;
import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.ecs.components.VelocityComponent;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.TrailDifficulty;
import com.project.tycoon.world.model.WorldMap;

import java.util.Random;

/**
 * Main orchestrator for skier behavior.
 * Delegates to specialized components for flow field, trail seeking, carving, etc.
 */
public class SkierBehaviorSystem implements System {

    private final Engine engine;
    private final WorldMap map;
    
    // Specialized components
    private final TrailFlowFieldCalculator flowField;
    private final TrailScanner trailScanner;
    private final TrailSeeker trailSeeker;
    private final CarvingPhysics carvingPhysics;
    private final LiftProximityDetector liftDetector;

    public SkierBehaviorSystem(Engine engine, WorldMap map) {
        this.engine = engine;
        this.map = map;
        
        // Initialize components
        this.flowField = new TrailFlowFieldCalculator(map);
        this.trailScanner = new TrailScanner(map);
        this.trailSeeker = new TrailSeeker(map);
        this.carvingPhysics = new CarvingPhysics(map, flowField, trailScanner, trailSeeker);
        this.liftDetector = new LiftProximityDetector(engine);
    }

    @Override
    public void update(double dt) {
        // Update flow field if map changed
        flowField.update();
        
        // Update all skiers
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, SkierComponent.class) &&
                    engine.hasComponent(entity, TransformComponent.class) &&
                    engine.hasComponent(entity, VelocityComponent.class)) {

                SkierComponent skier = engine.getComponent(entity, SkierComponent.class);
                TransformComponent pos = engine.getComponent(entity, TransformComponent.class);
                VelocityComponent vel = engine.getComponent(entity, VelocityComponent.class);

                if (skier.state == SkierComponent.State.SKIING) {
                    handleSkiingState(skier, pos, vel, dt);
                    
                    // Check if reached bottom
                    if (pos.z >= SkierSpawnerSystem.BASE_Z - 2) {
                        vel.dx = 0;
                        vel.dz = 0;
                        skier.state = SkierComponent.State.FINISHED;
                    }
                } else if (skier.state == SkierComponent.State.FINISHED) {
                    vel.dx = 0;
                    vel.dz = 0;
                }
                // QUEUED and RIDING_LIFT states are handled by LiftSystem
                // WAITING state is handled by SkierNavigationSystem
            }
        }
    }

    private void handleSkiingState(SkierComponent skier, TransformComponent pos, VelocityComponent vel, double dt) {
        int x = (int) Math.floor(pos.x);
        int z = (int) Math.floor(pos.z);

        if (!map.isValid(x, z)) {
            return;
        }

        Tile current = map.getTile(x, z);
        if (current == null) {
            return;
        }

        // Check if reached base area
        if (z >= SkierSpawnerSystem.BASE_Z - 2) {
            vel.dx = 0;
            vel.dz = 0;
            
            if (current.isTrail()) {
                java.lang.System.out.println("🏁 BEHAVIOR: Skier at base on trail → FINISHED");
                skier.state = SkierComponent.State.FINISHED;
            } else {
                java.lang.System.out.println("🔄 BEHAVIOR: Skier at base off-trail → WAITING (looking for lift)");
                skier.state = SkierComponent.State.WAITING;
            }
            return;
        }

        // Check if near a lift base (for mid-mountain boarding)
        if (liftDetector.isNearLiftBase(pos)) {
            java.lang.System.out.println("🛑 BEHAVIOR: Skier near lift at (" + Math.round(pos.x) + "," + Math.round(pos.z) + ") → WAITING");
            vel.dx = 0;
            vel.dz = 0;
            skier.state = SkierComponent.State.WAITING;
            return;
        }

        // Choose target difficulty if not already chosen
        if (skier.targetTrailDifficulty == null) {
            skier.targetTrailDifficulty = chooseTrailDifficulty(skier);
        }

        if (current.isTrail()) {
            // On trail: update satisfaction and apply carving
            TrailDifficulty currentDifficulty = current.getTrailDifficulty();
            updateSatisfaction(skier, currentDifficulty, dt);
            carvingPhysics.applyCarving(skier, pos, vel, dt);
        } else {
            // Off trail: seek back to trails
            java.lang.System.out.println("⚠️  BEHAVIOR: Skier at (" + Math.round(pos.x) + "," + Math.round(pos.z) + ") off-trail, seeking trail");
            
            if (!trailSeeker.seekPreferredTrail(pos, vel, skier)) {
                trailSeeker.seekNearestTrail(pos, vel);
            }
        }

        // Keep skier snapped to terrain height
        pos.y = current.getHeight();
    }

    /**
     * Choose a trail difficulty based on skier's skill level and preferences.
     */
    private TrailDifficulty chooseTrailDifficulty(SkierComponent skier) {
        Random rand = new Random();
        float r = rand.nextFloat();

        // Use preference weights for weighted random selection
        float greenWeight = TrailPreferences.getPreference(skier.skillLevel, TrailDifficulty.GREEN);
        float blueWeight = TrailPreferences.getPreference(skier.skillLevel, TrailDifficulty.BLUE);
        float blackWeight = TrailPreferences.getPreference(skier.skillLevel, TrailDifficulty.BLACK);
        float doubleBlackWeight = TrailPreferences.getPreference(skier.skillLevel, TrailDifficulty.DOUBLE_BLACK);

        float total = greenWeight + blueWeight + blackWeight + doubleBlackWeight;

        // Normalize and use cumulative distribution
        float greenThreshold = greenWeight / total;
        float blueThreshold = greenThreshold + (blueWeight / total);
        float blackThreshold = blueThreshold + (blackWeight / total);

        if (r < greenThreshold)
            return TrailDifficulty.GREEN;
        if (r < blueThreshold)
            return TrailDifficulty.BLUE;
        if (r < blackThreshold)
            return TrailDifficulty.BLACK;
        return TrailDifficulty.DOUBLE_BLACK;
    }

    /**
     * Update skier satisfaction based on trail difficulty match.
     */
    private void updateSatisfaction(SkierComponent skier, TrailDifficulty trailDifficulty, double dt) {
        float rawChange = TrailPreferences.getSatisfactionChange(skier.skillLevel, trailDifficulty);
        float change = rawChange * (float) dt * 0.15f;

        skier.satisfaction += change;
        skier.satisfaction = Math.max(0, Math.min(100, skier.satisfaction));
    }
}
