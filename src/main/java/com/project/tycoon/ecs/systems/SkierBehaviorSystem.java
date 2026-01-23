package com.project.tycoon.ecs.systems;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.System;
import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.ecs.components.VelocityComponent;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;

/**
 * Applies Skiing logic (Gravity on slopes).
 */
public class SkierBehaviorSystem implements System {

    private final Engine engine;
    private final WorldMap map;

    private static final float GRAVITY = 5.0f;
    private static final float BASE_SKI_SPEED = 4.0f;
    private static final float MAX_SKI_SPEED = 10.0f;
    private static final float TRAIL_SEEK_SPEED = 3.5f;
    private static final float TURN_LERP = 0.18f;
    private static final int TRAIL_SEEK_RADIUS = 10;

    private int[][] trailDistance;
    private int[][] trailNextX;
    private int[][] trailNextZ;
    private boolean trailFlowReady = false;

    public SkierBehaviorSystem(Engine engine, WorldMap map) {
        this.engine = engine;
        this.map = map;
    }

    @Override
    public void update(double dt) {
        ensureTrailFlow();
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, SkierComponent.class) &&
                    engine.hasComponent(entity, TransformComponent.class) &&
                    engine.hasComponent(entity, VelocityComponent.class)) {

                SkierComponent skier = engine.getComponent(entity, SkierComponent.class);
                TransformComponent pos = engine.getComponent(entity, TransformComponent.class);
                VelocityComponent vel = engine.getComponent(entity, VelocityComponent.class);

                // Handle different states
                if (skier.state == SkierComponent.State.WAITING) {
                    handleWaitingState(skier, pos, vel);
                } else if (skier.state == SkierComponent.State.SKIING) {
                    handleSkiingState(skier, pos, vel, dt);

                    // Check if reached bottom (finish line)
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

        if (z >= SkierSpawnerSystem.BASE_Z - 2) {
            vel.dx = 0;
            vel.dz = 0;
            skier.state = SkierComponent.State.FINISHED;
            return;
        }

        if (current.isTrail()) {
            TrailStep next = getFlowStep(x, z, current.getHeight());
            if (next != null) {
                steerTowardTile(pos, vel, next.x, next.z, skier, next.heightDrop);
            } else {
                boolean moved = steerTowardNearestTrail(pos, vel, skier);
                if (!moved) {
                    applySlopePhysics(pos, vel, dt);
                }
            }
        } else {
            boolean moved = steerTowardNearestTrail(pos, vel, skier);
            if (!moved) {
                applySlopePhysics(pos, vel, dt);
            }
        }

        // Keep skier snapped to terrain height
        pos.y = current.getHeight();
    }

    private void ensureTrailFlow() {
        if (trailFlowReady && !map.isDirty()) {
            return;
        }

        int width = map.getWidth();
        int depth = map.getDepth();
        trailDistance = new int[width][depth];
        trailNextX = new int[width][depth];
        trailNextZ = new int[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                trailDistance[x][z] = -1;
                trailNextX[x][z] = -1;
                trailNextZ[x][z] = -1;
            }
        }

        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        int maxTrailZ = -1;
        boolean hasTrail = false;

        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                Tile tile = map.getTile(x, z);
                if (tile != null && tile.isTrail()) {
                    hasTrail = true;
                    if (z > maxTrailZ) {
                        maxTrailZ = z;
                    }
                }
            }
        }

        if (!hasTrail) {
            trailFlowReady = false;
            return;
        }

        boolean seeded = false;
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                Tile tile = map.getTile(x, z);
                if (tile == null || !tile.isTrail()) {
                    continue;
                }
                if (z >= SkierSpawnerSystem.BASE_Z - 2) {
                    trailDistance[x][z] = 0;
                    queue.add(new int[]{x, z});
                    seeded = true;
                }
            }
        }

        if (!seeded && maxTrailZ >= 0) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    Tile tile = map.getTile(x, z);
                    if (tile == null || !tile.isTrail()) {
                        continue;
                    }
                    if (z == maxTrailZ) {
                        trailDistance[x][z] = 0;
                        queue.add(new int[]{x, z});
                    }
                }
            }
        }

        int[] dxs = {-1, 0, 1, -1, 1, -1, 0, 1};
        int[] dzs = {-1, -1, -1, 0, 0, 1, 1, 1};

        while (!queue.isEmpty()) {
            int[] cur = queue.removeFirst();
            int cx = cur[0];
            int cz = cur[1];
            int dist = trailDistance[cx][cz];

            for (int i = 0; i < dxs.length; i++) {
                int nx = cx + dxs[i];
                int nz = cz + dzs[i];
                if (!map.isValid(nx, nz)) {
                    continue;
                }
                if (trailDistance[nx][nz] != -1) {
                    continue;
                }
                Tile neighbor = map.getTile(nx, nz);
                if (neighbor == null || !neighbor.isTrail()) {
                    continue;
                }
                trailDistance[nx][nz] = dist + 1;
                queue.add(new int[]{nx, nz});
            }
        }

        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                if (trailDistance[x][z] <= 0) {
                    continue;
                }
                int bestNx = -1;
                int bestNz = -1;
                int bestDist = trailDistance[x][z];
                for (int i = 0; i < dxs.length; i++) {
                    int nx = x + dxs[i];
                    int nz = z + dzs[i];
                    if (!map.isValid(nx, nz)) {
                        continue;
                    }
                    int nDist = trailDistance[nx][nz];
                    if (nDist >= 0 && nDist < bestDist) {
                        bestDist = nDist;
                        bestNx = nx;
                        bestNz = nz;
                    }
                }
                if (bestNx != -1) {
                    trailNextX[x][z] = bestNx;
                    trailNextZ[x][z] = bestNz;
                }
            }
        }

        trailFlowReady = true;
    }

    private TrailStep getFlowStep(int x, int z, int currentHeight) {
        if (!trailFlowReady || trailNextX == null || trailNextZ == null) {
            return null;
        }
        if (!map.isValid(x, z)) {
            return null;
        }
        int nx = trailNextX[x][z];
        int nz = trailNextZ[x][z];
        if (nx < 0 || nz < 0) {
            return null;
        }
        Tile next = map.getTile(nx, nz);
        if (next == null) {
            return null;
        }
        int heightDrop = currentHeight - next.getHeight();
        return new TrailStep(nx, nz, heightDrop);
    }

    /**
     * Handle WAITING state - find and move toward nearest lift.
     */
    private void handleWaitingState(SkierComponent skier, TransformComponent pos, VelocityComponent vel) {
        // Find nearest lift base
        Entity nearestLift = findNearestLiftBase(pos);

        if (nearestLift != null) {
            skier.targetLiftId = nearestLift.getId();
            moveTowardLift(pos, vel, nearestLift);
        } else {
            // No lift found, stop moving
            vel.dx = 0;
            vel.dz = 0;
        }
    }

    /**
     * Find the nearest lift base to the skier.
     */
    private Entity findNearestLiftBase(TransformComponent skierPos) {
        Entity nearest = null;
        float minDistance = Float.MAX_VALUE;

        // Find all lift base entities (pylons with no incoming links)
        java.util.Set<java.util.UUID> hasIncoming = new java.util.HashSet<>();

        // First pass: identify all pylons that are pointed to
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, com.project.tycoon.ecs.components.LiftComponent.class)) {
                com.project.tycoon.ecs.components.LiftComponent lift = engine.getComponent(entity,
                        com.project.tycoon.ecs.components.LiftComponent.class);
                if (lift.nextPylonId != null) {
                    hasIncoming.add(lift.nextPylonId);
                }
            }
        }

        // Second pass: find nearest base (pylon with no incoming link)
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, com.project.tycoon.ecs.components.LiftComponent.class) &&
                    !hasIncoming.contains(entity.getId())) {

                TransformComponent liftPos = engine.getComponent(entity, TransformComponent.class);
                if (liftPos == null)
                    continue;

                float dx = liftPos.x - skierPos.x;
                float dz = liftPos.z - skierPos.z;
                float distance = (float) Math.sqrt(dx * dx + dz * dz);

                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = entity;
                }
            }
        }

        return nearest;
    }

    /**
     * Move skier toward the target lift.
     */
    private void moveTowardLift(TransformComponent pos, VelocityComponent vel, Entity liftEntity) {
        TransformComponent liftPos = engine.getComponent(liftEntity, TransformComponent.class);
        if (liftPos == null)
            return;

        // Calculate direction to lift
        float dx = liftPos.x - pos.x;
        float dz = liftPos.z - pos.z;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);

        // If already close enough, stop moving
        if (distance < 2.0f) {
            vel.dx = 0;
            vel.dz = 0;
            return;
        }

        // Move toward lift at constant speed
        float speed = 3.0f;
        vel.dx = (dx / distance) * speed;
        vel.dz = (dz / distance) * speed;
    }

    private void applySlopePhysics(TransformComponent pos, VelocityComponent vel, double dt) {
        int x = (int) pos.x;
        int z = (int) pos.z;

        if (!map.isValid(x, z))
            return;

        // Calculate Local Gradient
        // Compare height with neighbors to find "Downhill" direction
        Tile current = map.getTile(x, z);

        // Sample neighbors
        // We look at X+1 and Z+1 to get a rough gradient vector
        Tile right = map.getTile(x + 1, z);
        Tile down = map.getTile(x, z + 1);

        float h = current.getHeight();
        float hRight = (right != null) ? right.getHeight() : h;
        float hDown = (down != null) ? down.getHeight() : h;

        // Gradient: Negative means downhill
        float dx = hRight - h;
        float dz = hDown - h;

        // Apply forces downhill
        // If dx is negative (right is lower), we accelerate positive X
        // Force is proportional to steepness

        float delta = (float) dt;
        if (dx < 0)
            vel.dx += (-dx * GRAVITY * delta); // Accelerate Right
        else if (dx > 0)
            vel.dx -= (dx * GRAVITY * delta); // Accelerate Left (slide back)

        if (dz < 0)
            vel.dz += (-dz * GRAVITY * delta); // Accelerate Down
        else if (dz > 0)
            vel.dz -= (dz * GRAVITY * delta); // Accelerate Up

        // Snap Y to terrain height (simple collision)
        pos.y = h;
    }

    private boolean steerTowardNearestTrail(TransformComponent pos, VelocityComponent vel, SkierComponent skier) {
        int x = (int) Math.floor(pos.x);
        int z = (int) Math.floor(pos.z);

        int bestX = -1;
        int bestZ = -1;
        float bestDistSq = Float.MAX_VALUE;

        for (int dz = -TRAIL_SEEK_RADIUS; dz <= TRAIL_SEEK_RADIUS; dz++) {
            for (int dx = -TRAIL_SEEK_RADIUS; dx <= TRAIL_SEEK_RADIUS; dx++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                int nx = x + dx;
                int nz = z + dz;
                if (!map.isValid(nx, nz)) {
                    continue;
                }

                Tile tile = map.getTile(nx, nz);
                if (tile == null || !tile.isTrail()) {
                    continue;
                }

                float distSq = dx * dx + dz * dz;
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    bestX = nx;
                    bestZ = nz;
                }
            }
        }

        if (bestX == -1) {
            return false;
        }

        steerTowardTileAtSpeed(pos, vel, bestX, bestZ, TRAIL_SEEK_SPEED);
        return true;
    }

    private void steerTowardTile(TransformComponent pos, VelocityComponent vel, int tileX, int tileZ,
                                 SkierComponent skier, int heightDrop) {
        float targetX = tileX + 0.5f;
        float targetZ = tileZ + 0.5f;

        float dx = targetX - pos.x;
        float dz = targetZ - pos.z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        if (dist < 0.05f) {
            return;
        }

        float desiredSpeed = BASE_SKI_SPEED + (skier.skillLevel * 2.5f) + Math.max(0, heightDrop) * 0.6f;
        desiredSpeed = Math.min(desiredSpeed, MAX_SKI_SPEED);

        float desiredDx = (dx / dist) * desiredSpeed;
        float desiredDz = (dz / dist) * desiredSpeed;

        vel.dx = lerp(vel.dx, desiredDx, TURN_LERP);
        vel.dz = lerp(vel.dz, desiredDz, TURN_LERP);
    }

    private void steerTowardTileAtSpeed(TransformComponent pos, VelocityComponent vel, int tileX, int tileZ,
                                        float speed) {
        float targetX = tileX + 0.5f;
        float targetZ = tileZ + 0.5f;

        float dx = targetX - pos.x;
        float dz = targetZ - pos.z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        if (dist < 0.05f) {
            return;
        }

        float desiredDx = (dx / dist) * speed;
        float desiredDz = (dz / dist) * speed;

        vel.dx = lerp(vel.dx, desiredDx, TURN_LERP);
        vel.dz = lerp(vel.dz, desiredDz, TURN_LERP);
    }

    private float lerp(float from, float to, float alpha) {
        return from + (to - from) * alpha;
    }

    private static class TrailStep {
        final int x;
        final int z;
        final int heightDrop;

        TrailStep(int x, int z, int heightDrop) {
            this.x = x;
            this.z = z;
            this.heightDrop = heightDrop;
        }
    }
}
