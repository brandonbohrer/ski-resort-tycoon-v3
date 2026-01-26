package com.project.tycoon.view.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.components.*;

/**
 * UI panel that displays information about the selected entity.
 */
public class EntityInfoPanel {
    
    private final Engine engine;
    private final Skin skin;
    private final Stage stage;
    
    private Table panel;
    private Label titleLabel;
    private Label line1Label;
    private Label line2Label;
    private Label line3Label;
    
    private Entity currentEntity = null;
    
    public EntityInfoPanel(Skin skin, Stage stage, Engine engine) {
        this.skin = skin;
        this.stage = stage;
        this.engine = engine;
        
        buildUI();
    }
    
    private void buildUI() {
        panel = new Table();
        panel.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                skin.get("dark_bg", com.badlogic.gdx.graphics.Texture.class)));
        panel.pad(15);
        
        // Position in bottom-left corner
        Table container = new Table();
        container.setFillParent(true);
        container.bottom().left();
        container.pad(15, 15, 80, 15); // Extra bottom padding to avoid category bar
        
        // Title
        titleLabel = new Label("", skin);
        titleLabel.setColor(new Color(0.7f, 0.8f, 0.95f, 1f));
        panel.add(titleLabel).left().pad(5).row();
        
        // Info lines
        line1Label = new Label("", skin);
        line1Label.setColor(Color.WHITE);
        panel.add(line1Label).left().pad(2).row();
        
        line2Label = new Label("", skin);
        line2Label.setColor(new Color(0.85f, 0.85f, 0.85f, 1f));
        panel.add(line2Label).left().pad(2).row();
        
        line3Label = new Label("", skin);
        line3Label.setColor(new Color(0.85f, 0.85f, 0.85f, 1f));
        panel.add(line3Label).left().pad(2).row();
        
        container.add(panel);
        panel.setVisible(false);
        
        stage.addActor(container);
    }
    
    /**
     * Update the panel to show info about the given entity.
     */
    public void setSelectedEntity(Entity entity) {
        this.currentEntity = entity;
        
        if (entity == null) {
            panel.setVisible(false);
            return;
        }
        
        panel.setVisible(true);
        updateInfo();
    }
    
    /**
     * Update info display (call this each frame to show dynamic info like skier state).
     */
    public void update() {
        if (currentEntity != null) {
            updateInfo();
        }
    }
    
    private void updateInfo() {
        if (currentEntity == null) return;
        
        // Check if entity still exists (check if it's in the entities set)
        boolean exists = false;
        for (Entity e : engine.getEntities()) {
            if (e.getId().equals(currentEntity.getId())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            setSelectedEntity(null);
            return;
        }
        
        // Skier
        if (engine.hasComponent(currentEntity, SkierComponent.class)) {
            displaySkierInfo();
        }
        // Lift
        else if (engine.hasComponent(currentEntity, LiftComponent.class)) {
            displayLiftInfo();
        }
        // Base Camp
        else if (engine.hasComponent(currentEntity, BaseCampComponent.class)) {
            displayBaseCampInfo();
        }
        else {
            titleLabel.setText("Unknown Entity");
            line1Label.setText("");
            line2Label.setText("");
            line3Label.setText("");
        }
    }
    
    private void displaySkierInfo() {
        SkierComponent skier = engine.getComponent(currentEntity, SkierComponent.class);
        TransformComponent pos = engine.getComponent(currentEntity, TransformComponent.class);
        
        titleLabel.setText("Skier - " + skier.skillLevel.name());
        
        // State
        String stateText = "Status: " + getSkierStateDescription(skier);
        line1Label.setText(stateText);
        
        // Position
        if (pos != null) {
            line2Label.setText(String.format("Position: (%.0f, %.0f)", pos.x, pos.z));
        }
        
        // Satisfaction
        line3Label.setText(String.format("Satisfaction: %.0f%%", skier.satisfaction * 100));
    }
    
    private String getSkierStateDescription(SkierComponent skier) {
        switch (skier.state) {
            case WAITING:
                if (skier.targetLiftId != null) {
                    return "Walking to lift";
                }
                return "Waiting";
            case QUEUED:
                return "Queued for lift";
            case RIDING_LIFT:
                return "Riding lift";
            case SKIING:
                if (skier.targetLiftId != null) {
                    return "Skiing to next lift";
                }
                return "Skiing down";
            case FINISHED:
                return "Leaving resort";
            default:
                return skier.state.name();
        }
    }
    
    private void displayLiftInfo() {
        LiftComponent lift = engine.getComponent(currentEntity, LiftComponent.class);
        TransformComponent pos = engine.getComponent(currentEntity, TransformComponent.class);
        
        titleLabel.setText("Lift - " + lift.type.name());
        
        // Pylon info
        if (lift.nextPylonId == null) {
            line1Label.setText("Top Station");
        } else {
            line1Label.setText("Pylon");
        }
        
        // Position
        if (pos != null) {
            line2Label.setText(String.format("Position: (%.0f, %.0f)", pos.x, pos.z));
        }
        
        line3Label.setText("Click to follow lift");
    }
    
    private void displayBaseCampInfo() {
        titleLabel.setText("Base Camp");
        line1Label.setText("Main lodge");
        line2Label.setText("Skier spawn point");
        line3Label.setText("");
    }
}

