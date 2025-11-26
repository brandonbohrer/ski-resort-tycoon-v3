package com.project.tycoon.view.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.project.tycoon.world.model.TerrainType;

import java.util.EnumMap;
import java.util.Map;

import com.project.tycoon.view.util.IsoUtils; // Import constants

/**
 * Generates simple procedural textures for terrain types.
 * This avoids the need for external asset files during early development.
 */
public class TextureFactory {

    private static final Map<TerrainType, Texture> textureCache = new EnumMap<>(TerrainType.class);
    private static Texture cursorTexture;
    
    private static final int TEX_W = (int) IsoUtils.TILE_WIDTH;
    private static final int TEX_H = (int) IsoUtils.TILE_HEIGHT;

    public static void dispose() {
        for (Texture t : textureCache.values()) {
            t.dispose();
        }
        textureCache.clear();
        if (cursorTexture != null) {
            cursorTexture.dispose();
            cursorTexture = null;
        }
    }

    public static Texture getCursorTexture() {
        if (cursorTexture == null) {
            Pixmap pixmap = new Pixmap(TEX_W, TEX_H, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.YELLOW);
            // Draw outline only
            // Top: (W/2, 0)
            // Right: (W, H/2)
            // Bottom: (W/2, H)
            // Left: (0, H/2)
            pixmap.drawLine(0, TEX_H/2, TEX_W/2, 0);
            pixmap.drawLine(TEX_W/2, 0, TEX_W, TEX_H/2);
            pixmap.drawLine(TEX_W, TEX_H/2, TEX_W/2, TEX_H);
            pixmap.drawLine(TEX_W/2, TEX_H, 0, TEX_H/2);
            
            cursorTexture = new Texture(pixmap);
            pixmap.dispose();
        }
        return cursorTexture;
    }

    public static Texture getTexture(TerrainType type) {
        if (textureCache.containsKey(type)) {
            return textureCache.get(type);
        }

        Color color;
        switch (type) {
            case GRASS: color = Color.FOREST; break;
            case SNOW:  color = Color.WHITE; break;
            case ROCK:  color = Color.GRAY; break;
            case DIRT:  color = Color.BROWN; break;
            default:    color = Color.MAGENTA; break;
        }

        Texture texture = createSolidTexture(color);
        textureCache.put(type, texture);
        return texture;
    }

    private static Texture createSolidTexture(Color color) {
        Pixmap pixmap = new Pixmap(TEX_W, TEX_H, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        // Draw a diamond shape for isometric tile
        // Top
        pixmap.fillTriangle(TEX_W/2, 0, 0, TEX_H/2, TEX_W, TEX_H/2);
        // Bottom
        pixmap.fillTriangle(0, TEX_H/2, TEX_W/2, TEX_H, TEX_W, TEX_H/2);
        
        // Add a darker outline for grid effect
        pixmap.setColor(Color.BLACK);
        pixmap.drawLine(0, TEX_H/2, TEX_W/2, 0);
        pixmap.drawLine(TEX_W/2, 0, TEX_W, TEX_H/2);
        pixmap.drawLine(TEX_W, TEX_H/2, TEX_W/2, TEX_H);
        pixmap.drawLine(TEX_W/2, TEX_H, 0, TEX_H/2);

        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
}

