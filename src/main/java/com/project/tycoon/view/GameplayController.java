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
import com.project.tycoon.world.model.TrailDifficulty;
import com.project.tycoon.world.TrailDifficultyCalculator;
import com.project.tycoon.view.LiftBuilder.LiftPreview;
import com.project.tycoon.view.ui.TrailConfirmDialog;

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
    public enum TrailBuildState {
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
    private static final float SNAP_RADIUS = 8.0f; // tiles (increased from 3.0 for easier snapping)

    private TrailConfirmDialog trailConfirmDialog; // UI dialog for confirmation

    public GameplayController(TycoonSimulation simulation, OrthographicCamera camera) {
        this.simulation = simulation;
        this.mousePicker = new MousePicker(simulation.getWorldMap(), camera); // Pass camera
        this.economy = simulation.getEconomyManager();
    }

    public void setTrailConfirmDialog(TrailConfirmDialog dialog) {
        this.trailConfirmDialog = dialog;

        // Set up listeners
        if (dialog != null) {
            dialog.setListener(new TrailConfirmDialog.ConfirmListener() {
                @Override
                public void onConfirm() {
                    confirmTrail();
                }

                @Override
                public void onCancel() {
                    cancelTrail();
                }
            });
        }
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.B) {
            setMode(InteractionMode.BUILD);
            return true;
        } else if (keycode == Input.Keys.T) { // Trail Mode
            setMode(InteractionMode.TRAIL);
            return true;
        } else if (keycode == Input.Keys.M) { // Back to Terrain/Move
            setMode(InteractionMode.TERRAIN);
            return true;
        } else if (keycode == Input.Keys.ENTER) {
            // Confirm trail
            if (currentMode == InteractionMode.TRAIL && trailBuildState == TrailBuildState.CONFIRMATION_DIALOG) {
                confirmTrail();
                return true;
            }
        } else if (keycode == Input.Keys.ESCAPE) {
            // Cancel trail confirmation
            if (currentMode == InteractionMode.TRAIL && trailBuildState == TrailBuildState.CONFIRMATION_DIALOG) {
                cancelTrail();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        updateHoveredTile(screenX, screenY);
        updateLiftPreview();

        // Update nearby snap point for trail mode visual feedback
        if (currentMode == InteractionMode.TRAIL) {
            nearbySnapPoint = findNearbySnapPoint(hoveredX, hoveredZ);

            // Auto-paint when in PAINTING state
            if (trailBuildState == TrailBuildState.PAINTING) {
                autoPaintTrailTile();
            }
        }

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
        int radius = 2; // Brush Radius (Diameter ~5 tiles like original)

        for (int z = hoveredZ - radius; z <= hoveredZ + radius; z++) {
            for (int x = hoveredX - radius; x <= hoveredX + radius; x++) {
                // Circular Brush
                float dx = x - hoveredX;
                float dz = z - hoveredZ;
                if (dx * dx + dz * dz > (radius + 0.5f) * (radius + 0.5f))
                    continue; // +0.5 for smoother circle

                Tile tile = simulation.getWorldMap().getTile(x, z);
                if (tile != null && !tile.isTrail()) {
                    Vector2 tilePos = new Vector2(x, z);

                    // Check if already painted
                    boolean exists = false;
                    for (Vector2 v : pendingTrailTiles) {
                        if ((int) v.x == x && (int) v.y == z) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        pendingTrailTiles.add(tilePos);

                        // Visual preview: temporarily mark as trail
                        tile.setTrail(true);
                        simulation.getWorldMap().setTile(x, z, tile);
                    }
                }
            }
        }

        // Update last painted to center position
        lastPaintedTile = new Vector2(hoveredX, hoveredZ);
    }

    private void clearTrailCorridor(int centerX, int centerZ) {
        int corridorRadius = 1; // 3x3 area around trail tile
        for (int z = centerZ - corridorRadius; z <= centerZ + corridorRadius; z++) {
            for (int x = centerX - corridorRadius; x <= centerX + corridorRadius; x++) {
                Tile neighbor = simulation.getWorldMap().getTile(x, z);
                if (neighbor == null) {
                    continue;
                }
                if (neighbor.getDecoration() == com.project.tycoon.world.model.Decoration.ROCK) {
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
                    lastPaintedTile = new Vector2(hoveredX, hoveredZ);

                    System.out.println("Trail started from " + nearbySnapPoint.getType());
                } else {
                    System.out.println("Must click on snap point to start trail!");
                }
                break;

            case PAINTING:
                // Propose ending at snap point
                if (nearbySnapPoint != null) {
                    proposedEndSnapPoint = nearbySnapPoint;
                    showTrailConfirmationDialog();
                } else {
                    System.out.println("Must click on snap point to end trail!");
                }
                break;

            case CONFIRMATION_DIALOG:
                // Handled by dialog UI
                break;
        }
    }

    private void showTrailConfirmationDialog() {
        float cost = calculateTrailCost();
        TrailDifficulty difficulty = calculateCurrentTrailDifficulty();
        trailBuildState = TrailBuildState.CONFIRMATION_DIALOG;

        // Show UI dialog with calculated difficulty
        if (trailConfirmDialog != null) {
            String startType = trailStartSnapPoint != null ? trailStartSnapPoint.getType().toString() : "Unknown";
            String endType = proposedEndSnapPoint != null ? proposedEndSnapPoint.getType().toString() : "Unknown";
            trailConfirmDialog.show(pendingTrailTiles.size(), cost, startType, endType, difficulty);
        }
    }

    private float calculateTrailCost() {
        return pendingTrailTiles.size() * 5.0f; // $5 per tile (very cheap!)
    }

    /**
     * Calculate current trail difficulty while painting.
     * Used for real-time visual feedback and confirmation dialog.
     */
    private TrailDifficulty calculateCurrentTrailDifficulty() {
        if (pendingTrailTiles.isEmpty()) {
            return TrailDifficulty.GREEN;
        }
        return TrailDifficultyCalculator.calculate(simulation.getWorldMap(), pendingTrailTiles);
    }

    /**
     * Get current trail difficulty (for real-time UI updates).
     */
    public TrailDifficulty getPendingTrailDifficulty() {
        return calculateCurrentTrailDifficulty();
    }

    public void confirmTrail() {
        if (trailBuildState != TrailBuildState.CONFIRMATION_DIALOG) {
            return;
        }

        float cost = calculateTrailCost();
        if (!economy.canAfford(cost)) {
            System.out.println("Cannot afford trail! Cost: $" + cost);
            // Full reset to avoid confusion/stuck state
            resetTrailState();
            return;
        }

        // Finalize trail
        finalizeTrail();

        // Deduct cost
        economy.purchase(cost);
        System.out.println("Trail built for $" + cost + ". Remaining: $" + economy.getCurrentMoney());

        // Reset to waiting for new trail
        trailBuildState = TrailBuildState.WAITING_FOR_START;
        trailStartSnapPoint = null;
        proposedEndSnapPoint = null;
        pendingTrailTiles.clear();
        nearbySnapPoint = null;
        lastPaintedTile = null;
    }

    public void cancelTrail() {
        if (trailBuildState == TrailBuildState.CONFIRMATION_DIALOG) {
            // Go back to painting
            trailBuildState = TrailBuildState.PAINTING;
            System.out.println("Trail confirmation cancelled, back to painting");
        } else {
            // Full cancel
            resetTrailState();
        }
    }

    private void finalizeTrail() {
        // Clear obstacles along trail
        for (Vector2 tile : pendingTrailTiles) {
            clearTrailCorridor((int) tile.x, (int) tile.y);
        }

        // Get start and end positions
        Vector2 startTile = pendingTrailTiles.get(0);
        Vector2 endTile = pendingTrailTiles.get(pendingTrailTiles.size() - 1);

        // Create TRAIL_START snap point at beginning
        SnapPoint trailStart = new SnapPoint(
                startTile.x, startTile.y,
                SnapPoint.SnapPointType.TRAIL_START,
                null // No owner entity for trail segments
        );
        simulation.getSnapPointManager().registerSnapPoint(trailStart);

        // Create TRAIL_END snap point at end
        SnapPoint trailEnd = new SnapPoint(
                endTile.x, endTile.y,
                SnapPoint.SnapPointType.TRAIL_END,
                null);
        simulation.getSnapPointManager().registerSnapPoint(trailEnd);

        // Connect start snap point to trail start
        simulation.getSnapPointManager().connectSnapPoints(
                trailStartSnapPoint.getId(),
                trailStart.getId());

        // Connect trail end to end snap point
        simulation.getSnapPointManager().connectSnapPoints(
                trailEnd.getId(),
                proposedEndSnapPoint.getId());

        System.out.println("Trail created from " + trailStartSnapPoint.getType() +
                " to " + proposedEndSnapPoint.getType());

        // Save trail difficulty to tiles
        TrailDifficulty difficulty = calculateCurrentTrailDifficulty();
        for (Vector2 tilePos : pendingTrailTiles) {
            Tile t = simulation.getWorldMap().getTile((int) tilePos.x, (int) tilePos.y);
            if (t != null) {
                t.setTrailDifficulty(difficulty);
                simulation.getWorldMap().setTile((int) tilePos.x, (int) tilePos.y, t);
            }
        }
    }

    private void resetTrailState() {
        // Undo visual preview of pending trail tiles
        for (Vector2 tile : pendingTrailTiles) {
            Tile t = simulation.getWorldMap().getTile((int) tile.x, (int) tile.y);
            if (t != null) {
                t.setTrail(false);
                simulation.getWorldMap().setTile((int) tile.x, (int) tile.y, t);
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
            // Right-click behavior in trail mode
            if (currentMode == InteractionMode.TRAIL) {
                if (trailBuildState == TrailBuildState.PAINTING) {
                    // Cancel current trail, back to waiting
                    System.out.println("Trail cancelled");
                    resetTrailState();
                    return true;
                } else if (trailBuildState == TrailBuildState.WAITING_FOR_START) {
                    // Exit trail mode entirely
                    System.out.println("Exiting trail mode");
                    setMode(InteractionMode.NONE);
                    return true;
                }
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
                    Entity firstPylon = null;
                    Entity lastPylon = null;

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
                        } else {
                            firstPylon = pylon; // First pylon
                        }

                        prevPylon = pylon;
                        lastPylon = pylon; // Keep track of last pylon
                    }

                    // Create snap points for lift bottom and top
                    if (firstPylon != null) {
                        TransformComponent firstT = simulation.getEcsEngine().getComponent(firstPylon,
                                TransformComponent.class);
                        SnapPoint liftBottom = new SnapPoint(
                                firstT.x, firstT.z,
                                SnapPoint.SnapPointType.LIFT_BOTTOM,
                                firstPylon.getId());
                        simulation.getSnapPointManager().registerSnapPoint(liftBottom);
                        
                        // Connect lift bottom to BASE_CAMP (so skiers can path from base to lift)
                        List<SnapPoint> baseCamps = simulation.getSnapPointManager()
                                .getSnapPointsByType(SnapPoint.SnapPointType.BASE_CAMP);
                        if (!baseCamps.isEmpty()) {
                            simulation.getSnapPointManager().connectSnapPoints(
                                    baseCamps.get(0).getId(), 
                                    liftBottom.getId());
                            System.out.println("Connected LIFT_BOTTOM to BASE_CAMP");
                        }
                    }

                    if (lastPylon != null && lastPylon != firstPylon) {
                        TransformComponent lastT = simulation.getEcsEngine().getComponent(lastPylon,
                                TransformComponent.class);
                        SnapPoint liftTop = new SnapPoint(
                                lastT.x, lastT.z,
                                SnapPoint.SnapPointType.LIFT_TOP,
                                lastPylon.getId());
                        simulation.getSnapPointManager().registerSnapPoint(liftTop);
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
        // Cleanup previous mode
        if (this.currentMode == InteractionMode.TRAIL) {
            resetTrailState();
        }

        this.currentMode = mode;
        this.isBuildingLift = false;
        this.currentPreview = null;

        // Reset state for new mode if needed
        if (mode == InteractionMode.TRAIL) {
            resetTrailState();
        }

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

    // ==== SNAP POINT VISUAL FEEDBACK ====

    public boolean isValidSnapPointNearby() {
        return nearbySnapPoint != null;
    }

    public SnapPoint getNearbySnapPoint() {
        return nearbySnapPoint;
    }

    public boolean isTrailMode() {
        return currentMode == InteractionMode.TRAIL;
    }

    public TrailBuildState getTrailBuildState() {
        return trailBuildState;
    }

    public List<Vector2> getPendingTrailTiles() {
        return new ArrayList<>(pendingTrailTiles);
    }

    public SnapPoint getTrailStartSnapPoint() {
        return trailStartSnapPoint;
    }
}