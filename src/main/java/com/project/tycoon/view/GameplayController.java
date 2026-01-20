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
import com.project.tycoon.economy.EconomyManager;
import com.project.tycoon.simulation.TycoonSimulation;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.view.LiftBuilder.LiftPreview;

import com.project.tycoon.ecs.components.TrailComponent; // Add Import

/**
 * Handles gameplay input (Selection, Terrain modification).
 * Separated from CameraController to keep logic clean.
 */
public class GameplayController extends InputAdapter {

    public enum InteractionMode {
        TERRAIN,
        BUILD,
        TRAIL // New Mode
    }

    private final TycoonSimulation simulation;
    private final OrthographicCamera camera;
    private final MousePicker mousePicker;
    private final EconomyManager economy;

    private InteractionMode currentMode = InteractionMode.TERRAIN;

    // Hover state
    private int hoveredX = -1;
    private int hoveredZ = -1;

    // Lift Building State
    private boolean isBuildingLift = false;
    private int buildStartX = -1;
    private int buildStartZ = -1;
    private LiftPreview currentPreview = null;
    private LiftComponent.LiftType selectedLiftType = LiftComponent.LiftType.TBAR; // Default cheapest

    // Trail Building State
    private boolean isPaintingTrail = false;
    private Entity currentTrailEntity = null;

    public GameplayController(TycoonSimulation simulation, OrthographicCamera camera) {
        this.simulation = simulation;
        this.camera = camera;
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

        if (currentMode == InteractionMode.TRAIL && isPaintingTrail) {
            paintTrailTile();
        }

        return false;
    }

    private void paintTrailTile() {
        int radius = 2; // Brush Radius (Diameter ~5)

        for (int z = hoveredZ - radius; z <= hoveredZ + radius; z++) {
            for (int x = hoveredX - radius; x <= hoveredX + radius; x++) {
                // Circular Brush
                float dx = x - hoveredX;
                float dz = z - hoveredZ;
                if (dx * dx + dz * dz > (radius + 0.5f) * (radius + 0.5f))
                    continue; // +0.5 for smoother circle

                Tile tile = simulation.getWorldMap().getTile(x, z);
                if (tile != null && !tile.isTrail()) {
                    // Add to component
                    if (currentTrailEntity != null) {
                        TrailComponent trail = simulation.getEcsEngine().getComponent(currentTrailEntity,
                                TrailComponent.class);

                        // Check if already added to this specific trail entity
                        boolean exists = false;
                        for (Vector2 v : trail.tiles) {
                            if ((int) v.x == x && (int) v.y == z) {
                                exists = true;
                                break;
                            }
                        }

                        if (!exists) {
                            trail.addTile(x, z);

                            // Modify World
                            tile.setTrail(true);
                            tile.setDecoration(com.project.tycoon.world.model.Decoration.NONE); // Clear trees
                            simulation.getWorldMap().setTile(x, z, tile); // Updates dirty flag
                        }
                    }
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

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            updateHoveredTile(screenX, screenY);

            Tile tile = simulation.getWorldMap().getTile(hoveredX, hoveredZ);
            if (tile != null) {
                if (currentMode == InteractionMode.TRAIL) {
                    // Start new trail
                    isPaintingTrail = true;
                    currentTrailEntity = simulation.getEcsEngine().createEntity();
                    simulation.getEcsEngine().addComponent(currentTrailEntity, new TrailComponent());
                    paintTrailTile(); // Paint initial tile
                } else {
                    handleInteraction(tile);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (currentMode == InteractionMode.TRAIL && isPaintingTrail) {
            isPaintingTrail = false;
            currentTrailEntity = null;
            System.out.println("Trail Finished");
            return true;
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