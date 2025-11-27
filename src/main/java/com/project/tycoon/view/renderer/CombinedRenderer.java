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
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.components.LiftComponent;
import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.world.model.Decoration;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.TerrainType;
import com.project.tycoon.world.model.WorldMap;
import com.project.tycoon.view.util.IsoUtils;
import com.project.tycoon.view.LiftBuilder.LiftPreview;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    private Model cableModel; 
    private Model cursorModel;
    
    // Decoration Models
    private Model treeModel1; 
    private Model treeModel2; 
    private Model rockModel;
    
    // Trail Marker
    private Model trailMarkerModel;

    public CombinedRenderer(WorldMap worldMap, Engine ecsEngine) {
        this.worldMap = worldMap;
        this.ecsEngine = ecsEngine;
        
        this.modelBatch = new ModelBatch();
        
        // 1. Setup Lighting (Improved)
        this.environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(1.0f, 1.0f, 1.0f, -1f, -0.8f, -0.2f));
        
        // 2. Build Models
        buildTerrainModel();
        buildEntityModels();
    }
    
    private void buildEntityModels() {
        ModelBuilder mb = new ModelBuilder();
        
        // Skier
        skierModel = mb.createBox(0.5f, 1.0f, 0.5f, 
            new Material(ColorAttribute.createDiffuse(Color.FIREBRICK)),
            Usage.Position | Usage.Normal);
            
        // Lift Pylon
        liftPylonModel = mb.createBox(0.2f, 3.0f, 0.2f, 
            new Material(ColorAttribute.createDiffuse(Color.GRAY)),
            Usage.Position | Usage.Normal);
            
        // Cable: Box along Z axis
        mb.begin();
        mb.node().id = "cable";
        mb.part("cable", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
            new Material(
                ColorAttribute.createDiffuse(Color.BLACK),
                new IntAttribute(IntAttribute.CullFace, 0) // Disable culling to ensure visibility
            ))
            .box(0.1f, 0.1f, 1.0f);
        cableModel = mb.end();

        // Cursor
        cursorModel = mb.createBox(1.0f, 0.1f, 1.0f,
            new Material(ColorAttribute.createDiffuse(new Color(0f, 0f, 1f, 0.5f))),
            Usage.Position | Usage.Normal);
            
        // Tree 1
        mb.begin();
        mb.node().id = "trunk";
        mb.part("trunk", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, 
            new Material(ColorAttribute.createDiffuse(Color.BROWN)))
            .cylinder(0.2f, 1f, 0.2f, 6);
        mb.node().id = "leaves";
        mb.node().translation.set(0, 0.5f, 0);
        mb.part("leaves", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
            new Material(ColorAttribute.createDiffuse(Color.FOREST)))
            .cone(1f, 2f, 1f, 8);
        treeModel1 = mb.end();
        
        // Tree 2
        mb.begin();
        mb.node().id = "trunk";
        mb.part("trunk", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, 
            new Material(ColorAttribute.createDiffuse(Color.BROWN)))
            .cylinder(0.2f, 1f, 0.2f, 6);
        mb.node().id = "leaves";
        mb.node().translation.set(0, 0.5f, 0);
        mb.part("leaves", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
            new Material(ColorAttribute.createDiffuse(new Color(0.8f, 0.9f, 0.8f, 1f))))
            .cone(1.2f, 1.8f, 1.2f, 8);
        treeModel2 = mb.end();
        
        // Rock
        rockModel = mb.createSphere(1f, 0.8f, 1f, 4, 4, 
            new Material(ColorAttribute.createDiffuse(Color.GRAY)),
            Usage.Position | Usage.Normal);
            
        // Trail Marker (Orange Cylinder)
        mb.begin();
        mb.node().id = "pole";
        mb.part("pole", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)))
            .cylinder(0.1f, 0.5f, 0.1f, 6);
        trailMarkerModel = mb.end();
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

        for (int cz = 0; cz < depth - 1; cz += chunkSize) {
            for (int cx = 0; cx < width - 1; cx += chunkSize) {
                
                MeshPartBuilder builder = mb.part(
                    "chunk_" + cx + "_" + cz, 
                    GL20.GL_TRIANGLES, 
                    Usage.Position | Usage.ColorPacked | Usage.Normal, 
                    new Material(
                        ColorAttribute.createDiffuse(Color.WHITE), 
                        new IntAttribute(IntAttribute.CullFace, 0)
                    )
                );
                
                int endX = Math.min(cx + chunkSize, width - 1);
                int endZ = Math.min(cz + chunkSize, depth - 1);

                for (int z = cz; z < endZ; z++) {
                    for (int x = cx; x < endX; x++) {
                        float h1 = worldMap.getTile(x, z).getHeight() * IsoUtils.HEIGHT_SCALE;
                        float h2 = worldMap.getTile(x+1, z).getHeight() * IsoUtils.HEIGHT_SCALE;
                        float h3 = worldMap.getTile(x+1, z+1).getHeight() * IsoUtils.HEIGHT_SCALE;
                        float h4 = worldMap.getTile(x, z+1).getHeight() * IsoUtils.HEIGHT_SCALE;
                        
                        Tile tile = worldMap.getTile(x, z);
                        builder.setColor(getTerrainColor(tile, h1));
                        
                        p1.set(x, h1, z);
                        p2.set(x+1, h2, z);
                        p3.set(x+1, h3, z+1);
                        p4.set(x, h4, z+1);
                        
                        Vector3 u = new Vector3(p2).sub(p1);
                        Vector3 v = new Vector3(p4).sub(p1);
                        Vector3 faceNorm = v.crs(u).nor(); 
                        
                        builder.rect(p1, p2, p3, p4, faceNorm);
                    }
                }
            }
        }
        
        terrainModel = mb.end();
        terrainInstance = new ModelInstance(terrainModel);
    }
    
    private Color getTerrainColor(Tile tile, float height) {
        if (tile.isTrail()) {
            return new Color(0.95f, 0.98f, 1.0f, 1f); // Groomed Snow (Cleaner/Brighter)
        }
        switch(tile.getType()) {
            case SNOW: return new Color(0.9f, 0.95f, 1.0f, 1f);
            case GRASS: return new Color(0.2f, 0.6f, 0.1f, 1f); 
            case ROCK: return Color.GRAY;
            case DIRT: return new Color(0.5f, 0.3f, 0.1f, 1f);
            default: return Color.WHITE;
        }
    }

    public void render(OrthographicCamera camera, int hoveredX, int hoveredZ, boolean isBuildMode, LiftPreview preview) {
        if (worldMap.isDirty()) {
            if (terrainModel != null) terrainModel.dispose();
            buildTerrainModel();
            worldMap.clean();
        }

        modelBatch.begin(camera);
        
        if (terrainInstance != null) {
             modelBatch.render(terrainInstance, environment);
        }
        
        for (int z = 0; z < worldMap.getDepth(); z++) {
            for (int x = 0; x < worldMap.getWidth(); x++) {
                Tile t = worldMap.getTile(x, z);
                float h = t.getHeight() * IsoUtils.HEIGHT_SCALE;
                
                // Render Decorations
                if (t.getDecoration() != Decoration.NONE) {
                    Model model = rockModel;
                    if (t.getDecoration() == Decoration.TREE) {
                        model = ((x + z) % 3 == 0) ? treeModel2 : treeModel1; 
                    }
                    renderModelAt(model, x, h, z, Color.WHITE);
                }
                
                // Render Trail Borders
                if (t.isTrail()) {
                     renderTrailBorders(x, z);
                }
            }
        }
        
        // Cache Transforms
        Map<UUID, TransformComponent> transformCache = new HashMap<>();
        for (Entity entity : ecsEngine.getEntities()) {
            if (ecsEngine.hasComponent(entity, TransformComponent.class)) {
                transformCache.put(entity.getId(), ecsEngine.getComponent(entity, TransformComponent.class));
            }
        }
        
        // Render Entities & Cables
        for (Entity entity : ecsEngine.getEntities()) {
            if (ecsEngine.hasComponent(entity, TransformComponent.class)) {
                TransformComponent t = ecsEngine.getComponent(entity, TransformComponent.class);
                
                float drawX = t.x;
                float drawZ = t.z;
                float drawY = t.y * IsoUtils.HEIGHT_SCALE; 
                
                if (ecsEngine.hasComponent(entity, LiftComponent.class)) {
                    renderModelAt(liftPylonModel, drawX, drawY, drawZ, Color.WHITE);
                    
                    // Draw Cable
                    LiftComponent lift = ecsEngine.getComponent(entity, LiftComponent.class);
                    if (lift.nextPylonId != null && transformCache.containsKey(lift.nextPylonId)) {
                        TransformComponent next = transformCache.get(lift.nextPylonId);
                        drawCable(t, next);
                    }
                    
                } else if (ecsEngine.hasComponent(entity, SkierComponent.class)) {
                    renderModelAt(skierModel, drawX, drawY, drawZ, Color.WHITE);
                }
            }
        }
        
        if (hoveredX >= 0 && hoveredZ >= 0) {
            Tile t = worldMap.getTile(hoveredX, hoveredZ);
            if (t != null) {
                float h = t.getHeight() * IsoUtils.HEIGHT_SCALE;
                Color cursorColor = isBuildMode ? Color.BLUE : Color.YELLOW;
                renderModelAt(cursorModel, hoveredX, h + 0.1f, hoveredZ, cursorColor); 
            }
        }
        
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
    
    private void renderTrailBorders(int x, int z) {
        float h00 = getH(x, z);
        float h10 = getH(x+1, z);
        float h01 = getH(x, z+1);
        float h11 = getH(x+1, z+1);
        
        // North (z-1)
        if (isNotTrail(x, z-1)) renderMarker(x + 0.5f, (h00 + h10)*0.5f, z); 
        // South (z+1)
        if (isNotTrail(x, z+1)) renderMarker(x + 0.5f, (h01 + h11)*0.5f, z + 1f);
        // West (x-1)
        if (isNotTrail(x-1, z)) renderMarker(x, (h00 + h01)*0.5f, z + 0.5f);
        // East (x+1)
        if (isNotTrail(x+1, z)) renderMarker(x + 1f, (h10 + h11)*0.5f, z + 0.5f);
    }

    private float getH(int x, int z) {
        Tile t = worldMap.getTile(x, z);
        return (t != null) ? t.getHeight() * IsoUtils.HEIGHT_SCALE : 0;
    }
    
    private boolean isNotTrail(int x, int z) {
        if (x < 0 || z < 0 || x >= worldMap.getWidth() || z >= worldMap.getDepth()) return true;
        return !worldMap.getTile(x, z).isTrail();
    }
    
    private void renderMarker(float x, float y, float z) {
        ModelInstance marker = new ModelInstance(trailMarkerModel);
        marker.transform.setToTranslation(x, y + 0.25f, z); 
        
        // Alternating Color based on position
        boolean isOrange = ((int)(x + z)) % 2 == 0;
        Color c = isOrange ? Color.ORANGE : Color.BLACK;
        
        for (Material m : marker.materials) {
            m.set(ColorAttribute.createDiffuse(c));
        }
        
        modelBatch.render(marker, environment);
    }

    private void drawCable(TransformComponent start, TransformComponent end) {
        float startY = start.y * IsoUtils.HEIGHT_SCALE + 2.8f; // Top of pylon
        float endY = end.y * IsoUtils.HEIGHT_SCALE + 2.8f;
        
        Vector3 p1 = new Vector3(start.x + 0.5f, startY, start.z + 0.5f);
        Vector3 p2 = new Vector3(end.x + 0.5f, endY, end.z + 0.5f);
        
        Vector3 direction = new Vector3(p2).sub(p1);
        float length = direction.len();
        
        Vector3 mid = new Vector3(p1).add(p2).scl(0.5f);
        
        ModelInstance cable = new ModelInstance(cableModel);
        // Manual Rotation Logic to avoid Matrix4.lookAt issues
        cable.transform.setToTranslation(mid);
        
        // Rotate 'Z' to align with 'direction'
        // Default Z is (0,0,1)
        Vector3 dirNorm = direction.cpy().nor();
        // Cross product gives axis of rotation
        Vector3 axis = new Vector3(Vector3.Z).crs(dirNorm).nor();
        // Dot product gives angle
        float dot = Vector3.Z.dot(dirNorm);
        // Angle in degrees
        float angle = (float) Math.toDegrees(Math.acos(dot));
        
        // If parallel (angle 0) or opposite (angle 180), cross product is zero.
        // Handle edge cases if needed, but lifts are rarely vertical.
        if (axis.len2() < 0.001f) {
            // Parallel or anti-parallel
            if (dot < 0) cable.transform.rotate(Vector3.X, 180); // Flip if opposite
        } else {
            cable.transform.rotate(axis, angle);
        }
        
        cable.transform.scale(1f, 1f, length);
        
        modelBatch.render(cable, environment);
        
        // Chair
        ModelInstance chair = new ModelInstance(skierModel); 
        chair.transform.setToTranslation(mid.x, mid.y - 0.5f, mid.z);
        chair.transform.scale(0.8f, 0.8f, 0.8f); 
        modelBatch.render(chair, environment);
    }
    
    private void renderModelAt(Model model, float x, float y, float z, Color tint) {
        ModelInstance instance = new ModelInstance(model);
        instance.transform.setToTranslation(x + 0.5f, y, z + 0.5f); 
        
        if (!tint.equals(Color.WHITE)) {
             for (Material m : instance.materials) {
                 m.set(ColorAttribute.createDiffuse(tint));
             }
        }
        
        modelBatch.render(instance, environment);
    }

    public void dispose() {
        modelBatch.dispose();
        if (terrainModel != null) terrainModel.dispose();
        if (skierModel != null) skierModel.dispose();
        if (liftPylonModel != null) liftPylonModel.dispose();
        if (cursorModel != null) cursorModel.dispose();
        if (treeModel1 != null) treeModel1.dispose();
        if (treeModel2 != null) treeModel2.dispose();
        if (rockModel != null) rockModel.dispose();
        if (cableModel != null) cableModel.dispose();
        if (trailMarkerModel != null) trailMarkerModel.dispose();
    }
}