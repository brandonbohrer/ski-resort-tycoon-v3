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
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.view.LiftBuilder.LiftPreview;

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
    private final MousePicker mousePicker; 
    
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
        this.mousePicker = new MousePicker(simulation.getWorldMap(), camera); // Pass camera
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
            int newHeight = tile.getHeight();
            if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                newHeight--;
            } else {
                newHeight++;
            }
            // Use WorldMap setter to trigger dirty flag for renderer update
            simulation.getWorldMap().setTileHeight(hoveredX, hoveredZ, newHeight);
            
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
                    Entity prevPylon = null;
                    for (Vector2 pos : currentPreview.pylonPositions) {
                         Entity pylon = simulation.getEcsEngine().createEntity();
                         
                         int px = (int)pos.x;
                         int pz = (int)pos.y;
                         // Re-fetch tile to get height
                         Tile t = simulation.getWorldMap().getTile(px, pz);
                         int ph = (t != null) ? t.getHeight() : 0;

                         simulation.getEcsEngine().addComponent(pylon, new TransformComponent(px, ph, pz));
                         LiftComponent liftComp = new LiftComponent(LiftComponent.LiftType.CHAIRLIFT);
                         simulation.getEcsEngine().addComponent(pylon, liftComp);
                         
                         if (prevPylon != null) {
                             // Link previous to current
                             simulation.getEcsEngine().getComponent(prevPylon, LiftComponent.class).nextPylonId = pylon.getId();
                             System.out.println("Linked Pylon " + prevPylon.getId() + " to " + pylon.getId());
                         }
                         prevPylon = pylon;
                    }
                    System.out.println("Lift Built!");
                }
                currentPreview = null;
            }
        }
    }

    private void updateHoveredTile(int screenX, int screenY) {
        // Use new 3D picking
        Vector2 gridPos = mousePicker.pickTile(screenX, screenY);
        
        if (gridPos != null) {
            this.hoveredX = (int) gridPos.x;
            this.hoveredZ = (int) gridPos.y;
        } else {
            // Invalid or off-map
            this.hoveredX = -1;
            this.hoveredZ = -1;
        }
    }

    public int getHoveredX() { return hoveredX; }
    public int getHoveredZ() { return hoveredZ; }
    public InteractionMode getCurrentMode() { return currentMode; }
    public LiftPreview getCurrentPreview() { return currentPreview; }
}