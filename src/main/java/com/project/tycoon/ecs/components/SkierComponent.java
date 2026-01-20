package com.project.tycoon.ecs.components;

import com.project.tycoon.ecs.Component;
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
    public float skillLevel; // 0.0 to 1.0 (Affects max speed / turning)
    public UUID targetLiftId; // Which lift to ride
    public int queuePosition; // Position in lift queue
    public float stamina; // Affects when they want to stop (0-1)

    public SkierComponent() {
        this.state = State.WAITING;
        this.skillLevel = 0.5f;
        this.targetLiftId = null;
        this.queuePosition = -1;
        this.stamina = 1.0f;
    }
}
