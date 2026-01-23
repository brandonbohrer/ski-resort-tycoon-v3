package com.project.tycoon.view.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.project.tycoon.ecs.components.LiftComponent;
import com.project.tycoon.economy.EconomyManager;
import com.project.tycoon.view.GameplayController;
import com.project.tycoon.view.GameplayController.InteractionMode;

import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import java.util.HashMap;
import java.util.Map;

public class GameHUD {

    private final Stage stage;
    private final Skin skin;
    private final GameplayController controller;
    private final EconomyManager economy;

    // Category State
    private final ButtonGroup<TextButton> categoryGroup = new ButtonGroup<>();
    private final Map<String, Table> toolbars = new HashMap<>();

    // Menu navigation state
    private MenuLevel currentMenuLevel = MenuLevel.MAIN_CATEGORIES;
    private Table liftTypesToolbar;

    private enum MenuLevel {
        MAIN_CATEGORIES, // Construction / Finances / etc
        CONSTRUCTION_TOOLS, // Terrain / Lifts / Trails
        LIFT_TYPES // T-Bar / Chairlift / Gondola
    }

    // Tool State
    private final ButtonGroup<TextButton> constructionToolGroup = new ButtonGroup<>();
    private final ButtonGroup<TextButton> liftTypeGroup = new ButtonGroup<>();

    // Finance labels
    private Label moneyLabel;
    private Label revenueLabel;
    private Label expenseLabel;
    private Label netLabel;

    public GameHUD(GameplayController controller, EconomyManager economy) {
        this.controller = controller;
        this.economy = economy;
        this.stage = new Stage(new ScreenViewport());
        this.skin = UIStyleGenerator.generateSkin();

        buildUI();
    }

