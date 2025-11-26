package com.project.tycoon.ecs.components;

import com.project.tycoon.ecs.Component;

public class TransformComponent implements Component {
    public float x, y, z; // World coordinates (not grid)

    public TransformComponent(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}

