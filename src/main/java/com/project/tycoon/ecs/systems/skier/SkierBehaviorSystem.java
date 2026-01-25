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

        // ⭐ FIRST: Check if near target lift (before base check!)
        if (liftDetector.isNearTargetLift(pos, skier.targetLiftId)) {
            vel.dx = 0;
            vel.dz = 0;
            skier.state = SkierComponent.State.WAITING;
            return;
        }

        // Check if reached base area (only if no target lift or past target)
        if (z >= SkierSpawnerSystem.BASE_Z - 2) {
            vel.dx = 0;
            vel.dz = 0;
            
            if (current.isTrail()) {
            skier.state = SkierComponent.State.FINISHED;
            } else {
                skier.state = SkierComponent.State.WAITING;
            }
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

            // ⭐ NEW: Steer toward target lift while skiing
            if (skier.targetLiftId != null) {
                steerTowardTargetLift(skier, pos, vel, dt);
            } else {
                carvingPhysics.applyCarving(skier, pos, vel, dt);
            }
        } else {
            // Off trail: seek back to trails
            if (!trailSeeker.seekPreferredTrail(pos, vel, skier)) {
                trailSeeker.seekNearestTrail(pos, vel);
            }
        }

        // Keep skier snapped to terrain height
        pos.y = current.getHeight();
    }

    /**
     * Steer skier toward their target lift while skiing down.
     */
    private void steerTowardTargetLift(SkierComponent skier, TransformComponent pos, VelocityComponent vel, double dt) {
        // Find target lift position
        Entity targetLift = null;
        for (Entity entity : engine.getEntities()) {
            if (entity.getId().equals(skier.targetLiftId)) {
                targetLift = entity;
                break;
            }
        }
        
        if (targetLift == null) {
            // Target lift not found, just ski normally
            carvingPhysics.applyCarving(skier, pos, vel, dt);
            return;
        }

        TransformComponent liftPos = engine.getComponent(targetLift, TransformComponent.class);
        if (liftPos == null) {
            carvingPhysics.applyCarving(skier, pos, vel, dt);
            return;
        }

        // Calculate direction to target lift
        float dx = liftPos.x - pos.x;
        float dz = liftPos.z - pos.z;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);

        if (distance < 0.1f) {
            carvingPhysics.applyCarving(skier, pos, vel, dt);
            return;
        }

        // Normalize direction
        float targetDx = dx / distance;
        float targetDz = dz / distance;
        
        // Get current carving velocity
        carvingPhysics.applyCarving(skier, pos, vel, dt);
        
        // Blend toward target (70% carving, 30% steering toward target)
        float steerWeight = 0.3f;
        vel.dx = vel.dx * (1 - steerWeight) + targetDx * 4.0f * steerWeight;
        vel.dz = vel.dz * (1 - steerWeight) + targetDz * 4.0f * steerWeight;
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
