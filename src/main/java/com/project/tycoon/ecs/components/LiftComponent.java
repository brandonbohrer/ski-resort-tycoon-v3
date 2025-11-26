package com.project.tycoon.ecs.components;

import com.project.tycoon.ecs.Component;

public class LiftComponent implements Component {
    public enum LiftType { CHAIRLIFT, GONDOLA, TBAR }
    
    public LiftType type;
    public float speed;
    
    // For now, we just mark it as a lift. 
    // Later we will add connected entities (next pylon ID, etc.)
    
    public LiftComponent(LiftType type) {
        this.type = type;
        this.speed = 2.0f;
    }
}

