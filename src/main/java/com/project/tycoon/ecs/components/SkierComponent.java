package com.project.tycoon.ecs.components;

import com.project.tycoon.ecs.Component;
import com.project.tycoon.world.model.TrailDifficulty;
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

    public SkierComponent() {
        this.state = State.WAITING;
        this.skillLevel = SkillLevel.INTERMEDIATE; // Default, overridden at spawn
        this.targetLiftId = null;
        this.queuePosition = -1;
        this.satisfaction = 50.0f; // Start neutral
        this.targetTrailDifficulty = null; // Chosen when looking for trails
    }
}
