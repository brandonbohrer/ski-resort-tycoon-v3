package com.project.tycoon.view;

import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.components.*;
import com.project.tycoon.world.model.WorldMap;

/**
 * Manages entity selection - determines what entity is at a given world position.
 */
public class SelectionManager {
    
    private final Engine engine;
    private final WorldMap worldMap;
    private Entity selectedEntity = null;
    private Entity hoveredEntity = null; // Track what entity is under cursor
    
    private static final float SELECTION_RADIUS = 2.0f; // How close to click for selection
    
    public SelectionManager(Engine engine, WorldMap worldMap) {
        this.engine = engine;
        this.worldMap = worldMap;
    }
    
    /**
     * Update what entity is being hovered over (for cursor and highlight).
     */
    public void updateHover(int worldX, int worldZ) {
        // Check for entities at this position (same priority as selection)
        Entity skier = findSkierAt(worldX, worldZ);
        if (skier != null) {
            hoveredEntity = skier;
            return;
        }
        
        Entity building = findBuildingAt(worldX, worldZ);
        if (building != null) {
            hoveredEntity = building;
            return;
        }
        
        Entity lift = findLiftAt(worldX, worldZ);
        if (lift != null) {
            hoveredEntity = lift;
            return;
        }
        
        hoveredEntity = null;
    }
    
    /**
     * Attempt to select an entity at the given world coordinates.
     * Returns true if something was selected.
     */
    public boolean selectAt(int worldX, int worldZ) {
        // Priority: Skiers > Buildings > Lifts (skiers are small, need priority)
        
        // Try to select a skier first
        Entity skier = findSkierAt(worldX, worldZ);
        if (skier != null) {
            selectedEntity = skier;
            return true;
        }
        
        // Try to select a building
        Entity building = findBuildingAt(worldX, worldZ);
        if (building != null) {
            selectedEntity = building;
            return true;
        }
        
        // Try to select a lift pylon
        Entity lift = findLiftAt(worldX, worldZ);
        if (lift != null) {
            selectedEntity = lift;
            return true;
        }
        
        // Nothing found, deselect
        selectedEntity = null;
        return false;
    }
    
    /**
     * Clear current selection.
     */
    public void clearSelection() {
        selectedEntity = null;
    }
    
    public Entity getSelectedEntity() {
        return selectedEntity;
    }
    
    public boolean hasSelection() {
        return selectedEntity != null;
    }
    
    public Entity getHoveredEntity() {
        return hoveredEntity;
    }
    
    public boolean hasHoveredEntity() {
        return hoveredEntity != null;
    }
    
    private Entity findSkierAt(int worldX, int worldZ) {
        for (Entity entity : engine.getEntities()) {
            if (!engine.hasComponent(entity, SkierComponent.class)) continue;
            
            TransformComponent transform = engine.getComponent(entity, TransformComponent.class);
            if (transform == null) continue;
            
            float dx = transform.x - worldX;
            float dz = transform.z - worldZ;
            float distSq = dx * dx + dz * dz;
            
            if (distSq < SELECTION_RADIUS * SELECTION_RADIUS) {
                return entity;
            }
        }
        return null;
    }
    
    private Entity findBuildingAt(int worldX, int worldZ) {
        for (Entity entity : engine.getEntities()) {
            if (!engine.hasComponent(entity, BaseCampComponent.class)) continue;
            
            TransformComponent transform = engine.getComponent(entity, TransformComponent.class);
            if (transform == null) continue;
            
            // Buildings are larger, use larger selection area
            float selectionSize = 8.0f;
            float dx = Math.abs(transform.x - worldX);
            float dz = Math.abs(transform.z - worldZ);
            
            if (dx < selectionSize && dz < selectionSize) {
                return entity;
            }
        }
        return null;
    }
    
    private Entity findLiftAt(int worldX, int worldZ) {
        for (Entity entity : engine.getEntities()) {
            if (!engine.hasComponent(entity, LiftComponent.class)) continue;
            
            TransformComponent transform = engine.getComponent(entity, TransformComponent.class);
            if (transform == null) continue;
            
            float dx = transform.x - worldX;
            float dz = transform.z - worldZ;
            float distSq = dx * dx + dz * dz;
            
            if (distSq < SELECTION_RADIUS * SELECTION_RADIUS) {
                return entity;
            }
        }
        return null;
    }
}

