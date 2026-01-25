package com.project.tycoon.ecs.systems.skier;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.System;
import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.ecs.components.VelocityComponent;
import com.project.tycoon.world.SnapPointManager;
import com.project.tycoon.world.model.SnapPoint;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * New high-level navigation system for skiers.
 * Uses snap point graph to plan paths through the resort.
 * Replaces the "find nearest" logic with goal-oriented pathfinding.
 */
public class SkierNavigationSystem implements System {

    private final Engine engine;
    private final SnapPointManager snapPointManager;

    private static final float WAYPOINT_REACHED_DISTANCE = 5.0f;
    private static final float NAVIGATION_SPEED = 3.0f;
    private static final float SNAP_POINT_SEARCH_RADIUS = 150.0f; // Much larger radius to find any snap point

    public SkierNavigationSystem(Engine engine, SnapPointManager snapPointManager) {
        this.engine = engine;
        this.snapPointManager = snapPointManager;
    }

    @Override
    public void update(double dt) {
        for (Entity entity : engine.getEntities()) {
            if (!engine.hasComponent(entity, SkierComponent.class)) {
                continue;
            }

            SkierComponent skier = engine.getComponent(entity, SkierComponent.class);
            TransformComponent pos = engine.getComponent(entity, TransformComponent.class);
            VelocityComponent vel = engine.getComponent(entity, VelocityComponent.class);

            if (pos == null || vel == null) {
                continue;
            }

            // Only handle WAITING state - navigation to lift
            if (skier.state == SkierComponent.State.WAITING) {
                updateWaitingNavigation(skier, pos, vel);
            }
        }
    }

    /**
     * Handle navigation for skiers in WAITING state.
     * Plans a path to a lift base using the snap point graph.
     * IMPORTANT: Only allows walking navigation from BASE_CAMP area!
     * Skiers mid-mountain must SKI to reach lifts, not walk.
     */
    private void updateWaitingNavigation(SkierComponent skier, TransformComponent pos, VelocityComponent vel) {
        // CRITICAL: If skier is already near a lift (anywhere), let LiftSystem handle them
        // Don't interfere! They're about to board.
        if (isNearAnyLift(pos)) {
            java.lang.System.out.println("✅ NAV: Skier at (" + Math.round(pos.x) + "," + Math.round(pos.z) + ") near lift, waiting for LiftSystem");
            // Skier is near a lift, LiftSystem will pick them up
            vel.dx = 0;
            vel.dz = 0;
            return; // Do nothing, just wait
        }
        
        // CRITICAL: Only allow walking navigation from BASE area
        // Skiers mid-mountain should NOT walk to lifts - they must ski!
        if (pos.z < com.project.tycoon.ecs.systems.skier.SkierSpawnerSystem.BASE_Z - 20) {
            java.lang.System.out.println("⛷️  NAV: Skier at (" + Math.round(pos.x) + "," + Math.round(pos.z) + ") mid-mountain WAITING → forcing back to SKIING");
            // Mid-mountain! Skier should not walk. They finished a trail but aren't at base.
            // They should either: find another trail to ski down, or be marked as finished/stuck
            vel.dx = 0;
            vel.dz = 0;
            skier.state = com.project.tycoon.ecs.components.SkierComponent.State.SKIING;
            // Force them back to skiing so they'll seek trails
            return;
        }
        
        // Check if we need to plan a new path
        if (skier.plannedPath == null || skier.plannedPath.isEmpty()) {
            planPathToLift(skier, pos);
            
            // If still no path after planning, give up (stop trying every frame)
            if (skier.plannedPath == null || skier.plannedPath.isEmpty()) {
                // Mark with empty list to stop re-planning every frame
                skier.plannedPath = new java.util.ArrayList<>();
                vel.dx = 0;
                vel.dz = 0;
                return;
            }
        }
        
        // If path is empty (failed planning), don't navigate
        if (skier.plannedPath.isEmpty()) {
            vel.dx = 0;
            vel.dz = 0;
            return;
        }

        // Get current waypoint
        if (skier.pathIndex >= skier.plannedPath.size()) {
            // Reached end of path
            skier.plannedPath = null;
            skier.pathIndex = 0;
            vel.dx = 0;
            vel.dz = 0;
            return;
        }

        UUID waypointId = skier.plannedPath.get(skier.pathIndex);
        SnapPoint waypoint = snapPointManager.getSnapPoint(waypointId);

        if (waypoint == null) {
            // Waypoint no longer exists, re-plan
            skier.plannedPath = null;
            skier.pathIndex = 0;
            return;
        }

        // Navigate toward waypoint
        float dx = waypoint.getX() - pos.x;
        float dz = waypoint.getZ() - pos.z;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);

        if (distance < WAYPOINT_REACHED_DISTANCE) {
            // Reached waypoint, move to next
            skier.pathIndex++;
            return;
        }

        // Move toward waypoint
        float normalizedDx = dx / distance;
        float normalizedDz = dz / distance;

