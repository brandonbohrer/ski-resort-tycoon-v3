package com.project.tycoon.view.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.project.tycoon.economy.EconomyManager;
import com.project.tycoon.simulation.TycoonSimulation;
import com.project.tycoon.view.GameplayController;

/**
 * Main HUD orchestrator. Manages UI components and coordinates between them.
 */
public class GameHUD {

    private final Stage stage;
    private final Skin skin;

    // UI Components
    private MoneyTicker moneyTicker;
    private CategoryBar categoryBar;
    private ConstructionMenu constructionMenu;
    private SpeedControls speedControls;
    private TimeDisplay timeDisplay;
    private FinancesScreen financesScreen;
    private TrailConfirmDialog trailConfirmDialog;
    private ModeIndicator modeIndicator;
    private EntityInfoPanel entityInfoPanel;

    private final TycoonSimulation simulation;
    private final GameplayController controller;

    public GameHUD(GameplayController controller, EconomyManager economy, TycoonSimulation simulation) {
        this.stage = new Stage(new ScreenViewport());
        this.skin = UIStyleGenerator.generateSkin();
        this.simulation = simulation;
        this.controller = controller;

        buildUI(controller, economy);
    }

    private void buildUI(GameplayController controller, EconomyManager economy) {
        // Create components
        moneyTicker = new MoneyTicker(skin, economy, stage);

        categoryBar = new CategoryBar(skin, stage, categoryName -> {
            handleCategorySelected(categoryName);
        });

        constructionMenu = new ConstructionMenu(skin, stage, controller);
        constructionMenu.setCloseListener(() -> {
            // When construction menu closes via right-click, uncheck category button
            categoryBar.uncheckAll();
        });

        speedControls = new SpeedControls(skin, stage, new SpeedControls.SpeedChangeListener() {
            @Override
            public void onPauseToggled(boolean paused) {
                simulation.setPaused(paused);
            }

            @Override
            public void onSpeedChanged(float timeScale) {
                simulation.setTimeScale(timeScale);
            }
        });

        timeDisplay = new TimeDisplay(skin, stage, simulation.getDayTimeSystem());

        // Trail confirmation dialog
        trailConfirmDialog = new TrailConfirmDialog(skin);

        // Mode indicator
        modeIndicator = new ModeIndicator(skin, stage);
        
        // Entity info panel
        entityInfoPanel = new EntityInfoPanel(skin, stage, simulation.getEcsEngine());
    }

    private void handleCategorySelected(String categoryName) {
        // Hide all menus first
        constructionMenu.hide();

        // If null, user deselected (clicked same button again)
        if (categoryName == null) {
            return;
        }

        // Special handling for Finances - open fullscreen overlay
        if ("Finances".equals(categoryName)) {
            // Reset category bar and construction menu state
            categoryBar.uncheckAll();
            constructionMenu.reset();

            if (financesScreen != null) {
                financesScreen.show();
            }
            return;
        }

        // Show appropriate menu
        if ("Construction".equals(categoryName)) {
            constructionMenu.show();
        }

        System.out.println("Selected Category: " + categoryName);
    }

    public void setFinancesScreen(FinancesScreen financesScreen) {
        this.financesScreen = financesScreen;
        moneyTicker.setFinancesScreen(financesScreen);
    }

    public TrailConfirmDialog getTrailConfirmDialog() {
        return trailConfirmDialog;
    }

    public void render(float dt) {
        moneyTicker.update(dt);
        timeDisplay.update(dt);
        entityInfoPanel.update(); // Update selected entity info

        // Update mode indicator
        updateModeIndicator();

        stage.act(dt);
        stage.draw();

        // Render trail confirmation dialog (on top of everything)
        trailConfirmDialog.render(dt);
    }

    private void updateModeIndicator() {
        String modeText = "";

        switch (controller.getCurrentMode()) {
            case BUILD:
                modeText = "BUILD MODE - Click to place lift";
                break;
            case TRAIL:
                // Show trail-specific state
                switch (controller.getTrailBuildState()) {
                    case WAITING_FOR_START:
                        modeText = "TRAIL MODE - Click snap point to start";
                        break;
                    case PAINTING:
                        int tileCount = controller.getPendingTrailTiles().size();
                        float cost = tileCount * 5.0f;
                        modeText = "TRAIL MODE - Painting (" + tileCount + " tiles, $" +
                                String.format("%.0f", cost) + ") - Right-click to cancel";
                        break;
                    case CONFIRMATION_DIALOG:
                        modeText = "";
                        break;
                }
                break;
            case TERRAIN:
                modeText = "TERRAIN MODE - Click to modify terrain";
                break;
            case NONE:
            default:
                modeText = "";
                break;
        }

        modeIndicator.update(modeText);
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        trailConfirmDialog.resize(width, height);
    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
        trailConfirmDialog.dispose();
    }

    public Stage getStage() {
        return stage;
    }

    public Skin getSkin() {
        return skin;
    }

    public ModeIndicator getModeIndicator() {
        return modeIndicator;
    }
    
    public EntityInfoPanel getEntityInfoPanel() {
        return entityInfoPanel;
    }

    /**
     * Get InputProcessor for keyboard shortcuts.
     */
    public InputAdapter getKeyboardProcessor() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                    case Input.Keys.SPACE:
                        speedControls.togglePause();
                        return true;
                    case Input.Keys.NUM_1:
                    case Input.Keys.NUMPAD_1:
                        speedControls.setSpeed(1.0f);
                        return true;
                    case Input.Keys.NUM_2:
                    case Input.Keys.NUMPAD_2:
                        speedControls.setSpeed(2.0f);
                        return true;
                    case Input.Keys.NUM_3:
                    case Input.Keys.NUMPAD_3:
                        speedControls.setSpeed(3.0f);
                        return true;
                }
                return false;
            }
        };
    }
}
