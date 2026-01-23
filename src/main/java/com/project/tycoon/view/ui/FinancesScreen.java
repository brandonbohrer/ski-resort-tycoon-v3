package com.project.tycoon.view.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.project.tycoon.economy.EconomyManager;
import com.project.tycoon.ecs.Engine;

/**
 * Fullscreen finances overlay that shows comprehensive financial statistics.
 * Auto-pauses game when opened.
 */
public class FinancesScreen {

    private final Stage stage;
    private final Skin skin;
    private final EconomyManager economy;
    private final Engine engine;

    private Table mainPanel;
    private boolean visible;

    public FinancesScreen(Skin skin, EconomyManager economy, Engine engine) {
        this.skin = skin;
        this.economy = economy;
        this.engine = engine;
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
        background.getColor().a = 0.8f; // Semi-transparent
        stage.addActor(background);

        // Main content panel (centered)
        mainPanel = new Table();
        mainPanel.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                skin.get("white", com.badlogic.gdx.graphics.Texture.class)));
        mainPanel.pad(40);

        // Header
        Label title = new Label("FINANCES", skin, "title");
        title.setColor(Color.BLACK);
        mainPanel.add(title).center().padBottom(20).row();

        // Placeholder content
        Label placeholder = new Label("Comprehensive stats coming soon...", skin);
        placeholder.setColor(Color.BLACK);
        mainPanel.add(placeholder).center().padBottom(20).row();

        // Close button
        TextButton closeButton = new TextButton("Close", skin);
        closeButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                hide();
            }
        });
        mainPanel.add(closeButton).center().width(150).height(50);

        // Center the panel
        Table container = new Table();
        container.setFillParent(true);
        container.add(mainPanel).center();
        stage.addActor(container);

        // Initially hidden
        stage.getRoot().setVisible(false);
    }

    public void show() {
        visible = true;
        stage.getRoot().setVisible(true);
        System.out.println("FinancesScreen opened");
    }

    public void hide() {
        visible = false;
        stage.getRoot().setVisible(false);
        System.out.println("FinancesScreen closed");
    }

    public boolean isVisible() {
        return visible;
    }

    public void render(float dt) {
        if (visible) {
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
