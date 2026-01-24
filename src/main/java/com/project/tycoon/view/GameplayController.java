package com.project.tycoon.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.components.LiftComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.economy.EconomyManager;
import com.project.tycoon.simulation.TycoonSimulation;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.SnapPoint;
import com.project.tycoon.world.SnapPointManager;
import com.project.tycoon.view.LiftBuilder.LiftPreview;

import com.project.tycoon.ecs.components.TrailComponent; // Add Import

import java.util.*;

/**
 * Handles gameplay input (Selection, Terrain modification).
 * Separated from CameraController to keep logic clean.
 */
public class GameplayController extends InputAdapter {

    public enum InteractionMode {
        NONE, // Default - no interaction
        TERRAIN,
        BUILD,
        TRAIL // New Mode
    }

    private final TycoonSimulation simulation;
    private final MousePicker mousePicker;
    private final EconomyManager economy;

    private InteractionMode currentMode = InteractionMode.NONE;

    // Hover state
    private int hoveredX = -1;
    private int hoveredZ = -1;

    // Lift Building State
    private boolean isBuildingLift = false;
    private int buildStartX = -1;
    private int buildStartZ = -1;
    private LiftPreview currentPreview = null;
    private LiftComponent.LiftType selectedLiftType = LiftComponent.LiftType.TBAR; // Default cheapest

    // Trail Building State (Snap Point System)
    private enum TrailBuildState {
        WAITING_FOR_START, // Waiting to click on start snap point
        PAINTING, // Actively painting trail tiles (auto-follows cursor)
        CONFIRMATION_DIALOG // Showing confirm/cancel dialog
    }

    private TrailBuildState trailBuildState = TrailBuildState.WAITING_FOR_START;
    private SnapPoint trailStartSnapPoint = null;
    private SnapPoint proposedEndSnapPoint = null;
    private List<Vector2> pendingTrailTiles = new ArrayList<>();
    private SnapPoint nearbySnapPoint = null; // Currently hovered snap point
    private Vector2 lastPaintedTile = null; // Prevent duplicate painting
    private static final float SNAP_RADIUS = 3.0f; // tiles

    public GameplayController(TycoonSimulation simulation, OrthographicCamera camera) {
        this.simulation = simulation;
        this.mousePicker = new MousePicker(simulation.getWorldMap(), camera); // Pass camera
        this.economy = simulation.getEconomyManager();
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.B) {
            currentMode = InteractionMode.BUILD;
            isBuildingLift = false;
            currentPreview = null;
            System.out.println("Switched to Mode: " + currentMode);
            return true;
        } else if (keycode == Input.Keys.T) { // Trail Mode
            currentMode = InteractionMode.TRAIL;
            isBuildingLift = false;
            currentPreview = null;
            System.out.println("Switched to Mode: " + currentMode);
            return true;
        } else if (keycode == Input.Keys.M) { // Back to Terrain/Move
            currentMode = InteractionMode.TERRAIN;
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

        // Auto-paint trail tiles when in PAINTING state
        if (currentMode == InteractionMode.TRAIL && trailBuildState == TrailBuildState.PAINTING) {
            autoPaintTrailTile();
        }

        return false;
    }

    // Auto-paint trail tiles as cursor moves (snap point system)
    private void autoPaintTrailTile() {
        Vector2 currentTile = new Vector2(hoveredX, hoveredZ);

        // Prevent duplicate painting
        if (lastPaintedTile != null && lastPaintedTile.equals(currentTile)) {
            return;
        }

        // Paint this tile
        Tile tile = simulation.getWorldMap().getTile(hoveredX, hoveredZ);
        if (tile != null) {
            pendingTrailTiles.add(currentTile);
            lastPaintedTile = currentTile;

            // Visual preview: temporarily mark as trail
            // (will be finalized only on confirmation)
            tile.setTrail(true);
            simulation.getWorldMap().setTile(hoveredX, hoveredZ, tile);
        }
    }

    private void clearTrailCorridor(int centerX, int centerZ) {
        int corridorRadius = 1; // 3x3 area around trail tile
        for (int z = centerZ - corridorRadius; z <= centerZ + corridorRadius; z++) {
            for (int x = centerX - corridorRadius; x <= centerX + corridorRadius; x++) {
                Tile neighbor = simulation.getWorldMap().getTile(x, z);
                if (neighbor == null) {
                    continue;
                }
                if (neighbor.getDecoration() != com.project.tycoon.world.model.Decoration.NONE) {
                    neighbor.setDecoration(com.project.tycoon.world.model.Decoration.NONE);
                    simulation.getWorldMap().setTile(x, z, neighbor);
                }
            }
        }
    }

    private void updateLiftPreview() {
        if (currentMode == InteractionMode.BUILD && isBuildingLift) {
            currentPreview = LiftBuilder.calculatePreview(buildStartX, buildStartZ, hoveredX, hoveredZ);
        } else {
            currentPreview = null;
        }
    }


    // ==== SNAP POINT TRAIL BUILDING METHODS ====
    
    private SnapPoint findNearbySnapPoint(int tileX, int tileZ) {
        List<SnapPoint> nearby = simulation.getSnapPointManager().getSnapPointsNear(tileX, tileZ, SNAP_RADIUS);
        
        // Return closest valid snap point
        for (SnapPoint sp : nearby) {
            if (isValidTrailSnapPoint(sp)) {
                return sp;
            }
        }
        return null;
    }
    
