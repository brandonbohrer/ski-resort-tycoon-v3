package com.project.tycoon.world.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a connection point where trails, lifts, and buildings connect.
 * Like road connection points in Cities: Skylines.
 */
public class SnapPoint {

    public enum SnapPointType {
        BASE_CAMP, // Base camp entry/exit
        LIFT_BOTTOM, // Bottom of lift (entry)
        LIFT_TOP, // Top of lift (exit)
        TRAIL_START, // Where trail begins
        TRAIL_END // Where trail ends
    }

    private final UUID id;
    private final float x;
    private final float z;
    private final SnapPointType type;
    private final UUID ownerId; // Entity that owns this snap point
    private final List<UUID> connections; // IDs of connected snap points

    public SnapPoint(float x, float z, SnapPointType type, UUID ownerId) {
        this.id = UUID.randomUUID();
        this.x = x;
        this.z = z;
        this.type = type;
        this.ownerId = ownerId;
        this.connections = new ArrayList<>();
    }

    public void addConnection(UUID snapPointId) {
        if (!connections.contains(snapPointId)) {
            connections.add(snapPointId);
        }
    }

    public void removeConnection(UUID snapPointId) {
        connections.remove(snapPointId);
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public float getX() {
        return x;
    }

    public float getZ() {
        return z;
    }

    public SnapPointType getType() {
        return type;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public List<UUID> getConnections() {
        return new ArrayList<>(connections);
    }

    public boolean isConnectedTo(UUID snapPointId) {
        return connections.contains(snapPointId);
    }
}
