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

    public RenderAssetManager() {
        buildModels();
    }

    private void buildModels() {
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
                        new IntAttribute(IntAttribute.CullFace, 0) // Disable culling
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
