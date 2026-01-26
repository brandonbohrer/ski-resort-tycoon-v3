package com.project.tycoon.view.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

public class RenderAssetManager {

    // Reusable entity models
    public Model skierModel;
    public Model liftPylonModel;
    public Model cableModel;
    public Model cursorModel;

    // Decoration Models
    public Model treeModel1;
    public Model treeModel2;
    public Model rockModel;

    // Trail Marker
    public Model trailMarkerModel;

    // Base Camp Building
    public Model baseCampModel;
    public Model skisModel; // Ski equipment

    public RenderAssetManager() {
        buildModels();
    }

    private void buildModels() {
        ModelBuilder mb = new ModelBuilder();

        // Enhanced Skier Model - More humanoid appearance
        mb.begin();
        
        // Legs (dark pants)
        mb.node().id = "legs";
        mb.part("legs", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.2f, 0.3f, 1f))))
                .box(0.3f, 0.5f, 0.3f); // Lower half
        
        // Torso (colorful ski jacket - bright cyan/teal)
        mb.node().id = "torso";
        mb.node().translation.set(0, 0.5f, 0);
        mb.part("torso", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(new Color(0.1f, 0.7f, 0.9f, 1f))))
                .box(0.4f, 0.6f, 0.35f);
        
        // Head (skin tone)
        mb.node().id = "head";
        mb.node().translation.set(0, 1.0f, 0);
        mb.part("head", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(new Color(0.95f, 0.8f, 0.7f, 1f))))
                .sphere(0.25f, 0.25f, 0.25f, 6, 6);
        
        // Hat (bright color - red/orange)
        mb.node().id = "hat";
        mb.node().translation.set(0, 1.25f, 0);
        mb.part("hat", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(new Color(0.9f, 0.3f, 0.1f, 1f))))
                .sphere(0.28f, 0.15f, 0.28f, 6, 4);
        
        skierModel = mb.end();

        // Enhanced Lift Pylon - Industrial metal structure
        mb.begin();
        
        // Main vertical support (metallic silver)
        mb.node().id = "main_support";
        mb.part("main_support", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(new Color(0.7f, 0.75f, 0.8f, 1f))))
                .cylinder(0.15f, 3.0f, 0.15f, 8);
        
        // Cross-bracing (darker metal)
        mb.node().id = "brace1";
        mb.node().translation.set(0, 1.0f, 0);
        mb.part("brace1", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(new Color(0.5f, 0.55f, 0.6f, 1f))))
                .box(0.4f, 0.08f, 0.08f);
        
        mb.node().id = "brace2";
        mb.node().translation.set(0, 2.0f, 0);
        mb.part("brace2", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(new Color(0.5f, 0.55f, 0.6f, 1f))))
                .box(0.08f, 0.08f, 0.4f);
        
        // Top platform (yellow safety marking)
        mb.node().id = "platform";
        mb.node().translation.set(0, 2.8f, 0);
        mb.part("platform", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(new Color(0.9f, 0.9f, 0.1f, 1f))))
                .cylinder(0.25f, 0.15f, 0.25f, 8);
        
        liftPylonModel = mb.end();

        // Enhanced Cable - Thicker, more visible
        mb.begin();
        mb.node().id = "cable";
        mb.part("cable", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(
                        ColorAttribute.createDiffuse(new Color(0.15f, 0.15f, 0.15f, 1f)), // Dark gray instead of pure black
                        new IntAttribute(IntAttribute.CullFace, 0) // Disable culling
                ))
                .cylinder(0.06f, 1.0f, 0.06f, 6); // Cylindrical cable instead of box
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

        // Trail Marker (White for tinting)
        mb.begin();
        mb.node().id = "pole";
        mb.part("pole", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(Color.WHITE)))
                .cylinder(0.1f, 0.5f, 0.1f, 6);
        trailMarkerModel = mb.end();

        // Base Camp - Ski lodge with brown walls and white gabled roof
        mb.begin();

        // Brown wooden walls (base)
        mb.node().id = "walls";
        mb.part("walls", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(new Color(0.55f, 0.35f, 0.2f, 1f))))
                .box(16f, 6f, 12f);

        // White snowy roof (box on top, angled to simulate gable)
        mb.node().id = "roof_main";
        mb.node().translation.set(0, 6f, 0);
        mb.part("roof_main", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(new Color(0.95f, 0.95f, 0.98f, 1f))))
                .box(16.8f, 3.2f, 12.8f);

        // Roof peak (triangular prism effect using thin box)
        mb.node().id = "roof_peak";
        mb.node().translation.set(0, 8.4f, 0);
        mb.part("roof_peak", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(new Color(0.95f, 0.95f, 0.98f, 1f))))
                .box(16.8f, 1.2f, 3.2f);

        baseCampModel = mb.end();
        
        // Skis - Two thin boxes for realistic ski equipment
        mb.begin();
        
        // Left ski (thin, long box)
        mb.node().id = "ski_left";
        mb.node().translation.set(-0.15f, -0.25f, 0);
        mb.part("ski_left", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(new Color(1.0f, 0.2f, 0.0f, 1f)))) // Bright orange
                .box(0.08f, 0.02f, 0.9f); // Thin, flat, long
        
        // Right ski
        mb.node().id = "ski_right";
        mb.node().translation.set(0.15f, -0.25f, 0);
        mb.part("ski_right", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(new Color(1.0f, 0.2f, 0.0f, 1f))))
                .box(0.08f, 0.02f, 0.9f);
        
        skisModel = mb.end();
    }

    public void dispose() {
        if (skierModel != null)
            skierModel.dispose();
        if (liftPylonModel != null)
            liftPylonModel.dispose();
        if (cursorModel != null)
            cursorModel.dispose();
        if (treeModel1 != null)
            treeModel1.dispose();
        if (treeModel2 != null)
            treeModel2.dispose();
        if (rockModel != null)
            rockModel.dispose();
        if (cableModel != null)
            cableModel.dispose();
        if (trailMarkerModel != null)
            trailMarkerModel.dispose();
        if (baseCampModel != null)
            baseCampModel.dispose();
    }
}
