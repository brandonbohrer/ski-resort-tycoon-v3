package com.project.tycoon.view;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.ScreenUtils;
import com.project.tycoon.simulation.TycoonSimulation;
import com.project.tycoon.view.renderer.CombinedRenderer;
import com.project.tycoon.view.ui.GameHUD; // Import

/**
 * The visual entry point of the game (LibGDX adapter).
 * It bridges the gap between the visual frame-rate (LibGDX render)
 * and the deterministic simulation logic.
 */
public class TycoonGame extends Game {

    private final TycoonSimulation simulation;
    private CombinedRenderer combinedRenderer;
    private GameHUD gameHUD; // Add HUD
    private OrthographicCamera camera;
    private CameraController cameraController;
    private GameplayController gameplayController;
    
    // Simulation timing accumulator
    private double accumulator = 0.0;
    private final double TIME_STEP = 1.0 / 60.0;

    public TycoonGame(TycoonSimulation simulation) {
        this.simulation = simulation;
    }

    @Override
    public void create() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Setup 3D Isometric Camera
        // Viewport size determines how much of the world we see.
        float viewportSize = 100f; // Increased to see more
        camera = new OrthographicCamera(viewportSize, viewportSize * (h / w));
        
        // Position at isometric angle looking at the CENTER of the map
        float mapCenter = 128f; // Map is 256x256
        camera.position.set(mapCenter + 100f, 100f, mapCenter + 100f); 
        camera.lookAt(mapCenter, 0f, mapCenter);
        camera.near = 1f;
        camera.far = 3000f; // Draw distance
        camera.update();
        
        cameraController = new CameraController(camera);
        gameplayController = new GameplayController(simulation, camera);
        
        // Initialize HUD
        gameHUD = new GameHUD(gameplayController);
        
        // Multiplexer to handle both camera and gameplay inputs
        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
        multiplexer.addProcessor(gameHUD.getStage()); // UI First!
        multiplexer.addProcessor(gameplayController); // Gameplay second
        multiplexer.addProcessor(cameraController);   // Camera last
        Gdx.input.setInputProcessor(multiplexer);
        
        combinedRenderer = new CombinedRenderer(simulation.getWorldMap(), simulation.getEcsEngine());
    }

    @Override
    public void render() {
        // 1. Update Simulation (Fixed Timestep)
        double deltaTime = Gdx.graphics.getDeltaTime();
        
        // Cap dt to avoid spiral of death
        if (deltaTime > 0.25) deltaTime = 0.25;
        
        accumulator += deltaTime;
        
        while (accumulator >= TIME_STEP) {
            // We use a dummy tick number for now, but simulation tracks its own state mostly
            simulation.tick(0); 
            accumulator -= TIME_STEP;
        }

        // Update Controller (Keyboard movement)
        cameraController.update();

        // 2. Render
        // Clear Color AND Depth buffer (Required for 3D)
        ScreenUtils.clear(Color.SKY, true);
        
        // Pass gameplay controller to renderer to draw selection cursor
        boolean isBuildMode = gameplayController.getCurrentMode() == GameplayController.InteractionMode.BUILD;
        combinedRenderer.render(camera, gameplayController.getHoveredX(), gameplayController.getHoveredZ(), isBuildMode, gameplayController.getCurrentPreview());
        
        // 3. Render UI (On top)
        gameHUD.render((float)deltaTime);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        gameHUD.resize(width, height);
    }
    
    @Override
    public void dispose() {
        if (combinedRenderer != null) {
            combinedRenderer.dispose();
        }
        if (gameHUD != null) {
            gameHUD.dispose();
        }
    }
}

