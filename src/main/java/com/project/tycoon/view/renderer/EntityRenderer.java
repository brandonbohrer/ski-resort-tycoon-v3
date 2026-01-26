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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
            boolean isTrailMode, boolean isValidSnapPoint, LiftPreview preview, Entity selectedEntity, Entity hoveredEntity) {
        // Cache Transforms
        Map<UUID, TransformComponent> transformCache = new HashMap<>();
        for (Entity entity : ecsEngine.getEntities()) {
            if (ecsEngine.hasComponent(entity, TransformComponent.class)) {
                transformCache.put(entity.getId(), ecsEngine.getComponent(entity, TransformComponent.class));
            }
        }
        
        // Find all pylons in the hovered lift chain
        Set<UUID> hoveredLiftChain = new HashSet<>();
        if (hoveredEntity != null && ecsEngine.hasComponent(hoveredEntity, LiftComponent.class)) {
            hoveredLiftChain = findEntireLiftChain(hoveredEntity);
        }

        // Render Entities & Cables
        for (Entity entity : ecsEngine.getEntities()) {
            if (ecsEngine.hasComponent(entity, TransformComponent.class)) {
                TransformComponent t = ecsEngine.getComponent(entity, TransformComponent.class);

                float drawX = t.x;
                float drawZ = t.z;
                float drawY = t.y * IsoUtils.HEIGHT_SCALE;

                if (ecsEngine.hasComponent(entity, LiftComponent.class)) {
                    // Pulse brightness if this pylon is part of the hovered lift chain
                    Color liftColor = Color.WHITE.cpy();
                    if (hoveredLiftChain.contains(entity.getId())) {
                        float pulse = 0.7f + 0.3f * (float)Math.sin(java.lang.System.currentTimeMillis() / 200.0);
                        liftColor = new Color(pulse, pulse, pulse, 1f);
                    }
                    renderModelAt(batch, environment, assets.liftPylonModel, drawX, drawY, drawZ, liftColor);

                    // Draw Cable
                    LiftComponent lift = ecsEngine.getComponent(entity, LiftComponent.class);
                    if (lift.nextPylonId != null && transformCache.containsKey(lift.nextPylonId)) {
                        TransformComponent next = transformCache.get(lift.nextPylonId);
                        // Pulse cable if part of hovered lift chain
                        boolean cableShouldPulse = hoveredLiftChain.contains(entity.getId());
                        drawCable(batch, environment, t, next, cableShouldPulse);
                    }
                    
                    // Highlight if selected
                    if (selectedEntity != null && entity.getId().equals(selectedEntity.getId())) {
                        renderSelectionRing(batch, environment, drawX, drawY, drawZ, 2f, Color.GREEN);
                    }

                } else if (ecsEngine.hasComponent(entity, SkierComponent.class)) {
                    // Add variety to skier jacket colors
                    Color skierColor = getSkierColor(entity.getId());
                    
                    // Pulse brightness if hovered (brighten the entire model)
                    if (hoveredEntity != null && entity.getId().equals(hoveredEntity.getId())) {
                        float pulse = 0.8f + 0.2f * (float)Math.sin(java.lang.System.currentTimeMillis() / 150.0);
                        // Brighten by multiplying with a brighter color
                        skierColor = new Color(
                            Math.min(1f, skierColor.r * (1f + pulse * 0.5f)),
                            Math.min(1f, skierColor.g * (1f + pulse * 0.5f)),
                            Math.min(1f, skierColor.b * (1f + pulse * 0.5f)),
                            1f
                        );
                    }
                    
                    renderModelAt(batch, environment, assets.skierModel, drawX, drawY, drawZ, skierColor);
                    
                    // Highlight if selected
                    if (selectedEntity != null && entity.getId().equals(selectedEntity.getId())) {
                        renderSelectionRing(batch, environment, drawX, drawY, drawZ, 1.5f, Color.CYAN);
                    }

                } else if (ecsEngine.hasComponent(entity, BaseCampComponent.class)) {
                    // Pulse brightness if hovered
                    Color buildingColor = Color.WHITE.cpy();
                    if (hoveredEntity != null && entity.getId().equals(hoveredEntity.getId())) {
                        float pulse = 0.7f + 0.3f * (float)Math.sin(java.lang.System.currentTimeMillis() / 200.0);
                        buildingColor = new Color(pulse, pulse, pulse, 1f);
                    }
                    
                    // Offset +3 units so building sits on terrain instead of sinking
                    renderModelAt(batch, environment, assets.baseCampModel, drawX, drawY + 3f, drawZ, buildingColor);
                    
                    // Highlight if selected
                    if (selectedEntity != null && entity.getId().equals(selectedEntity.getId())) {
                        renderSelectionRing(batch, environment, drawX, drawY, drawZ, 5f, Color.YELLOW);
                    }
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

    private void drawCable(ModelBatch batch, Environment env, TransformComponent start, TransformComponent end, boolean shouldPulse) {
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
        
        // Apply pulse color if needed
        if (shouldPulse) {
            float pulse = 0.7f + 0.3f * (float)Math.sin(java.lang.System.currentTimeMillis() / 200.0);
            Color pulseColor = new Color(pulse, pulse, pulse, 1f);
            for (Material m : cable.materials) {
                m.set(ColorAttribute.createDiffuse(pulseColor));
            }
        }

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

    /**
     * Find all pylons in a lift chain (follow the linked list in both directions).
     */
    private Set<UUID> findEntireLiftChain(Entity startPylon) {
        Set<UUID> chain = new HashSet<>();
        
        // Start from the very first pylon (go backwards first)
        Entity firstPylon = findFirstPylonInChain(startPylon);
        
        // Now follow forward through the entire chain
        Entity current = firstPylon;
        while (current != null) {
            chain.add(current.getId());
            
            LiftComponent lift = ecsEngine.getComponent(current, LiftComponent.class);
            if (lift != null && lift.nextPylonId != null) {
                // Find the next pylon entity
                Entity next = null;
                for (Entity e : ecsEngine.getEntities()) {
                    if (e.getId().equals(lift.nextPylonId)) {
                        next = e;
                        break;
                    }
                }
                current = next;
            } else {
                break;
            }
        }
        
        return chain;
    }
    
    /**
     * Find the first pylon in a lift chain by going backwards.
     */
    private Entity findFirstPylonInChain(Entity pylon) {
        Entity first = pylon;
        
        // Keep searching for pylons that point to this one
        boolean foundPrevious = true;
        while (foundPrevious) {
            foundPrevious = false;
            for (Entity e : ecsEngine.getEntities()) {
                if (!ecsEngine.hasComponent(e, LiftComponent.class)) continue;
                
                LiftComponent lift = ecsEngine.getComponent(e, LiftComponent.class);
                if (lift.nextPylonId != null && lift.nextPylonId.equals(first.getId())) {
                    first = e;
                    foundPrevious = true;
                    break;
                }
            }
        }
        
        return first;
    }

    private void renderModelAt(ModelBatch batch, Environment env, Model model, float x, float y, float z, Color tint) {
        ModelInstance instance = new ModelInstance(model);
        instance.transform.setToTranslation(x + 0.5f, y, z + 0.5f);

        // Only apply tint if it's not white (preserve original model colors when white)
        if (!tint.equals(Color.WHITE)) {
            for (Material m : instance.materials) {
                m.set(ColorAttribute.createDiffuse(tint));
            }
        }
        
        batch.render(instance, env);
    }
    
    /**
     * Render a selection ring/highlight around an entity.
     */
    private void renderSelectionRing(ModelBatch batch, Environment env, float x, float y, float z, float radius, Color color) {
        // Create a thin, flat cylinder as a ring
        ModelInstance ring = new ModelInstance(assets.cursorModel); // Reuse cursor model for now
        ring.transform.setToTranslation(x + 0.5f, y + 0.1f, z + 0.5f); // Slightly above ground
        ring.transform.scale(radius, 0.1f, radius); // Flat, wide ring
        
        // Set bright color
        for (Material m : ring.materials) {
            m.set(ColorAttribute.createDiffuse(color));
        }
        
        batch.render(ring, env);
    }
}