    private void buildUI() {
        // Configure Groups
        categoryGroup.setMinCheckCount(0); // Allow deselecting all
        categoryGroup.setMaxCheckCount(1);

        constructionToolGroup.setMinCheckCount(0); // Allow none selected initially
        constructionToolGroup.setMaxCheckCount(1);

        liftTypeGroup.setMinCheckCount(0); // Allow none selected initially
        liftTypeGroup.setMaxCheckCount(1);

        // Main Bottom Bar
        Table bottomBar = new Table();
        bottomBar.setFillParent(true);
        bottomBar.bottom();

        // Add buttons
        bottomBar.add(createCategoryButton("Construction")).height(60).pad(10).width(150);
        bottomBar.add(createCategoryButton("Finances")).height(60).pad(10).width(150);
        bottomBar.add(createCategoryButton("Manage")).height(60).pad(10).width(150);
        bottomBar.add(createCategoryButton("Overview")).height(60).pad(10).width(150);
        bottomBar.add(createCategoryButton("System")).height(60).pad(10).width(150);

        stage.addActor(bottomBar);

        // 1. Construction Toolbar (Main Level)
        Table constructionToolbar = new Table();
        constructionToolbar.setFillParent(true);
        constructionToolbar.bottom().padBottom(80);
        constructionToolbar.setVisible(false);

        constructionToolbar.add(createToolButton("Terrain", () -> controller.setMode(InteractionMode.TERRAIN)))
                .height(50).width(120).pad(5);
        constructionToolbar.add(createToolButton("Lifts", () -> showLiftTypesMenu())).height(50)
                .width(120).pad(5);
        constructionToolbar.add(createToolButton("Trails", () -> controller.setMode(InteractionMode.TRAIL))).height(50)
                .width(120).pad(5);

        stage.addActor(constructionToolbar);
        toolbars.put("Construction", constructionToolbar);

        // 2. Lift Types Toolbar (Sub-menu)
        liftTypesToolbar = new Table();
        liftTypesToolbar.setFillParent(true);
        liftTypesToolbar.bottom().padBottom(80);
        liftTypesToolbar.setVisible(false);

        liftTypesToolbar.add(createBackButton()).height(50).width(100).pad(5);
        liftTypesToolbar.add(createLiftTypeButton("T-Bar", LiftComponent.LiftType.TBAR)).height(50).width(120)
                .pad(5);
        liftTypesToolbar.add(createLiftTypeButton("Chairlift", LiftComponent.LiftType.CHAIRLIFT)).height(50)
                .width(120).pad(5);
        liftTypesToolbar.add(createLiftTypeButton("Gondola", LiftComponent.LiftType.GONDOLA)).height(50).width(120)
                .pad(5);

        stage.addActor(liftTypesToolbar);

        // 3. Finances Toolbar
        Table financesToolbar = new Table();
        financesToolbar.setFillParent(true);
        financesToolbar.top().left().pad(20);
        financesToolbar.setVisible(false);

        moneyLabel = new Label("", skin);
        revenueLabel = new Label("", skin);
        expenseLabel = new Label("", skin);
        netLabel = new Label("", skin);

        financesToolbar.add(moneyLabel).left().row();
        financesToolbar.add(revenueLabel).left().row();
        financesToolbar.add(expenseLabel).left().row();
        financesToolbar.add(netLabel).left().row();

        stage.addActor(financesToolbar);
        toolbars.put("Finances", financesToolbar);

        // Add right-click listener for back navigation
        stage.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                    float x, float y, int pointer, int button) {
                // Only consume right-click if we're in a sub-menu
                if (button == com.badlogic.gdx.Input.Buttons.RIGHT && currentMenuLevel == MenuLevel.LIFT_TYPES) {
                    goBack();
                    return true; // Consume event
                }
                return false; // Don't consume - let camera controller handle it
            }
        });
    }

    private TextButton createCategoryButton(String name) {
        TextButton btn = new TextButton(name, skin, "toggle");
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (btn.isChecked()) {
                    showCategory(name);
                } else {
                    hideCategory(name);
                }
            }
        });
        categoryGroup.add(btn);
        return btn;
    }

    private void showCategory(String name) {
        // Hide all toolbars first (safety)
        for (Table t : toolbars.values())
            t.setVisible(false);

        if (toolbars.containsKey(name)) {
            toolbars.get(name).setVisible(true);
        }
        System.out.println("Selected Category: " + name);
    }

    private void hideCategory(String name) {
        if (toolbars.containsKey(name)) {
            toolbars.get(name).setVisible(false);
        }
    }

    private TextButton createToolButton(String text, Runnable action) {
        TextButton btn = new TextButton(text, skin, "toggle");
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (btn.isChecked()) {
                    action.run(); // Execute only when selected
                }
            }
        });
        constructionToolGroup.add(btn);
        return btn;
    }

    private TextButton createLiftTypeButton(String text, LiftComponent.LiftType type) {
        TextButton btn = new TextButton(text, skin, "toggle");
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (btn.isChecked()) {
                    controller.setSelectedLiftType(type);
                    controller.setMode(InteractionMode.BUILD); // Activate build mode when lift type selected
                }
            }
        });
        liftTypeGroup.add(btn);
        return btn;
    }

    private void showLiftTypesMenu() {
        currentMenuLevel = MenuLevel.LIFT_TYPES;
        toolbars.get("Construction").setVisible(false);
        liftTypesToolbar.setVisible(true);
        System.out.println("Showing lift types menu");
    }

    private void goBack() {
        if (currentMenuLevel == MenuLevel.LIFT_TYPES) {
            currentMenuLevel = MenuLevel.CONSTRUCTION_TOOLS;
            liftTypesToolbar.setVisible(false);
            toolbars.get("Construction").setVisible(true);
            System.out.println("Back to construction tools");
        }
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

    // Removed manual tracking methods (selectTool, selectCategory) as ButtonGroup
    // handles it.

    public void render(float dt) {
        // Update finance labels
        if (moneyLabel != null) {
            moneyLabel.setText(String.format("Money: $%.0f", economy.getCurrentMoney()));
            revenueLabel.setText(String.format("Revenue: $%.1f/sec", economy.getRevenuePerSecond()));
            expenseLabel.setText(String.format("Expenses: $%.1f/sec", economy.getExpensesPerSecond()));
            float net = economy.getNetIncomePerSecond();
            netLabel.setText(String.format("Net: $%.1f/sec", net));
        }

        stage.act(dt);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    public Stage getStage() {
        return stage;
    }
}
