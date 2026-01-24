package com.project.tycoon.view.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.project.tycoon.ecs.components.LiftComponent;
import com.project.tycoon.view.GameplayController;
import com.project.tycoon.view.GameplayController.InteractionMode;

/**
 * Construction menu with main toolbar and lift types submenu.
 */
public class ConstructionMenu {

    public interface MenuCloseListener {
        void onMenuClosed();
    }

    private enum MenuLevel {
        MAIN_TOOLS, // Terrain / Lifts / Trails
        LIFT_TYPES // T-Bar / Chairlift / Gondola + Back
    }

    private final Skin skin;
    private final Stage stage;
    private final GameplayController controller;

    private Table mainToolbar;
    private Table liftTypesToolbar;
    private MenuLevel currentLevel = MenuLevel.MAIN_TOOLS;

    private final ButtonGroup<TextButton> toolButtonGroup = new ButtonGroup<>();
    private final ButtonGroup<TextButton> liftTypeButtonGroup = new ButtonGroup<>();
    private MenuCloseListener closeListener;

    public ConstructionMenu(Skin skin, Stage stage, GameplayController controller) {
        this.skin = skin;
        this.stage = stage;
        this.controller = controller;

        buildUI();
    }

    private void buildUI() {
        toolButtonGroup.setMinCheckCount(0);
        toolButtonGroup.setMaxCheckCount(1);

        liftTypeButtonGroup.setMinCheckCount(0);
        liftTypeButtonGroup.setMaxCheckCount(1);

        // Main construction toolbar
        mainToolbar = new Table();
        mainToolbar.setFillParent(true);
        mainToolbar.bottom().padBottom(80);
        mainToolbar.setVisible(false);

        mainToolbar.add(createToolButton("Terrain", () -> controller.setMode(InteractionMode.TERRAIN)))
                .height(50).width(120).pad(5);
        mainToolbar.add(createToolButton("Lifts", this::showLiftTypesMenu))
                .height(50).width(120).pad(5);
        mainToolbar.add(createToolButton("Trails", () -> controller.setMode(InteractionMode.TRAIL)))
                .height(50).width(120).pad(5);

        stage.addActor(mainToolbar);

        // Lift types submenu
        liftTypesToolbar = new Table();
        liftTypesToolbar.setFillParent(true);
        liftTypesToolbar.bottom().padBottom(80);
        liftTypesToolbar.setVisible(false);

        liftTypesToolbar.add(createBackButton()).height(50).width(100).pad(5);
        liftTypesToolbar.add(createLiftTypeButton("T-Bar", LiftComponent.LiftType.TBAR))
                .height(50).width(120).pad(5);
        liftTypesToolbar.add(createLiftTypeButton("Chairlift", LiftComponent.LiftType.CHAIRLIFT))
                .height(50).width(120).pad(5);
        liftTypesToolbar.add(createLiftTypeButton("Gondola", LiftComponent.LiftType.GONDOLA))
                .height(50).width(120).pad(5);

        stage.addActor(liftTypesToolbar);

        // Right-click listener for back navigation
        stage.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                    float x, float y, int pointer, int button) {
                if (button == Input.Buttons.RIGHT) {
                    // Only handle if menu is actually visible
                    boolean isMenuVisible = mainToolbar.isVisible() || liftTypesToolbar.isVisible();
                    if (!isMenuVisible) {
                        return false; // Let camera controller handle it
                    }

                    if (currentLevel == MenuLevel.LIFT_TYPES) {
                        // In lift types submenu - go back to main tools
                        goBack();
                        return true;
                    } else if (currentLevel == MenuLevel.MAIN_TOOLS) {
                        // In main tools - exit tool mode and close menu
                        toolButtonGroup.uncheckAll();
                        controller.setMode(InteractionMode.NONE);
                        hide();
                        if (closeListener != null) {
                            closeListener.onMenuClosed();
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private TextButton createToolButton(String text, Runnable action) {
        TextButton btn = new TextButton(text, skin, "toggle");
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (btn.isChecked()) {
                    action.run();
                }
            }
        });
        toolButtonGroup.add(btn);
        return btn;
    }

    private TextButton createLiftTypeButton(String text, LiftComponent.LiftType type) {
        TextButton btn = new TextButton(text, skin, "toggle");
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (btn.isChecked()) {
                    controller.setSelectedLiftType(type);
                    controller.setMode(InteractionMode.BUILD);
                }
            }
        });
        liftTypeButtonGroup.add(btn);
        return btn;
    }

    private TextButton createBackButton() {
        TextButton btn = new TextButton("← Back", skin);
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                goBack();
            }
        });
        return btn;
    }

    private void showLiftTypesMenu() {
        currentLevel = MenuLevel.LIFT_TYPES;
        mainToolbar.setVisible(false);
        liftTypesToolbar.setVisible(true);
    }

    private void goBack() {
        if (currentLevel == MenuLevel.LIFT_TYPES) {
            currentLevel = MenuLevel.MAIN_TOOLS;
            liftTypesToolbar.setVisible(false);
            mainToolbar.setVisible(true);
        }
    }

    public void show() {
        mainToolbar.setVisible(true);
        liftTypesToolbar.setVisible(false);
        currentLevel = MenuLevel.MAIN_TOOLS;
    }

    public void hide() {
        mainToolbar.setVisible(false);
        liftTypesToolbar.setVisible(false);
        currentLevel = MenuLevel.MAIN_TOOLS;
    }

    public void reset() {
        hide();
        toolButtonGroup.uncheckAll();
        liftTypeButtonGroup.uncheckAll();
    }

    public void setCloseListener(MenuCloseListener listener) {
        this.closeListener = listener;
    }
}
