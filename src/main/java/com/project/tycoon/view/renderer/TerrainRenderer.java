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
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;

import java.util.ArrayList;
import java.util.List;

public class TerrainRenderer {

    private final WorldMap worldMap;
    private final RenderAssetManager assets;
    
    // Cached max height for elevation-based coloring
    private float maxHeight = 100f; // Default, recalculated when terrain is rebuilt

    private Model terrainModel;
    private ModelInstance terrainInstance;

    // Cached instances to avoid per-frame allocation (PERFORMANCE FIX)
    private final List<ModelInstance> decorationInstances = new ArrayList<>();
    private final List<ModelInstance> trailMarkerInstances = new ArrayList<>();

    public TerrainRenderer(WorldMap worldMap, RenderAssetManager assets) {
        this.worldMap = worldMap;
        this.assets = assets;
        buildTerrainModel();
    }

    public void update() {
        if (worldMap.isDirty()) {
            if (terrainModel != null)
                terrainModel.dispose();
            buildTerrainModel();
            rebuildCachedInstances();
            worldMap.clean();
        }
    }

    public void render(ModelBatch batch, Environment environment) {
        if (terrainInstance != null) {
            batch.render(terrainInstance, environment);
        }

        // Render cached decoration instances (PERFORMANCE FIX: No per-frame allocation)
        for (ModelInstance instance : decorationInstances) {
            batch.render(instance, environment);
        }

        // Render cached trail marker instances (PERFORMANCE FIX: No per-frame
        // allocation)
        for (ModelInstance instance : trailMarkerInstances) {
            batch.render(instance, environment);
        }
    }

    private float getH(int x, int z) {
        Tile t = worldMap.getTile(x, z);
        return (t != null) ? t.getHeight() * IsoUtils.HEIGHT_SCALE : 0;
    }

    private boolean isNotTrail(int x, int z) {
        if (x < 0 || z < 0 || x >= worldMap.getWidth() || z >= worldMap.getDepth())
            return true;
        return !worldMap.getTile(x, z).isTrail();
    }

    /**
     * Rebuilds cached ModelInstances for decorations and trail markers.
     * Called once when terrain is dirty instead of creating instances every frame.
     * PERFORMANCE: Prevents thousands of allocations per frame on large maps.
     */
    private void rebuildCachedInstances() {
        // Clear old instances
        decorationInstances.clear();
        trailMarkerInstances.clear();

        // Rebuild decoration instances
        for (int z = 0; z < worldMap.getDepth(); z++) {
            for (int x = 0; x < worldMap.getWidth(); x++) {
                Tile t = worldMap.getTile(x, z);
                float h = t.getHeight() * IsoUtils.HEIGHT_SCALE;

                // Cache Decoration instances
                if (t.getDecoration() != Decoration.NONE) {
                    Model model = assets.rockModel;
                    if (t.getDecoration() == Decoration.TREE) {
                        model = ((x + z) % 3 == 0) ? assets.treeModel2 : assets.treeModel1;
                    }

                    ModelInstance instance = new ModelInstance(model);
                    instance.transform.setToTranslation(x + 0.5f, h, z + 0.5f);
                    decorationInstances.add(instance);
                }

                // Cache Trail Marker instances
                if (t.isTrail()) {
                    cacheTrailMarkersForTile(x, z);
                }
            }
        }
    }

    /**
     * Creates and caches trail marker instances for borders of a trail tile.
     */
    /**
     * Creates and caches trail marker instances for borders of a trail tile.
     */
    private void cacheTrailMarkersForTile(int x, int z) {
        float h00 = getH(x, z);
        float h10 = getH(x + 1, z);
        float h01 = getH(x, z + 1);
        float h11 = getH(x + 1, z + 1);

        Tile t = worldMap.getTile(x, z);
        Color markerColor = t.getTrailDifficulty().getMarkerColor();

        // North (z-1)
        if (isNotTrail(x, z - 1)) {
            createTrailMarker(x + 0.5f, (h00 + h10) * 0.5f, z, markerColor);
        }
        // South (z+1)
        if (isNotTrail(x, z + 1)) {
            createTrailMarker(x + 0.5f, (h01 + h11) * 0.5f, z + 1f, markerColor);
        }
        // West (x-1)
        if (isNotTrail(x - 1, z)) {
            createTrailMarker(x, (h00 + h01) * 0.5f, z + 0.5f, markerColor);
        }
        // East (x+1)
        if (isNotTrail(x + 1, z)) {
            createTrailMarker(x + 1f, (h10 + h11) * 0.5f, z + 0.5f, markerColor);
        }
    }

    /**
     * Creates a single trail marker instance and adds it to the cache.
     */
    private void createTrailMarker(float x, float y, float z, Color color) {
        ModelInstance marker = new ModelInstance(assets.trailMarkerModel);
        marker.transform.setToTranslation(x, y + 0.25f, z);

        for (Material m : marker.materials) {
            m.set(ColorAttribute.createDiffuse(color));
        }

        trailMarkerInstances.add(marker);
    }

