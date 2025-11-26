package com.project.tycoon.ecs.components;

import com.project.tycoon.ecs.Component;

public class SkierComponent implements Component {
    public enum State {
        SKIING,
        CRASHED,
        IDLE,
        LIFT
    }
    
    public State state;
    public float skillLevel; // 0.0 to 1.0 (Affects max speed / turning)
    
    public SkierComponent() {
        this.state = State.SKIING;
        this.skillLevel = 0.5f;
    }
}

