package com.project.tycoon.ecs.systems.lift;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.System;
import com.project.tycoon.ecs.components.LiftComponent;
import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.ecs.components.VelocityComponent;
import com.project.tycoon.economy.EconomyManager;
import com.project.tycoon.ecs.systems.skier.LiftPlanner;
import com.project.tycoon.world.SnapPointManager;
import com.project.tycoon.world.model.WorldMap;

import java.util.*;

/**
 * Manages lift operations: queuing, boarding, transport, and release.
 * Handles the complete lift lifecycle for skiers.
 */
public class LiftSystem implements System {

    private final Engine engine;
    private final EconomyManager economy;
    private final LiftPlanner liftPlanner;
    private final WorldMap worldMap;

    // Queue per lift: Map<liftBaseEntityId, List<skierEntityId>>
    private final Map<UUID, List<UUID>> liftQueues = new HashMap<>();

    // Boarding timer per lift
    private final Map<UUID, Float> boardingTimers = new HashMap<>();
    private static final float BOARDING_INTERVAL = 2.0f;

    // Detection radius for lift base (increased for better mid-mountain boarding)
    private static final float QUEUE_DETECTION_RADIUS = 15.0f;

    public LiftSystem(Engine engine, EconomyManager economy, SnapPointManager snapPointManager, WorldMap worldMap) {
        this.engine = engine;
        this.economy = economy;
        this.worldMap = worldMap;
        this.liftPlanner = new LiftPlanner(engine, snapPointManager, worldMap);
    }

    @Override
    public void update(double dt) {
        // 1. Detect skiers near lift bases, add to queues
        detectAndQueueSkiers();

        // 2. Board skiers from front of queue
        boardSkiers((float) dt);

        // 3. Transport skiers along lift path
        transportSkiers((float) dt);

        // 4. Release skiers at top
        releaseSkiers();

        // 5. Deduct maintenance costs
        deductMaintenanceCosts((float) dt);
    }

    /**
     * Detect skiers in WAITING state near lift bases and add them to queues.
     */
    private void detectAndQueueSkiers() {
        // Find all lift base entities
        Map<UUID, Entity> liftBases = findLiftBases();

        for (Entity skierEntity : engine.getEntities()) {
            if (!engine.hasComponent(skierEntity, SkierComponent.class)) {
                continue;
            }

            SkierComponent skier = engine.getComponent(skierEntity, SkierComponent.class);
            TransformComponent skierPos = engine.getComponent(skierEntity, TransformComponent.class);

            if (skierPos == null) {
                continue;
            }

            // Only queue skiers in WAITING state
            if (skier.state == SkierComponent.State.WAITING) {
                // Find nearest lift base within detection radius
                Entity nearestLift = findNearestLiftBase(skierPos, liftBases);

                if (nearestLift != null) {
                    UUID liftId = nearestLift.getId();

                    // Initialize queue for this lift if needed
                    liftQueues.putIfAbsent(liftId, new ArrayList<>());
                    List<UUID> queue = liftQueues.get(liftId);

                    // Add to queue if not already in it
                    if (!queue.contains(skierEntity.getId())) {
                        queue.add(skierEntity.getId());
                        skier.state = SkierComponent.State.QUEUED;
                        skier.queuePosition = queue.size() - 1;
                        skier.targetLiftId = liftId;

                        // Stop movement while in queue
                        VelocityComponent vel = engine.getComponent(skierEntity, VelocityComponent.class);
                        if (vel != null) {
                            vel.dx = 0;
                            vel.dz = 0;
                        }
                    }
                }
            }
        }
    }

