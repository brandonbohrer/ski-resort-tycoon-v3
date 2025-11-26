package com.project.tycoon.view;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.ScreenUtils;
import com.project.tycoon.simulation.TycoonSimulation;
import com.project.tycoon.view.renderer.CombinedRenderer;

/**
 * The visual entry point of the game (LibGDX adapter).
 * It bridges the gap between the visual frame-rate (LibGDX render)
 * and the deterministic simulation logic.
 */
public class TycoonGame extends Game {

    private final TycoonSimulation simulation;
    private CombinedRenderer combinedRenderer;
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

        // Zoom out a bit to see more tiles (30 units wide is about 1 tile since we drew 32px tiles)
        // Let's make the viewport much larger relative to our tile size
        camera = new OrthographicCamera();
        camera.setToOrtho(false, w, h);
        
        // Center camera on map start (approx)
        camera.position.set(0, 0, 0);
        camera.zoom = 2.0f; // Start zoomed out
        camera.update();
        
        cameraController = new CameraController(camera);
        gameplayController = new GameplayController(simulation, camera);
        
        // Multiplexer to handle both camera and gameplay inputs
        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
        multiplexer.addProcessor(gameplayController); // Gameplay first (clicks)
        multiplexer.addProcessor(cameraController);   // Camera second (drags/zooms)
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
        ScreenUtils.clear(Color.BLACK);
        
        // Pass gameplay controller to renderer to draw selection cursor
        boolean isBuildMode = gameplayController.getCurrentMode() == GameplayController.InteractionMode.BUILD;
        combinedRenderer.render(camera, gameplayController.getHoveredX(), gameplayController.getHoveredZ(), isBuildMode, gameplayController.getCurrentPreview());
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }
    
    @Override
    public void dispose() {
        if (combinedRenderer != null) {
            combinedRenderer.dispose();
        }
    }
}

