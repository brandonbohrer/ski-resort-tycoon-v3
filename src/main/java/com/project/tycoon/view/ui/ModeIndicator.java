package com.project.tycoon.view.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/**
 * Top-left widget showing current interaction mode and state.
 * Updates in real-time to show trail building progress and cost.
 */
public class ModeIndicator {

    private final Table container;
    private final Label modeLabel;

    public ModeIndicator(Skin skin, Stage stage) {

        // Create container
        container = new Table();
        container.top().left();
        container.setFillParent(true);
        container.pad(10);

        // Create label with background
        Table panel = new Table();
        panel.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                skin.get("dark_gray", com.badlogic.gdx.graphics.Texture.class)));
        panel.getColor().a = 0.7f;
        panel.pad(10);

        modeLabel = new Label("", skin);
        modeLabel.setColor(Color.WHITE);
        panel.add(modeLabel);

        container.add(panel);
        stage.addActor(container);

        // Initially hidden
        setVisible(false);
    }

    public void update(String modeText) {
        if (modeText == null || modeText.isEmpty()) {
            setVisible(false);
        } else {
            modeLabel.setText(modeText);
            setVisible(true);
        }
    }

    private void setVisible(boolean visible) {
        container.setVisible(visible);
    }
}