    /**
     * Calculate the maximum height in the world for elevation-based coloring.
     */
    private void calculateMaxHeight() {
        float max = 0f;
        for (int z = 0; z < worldMap.getDepth(); z++) {
            for (int x = 0; x < worldMap.getWidth(); x++) {
                Tile t = worldMap.getTile(x, z);
                if (t != null && t.getHeight() > max) {
                    max = t.getHeight();
                }
            }
        }
        // Ensure we don't divide by zero
        maxHeight = max > 0 ? max : 100f;
    }

    private void buildTerrainModel() {
        // Calculate max height for elevation-based coloring
        calculateMaxHeight();
        
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
                                new IntAttribute(IntAttribute.CullFace, 0)));

                int endX = Math.min(cx + chunkSize, width - 1);
                int endZ = Math.min(cz + chunkSize, depth - 1);

                for (int z = cz; z < endZ; z++) {
                    for (int x = cx; x < endX; x++) {
                        float h1 = worldMap.getTile(x, z).getHeight() * IsoUtils.HEIGHT_SCALE;
                        float h2 = worldMap.getTile(x + 1, z).getHeight() * IsoUtils.HEIGHT_SCALE;
                        float h3 = worldMap.getTile(x + 1, z + 1).getHeight() * IsoUtils.HEIGHT_SCALE;
                        float h4 = worldMap.getTile(x, z + 1).getHeight() * IsoUtils.HEIGHT_SCALE;

                        Tile tile = worldMap.getTile(x, z);
                        builder.setColor(getTerrainColor(tile));

                        p1.set(x, h1, z);
                        p2.set(x + 1, h2, z);
                        p3.set(x + 1, h3, z + 1);
                        p4.set(x, h4, z + 1);

                        Vector3 faceNorm;
                        if (tile.isTrail()) {
                            // Force flat "up" normal for trails to look smooth/groomed
                            faceNorm = Vector3.Y.cpy();
                        } else {
                            // Standard facet normal for terrain
                            Vector3 u = new Vector3(p2).sub(p1);
                            Vector3 v = new Vector3(p4).sub(p1);
                            faceNorm = v.crs(u).nor();
                        }

                        builder.rect(p1, p2, p3, p4, faceNorm);
                    }
                }
            }
        }
        terrainModel = mb.end();
        terrainInstance = new ModelInstance(terrainModel);
    }

    /**
     * Enhanced terrain coloring with elevation-based shading and trail difficulty colors.
     */
    private Color getTerrainColor(Tile tile) {
        // Trail colors based on difficulty
        if (tile.isTrail()) {
            return getTrailColor(tile);
        }
        
        // Elevation-based shading for non-trail terrain
        float elevation = tile.getHeight();
        float elevationFactor = Math.min(1.0f, elevation / maxHeight);
        
        Color baseColor;
        switch (tile.getType()) {
            case SNOW:
                // Brighter snow at higher elevations, slightly blue-tinted
                float snowBrightness = 0.80f + (elevationFactor * 0.15f); // 0.80 -> 0.95
                baseColor = new Color(snowBrightness, snowBrightness + 0.03f, snowBrightness + 0.08f, 1f);
                break;
            case GRASS:
                // Darker grass in valleys, lighter on hills
                float grassGreen = 0.50f + (elevationFactor * 0.15f);
                baseColor = new Color(0.15f, grassGreen, 0.08f, 1f);
                break;
            case ROCK:
                // Rocky areas with slight elevation variation
                float rockBrightness = 0.45f + (elevationFactor * 0.10f);
                baseColor = new Color(rockBrightness, rockBrightness * 0.95f, rockBrightness * 0.90f, 1f);
                break;
            case DIRT:
                // Brownish dirt
                float dirtBrightness = 0.40f + (elevationFactor * 0.10f);
                baseColor = new Color(dirtBrightness, dirtBrightness * 0.65f, dirtBrightness * 0.25f, 1f);
                break;
            default:
                baseColor = new Color(0.9f, 0.9f, 0.95f, 1f);
                break;
        }
        
        return baseColor;
    }
    
    /**
     * Get distinct trail color based on difficulty rating.
     * Makes trails highly visible and easy to distinguish.
     */
    private Color getTrailColor(Tile tile) {
        switch (tile.getTrailDifficulty()) {
            case GREEN:
                // Bright, groomed green trail
                return new Color(0.85f, 0.95f, 0.85f, 1f);
            case BLUE:
                // Icy blue trail
                return new Color(0.80f, 0.88f, 0.98f, 1f);
            case BLACK:
                // Packed, darker trail
                return new Color(0.75f, 0.78f, 0.80f, 1f);
            case DOUBLE_BLACK:
                // High-contrast expert trail
                return new Color(0.70f, 0.72f, 0.75f, 1f);
            default:
                return new Color(0.95f, 0.95f, 0.98f, 1f); // Default bright snow
        }
    }

    public void dispose() {
        if (terrainModel != null)
            terrainModel.dispose();
    }
}
