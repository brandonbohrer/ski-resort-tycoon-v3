package com.project.tycoon.view.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.project.tycoon.ecs.components.LiftComponent;

import java.util.HashMap;
import java.util.Map;

public class EntityTextureFactory {
    
    private static final Map<String, Texture> cache = new HashMap<>();
    
    public static Texture getLiftPylonTexture() {
        if (!cache.containsKey("pylon")) {
            Pixmap p = new Pixmap(16, 32, Pixmap.Format.RGBA8888);
            p.setColor(Color.CLEAR);
            p.fill();
            
            p.setColor(Color.DARK_GRAY);
            // Draw a vertical pole
            p.fillRectangle(6, 0, 4, 32);
            // Draw crossbar
            p.fillRectangle(0, 4, 16, 4);
            
            Texture t = new Texture(p);
            p.dispose();
            cache.put("pylon", t);
        }
        return cache.get("pylon");
    }
    
    public static void dispose() {
        for (Texture t : cache.values()) {
            t.dispose();
        }
        cache.clear();
    }
}

