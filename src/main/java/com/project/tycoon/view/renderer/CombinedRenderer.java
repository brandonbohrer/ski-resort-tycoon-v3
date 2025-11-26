package com.project.tycoon.view.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.Vector3;
import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.components.LiftComponent;
import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.TerrainType;
import com.project.tycoon.world.model.WorldMap;
import com.project.tycoon.view.util.IsoUtils;
import com.project.tycoon.view.LiftBuilder.LiftPreview;

import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute; // Import IntAttribute

/**
 * Renders the game world using 3D meshes for a smooth "Snowtopia" aesthetic.
 * Replaces the old Sprite-based rendering.
 */
public class CombinedRenderer {

    private final WorldMap worldMap;
    private final Engine ecsEngine;
    
    private final ModelBatch modelBatch;
    private final Environment environment;
    
    private Model terrainModel;
    private ModelInstance terrainInstance;
    
    // Reusable entity models (simple shapes for now)
    private Model skierModel;
    private Model liftPylonModel;
    private Model cursorModel;

    public CombinedRenderer(WorldMap worldMap, Engine ecsEngine) {
        this.worldMap = worldMap;
        this.ecsEngine = ecsEngine;
        
        this.modelBatch = new ModelBatch();
        
        // 1. Setup Lighting
        this.environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
        
        // 2. Build Models
        buildTerrainModel();
        buildEntityModels();
    }
    
    private void buildEntityModels() {
        ModelBuilder mb = new ModelBuilder();
        
        // Skier: Red Box
        skierModel = mb.createBox(0.4f, 0.8f, 0.4f, 
            new Material(ColorAttribute.createDiffuse(Color.RED)),
            Usage.Position | Usage.Normal);
            
        // Lift Pylon: Gray Pillar
        liftPylonModel = mb.createBox(0.2f, 3.0f, 0.2f, 
            new Material(ColorAttribute.createDiffuse(Color.GRAY)),
            Usage.Position | Usage.Normal);

        // Cursor: Semi-transparent Blue Box
        cursorModel = mb.createBox(1.0f, 0.1f, 1.0f,
            new Material(ColorAttribute.createDiffuse(new Color(0f, 0f, 1f, 0.5f))),
            Usage.Position | Usage.Normal);
    }

    private void buildTerrainModel() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        
        int chunkSize = 64;
        int width = worldMap.getWidth();
        int depth = worldMap.getDepth();
        
        Vector3 p1 = new Vector3();
        Vector3 p2 = new Vector3();
        Vector3 p3 = new Vector3();
        Vector3 p4 = new Vector3();
        Vector3 norm = new Vector3(0, 1, 0);

        for (int cz = 0; cz < depth - 1; cz += chunkSize) {
            for (int cx = 0; cx < width - 1; cx += chunkSize) {
                
                MeshPartBuilder builder = mb.part(
                    "chunk_" + cx + "_" + cz, 
                    GL20.GL_TRIANGLES, 
                    Usage.Position | Usage.ColorPacked | Usage.Normal, 
                    new Material(
                        ColorAttribute.createDiffuse(Color.WHITE), 
                        new IntAttribute(IntAttribute.CullFace, 0) // Disable Culling (Double Sided)
                    )
                );
                
                int endX = Math.min(cx + chunkSize, width - 1);
                int endZ = Math.min(cz + chunkSize, depth - 1);

                for (int z = cz; z < endZ; z++) {
                    for (int x = cx; x < endX; x++) {
                        // Get Heights
                        float h1 = worldMap.getTile(x, z).getHeight() * IsoUtils.HEIGHT_SCALE;
                        float h2 = worldMap.getTile(x+1, z).getHeight() * IsoUtils.HEIGHT_SCALE;
                        float h3 = worldMap.getTile(x+1, z+1).getHeight() * IsoUtils.HEIGHT_SCALE;
                        float h4 = worldMap.getTile(x, z+1).getHeight() * IsoUtils.HEIGHT_SCALE;
                        
                        // Set Color based on tile type
                        Tile tile = worldMap.getTile(x, z);
                        builder.setColor(getTerrainColor(tile.getType(), h1));
                        
                        // Define Vertices
                        p1.set(x, h1, z);
                        p2.set(x+1, h2, z);
                        p3.set(x+1, h3, z+1);
                        p4.set(x, h4, z+1);
                        
                        // Simple normal calc for p1-p2-p4 triangle
                        Vector3 u = new Vector3(p2).sub(p1);
                        Vector3 v = new Vector3(p4).sub(p1);
                        
                        // Fix: (v cross u) points UP. (u cross v) pointed DOWN.
                        Vector3 faceNorm = v.crs(u).nor(); 
                        
                        builder.rect(p1, p2, p3, p4, faceNorm);
                    }
                }
            }
        }
        
