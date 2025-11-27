package com.project.tycoon.ecs.components;

import com.project.tycoon.ecs.Component;
import java.util.UUID;

public class LiftComponent implements Component {
    public enum LiftType { CHAIRLIFT, GONDOLA, TBAR }
    
    public LiftType type;
    public float speed;
    public UUID nextPylonId; // For linked list of pylons
    
    public LiftComponent(LiftType type) {
        this.type = type;
        this.speed = 2.0f;
        this.nextPylonId = null;
    }
}