    private boolean isValidTrailSnapPoint(SnapPoint sp) {
        switch (sp.getType()) {
            case BASE_CAMP:
            case TRAIL_END:
            case LIFT_BOTTOM:
            case LIFT_TOP:
                return true;
            default:
                return false;
        }
    }
    
    private void handleTrailClick() {
        nearbySnapPoint = findNearbySnapPoint(hoveredX, hoveredZ);
        
        switch (trailBuildState) {
            case WAITING_FOR_START:
                // Must click on a valid snap point to start
                if (nearbySnapPoint != null) {
                    trailStartSnapPoint = nearbySnapPoint;
                    trailBuildState = TrailBuildState.PAINTING;
                    pendingTrailTiles.clear();
                    
                    // Paint initial tile
                    pendingTrailTiles.add(new Vector2(hoveredX, hoveredZ));
                    lastPaintedTile =  new Vector2(hoveredX, hoveredZ);
                    
                    System.out.println("Trail started from " + nearbySnapPoint.getType());
                } else {
                    System.out.println("Must click on snap point to start trail!");
                }
                break;
                
            case PAINTING:
                // Propose ending at snap point
                if (nearbySnapPoint != null) {
                    proposedEndSnapPoint = nearbySnapPoint;
                    trailBuildState = TrailBuildState.CONFIRMATION_DIALOG;
                    System.out.println("Trail proposed to end at " + nearbySnapPoint.getType());
                    // TODO: Show confirmation dialog
                } else {
                    System.out.println("Must click on snap point to end trail!");
                }
                break;
                
            case CONFIRMATION_DIALOG:
                // Handled by dialog UI
                break;
        }
    }
    
    private void resetTrailState() {
        // Undo visual preview of pending trail tiles
        for (Vector2 tile : pendingTrailTiles) {
            Tile t = simulation.getWorldMap().getTile((int)tile.x, (int)tile.y);
            if (t != null) {
                t.setTrail(false);
                simulation.getWorldMap().setTile((int)tile.x, (int)tile.y, t);
            }
        }
        
        trailBuildState = TrailBuildState.WAITING_FOR_START;
        trailStartSnapPoint = null;
        proposedEndSnapPoint = null;
        pendingTrailTiles.clear();
        nearbySnapPoint = null;
        lastPaintedTile = null;
    }


    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            updateHoveredTile(screenX, screenY);

            Tile tile = simulation.getWorldMap().getTile(hoveredX, hoveredZ);
            if (tile != null) {
                if (currentMode == InteractionMode.TRAIL) {
                    handleTrailClick();
                } else {
                    handleInteraction(tile);
                }
                return true;
            }
        } else if (button == Input.Buttons.RIGHT) {
            // Right-click to cancel trail in PAINTING state
            if (currentMode == InteractionMode.TRAIL && trailBuildState == TrailBuildState.PAINTING) {
                System.out.println("Trail cancelled");
                resetTrailState();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        // Touch up handled inline with clicks in touchDown for snap point system
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
                    int pylonCount = currentPreview.pylonPositions.size();
                    float totalCost = LiftComponent.calculateTotalCost(selectedLiftType, pylonCount);

                    if (!economy.canAfford(totalCost)) {
                        System.out.println("Cannot afford lift! Cost: $" + totalCost + ", Available: $"
                                + economy.getCurrentMoney());
                        currentPreview = null;
                        return;
                    }

                    Entity prevPylon = null;
                    for (Vector2 pos : currentPreview.pylonPositions) {
                        Entity pylon = simulation.getEcsEngine().createEntity();

                        int px = (int) pos.x;
                        int pz = (int) pos.y;
                        // Re-fetch tile to get height
                        Tile t = simulation.getWorldMap().getTile(px, pz);
                        int ph = (t != null) ? t.getHeight() : 0;

                        simulation.getEcsEngine().addComponent(pylon, new TransformComponent(px, ph, pz));
                        LiftComponent liftComp = new LiftComponent(selectedLiftType);
                        simulation.getEcsEngine().addComponent(pylon, liftComp);

                        if (prevPylon != null) {
                            // Link previous to current
                            simulation.getEcsEngine().getComponent(prevPylon, LiftComponent.class).nextPylonId = pylon
                                    .getId();
                            System.out.println("Linked Pylon " + prevPylon.getId() + " to " + pylon.getId());
                        }
                        prevPylon = pylon;
                    }

                    // Deduct cost
                    economy.purchase(totalCost);
                    System.out.println("Lift built for $" + totalCost + ". Remaining: $" + economy.getCurrentMoney());
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

    public int getHoveredX() {
        return hoveredX;
    }

    public int getHoveredZ() {
        return hoveredZ;
    }

    public InteractionMode getCurrentMode() {
        return currentMode;
    }

    public void setMode(InteractionMode mode) {
        this.currentMode = mode;
        this.isBuildingLift = false;
        this.currentPreview = null;
        System.out.println("Mode set to: " + mode);
    }

    public LiftPreview getCurrentPreview() {
        return currentPreview;
    }

    public void setSelectedLiftType(LiftComponent.LiftType type) {
        this.selectedLiftType = type;
        System.out.println("Selected lift type: " + type);
    }

    public LiftComponent.LiftType getSelectedLiftType() {
        return selectedLiftType;
    }
}