    /**
     * Board skiers from the front of each queue onto their lifts.
     */
    private void boardSkiers(float dt) {
        for (Map.Entry<UUID, List<UUID>> entry : liftQueues.entrySet()) {
            UUID liftId = entry.getKey();
            List<UUID> queue = entry.getValue();

            if (queue.isEmpty()) {
                continue;
            }

            // Check capacity before boarding
            Entity liftBase = findEntityById(liftId);
            if (liftBase == null)
                continue;

            LiftComponent liftComp = engine.getComponent(liftBase, LiftComponent.class);
            if (liftComp == null)
                continue;

            int currentRiders = countRidersOnLift(liftId);
            if (currentRiders >= liftComp.capacity) {
                // Lift is at capacity, cannot board more
                continue;
            }

            // Update boarding timer
            float timer = boardingTimers.getOrDefault(liftId, 0f);
            timer += dt;

            if (timer >= BOARDING_INTERVAL) {
                // Board the front skier
                UUID skierId = queue.remove(0);
                Entity skierEntity = findEntityById(skierId);

                if (skierEntity != null && engine.hasComponent(skierEntity, SkierComponent.class)) {
                    SkierComponent skier = engine.getComponent(skierEntity, SkierComponent.class);
                    TransformComponent skierPos = engine.getComponent(skierEntity, TransformComponent.class);

                    if (liftBase != null) {
                        TransformComponent liftPos = engine.getComponent(liftBase, TransformComponent.class);

                        // Position skier at lift base
                        if (skierPos != null && liftPos != null) {
                            skierPos.x = liftPos.x;
                            skierPos.y = liftPos.y;
                            skierPos.z = liftPos.z;
                        }

                        // Transition to RIDING_LIFT
                        skier.state = SkierComponent.State.RIDING_LIFT;
                        skier.targetLiftId = liftId;

                        // Charge ticket revenue
                        economy.recordTicketSale();
                    }
                }

                // Update queue positions
                for (int i = 0; i < queue.size(); i++) {
                    Entity queuedSkier = findEntityById(queue.get(i));
                    if (queuedSkier != null && engine.hasComponent(queuedSkier, SkierComponent.class)) {
                        SkierComponent sc = engine.getComponent(queuedSkier, SkierComponent.class);
                        sc.queuePosition = i;
                    }
                }

                // Reset timer
                boardingTimers.put(liftId, 0f);
            } else {
                boardingTimers.put(liftId, timer);
            }
        }
    }

    /**
     * Transport skiers along their lift paths.
     */
    private void transportSkiers(float dt) {
        for (Entity skierEntity : engine.getEntities()) {
            if (!engine.hasComponent(skierEntity, SkierComponent.class)) {
                continue;
            }

            SkierComponent skier = engine.getComponent(skierEntity, SkierComponent.class);
            TransformComponent skierPos = engine.getComponent(skierEntity, TransformComponent.class);

            if (skier.state != SkierComponent.State.RIDING_LIFT || skierPos == null) {
                continue;
            }

            // Find current and next pylon
            Entity currentPylon = findNearestPylonOnLift(skierPos, skier.targetLiftId);
            if (currentPylon == null) {
                continue;
            }

            LiftComponent lift = engine.getComponent(currentPylon, LiftComponent.class);
            TransformComponent currentPos = engine.getComponent(currentPylon, TransformComponent.class);

            if (lift == null || currentPos == null) {
                continue;
            }

            // Check if there's a next pylon
            if (lift.nextPylonId != null) {
                Entity nextPylon = findEntityById(lift.nextPylonId);
                if (nextPylon != null) {
                    TransformComponent nextPos = engine.getComponent(nextPylon, TransformComponent.class);
                    if (nextPos != null) {
                        // Move toward next pylon
                        float dx = nextPos.x - skierPos.x;
                        float dy = nextPos.y - skierPos.y;
                        float dz = nextPos.z - skierPos.z;
                        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

                        if (distance > 0.5f) {
                            // Normalize and apply speed
                            float moveDistance = lift.speed * dt;
                            skierPos.x += (dx / distance) * moveDistance;
                            skierPos.y += (dy / distance) * moveDistance;
                            skierPos.z += (dz / distance) * moveDistance;
                        }
                    }
                }
            }
        }
    }

    /**
     * Release skiers at the top of the lift.
     */
    private void releaseSkiers() {
        for (Entity skierEntity : engine.getEntities()) {
            if (!engine.hasComponent(skierEntity, SkierComponent.class)) {
                continue;
            }

            SkierComponent skier = engine.getComponent(skierEntity, SkierComponent.class);
            TransformComponent skierPos = engine.getComponent(skierEntity, TransformComponent.class);

            if (skier.state != SkierComponent.State.RIDING_LIFT || skierPos == null) {
                continue;
            }

            // Find current pylon
            Entity currentPylon = findNearestPylonOnLift(skierPos, skier.targetLiftId);
            if (currentPylon == null) {
                continue;
            }

            LiftComponent lift = engine.getComponent(currentPylon, LiftComponent.class);

            // Check if at top (last pylon has no next)
            if (lift != null && lift.nextPylonId == null) {
                TransformComponent topPos = engine.getComponent(currentPylon, TransformComponent.class);
                if (topPos != null) {
                    // Position skier ON a trail near the top pylon
                    boolean foundTrail = false;
                    
                    // Search for nearest trail tile near lift top
                    for (int radius = 1; radius <= 15 && !foundTrail; radius++) {
                        for (int dz = -radius; dz <= radius && !foundTrail; dz++) {
                            for (int dx = -radius; dx <= radius && !foundTrail; dx++) {
                                int testX = (int)Math.floor(topPos.x) + dx;
                                int testZ = (int)Math.floor(topPos.z) + dz;
                                
                                if (worldMap.isValid(testX, testZ)) {
                                    com.project.tycoon.world.model.Tile tile = worldMap.getTile(testX, testZ);
                                    if (tile != null && tile.isTrail()) {
                                        skierPos.x = testX + 0.5f;
                                        skierPos.y = tile.getHeight();
                                        skierPos.z = testZ + 0.5f;
                                        foundTrail = true;
                                        java.lang.System.out.println("✅ LIFT RELEASE: Placed skier on trail at (" + testX + "," + testZ + ")");
                                    }
                                }
                            }
                        }
                    }
                    
                    // Fallback: if no trail found, use old position
                    if (!foundTrail) {
                        skierPos.x = topPos.x + 2;
                        skierPos.y = topPos.y;
                        skierPos.z = topPos.z + 2;
                        java.lang.System.out.println("⚠️  LIFT RELEASE: No trail found near lift top, using default position");
                    }
                }

                // Transition to SKIING
                skier.state = SkierComponent.State.SKIING;
                
                // ⭐ NEW: Plan next lift target based on skill level
                UUID nextLiftTarget = liftPlanner.chooseNextLift(skier, skierPos);
                skier.targetLiftId = nextLiftTarget;
            }
        }
    }

