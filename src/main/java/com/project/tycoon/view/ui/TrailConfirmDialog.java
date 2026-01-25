package com.project.tycoon.view.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Modal dialog for confirming trail construction.
 * Shows trail details and cost before finalizing.
 */
public class TrailConfirmDialog {

    private final Stage stage;
    private final Skin skin;

    private Table mainPanel;
    private boolean visible;

    // Confirmation callback
    public interface ConfirmListener {
        void onConfirm();

        void onCancel();
    }

    private ConfirmListener listener;

    // Trail info to display
    private Label lengthLabel;
    private Label costLabel;
    private Label routeLabel;

    public TrailConfirmDialog(Skin skin) {
        this.skin = skin;
        this.stage = new Stage(new ScreenViewport());
        this.visible = false;

        buildUI();
    }

    private void buildUI() {
        // Fullscreen semi-transparent background
        Table background = new Table();
        background.setFillParent(true);
        background.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                skin.get("dark_gray", com.badlogic.gdx.graphics.Texture.class)));
        background.getColor().a = 0.7f; // Semi-transparent
        stage.addActor(background);

        // Main content panel (centered)
        mainPanel = new Table();
        mainPanel.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                skin.get("white", com.badlogic.gdx.graphics.Texture.class)));
        mainPanel.pad(30);

        // Header
        Label title = new Label("BUILD TRAIL?", skin, "title");
        title.setColor(Color.BLACK);
        mainPanel.add(title).center().padBottom(20).colspan(2).row();

        // Trail details
        lengthLabel = new Label("Length: 0 tiles", skin);
        lengthLabel.setColor(Color.BLACK);
        mainPanel.add(lengthLabel).left().padBottom(10).colspan(2).row();

        costLabel = new Label("Cost: $0", skin);
        costLabel.setColor(Color.BLACK);
        mainPanel.add(costLabel).left().padBottom(10).colspan(2).row();

        routeLabel = new Label("Route: ... -> ...", skin);
        routeLabel.setColor(Color.DARK_GRAY);
        mainPanel.add(routeLabel).left().padBottom(20).colspan(2).row();

        // Buttons
        TextButton confirmButton = new TextButton("Confirm (ENTER)", skin);
        confirmButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                if (listener != null) {
                    listener.onConfirm();
                }
                hide();
            }
        });

        TextButton cancelButton = new TextButton("Cancel (ESC)", skin);
        cancelButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                if (listener != null) {
                    listener.onCancel();
                }
                hide();
            }
        });

        mainPanel.add(confirmButton).width(200).height(50).padRight(10);
        mainPanel.add(cancelButton).width(200).height(50);

        // Center the panel
        Table container = new Table();
        container.setFillParent(true);
        container.add(mainPanel).center();
        stage.addActor(container);

        // Initially hidden
        stage.getRoot().setVisible(false);
    }

    public void show(int tileCount, float cost, String startType, String endType) {
        // Update labels
        lengthLabel.setText("Length: " + tileCount + " tiles");
        costLabel.setText("Cost: $" + String.format("%.0f", cost));
        routeLabel.setText("Route: " + startType + " -> " + endType);

        visible = true;
        stage.getRoot().setVisible(true);
    }

    public void hide() {
        visible = false;
        stage.getRoot().setVisible(false);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setListener(ConfirmListener listener) {
        this.listener = listener;
    }

    public void render(float dt) {
        if (visible) {
            // Handle keyboard shortcuts
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                if (listener != null) {
                    listener.onConfirm();
                }
                hide();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                if (listener != null) {
                    listener.onCancel();
                }
                hide();
            }

            stage.act(dt);
            stage.draw();
        }
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
    }

    public Stage getStage() {
        return stage;
    }
}
