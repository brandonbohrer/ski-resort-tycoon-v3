package com.project.tycoon.view.renderer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.components.LiftComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;
import com.badlogic.gdx.math.Vector2;
import com.project.tycoon.view.LiftBuilder.LiftPreview;
import com.badlogic.gdx.graphics.Color; // Import Color
import com.project.tycoon.view.util.IsoUtils; // Import shared utils

import com.project.tycoon.ecs.components.SkierComponent; // Add Skier import

/**
 * Handles rendering of both Tiles and Entities, sorted by depth.
 * Replaces the simple WorldRenderer.
 */
public class CombinedRenderer {

    private final WorldMap worldMap;
    private final Engine ecsEngine;
    private final SpriteBatch batch;
    
    // Constants now come from IsoUtils to ensure sync with Logic/Mouse
    private static final float TILE_WIDTH = IsoUtils.TILE_WIDTH;
    private static final float TILE_HEIGHT = IsoUtils.TILE_HEIGHT;

    public CombinedRenderer(WorldMap worldMap, Engine ecsEngine) {
        this.worldMap = worldMap;
        this.ecsEngine = ecsEngine;
        this.batch = new SpriteBatch();
    }

    // Updated signature to accept LiftPreview
    public void render(OrthographicCamera camera, int hoveredX, int hoveredZ, boolean isBuildMode, LiftPreview preview) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // --- RENDER TERRAIN ---
        for (int z = 0; z < worldMap.getDepth(); z++) {
            for (int x = 0; x < worldMap.getWidth(); x++) {
                drawTile(x, z, hoveredX, hoveredZ, isBuildMode);
            }
        }
        
        // --- RENDER ENTITIES ---
        for (Entity entity : ecsEngine.getEntities()) {
            if (ecsEngine.hasComponent(entity, TransformComponent.class)) {
                TransformComponent transform = ecsEngine.getComponent(entity, TransformComponent.class);
                
                if (ecsEngine.hasComponent(entity, LiftComponent.class)) {
                    drawLift(transform, Color.WHITE);
                } else if (ecsEngine.hasComponent(entity, SkierComponent.class)) {
                    drawSkier(transform);
                }
            }
        }
        
        // --- RENDER PREVIEW (Ghosts) ---
        if (preview != null && !preview.pylonPositions.isEmpty()) {
            batch.setColor(1f, 1f, 1f, 0.5f); // 50% transparency
            for (Vector2 pos : preview.pylonPositions) {
                int x = (int) pos.x;
                int z = (int) pos.y;
                
                // Get height from map so ghost sits on terrain
                Tile tile = worldMap.getTile(x, z);
                float h = (tile != null) ? tile.getHeight() : 0;
                
                // Create temporary transform for drawing logic
                TransformComponent ghostTransform = new TransformComponent(x, h, z);
                drawLift(ghostTransform, preview.isValid ? preview.statusColor : Color.RED);
            }
            batch.setColor(Color.WHITE); // Reset
        }

        batch.end();
    }
    
    private void drawTile(int x, int z, int hoveredX, int hoveredZ, boolean isBuildMode) {
        Tile tile = worldMap.getTile(x, z);
        if (tile == null) return;

        Texture tex = TextureFactory.getTexture(tile.getType());
        
        float isoX = (x - z) * (TILE_WIDTH / 2f);
        float isoY = (x + z) * (TILE_HEIGHT / 2f);
        float heightOffset = tile.getHeight() * 8f;

        batch.draw(tex, isoX, isoY + heightOffset);

        if (x == hoveredX && z == hoveredZ) {
            // Change cursor color based on mode
            if (isBuildMode) {
                batch.setColor(Color.BLUE); // Tint blue for build mode
            } else {
                batch.setColor(Color.WHITE); // Default
            }
            
            batch.draw(TextureFactory.getCursorTexture(), isoX, isoY + heightOffset);
            
            batch.setColor(Color.WHITE); // Reset
        }
    }
    
    private void drawSkier(TransformComponent t) {
        Texture tex = EntityTextureFactory.getSkierTexture();
        
        float isoX = (t.x - t.z) * (TILE_WIDTH / 2f);
        float isoY = (t.x + t.z) * (TILE_HEIGHT / 2f);
        float heightOffset = t.y * 8f;
        
        // Center the skier (4x8 px)
        float drawX = isoX + (TILE_WIDTH - 4)/2f;
        float drawY = isoY + heightOffset;
        
        batch.draw(tex, drawX, drawY);
    }

    private void drawLift(TransformComponent t, Color tint) {
        Texture tex = EntityTextureFactory.getLiftPylonTexture();
        
        float isoX = (t.x - t.z) * (TILE_WIDTH / 2f);
        float isoY = (t.x + t.z) * (TILE_HEIGHT / 2f);
        float heightOffset = t.y * 8f; 
        
        float drawX = isoX + (TILE_WIDTH - 16)/2f;
        float drawY = isoY + heightOffset; 
        
        Color old = batch.getColor();
        // Combine existing batch alpha with tint
        batch.setColor(tint.r, tint.g, tint.b, old.a);
        
        batch.draw(tex, drawX, drawY);
        
        batch.setColor(old); // Restore
    }

    public void dispose() {
        batch.dispose();
        TextureFactory.dispose();
        EntityTextureFactory.dispose();
    }
}

