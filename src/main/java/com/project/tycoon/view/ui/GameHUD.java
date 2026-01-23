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

    // Persistent money ticker (always visible)
    private Table persistentMoneyTicker;
    private Label tickerMoneyLabel;

    // Detailed finance panel labels
    private Label moneyLabel;
    private Label netLabel;
    private Label revenueLabel;
    private Label expenseLabel;
    private Label liftRevenueLabel;
    private Label maintenanceExpenseLabel;
    private Label skierCountLabel;
    private Label liftCountLabel;

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

        // Persistent Money Ticker (Always Visible, Top-Right)
        persistentMoneyTicker = new Table();
        persistentMoneyTicker.setFillParent(true);
        persistentMoneyTicker.top().right();
        persistentMoneyTicker.pad(15);

        tickerMoneyLabel = new Label("", skin, "title");
        tickerMoneyLabel.setColor(com.badlogic.gdx.graphics.Color.WHITE); // Always white

        Table tickerBackground = new Table();
        tickerBackground.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                skin.get("dark_gray", com.badlogic.gdx.graphics.Texture.class)));
        tickerBackground.pad(10, 20, 10, 20);
        tickerBackground.add(tickerMoneyLabel);

        // Add hover tooltip
        final Label tooltipLabel = new Label("", skin);
        final com.badlogic.gdx.scenes.scene2d.ui.Window tooltipWindow = new com.badlogic.gdx.scenes.scene2d.ui.Window(
                "", skin);
        tooltipWindow.add(tooltipLabel).pad(5);
        tooltipWindow.pack();
        tooltipWindow.setVisible(false);
        stage.addActor(tooltipWindow);

        tickerBackground.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer,
                    com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                float dailyProfit = economy.getNetIncomePerSecond() * 86400; // seconds in a day
                String profitText = String.format("%s$%.0f/day", dailyProfit >= 0 ? "+" : "", dailyProfit);
                tooltipLabel.setText(profitText);
                tooltipLabel.setColor(
                        dailyProfit >= 0 ? com.badlogic.gdx.graphics.Color.GREEN : com.badlogic.gdx.graphics.Color.RED);
                tooltipWindow.pack();
                tooltipWindow.setPosition(event.getStageX() - tooltipWindow.getWidth() / 2,
                        event.getStageY() - tooltipWindow.getHeight() - 10);
                tooltipWindow.setVisible(true);
            }

            @Override
            public void exit(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer,
                    com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                tooltipWindow.setVisible(false);
            }

            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer,
                    int button) {
                if (button == com.badlogic.gdx.Input.Buttons.LEFT) {
                    // TODO: Open finances screen
                    System.out.println("Ticker clicked! Opening finances screen...");
                    return true;
                }
                return false;
            }
        });

        persistentMoneyTicker.add(tickerBackground);
        stage.addActor(persistentMoneyTicker);

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

        // 3. Detailed Finances Panel
        Table financesToolbar = new Table();
        financesToolbar.setFillParent(true);
        financesToolbar.top().left().pad(20);
        financesToolbar.setVisible(false);

        // Panel background
        Table financesPanel = new Table();
        financesPanel.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                skin.get("dark_gray", com.badlogic.gdx.graphics.Texture.class)));
        financesPanel.pad(20);

        // Header Section
        Label financesTitle = new Label("=== FINANCES ===", skin, "title");
        financesPanel.add(financesTitle).colspan(2).center().padBottom(15).row();

        moneyLabel = new Label("", skin, "title");
        financesPanel.add(moneyLabel).colspan(2).center().padBottom(10).row();

        // Net Income Section (Color-coded)
        netLabel = new Label("", skin);
        financesPanel.add(netLabel).colspan(2).center().padBottom(20).row();

        // Revenue Section
        Label revenueSectionLabel = new Label("--- REVENUE ---", skin);
        financesPanel.add(revenueSectionLabel).colspan(2).left().padTop(10).row();

        revenueLabel = new Label("", skin);
        financesPanel.add(new Label("  Total:", skin)).left().padLeft(10);
        financesPanel.add(revenueLabel).left().padLeft(10).row();

        liftRevenueLabel = new Label("", skin);
        financesPanel.add(new Label("  Lift Tickets:", skin)).left().padLeft(10);
        financesPanel.add(liftRevenueLabel).left().padLeft(10).row();

        // Expense Section
        Label expenseSectionLabel = new Label("--- EXPENSES ---", skin);
        financesPanel.add(expenseSectionLabel).colspan(2).left().padTop(20).row();

        expenseLabel = new Label("", skin);
        financesPanel.add(new Label("  Total:", skin)).left().padLeft(10);
        financesPanel.add(expenseLabel).left().padLeft(10).row();

        maintenanceExpenseLabel = new Label("", skin);
        financesPanel.add(new Label("  Lift Maintenance:", skin)).left().padLeft(10);
        financesPanel.add(maintenanceExpenseLabel).left().padLeft(10).row();

        // Statistics Section
        Label statsSectionLabel = new Label("--- STATISTICS ---", skin);
        financesPanel.add(statsSectionLabel).colspan(2).left().padTop(20).row();

        skierCountLabel = new Label("", skin);
        financesPanel.add(new Label("  Active Skiers:", skin)).left().padLeft(10);
        financesPanel.add(skierCountLabel).left().padLeft(10).row();

        liftCountLabel = new Label("", skin);
        financesPanel.add(new Label("  Active Lifts:", skin)).left().padLeft(10);
        financesPanel.add(liftCountLabel).left().padLeft(10).row();

        financesToolbar.add(financesPanel);
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
        // Update persistent money ticker (always visible, white only)
        if (tickerMoneyLabel != null) {
            float money = economy.getCurrentMoney();
            tickerMoneyLabel.setText(String.format("$%.0f", money));
            // Color is always white (set in buildUI)
        }

        // Update detailed finance panel
        if (moneyLabel != null) {
            float money = economy.getCurrentMoney();
            moneyLabel.setText(String.format("Current Balance: $%.0f", money));

            // Net income with color coding
            float net = economy.getNetIncomePerSecond();
            netLabel.setText(String.format("Net Income: $%.2f/sec", net));
            if (net > 0) {
                netLabel.setColor(com.badlogic.gdx.graphics.Color.GREEN);
            } else if (net < 0) {
                netLabel.setColor(com.badlogic.gdx.graphics.Color.RED);
            } else {
                netLabel.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            }

            // Revenue breakdown
            float revenue = economy.getRevenuePerSecond();
            revenueLabel.setText(String.format("$%.2f/sec", revenue));
            revenueLabel.setColor(com.badlogic.gdx.graphics.Color.GREEN);
            liftRevenueLabel.setText(String.format("$%.2f/sec", revenue)); // For now, all revenue is from lifts
            liftRevenueLabel.setColor(com.badlogic.gdx.graphics.Color.GREEN);

            // Expense breakdown
            float expenses = economy.getExpensesPerSecond();
            expenseLabel.setText(String.format("$%.2f/sec", expenses));
            expenseLabel.setColor(com.badlogic.gdx.graphics.Color.RED);
            maintenanceExpenseLabel.setText(String.format("$%.2f/sec", expenses)); // For now, all expenses are
                                                                                   // maintenance
            maintenanceExpenseLabel.setColor(com.badlogic.gdx.graphics.Color.RED);

            // Statistics (placeholder for now)
            skierCountLabel.setText("N/A");
            liftCountLabel.setText("N/A");
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
