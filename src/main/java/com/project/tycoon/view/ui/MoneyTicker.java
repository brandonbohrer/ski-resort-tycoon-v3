package com.project.tycoon.view.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.project.tycoon.economy.EconomyManager;

/**
 * Persistent money display in top-right corner.
 * Shows current balance, hover tooltip with daily profit, and click to open
 * finances.
 */
public class MoneyTicker {

    private final EconomyManager economy;
    private final Skin skin;
    private final Stage stage;

    private Table tickerTable;
    private Label moneyLabel;
    private Window tooltipWindow;
    private Label tooltipLabel;

    private FinancesScreen financesScreen;

    public MoneyTicker(Skin skin, EconomyManager economy, Stage stage) {
        this.skin = skin;
        this.economy = economy;
        this.stage = stage;

        buildUI();
    }

    private void buildUI() {
        // Main ticker table
        tickerTable = new Table();
        tickerTable.setFillParent(true);
        tickerTable.top().right();
        tickerTable.pad(15);

        // Money label
        moneyLabel = new Label("", skin, "title");
        moneyLabel.setColor(Color.WHITE); // Always white

        // Background
        Table background = new Table();
        background.setBackground(new TextureRegionDrawable(
                skin.get("dark_gray", com.badlogic.gdx.graphics.Texture.class)));
        background.pad(10, 20, 10, 20);
        background.add(moneyLabel);

        // Tooltip
        tooltipLabel = new Label("", skin);
        tooltipWindow = new Window("", skin);
        tooltipWindow.add(tooltipLabel).pad(5);
        tooltipWindow.pack();
        tooltipWindow.setVisible(false);
        stage.addActor(tooltipWindow);

        // Input listener
        background.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                float lastRevenue = economy.getLastDailyRevenue();
                String revenueText = String.format("Yesterday: $%.0f", lastRevenue);
                tooltipLabel.setText(revenueText);
                tooltipLabel.setColor(Color.WHITE);
                tooltipWindow.pack();
                tooltipWindow.setPosition(
                        event.getStageX() - tooltipWindow.getWidth() / 2,
                        event.getStageY() - tooltipWindow.getHeight() - 10);
                tooltipWindow.setVisible(true);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                tooltipWindow.setVisible(false);
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button == Input.Buttons.LEFT) {
                    if (financesScreen != null) {
                        financesScreen.show();
                    }
                    return true;
                }
                return false;
            }
        });

        tickerTable.add(background);
        stage.addActor(tickerTable);
    }

    public void setFinancesScreen(FinancesScreen financesScreen) {
        this.financesScreen = financesScreen;
    }

    public void update(float dt) {
        if (moneyLabel != null) {
            float money = economy.getCurrentMoney();
            moneyLabel.setText(String.format("$%.0f", money));
        }
    }
}