        terrainModel = mb.end();
        terrainInstance = new ModelInstance(terrainModel);
    }
    
    private Color getTerrainColor(TerrainType type, float height) {
        switch(type) {
            case SNOW: return Color.WHITE;
            case GRASS: return new Color(0.2f, 0.6f, 0.1f, 1f); // Forest Green
            case ROCK: return Color.GRAY;
            case DIRT: return new Color(0.5f, 0.3f, 0.1f, 1f);
            default: return Color.WHITE;
        }
    }

    public void render(OrthographicCamera camera, int hoveredX, int hoveredZ, boolean isBuildMode, LiftPreview preview) {
        // Check for map updates
        if (worldMap.isDirty()) {
            if (terrainModel != null) terrainModel.dispose();
            buildTerrainModel();
            worldMap.clean();
        }

        modelBatch.begin(camera);
        
        // 1. Render Terrain
        if (terrainInstance != null) {
             modelBatch.render(terrainInstance, environment);
        }
        
        // 2. Render Entities
        for (Entity entity : ecsEngine.getEntities()) {
            if (ecsEngine.hasComponent(entity, TransformComponent.class)) {
                TransformComponent t = ecsEngine.getComponent(entity, TransformComponent.class);
                
                float drawX = t.x;
                float drawZ = t.z;
                float drawY = t.y * IsoUtils.HEIGHT_SCALE; 
                
                if (ecsEngine.hasComponent(entity, LiftComponent.class)) {
                    renderModelAt(liftPylonModel, drawX, drawY, drawZ, Color.WHITE);
                } else if (ecsEngine.hasComponent(entity, SkierComponent.class)) {
                    renderModelAt(skierModel, drawX, drawY, drawZ, Color.WHITE);
                }
            }
        }
        
        // 3. Render Cursor
        if (hoveredX >= 0 && hoveredZ >= 0) {
            Tile t = worldMap.getTile(hoveredX, hoveredZ);
            if (t != null) {
                float h = t.getHeight() * IsoUtils.HEIGHT_SCALE;
                Color cursorColor = isBuildMode ? Color.BLUE : Color.YELLOW;
                renderModelAt(cursorModel, hoveredX, h + 0.1f, hoveredZ, cursorColor); // Lift slightly
            }
        }
        
        // 4. Render Preview (Ghosts)
        if (preview != null && !preview.pylonPositions.isEmpty()) {
            Color ghostColor = preview.isValid ? Color.GREEN : Color.RED;
            for (com.badlogic.gdx.math.Vector2 pos : preview.pylonPositions) {
                int x = (int)pos.x;
                int z = (int)pos.y;
                Tile t = worldMap.getTile(x, z);
                float h = (t != null) ? t.getHeight() * IsoUtils.HEIGHT_SCALE : 0;
                renderModelAt(liftPylonModel, x, h, z, ghostColor);
            }
        }

        modelBatch.end();
    }
    
    private void renderModelAt(Model model, float x, float y, float z, Color tint) {
        // Note: Creating ModelInstance every frame is garbage-heavy.
        ModelInstance instance = new ModelInstance(model);
        instance.transform.setToTranslation(x + 0.5f, y, z + 0.5f); // Center in tile
        
        instance.materials.get(0).set(ColorAttribute.createDiffuse(tint));
        
        modelBatch.render(instance, environment);
    }

    public void dispose() {
        modelBatch.dispose();
        if (terrainModel != null) terrainModel.dispose();
        if (skierModel != null) skierModel.dispose();
        if (liftPylonModel != null) liftPylonModel.dispose();
        if (cursorModel != null) cursorModel.dispose();
    }
}