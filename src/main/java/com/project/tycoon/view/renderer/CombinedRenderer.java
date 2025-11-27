package com.project.tycoon.view.renderer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.project.tycoon.ecs.Engine;
import com.project.tycoon.view.LiftBuilder.LiftPreview;
import com.project.tycoon.world.model.WorldMap;

/**
 * Master Renderer that orchestrates sub-renderers.
 * Refactored for structural modularity.
 */
public class CombinedRenderer {

    private final ModelBatch modelBatch;
    private final Environment environment;
    
    private final RenderAssetManager assetManager;
    private final TerrainRenderer terrainRenderer;
    private final EntityRenderer entityRenderer;

    public CombinedRenderer(WorldMap worldMap, Engine ecsEngine) {
        this.modelBatch = new ModelBatch();
        
        // Setup Lighting
        this.environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(1.0f, 1.0f, 1.0f, -1f, -0.8f, -0.2f));
        
        // Initialize Sub-systems
        this.assetManager = new RenderAssetManager();
        this.terrainRenderer = new TerrainRenderer(worldMap, assetManager);
        this.entityRenderer = new EntityRenderer(ecsEngine, worldMap, assetManager);
    }

    public void render(OrthographicCamera camera, int hoveredX, int hoveredZ, boolean isBuildMode, LiftPreview preview) {
        // Update logic (e.g. terrain rebuild if dirty)
        terrainRenderer.update();

        modelBatch.begin(camera);
        
        // Delegate rendering
        terrainRenderer.render(modelBatch, environment);
        entityRenderer.render(modelBatch, environment, hoveredX, hoveredZ, isBuildMode, preview);
        
        modelBatch.end();
    }

    public void dispose() {
        modelBatch.dispose();
        assetManager.dispose();
        terrainRenderer.dispose();
        // EntityRenderer has no resources to dispose currently
    }
}