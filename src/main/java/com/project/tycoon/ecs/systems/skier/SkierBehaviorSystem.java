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
 * Applies Skiing logic (Gravity on slopes).
 */
public class SkierBehaviorSystem implements System {

    private final Engine engine;
    private final WorldMap map;

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

        // Choose target difficulty if not already chosen
        if (skier.targetTrailDifficulty == null) {
            skier.targetTrailDifficulty = chooseTrailDifficulty(skier);
        }

        if (current.isTrail()) {
            // Check if current trail matches preference
            TrailDifficulty currentDifficulty = current.getTrailDifficulty();
            updateSatisfaction(skier, currentDifficulty);

            // REALISTIC SKIING: Use carving system instead of simple flow
            handleCarvingSkiing(skier, pos, vel, dt);
        } else {
            // Not on trail, seek preferred difficulty
            if (!seekPreferredTrail(pos, vel, skier)) {
                // No preferred trail nearby, take any trail
                steerTowardNearestTrail(pos, vel, skier);
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
                    queue.add(new int[] { x, z });
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
                        queue.add(new int[] { x, z });
                    }
                }
            }
        }

        int[] dxs = { -1, 0, 1, -1, 1, -1, 0, 1 };
        int[] dzs = { -1, -1, -1, 0, 0, 1, 1, 1 };

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
                queue.add(new int[] { nx, nz });
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
        // Note: We don't call map.clean() here because TerrainRenderer needs to see the
        // dirty flag
        // to rebuild trail visuals. Trail flow will rebuild when map is dirty, which is
        // fine.
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

        // Convert skill level to numeric value (0-3 maps to different speed bonuses)
        float skillMultiplier = skier.skillLevel.ordinal() * 0.8f; // 0, 0.8, 1.6, 2.4
        float desiredSpeed = BASE_SKI_SPEED + skillMultiplier + Math.max(0, heightDrop) * 0.6f;
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

    /**
     * Choose a trail difficulty based on skier's skill level and preferences.
     * Uses weighted random selection.
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
     * Seek trails matching the skier's preferred difficulty.
     * 
     * @return true if found preferred trail, false otherwise
     */
    private boolean seekPreferredTrail(TransformComponent pos, VelocityComponent vel, SkierComponent skier) {
        int x = (int) Math.floor(pos.x);
        int z = (int) Math.floor(pos.z);

        int bestX = -1;
        int bestZ = -1;
        float bestScore = -1;

        // Search for trails matching preferred difficulty
        for (int dz = -TRAIL_SEEK_RADIUS; dz <= TRAIL_SEEK_RADIUS; dz++) {
            for (int dx = -TRAIL_SEEK_RADIUS; dx <= TRAIL_SEEK_RADIUS; dx++) {
                if (dx == 0 && dz == 0)
                    continue;

                int nx = x + dx;
                int nz = z + dz;
                if (!map.isValid(nx, nz))
                    continue;

                Tile tile = map.getTile(nx, nz);
                if (tile == null || !tile.isTrail())
                    continue;

                // Calculate score based on preference and distance
                float preference = TrailPreferences.getPreference(skier.skillLevel, tile.getTrailDifficulty());
                float distSq = dx * dx + dz * dz;
                float score = preference / (1 + distSq * 0.1f); // Prefer close trails

                if (score > bestScore) {
                    bestScore = score;
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

    /**
     * Update skier satisfaction based on trail difficulty match.
     */
    private void updateSatisfaction(SkierComponent skier, TrailDifficulty trailDifficulty) {
        // Only update once per trail (simple debounce using a flag would be better)
        // For now, we'll update continuously but the satisfaction change is clamped
        float change = TrailPreferences.getSatisfactionChange(skier.skillLevel, trailDifficulty) * 0.01f; // Small
                                                                                                          // increments

        skier.satisfaction += change;
        skier.satisfaction = Math.max(0, Math.min(100, skier.satisfaction)); // Clamp 0-100

        // Leave if too unhappy
        if (skier.satisfaction < 20) {
            skier.state = SkierComponent.State.FINISHED;
        }
    }

    /**
     * Handle realistic skiing with carving turns.
     */
    private void handleCarvingSkiing(SkierComponent skier, TransformComponent pos, VelocityComponent vel, double dt) {
        // Update carving phase (advance through turn cycle)
        skier.carvingPhase += (float) dt * skier.carvingSpeed;

        // Calculate current carving direction (sine wave for smooth S-turns)
        skier.carvingDirection = (float) Math.sin(skier.carvingPhase);

        // Get trail info at current position
        TrailInfo trailInfo = getTrailInfo(pos.x, pos.z);

        if (!trailInfo.onTrail) {
            // Lost trail, seek back
            steerTowardNearestTrail(pos, vel, skier);
            return;
        }

        // Get downhill direction from flow field
        int x = (int) Math.floor(pos.x);
        int z = (int) Math.floor(pos.z);
        TrailStep next = getFlowStep(x, z, map.getTile(x, z).getHeight());

        if (next == null) {
            // No flow, head straight down
            steerTowardNearestTrail(pos, vel, skier);
            return;
        }

        // Calculate lateral offset from centerline based on carving direction
        float lateralShift = skier.carvingDirection * (trailInfo.trailWidth * 0.35f);

        // Target position combines downhill + lateral movement
        float targetX = trailInfo.centerlineX + lateralShift;
        float targetZ = next.z + 0.5f;

        // Keep within trail boundaries
        targetX = Math.max(trailInfo.leftEdge + 0.5f, Math.min(trailInfo.rightEdge - 0.5f, targetX));

        // Steer toward target
        float dx = targetX - pos.x;
        float dz = targetZ - pos.z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        if (dist < 0.05f)
            return;

        // Calculate speed based on skill and turn sharpness
        float turnSharpness = Math.abs(skier.carvingDirection);
        float speedMultiplier = 1.0f - (turnSharpness * 0.3f); // Slow down 30% in sharp turns

        float skillMultiplier = skier.skillLevel.ordinal() * 0.8f;
        float baseSpeed = BASE_SKI_SPEED + skillMultiplier;
        float speed = baseSpeed * speedMultiplier;
        speed = Math.min(speed, MAX_SKI_SPEED);

        // Apply velocity
        float desiredDx = (dx / dist) * speed;
        float desiredDz = (dz / dist) * speed;

        vel.dx = lerp(vel.dx, desiredDx, TURN_LERP);
        vel.dz = lerp(vel.dz, desiredDz, TURN_LERP);
    }

    /**
     * Get trail width and centerline information at a position.
     */
    private TrailInfo getTrailInfo(float x, float z) {
        int ix = (int) x;
        int iz = (int) z;

        // Scan left to find edge
        int leftEdge = ix;
        for (int dx = 0; dx < 20; dx++) {
            int checkX = ix - dx;
            if (!map.isValid(checkX, iz))
                break;
            Tile t = map.getTile(checkX, iz);
            if (t == null || !t.isTrail()) {
                leftEdge = checkX + 1;
                break;
            }
            if (dx == 19)
                leftEdge = checkX; // Hit scan limit
        }

        // Scan right to find edge
        int rightEdge = ix;
        for (int dx = 0; dx < 20; dx++) {
            int checkX = ix + dx;
            if (!map.isValid(checkX, iz))
                break;
            Tile t = map.getTile(checkX, iz);
            if (t == null || !t.isTrail()) {
                rightEdge = checkX - 1;
                break;
            }
            if (dx == 19)
                rightEdge = checkX; // Hit scan limit
        }

        float centerX = (leftEdge + rightEdge) / 2.0f;
        float width = rightEdge - leftEdge;
        boolean onTrail = (ix >= leftEdge && ix <= rightEdge);

        return new TrailInfo(centerX, width, leftEdge, rightEdge, onTrail);
    }

    /**
     * Trail width information at a position.
     */
    private static class TrailInfo {
        final float centerlineX;
        final float trailWidth;
        final float leftEdge;
        final float rightEdge;
        final boolean onTrail;

        TrailInfo(float centerX, float width, float left, float right, boolean onTrail) {
            this.centerlineX = centerX;
            this.trailWidth = width;
            this.leftEdge = left;
            this.rightEdge = right;
            this.onTrail = onTrail;
        }
    }
}