    /**
     * Find all lift base entities (first pylon in each lift chain).
     */
    private Map<UUID, Entity> findLiftBases() {
        Map<UUID, Entity> bases = new HashMap<>();
        Set<UUID> hasIncoming = new HashSet<>();

        // First pass: identify all pylons that are pointed to
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, LiftComponent.class)) {
                LiftComponent lift = engine.getComponent(entity, LiftComponent.class);
                if (lift.nextPylonId != null) {
                    hasIncoming.add(lift.nextPylonId);
                }
            }
        }

        // Second pass: bases are pylons with no incoming links
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, LiftComponent.class)) {
                if (!hasIncoming.contains(entity.getId())) {
                    bases.put(entity.getId(), entity);
                }
            }
        }

        return bases;
    }

    /**
     * Find the nearest lift base within detection radius.
     */
    private Entity findNearestLiftBase(TransformComponent skierPos, Map<UUID, Entity> liftBases) {
        Entity nearest = null;
        float minDistance = QUEUE_DETECTION_RADIUS;

        for (Entity liftBase : liftBases.values()) {
            TransformComponent liftPos = engine.getComponent(liftBase, TransformComponent.class);
            if (liftPos == null) {
                continue;
            }

            float dx = liftPos.x - skierPos.x;
            float dz = liftPos.z - skierPos.z;
            float distance = (float) Math.sqrt(dx * dx + dz * dz);

            if (distance < minDistance) {
                minDistance = distance;
                nearest = liftBase;
            }
        }

        return nearest;
    }

    /**
     * Find the nearest pylon on the specified lift.
     */
    private Entity findNearestPylonOnLift(TransformComponent skierPos, UUID liftId) {
        if (liftId == null) {
            return null;
        }

        Entity nearest = null;
        float minDistance = Float.MAX_VALUE;

        // Start from the base pylon
        Entity current = findEntityById(liftId);
        while (current != null) {
            TransformComponent pylonPos = engine.getComponent(current, TransformComponent.class);
            if (pylonPos != null) {
                float dx = pylonPos.x - skierPos.x;
                float dy = pylonPos.y - skierPos.y;
                float dz = pylonPos.z - skierPos.z;
                float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = current;
                }
            }

            // Move to next pylon
            LiftComponent lift = engine.getComponent(current, LiftComponent.class);
            if (lift != null && lift.nextPylonId != null) {
                current = findEntityById(lift.nextPylonId);
            } else {
                break;
            }
        }

        return nearest;
    }

    /**
     * Find entity by UUID.
     */
    private Entity findEntityById(UUID id) {
        for (Entity entity : engine.getEntities()) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;
    }

    /**
     * Count how many skiers are currently riding a specific lift.
     */
    private int countRidersOnLift(UUID liftId) {
        int count = 0;
        for (Entity entity : engine.getEntities()) {
            if (engine.hasComponent(entity, SkierComponent.class)) {
                SkierComponent skier = engine.getComponent(entity, SkierComponent.class);
                if (skier.state == SkierComponent.State.RIDING_LIFT &&
                        liftId.equals(skier.targetLiftId)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Deduct maintenance costs from all active lifts.
     */
    private void deductMaintenanceCosts(float dt) {
        Map<UUID, Entity> liftBases = findLiftBases();
        for (Entity liftBase : liftBases.values()) {
            LiftComponent lift = engine.getComponent(liftBase, LiftComponent.class);
            if (lift != null) {
                float cost = lift.maintenanceCostPerSec * dt;
                economy.deductExpense(cost);
            }
        }
    }
}
