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
import com.project.tycoon.economy.EconomyManager;
import com.project.tycoon.ecs.Engine;
import com.project.tycoon.simulation.TycoonSimulation;

/**
 * Fullscreen finances overlay that shows comprehensive financial statistics.
 * Auto-pauses game when opened.
 */
public class FinancesScreen {

    private final Stage stage;
    private final Skin skin;
    private final EconomyManager economy;
    private final Engine engine;
    private final TycoonSimulation simulation;

    private Table mainPanel;
    private boolean visible;
    
    // Dynamic labels that need updating
    private Label balanceLabel;
    private Label todayRevenueLabel;
    private Label yesterdayRevenueLabel;
    private Label totalRevenueLabel;
    private Label totalExpensesLabel;
    private Label netProfitLabel;

    public FinancesScreen(Skin skin, EconomyManager economy, Engine engine, TycoonSimulation simulation) {
        this.skin = skin;
        this.economy = economy;
        this.engine = engine;
        this.simulation = simulation;
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
        Label title = new Label("FINANCIAL REPORT", skin, "title");
        title.setColor(Color.BLACK);
        mainPanel.add(title).colspan(2).center().padBottom(30).row();

        // === CURRENT BALANCE SECTION ===
        addSectionHeader("Current Balance");
        balanceLabel = addValueRow("Cash on Hand:", "$0.00", Color.BLACK);
        
        mainPanel.row().padTop(20);
        
        // === DAILY REVENUE SECTION ===
        addSectionHeader("Daily Revenue");
        todayRevenueLabel = addValueRow("Today (Pending):", "$0.00", new Color(0.2f, 0.6f, 0.2f, 1f));
        yesterdayRevenueLabel = addValueRow("Yesterday:", "$0.00", Color.DARK_GRAY);
        
        mainPanel.row().padTop(20);
        
        // === LIFETIME TOTALS SECTION ===
        addSectionHeader("Lifetime Totals");
        totalRevenueLabel = addValueRow("Total Revenue:", "$0.00", new Color(0.1f, 0.5f, 0.1f, 1f));
        totalExpensesLabel = addValueRow("Total Expenses:", "$0.00", new Color(0.7f, 0.1f, 0.1f, 1f));
        
        // Add separator line
        mainPanel.row().padTop(10);
        Table separator = new Table();
        separator.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                skin.get("dark_gray", com.badlogic.gdx.graphics.Texture.class)));
        mainPanel.add(separator).colspan(2).height(2).fillX().padBottom(10).row();
        
        netProfitLabel = addValueRow("Net Profit/Loss:", "$0.00", Color.BLACK);

        mainPanel.row().padTop(30);

        // Close button
        TextButton closeButton = new TextButton("Close", skin);
        closeButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                hide();
            }
        });
        mainPanel.add(closeButton).colspan(2).center().width(200).height(50);

        // Center the panel
        Table container = new Table();
        container.setFillParent(true);
        container.add(mainPanel).center();
        stage.addActor(container);

        // Initially hidden
        stage.getRoot().setVisible(false);
    }
    
    /**
     * Add a section header to the finance panel.
     */
    private void addSectionHeader(String text) {
        Label header = new Label(text, skin);
        header.setColor(new Color(0.3f, 0.3f, 0.3f, 1f));
        header.setFontScale(1.2f);
        mainPanel.add(header).colspan(2).left().padBottom(10).padTop(5).row();
    }
    
    /**
     * Add a labeled value row (e.g., "Total Revenue: $1,234.56")
     * Returns the value label for later updating.
     */
    private Label addValueRow(String labelText, String initialValue, Color valueColor) {
        // Label
        Label label = new Label(labelText, skin);
        label.setColor(Color.BLACK);
        mainPanel.add(label).left().padRight(20).padBottom(8);
        
        // Value
        Label value = new Label(initialValue, skin);
        value.setColor(valueColor);
        mainPanel.add(value).right().padBottom(8).row();
        
        return value;
    }
    
    /**
     * Update all financial data labels.
     */
    private void updateFinancialData() {
        float balance = economy.getCurrentMoney();
        float todayRevenue = economy.getPendingDailyRevenue();
        float yesterdayRevenue = economy.getLastDailyRevenue();
        float totalRevenue = economy.getTotalRevenue();
        float totalExpenses = economy.getTotalExpenses();
        float netProfit = totalRevenue - totalExpenses;
        
        balanceLabel.setText(formatMoney(balance));
        todayRevenueLabel.setText(formatMoney(todayRevenue));
        yesterdayRevenueLabel.setText(formatMoney(yesterdayRevenue));
        totalRevenueLabel.setText(formatMoney(totalRevenue));
        totalExpensesLabel.setText(formatMoney(totalExpenses));
        netProfitLabel.setText(formatMoney(netProfit));
        
        // Color code the balance and net profit
        balanceLabel.setColor(balance >= 0 ? Color.BLACK : Color.RED);
        netProfitLabel.setColor(netProfit >= 0 ? new Color(0.1f, 0.5f, 0.1f, 1f) : new Color(0.7f, 0.1f, 0.1f, 1f));
    }
    
    /**
     * Format a float as money string.
     */
    private String formatMoney(float amount) {
        return String.format("$%,.2f", amount);
    }

    public void show() {
        visible = true;
        updateFinancialData(); // Refresh data when opening
        stage.getRoot().setVisible(true);
        if (simulation != null) {
            simulation.setPaused(true);
        }
        System.out.println("FinancesScreen opened");
    }

    public void hide() {
        visible = false;
        stage.getRoot().setVisible(false);
        if (simulation != null) {
            simulation.setPaused(false);
        }
        System.out.println("FinancesScreen closed");
    }

    public boolean isVisible() {
        return visible;
    }

    public void render(float dt) {
        if (visible) {
            // Handle ESC key to close
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                hide();
            }

            // Update financial data every frame while open
            updateFinancialData();

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