        vel.dx = normalizedDx * NAVIGATION_SPEED;
        vel.dz = normalizedDz * NAVIGATION_SPEED;
    }

    /**
     * Plan a path from skier's current position to the nearest accessible lift base.
     */
    private void planPathToLift(SkierComponent skier, TransformComponent pos) {
        // Find nearest snap point to current position (large radius to find ANY snap point)
        SnapPoint startPoint = snapPointManager.findNearestSnapPoint(pos.x, pos.z, SNAP_POINT_SEARCH_RADIUS);

        if (startPoint == null) {
            // java.lang.System.out.println("Skier at (" + pos.x + "," + pos.z + ") cannot find nearby snap point (radius 50)");
            return;
        }

        // java.lang.System.out.println("Skier at (" + pos.x + "," + pos.z + ") found start point: " + startPoint.getType() + " at (" + startPoint.getX() + "," + startPoint.getZ() + ")");

        // Find all lift base snap points
        List<SnapPoint> liftBases = snapPointManager.getSnapPointsByType(SnapPoint.SnapPointType.LIFT_BOTTOM);

        if (liftBases.isEmpty()) {
            // java.lang.System.out.println("No lift bases found in resort");
            return;
        }

        // java.lang.System.out.println("Found " + liftBases.size() + " lift bases to try");

        // Try to find a path to each lift base, pick shortest
        List<UUID> bestPath = null;
        int shortestLength = Integer.MAX_VALUE;

        for (SnapPoint liftBase : liftBases) {
            List<UUID> path = snapPointManager.findPath(startPoint.getId(), liftBase.getId());
            // java.lang.System.out.println("  Path to lift at (" + liftBase.getX() + "," + liftBase.getZ() + "): " + 
            //     (path != null && !path.isEmpty() ? path.size() + " hops" : "NO PATH"));
            
            if (path != null && !path.isEmpty() && path.size() < shortestLength) {
                // Validate path is navigable (no skiing up trails)
                if (isPathNavigable(path, true)) {
                    // java.lang.System.out.println("    -> VALID and shorter!");
                    bestPath = path;
                    shortestLength = path.size();
                } else {
                    // java.lang.System.out.println("    -> INVALID (would walk up trails)");
                }
            }
        }

        if (bestPath != null) {
            skier.plannedPath = bestPath;
            skier.pathIndex = 0;
            skier.targetLiftId = liftBases.get(0).getOwnerId(); // Set target lift
            java.lang.System.out.println("Skier planned path to lift: " + bestPath.size() + " waypoints from (" + pos.x + "," + pos.z + ")");
        } else {
            java.lang.System.out.println("Skier at (" + pos.x + "," + pos.z + ") could not find valid path to any lift");
        }
    }

    /**
     * Check if a path is navigable on foot (can't walk up ski trails).
     * 
     * @param path      List of snap point IDs
     * @param isOnFoot  True if skier is walking (WAITING), false if skiing
     * @return True if path is valid
     */
    private boolean isPathNavigable(List<UUID> path, boolean isOnFoot) {
        if (path.size() < 2) {
            return true;
        }

        for (int i = 0; i < path.size() - 1; i++) {
            SnapPoint from = snapPointManager.getSnapPoint(path.get(i));
            SnapPoint to = snapPointManager.getSnapPoint(path.get(i + 1));

            if (from == null || to == null) {
                return false;
            }

            // If walking on foot, can't go from TRAIL_END to TRAIL_START (up the trail)
            if (isOnFoot) {
                if (from.getType() == SnapPoint.SnapPointType.TRAIL_END &&
                        to.getType() == SnapPoint.SnapPointType.TRAIL_START) {
                    return false; // Would require walking UP a ski trail
                }
            }
        }

        return true;
    }
    
    /**
     * Check if skier is near any lift base (within detection radius).
     * Used to avoid forcing skiers back to SKIING when they're already positioned to board.
     */
    private boolean isNearAnyLift(TransformComponent pos) {
        final float DETECTION_RADIUS = 8.0f;
        
        // Find all lift bases
        for (Entity entity : engine.getEntities()) {
            if (!engine.hasComponent(entity, com.project.tycoon.ecs.components.LiftComponent.class)) {
                continue;
            }
            
            // Check if this is a base pylon (no incoming connections)
            boolean isBase = true;
            UUID thisId = entity.getId();
            
            for (Entity other : engine.getEntities()) {
                if (engine.hasComponent(other, com.project.tycoon.ecs.components.LiftComponent.class)) {
                    com.project.tycoon.ecs.components.LiftComponent otherLift = 
                        engine.getComponent(other, com.project.tycoon.ecs.components.LiftComponent.class);
                    if (thisId.equals(otherLift.nextPylonId)) {
                        isBase = false;
                        break;
                    }
                }
            }
            
            if (isBase) {
                TransformComponent liftPos = engine.getComponent(entity, TransformComponent.class);
                if (liftPos != null) {
                    float dx = liftPos.x - pos.x;
                    float dz = liftPos.z - pos.z;
                    float distance = (float) Math.sqrt(dx * dx + dz * dz);
                    
                    if (distance < DETECTION_RADIUS) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
}

