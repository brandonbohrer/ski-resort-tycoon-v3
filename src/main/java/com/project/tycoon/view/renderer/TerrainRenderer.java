package com.project.tycoon.view.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.project.tycoon.view.util.IsoUtils;
import com.project.tycoon.world.model.Decoration;
import com.project.tycoon.world.model.TerrainType;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;

public class TerrainRenderer {
    
    private final WorldMap worldMap;
    private final RenderAssetManager assets;
    
    private Model terrainModel;
    private ModelInstance terrainInstance;
    
    public TerrainRenderer(WorldMap worldMap, RenderAssetManager assets) {
        this.worldMap = worldMap;
        this.assets = assets;
        buildTerrainModel();
    }
    
    public void update() {
        if (worldMap.isDirty()) {
            if (terrainModel != null) terrainModel.dispose();
            buildTerrainModel();
            worldMap.clean();
        }
    }
    
    public void render(ModelBatch batch, Environment environment) {
        if (terrainInstance != null) {
             batch.render(terrainInstance, environment);
        }
        
        // Render Decorations & Trail Borders
        for (int z = 0; z < worldMap.getDepth(); z++) {
            for (int x = 0; x < worldMap.getWidth(); x++) {
                Tile t = worldMap.getTile(x, z);
                float h = t.getHeight() * IsoUtils.HEIGHT_SCALE;
                
                // Render Decorations
                if (t.getDecoration() != Decoration.NONE) {
                    Model model = assets.rockModel;
                    if (t.getDecoration() == Decoration.TREE) {
                        model = ((x + z) % 3 == 0) ? assets.treeModel2 : assets.treeModel1; 
                    }
                    renderModelAt(batch, environment, model, x, h, z, Color.WHITE);
                }
                
                // Render Trail Borders
                if (t.isTrail()) {
                     renderTrailBorders(batch, environment, x, z);
                }
            }
        }
    }
    
    private void renderTrailBorders(ModelBatch batch, Environment env, int x, int z) {
        float h00 = getH(x, z);
        float h10 = getH(x+1, z);
        float h01 = getH(x, z+1);
        float h11 = getH(x+1, z+1);
        
        // North (z-1)
        if (isNotTrail(x, z-1)) renderMarker(batch, env, x + 0.5f, (h00 + h10)*0.5f, z); 
        // South (z+1)
        if (isNotTrail(x, z+1)) renderMarker(batch, env, x + 0.5f, (h01 + h11)*0.5f, z + 1f);
        // West (x-1)
        if (isNotTrail(x-1, z)) renderMarker(batch, env, x, (h00 + h01)*0.5f, z + 0.5f);
        // East (x+1)
        if (isNotTrail(x+1, z)) renderMarker(batch, env, x + 1f, (h10 + h11)*0.5f, z + 0.5f);
    }

    private float getH(int x, int z) {
        Tile t = worldMap.getTile(x, z);
        return (t != null) ? t.getHeight() * IsoUtils.HEIGHT_SCALE : 0;
    }
    
    private boolean isNotTrail(int x, int z) {
        if (x < 0 || z < 0 || x >= worldMap.getWidth() || z >= worldMap.getDepth()) return true;
        return !worldMap.getTile(x, z).isTrail();
    }
    
    private void renderMarker(ModelBatch batch, Environment env, float x, float y, float z) {
        ModelInstance marker = new ModelInstance(assets.trailMarkerModel);
        marker.transform.setToTranslation(x, y + 0.25f, z); 
        
        // Alternating Color based on position
        boolean isOrange = ((int)(x + z)) % 2 == 0;
        Color c = isOrange ? Color.ORANGE : Color.BLACK;
        
        for (Material m : marker.materials) {
            m.set(ColorAttribute.createDiffuse(c));
        }
        batch.render(marker, env);
    }
    
    private void renderModelAt(ModelBatch batch, Environment env, Model model, float x, float y, float z, Color tint) {
        ModelInstance instance = new ModelInstance(model);
        instance.transform.setToTranslation(x + 0.5f, y, z + 0.5f); 
        
        if (!tint.equals(Color.WHITE)) {
             for (Material m : instance.materials) {
                 m.set(ColorAttribute.createDiffuse(tint));
             }
        }
        batch.render(instance, env);
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
                        builder.setColor(getTerrainColor(tile));
                        
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
    
    private Color getTerrainColor(Tile tile) {
        if (tile.isTrail()) {
            return new Color(0.95f, 0.98f, 1.0f, 1f);
        }
        switch(tile.getType()) {
            case SNOW: return new Color(0.9f, 0.95f, 1.0f, 1f);
            case GRASS: return new Color(0.2f, 0.6f, 0.1f, 1f); 
            case ROCK: return Color.GRAY;
            case DIRT: return new Color(0.5f, 0.3f, 0.1f, 1f);
            default: return Color.WHITE;
        }
    }
    
    public void dispose() {
        if (terrainModel != null) terrainModel.dispose();
    }
}
