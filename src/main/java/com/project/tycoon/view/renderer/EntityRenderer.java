package com.project.tycoon.view.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.components.LiftComponent;
import com.project.tycoon.ecs.components.SkierComponent;
import com.project.tycoon.ecs.components.BaseCampComponent;
import com.project.tycoon.ecs.components.TransformComponent;
import com.project.tycoon.view.util.IsoUtils;
import com.project.tycoon.view.LiftBuilder.LiftPreview;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;
import com.project.tycoon.world.model.SnapPoint;
import com.project.tycoon.world.SnapPointManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityRenderer {

    private final Engine ecsEngine;
    private final RenderAssetManager assets;
    private final WorldMap worldMap; // For height lookups for cursor/preview
    private final SnapPointManager snapPointManager; // For rendering snap points

    public EntityRenderer(Engine ecsEngine, WorldMap worldMap, RenderAssetManager assets,
            SnapPointManager snapPointManager) {
        this.ecsEngine = ecsEngine;
        this.worldMap = worldMap;
        this.assets = assets;
        this.snapPointManager = snapPointManager;
    }

    public void render(ModelBatch batch, Environment environment, int hoveredX, int hoveredZ, boolean isBuildMode,
            boolean isTrailMode, boolean isValidSnapPoint, LiftPreview preview) {
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
                    renderModelAt(batch, environment, assets.liftPylonModel, drawX, drawY, drawZ, Color.WHITE);

                    // Draw Cable
                    LiftComponent lift = ecsEngine.getComponent(entity, LiftComponent.class);
                    if (lift.nextPylonId != null && transformCache.containsKey(lift.nextPylonId)) {
                        TransformComponent next = transformCache.get(lift.nextPylonId);
                        drawCable(batch, environment, t, next);
                    }

                } else if (ecsEngine.hasComponent(entity, SkierComponent.class)) {
                    // Add variety to skier jacket colors
                    Color skierColor = getSkierColor(entity.getId());
                    renderModelAt(batch, environment, assets.skierModel, drawX, drawY, drawZ, skierColor);

                } else if (ecsEngine.hasComponent(entity, BaseCampComponent.class)) {
                    // Offset +3 units so building sits on terrain instead of sinking
                    renderModelAt(batch, environment, assets.baseCampModel, drawX, drawY + 3f, drawZ, Color.WHITE);
                }
            }
        }

        // Render Cursor
        if (hoveredX >= 0 && hoveredZ >= 0) {
            Tile t = worldMap.getTile(hoveredX, hoveredZ);
            if (t != null) {
                float h = t.getHeight() * IsoUtils.HEIGHT_SCALE;

                // Color cursor based on mode and snap point validity
                Color cursorColor;
                if (isBuildMode || isTrailMode) {
                    cursorColor = isValidSnapPoint ? Color.GREEN : Color.RED;
                } else {
                    cursorColor = Color.YELLOW;
                }

                renderModelAt(batch, environment, assets.cursorModel, hoveredX, h + 0.1f, hoveredZ, cursorColor);
            }
        }

        // Render Lift Preview
        if (preview != null && !preview.pylonPositions.isEmpty()) {
            Color ghostColor = preview.isValid ? Color.GREEN : Color.RED;
            for (Vector2 pos : preview.pylonPositions) {
                int x = (int) pos.x;
                int z = (int) pos.y;
                Tile t = worldMap.getTile(x, z);
                float h = (t != null) ? t.getHeight() * IsoUtils.HEIGHT_SCALE : 0;
                renderModelAt(batch, environment, assets.liftPylonModel, x, h, z, ghostColor);
            }
        }

        // Render Snap Points (visual indicators in build/trail mode)
        if (isBuildMode || isTrailMode) {
            for (SnapPoint sp : snapPointManager.getAllSnapPoints()) {
                Tile t = worldMap.getTile((int) sp.getX(), (int) sp.getZ());
                float h = (t != null) ? t.getHeight() * IsoUtils.HEIGHT_SCALE : 0;

                // Color based on snap point type
                Color snapColor;
                switch (sp.getType()) {
                    case BASE_CAMP:
                        snapColor = new Color(0.2f, 0.8f, 1.0f, 0.8f); // Cyan
                        break;
                    case LIFT_BOTTOM:
                        snapColor = new Color(1.0f, 1.0f, 0.0f, 0.8f); // Yellow
                        break;
                    case LIFT_TOP:
                        snapColor = new Color(1.0f, 0.5f, 0.0f, 0.8f); // Orange
                        break;
                    case TRAIL_START:
                    case TRAIL_END:
                        // Color based on trail difficulty at this location
                        if (t != null && t.isTrail()) {
                            Color difficultyColor = t.getTrailDifficulty().getMarkerColor();
                            // Make it semi-transparent
                            snapColor = new Color(difficultyColor.r, difficultyColor.g, difficultyColor.b, 0.8f);
                        } else {
                            // Default if not on trail (shouldn't happen)
                            snapColor = new Color(0.8f, 0.8f, 0.8f, 0.8f); // Gray
                        }
                        break;
                    default:
                        snapColor = new Color(1.0f, 1.0f, 1.0f, 0.8f); // White
                }

                renderModelAt(batch, environment, assets.cursorModel, sp.getX(), h + 0.5f, sp.getZ(), snapColor);
            }
        }
    }

    private void drawCable(ModelBatch batch, Environment env, TransformComponent start, TransformComponent end) {
        float startY = start.y * IsoUtils.HEIGHT_SCALE + 2.8f; // Top of pylon
        float endY = end.y * IsoUtils.HEIGHT_SCALE + 2.8f;

        Vector3 p1 = new Vector3(start.x + 0.5f, startY, start.z + 0.5f);
        Vector3 p2 = new Vector3(end.x + 0.5f, endY, end.z + 0.5f);

        Vector3 direction = new Vector3(p2).sub(p1);
        float length = direction.len();

        Vector3 mid = new Vector3(p1).add(p2).scl(0.5f);

        ModelInstance cable = new ModelInstance(assets.cableModel);
        cable.transform.setToTranslation(mid);

        // For cylinder: rotate from Y-axis (cylinder default) to direction vector
        Vector3 dirNorm = direction.cpy().nor();
        Vector3 cylinderAxis = Vector3.Y.cpy(); // Cylinders are built along Y axis
        Vector3 rotationAxis = cylinderAxis.crs(dirNorm).nor();
        float dot = cylinderAxis.dot(dirNorm);
        float angle = (float) Math.toDegrees(Math.acos(Math.max(-1f, Math.min(1f, dot))));

        if (rotationAxis.len2() < 0.001f) {
            // Parallel or anti-parallel
            if (dot < 0) {
                cable.transform.rotate(Vector3.X, 180);
            }
        } else {
            cable.transform.rotate(rotationAxis, angle);
        }

        cable.transform.scale(1f, length, 1f); // Scale along Y for cylinder

        batch.render(cable, env);

        // Chair (simplified - using skier model as chair)
        ModelInstance chair = new ModelInstance(assets.skierModel);
        chair.transform.setToTranslation(mid.x, mid.y - 0.8f, mid.z); // Lower position
        chair.transform.scale(0.6f, 0.6f, 0.6f); // Smaller for chair representation
        batch.render(chair, env);
    }

    /**
     * Generate a varied jacket color for each skier based on their ID.
     * Creates vibrant, visible colors for better visual variety.
     */
    private Color getSkierColor(UUID skierId) {
        // Use hash code for deterministic but varied colors
        int hash = skierId.hashCode();
        int colorIndex = Math.abs(hash % 8);
        
        switch (colorIndex) {
            case 0: return new Color(0.1f, 0.7f, 0.9f, 1f);  // Cyan
            case 1: return new Color(0.9f, 0.3f, 0.1f, 1f);  // Orange
            case 2: return new Color(0.9f, 0.1f, 0.5f, 1f);  // Pink/Magenta
            case 3: return new Color(0.2f, 0.9f, 0.3f, 1f);  // Bright Green
            case 4: return new Color(0.9f, 0.9f, 0.1f, 1f);  // Yellow
            case 5: return new Color(0.5f, 0.2f, 0.9f, 1f);  // Purple
            case 6: return new Color(0.1f, 0.5f, 0.9f, 1f);  // Blue
            case 7: return new Color(0.9f, 0.5f, 0.1f, 1f);  // Bright Orange
            default: return new Color(0.1f, 0.7f, 0.9f, 1f); // Default Cyan
        }
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
}
