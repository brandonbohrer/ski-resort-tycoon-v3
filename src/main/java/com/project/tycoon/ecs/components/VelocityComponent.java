package com.project.tycoon.ecs.components;

import com.project.tycoon.ecs.Component;

public class VelocityComponent implements Component {
    public float dx, dy, dz;

    public VelocityComponent(float dx, float dy, float dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }
    
    public VelocityComponent() {
        this(0,0,0);
    }
}

