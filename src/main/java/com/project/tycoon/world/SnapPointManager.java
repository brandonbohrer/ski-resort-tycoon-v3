package com.project.tycoon.world;

import com.project.tycoon.world.model.SnapPoint;

import java.util.*;

/**
 * Manages all snap points in the world and their connections.
 * Provides snap point registry and pathfinding for skier navigation.
 */
public class SnapPointManager {

    private final Map<UUID, SnapPoint> snapPoints;

    public SnapPointManager() {
        this.snapPoints = new HashMap<>();
    }

    /**
     * Register a new snap point.
     */
    public void registerSnapPoint(SnapPoint snapPoint) {
        snapPoints.put(snapPoint.getId(), snapPoint);
        System.out.println("Registered snap point: " + snapPoint.getType() + " at (" +
                snapPoint.getX() + ", " + snapPoint.getZ() + ")");
    }

    /**
     * Remove a snap point (e.g., when building is demolished).
     */
    public void removeSnapPoint(UUID snapPointId) {
        SnapPoint removed = snapPoints.remove(snapPointId);
        if (removed != null) {
            // Remove all connections to this snap point
            for (SnapPoint sp : snapPoints.values()) {
                sp.removeConnection(snapPointId);
            }
        }
    }

    /**
     * Connect two snap points (bidirectional).
     */
    public void connectSnapPoints(UUID from, UUID to) {
        SnapPoint fromPoint = snapPoints.get(from);
        SnapPoint toPoint = snapPoints.get(to);

        if (fromPoint != null && toPoint != null) {
            fromPoint.addConnection(to);
            toPoint.addConnection(from);
            System.out.println("Connected snap points: " + fromPoint.getType() + " <-> " + toPoint.getType());
        }
    }

    /**
     * Get a snap point by ID.
     */
    public SnapPoint getSnapPoint(UUID id) {
        return snapPoints.get(id);
    }

    /**
     * Get all snap points.
     */
    public Collection<SnapPoint> getAllSnapPoints() {
        return new ArrayList<>(snapPoints.values());
    }

    /**
     * Get snap points of a specific type.
     */
    public List<SnapPoint> getSnapPointsByType(SnapPoint.SnapPointType type) {
        List<SnapPoint> result = new ArrayList<>();
        for (SnapPoint sp : snapPoints.values()) {
            if (sp.getType() == type) {
                result.add(sp);
            }
        }
        return result;
    }

    /**
     * Get snap points owned by a specific entity.
     */
    public List<SnapPoint> getSnapPointsByOwner(UUID ownerId) {
        List<SnapPoint> result = new ArrayList<>();
        for (SnapPoint sp : snapPoints.values()) {
            // Handle null ownerIds (e.g., trail snap points)
            if (ownerId == null) {
                if (sp.getOwnerId() == null) {
                    result.add(sp);
                }
            } else if (ownerId.equals(sp.getOwnerId())) {
                result.add(sp);
            }
        }
        return result;
    }

    /**
     * Find nearest snap point to given position.
     */
    public SnapPoint findNearestSnapPoint(float x, float z, float maxDistance) {
        SnapPoint nearest = null;
        float minDist = maxDistance;

        for (SnapPoint sp : snapPoints.values()) {
            float dx = sp.getX() - x;
            float dz = sp.getZ() - z;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);

            if (dist < minDist) {
                minDist = dist;
                nearest = sp;
            }
        }

        return nearest;
    }

    /**
     * Get all snap points within radius of position (sorted by distance).
     * Used for trail building validation.
     */
    public List<SnapPoint> getSnapPointsNear(float x, float z, float radius) {
        List<SnapPoint> result = new ArrayList<>();

        for (SnapPoint sp : snapPoints.values()) {
            float dx = sp.getX() - x;
            float dz = sp.getZ() - z;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);

            if (dist <= radius) {
                result.add(sp);
            }
        }

        // Sort by distance (closest first)
        result.sort((a, b) -> {
            float distA = (float) Math.sqrt(
                    Math.pow(a.getX() - x, 2) + Math.pow(a.getZ() - z, 2));
            float distB = (float) Math.sqrt(
                    Math.pow(b.getX() - x, 2) + Math.pow(b.getZ() - z, 2));
            return Float.compare(distA, distB);
        });

        return result;
    }

    /**
     * Simple pathfinding: find a path between two snap points.
     * Returns list of snap point IDs representing the path.
     * For now, just returns direct connection if exists.
     */
    public List<UUID> findPath(UUID start, UUID end) {
        List<UUID> path = new ArrayList<>();
        SnapPoint startPoint = snapPoints.get(start);

        if (startPoint == null) {
            return path;
        }

        // Simple: if directly connected, return path
        if (startPoint.isConnectedTo(end)) {
            path.add(start);
            path.add(end);
        }

        // TODO: Implement proper BFS/Dijkstra for multi-hop paths

        return path;
    }
}
