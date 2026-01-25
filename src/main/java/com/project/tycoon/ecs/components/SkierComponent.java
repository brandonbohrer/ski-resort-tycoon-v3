package com.project.tycoon.ecs.components;

import com.project.tycoon.ecs.Component;
import com.project.tycoon.world.model.TrailDifficulty;
import java.util.List;
import java.util.UUID;

public class SkierComponent implements Component {
    public enum State {
        WAITING, // At base, looking for lift
        QUEUED, // In line for lift
        RIDING_LIFT, // On lift going up
        SKIING, // Skiing down trail
        FINISHED // Completed run, ready to despawn
    }

    public State state;
    public SkillLevel skillLevel; // Beginner, Intermediate, Advanced, Expert
    public UUID targetLiftId; // Which lift to ride
    public int queuePosition; // Position in lift queue
    public float satisfaction; // 0-100 scale, determines if skier leaves early
    public TrailDifficulty targetTrailDifficulty; // What difficulty they're seeking this run

    // Navigation fields for goal-oriented pathfinding
    public List<UUID> plannedPath; // Sequence of snap point IDs to follow
    public int pathIndex; // Current position in path (which waypoint we're heading to)

    // Carving/turning state for realistic skiing
    public float carvingDirection = 0.0f; // -1.0 (left turn) to +1.0 (right turn)
    public float carvingPhase = 0.0f; // 0.0 to 2π, controls turn cycle
    public float carvingSpeed = 1.0f; // How fast they complete turns (skill-based)
    public long randomSeed; // For consistent but varied behavior

    public SkierComponent() {
        this.state = State.WAITING;
        this.skillLevel = SkillLevel.INTERMEDIATE; // Default, overridden at spawn
        this.targetLiftId = null;
        this.queuePosition = -1;
        this.satisfaction = 50.0f; // Start neutral
        this.targetTrailDifficulty = null; // Chosen when looking for trails
        this.plannedPath = null;
        this.pathIndex = 0;
        this.randomSeed = System.nanoTime(); // Unique per skier
    }
}
