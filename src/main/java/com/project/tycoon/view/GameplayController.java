package com.project.tycoon.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.components.LiftComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.simulation.TycoonSimulation;
import com.project.tycoon.view.util.IsoUtils;
import com.project.tycoon.world.model.Tile;

import com.project.tycoon.view.LiftBuilder.LiftPreview; // Add Import

/**
 * Handles gameplay input (Selection, Terrain modification).
 * Separated from CameraController to keep logic clean.
 */
public class GameplayController extends InputAdapter {

    public enum InteractionMode {
        TERRAIN,
        BUILD
    }

    private final TycoonSimulation simulation;
    private final OrthographicCamera camera;
    
    private InteractionMode currentMode = InteractionMode.TERRAIN;
    
    // Hover state
    private int hoveredX = -1;
    private int hoveredZ = -1;
    
    // Lift Building State
    private boolean isBuildingLift = false;
    private int buildStartX = -1;
    private int buildStartZ = -1;
    private LiftPreview currentPreview = null;

    public GameplayController(TycoonSimulation simulation, OrthographicCamera camera) {
        this.simulation = simulation;
        this.camera = camera;
    }
    
    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.B) {
            currentMode = (currentMode == InteractionMode.TERRAIN) ? InteractionMode.BUILD : InteractionMode.TERRAIN;
            // Reset state on switch
            isBuildingLift = false;
            currentPreview = null;
            System.out.println("Switched to Mode: " + currentMode);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        updateHoveredTile(screenX, screenY);
        updateLiftPreview();
        return false; 
    }
    
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
         updateHoveredTile(screenX, screenY);
         updateLiftPreview();
         return false;
    }
    
    private void updateLiftPreview() {
        if (currentMode == InteractionMode.BUILD && isBuildingLift) {
            currentPreview = LiftBuilder.calculatePreview(buildStartX, buildStartZ, hoveredX, hoveredZ);
        } else {
            currentPreview = null;
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            updateHoveredTile(screenX, screenY);
            
            Tile tile = simulation.getWorldMap().getTile(hoveredX, hoveredZ);
            if (tile != null) {
                handleInteraction(tile);
                return true;
            }
        }
        return false;
    }
    
    private void handleInteraction(Tile tile) {
        if (currentMode == InteractionMode.TERRAIN) {
             // Modify Terrain
            if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                tile.setHeight(tile.getHeight() - 1);
            } else {
                tile.setHeight(tile.getHeight() + 1);
            }
        } else if (currentMode == InteractionMode.BUILD) {
            if (!isBuildingLift) {
                // START BUILDING
                isBuildingLift = true;
                buildStartX = hoveredX;
                buildStartZ = hoveredZ;
                System.out.println("Lift Start: " + buildStartX + "," + buildStartZ);
            } else {
                // FINISH BUILDING
                isBuildingLift = false;
                
                if (currentPreview != null && currentPreview.isValid) {
                    for (Vector2 pos : currentPreview.pylonPositions) {
                         Entity pylon = simulation.getEcsEngine().createEntity();
                         
                         int px = (int)pos.x;
                         int pz = (int)pos.y;
                         // Re-fetch tile to get height
                         Tile t = simulation.getWorldMap().getTile(px, pz);
                         int ph = (t != null) ? t.getHeight() : 0;

                         simulation.getEcsEngine().addComponent(pylon, new TransformComponent(px, ph, pz));
                         simulation.getEcsEngine().addComponent(pylon, new LiftComponent(LiftComponent.LiftType.CHAIRLIFT));
                    }
                    System.out.println("Lift Built!");
                }
                currentPreview = null;
            }
        }
    }

    private void updateHoveredTile(int screenX, int screenY) {
        Vector3 worldPos = camera.unproject(new Vector3(screenX, screenY, 0));
        Vector2 gridPos = IsoUtils.worldToGrid(worldPos.x, worldPos.y);
        
        // Floor to get integer grid indices
        this.hoveredX = (int) Math.floor(gridPos.x);
        this.hoveredZ = (int) Math.floor(gridPos.y);
    }

    public int getHoveredX() { return hoveredX; }
    public int getHoveredZ() { return hoveredZ; }
    public InteractionMode getCurrentMode() { return currentMode; }
    public LiftPreview getCurrentPreview() { return currentPreview; }
}

