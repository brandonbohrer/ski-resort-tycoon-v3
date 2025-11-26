package com.project.tycoon.ecs.components;

import com.project.tycoon.ecs.Component;
import com.badlogic.gdx.graphics.Color;

/**
 * Data required to render an entity.
 */
public class RenderComponent implements Component {
    // For now, we use a procedural color/shape. 
    // In the future, this would hold a TextureRegion or Animation reference.
    public Color color;
    public float width, height; 
    
    public RenderComponent(Color color, float width, float height) {
        this.color = color;
        this.width = width;
        this.height = height;
    }
}

