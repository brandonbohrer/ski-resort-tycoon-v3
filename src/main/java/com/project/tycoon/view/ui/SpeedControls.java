package com.project.tycoon.view.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/**
 * Speed controls UI (pause, 1x, 2x, 3x) displayed in top-left corner.
 */
public class SpeedControls {

    public interface SpeedChangeListener {
        void onPauseToggled(boolean paused);

        void onSpeedChanged(float timeScale);
    }

    private final Skin skin;
    private final Stage stage;
    private final SpeedChangeListener listener;

    private Table controlsTable;
    private TextButton pauseButton;
    private final ButtonGroup<TextButton> speedGroup = new ButtonGroup<>();

    private boolean isPaused = false;

    public SpeedControls(Skin skin, Stage stage, SpeedChangeListener listener) {
        this.skin = skin;
        this.stage = stage;
        this.listener = listener;

        buildUI();
    }

    private void buildUI() {
        speedGroup.setMinCheckCount(1); // Always have a speed selected
        speedGroup.setMaxCheckCount(1);

        controlsTable = new Table();
        controlsTable.setFillParent(true);
        controlsTable.top().left();
        controlsTable.pad(15);

        // Pause button
        pauseButton = new TextButton("||", skin);
        pauseButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                togglePause();
            }
        });
        controlsTable.add(pauseButton).width(50).height(50).pad(5);

        // Speed buttons
        controlsTable.add(createSpeedButton("1x", 1.0f)).width(50).height(50).pad(5);
        controlsTable.add(createSpeedButton("2x", 2.0f)).width(50).height(50).pad(5);
        controlsTable.add(createSpeedButton("3x", 3.0f)).width(50).height(50).pad(5);

        stage.addActor(controlsTable);
    }

    private TextButton createSpeedButton(String text, float speed) {
        TextButton btn = new TextButton(text, skin, "toggle");
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (btn.isChecked()) {
                    setSpeed(speed);
                    // Unpause if paused
                    if (isPaused) {
                        togglePause();
                    }
                }
            }
        });
        speedGroup.add(btn);

        // Default to 1x speed
        if (speed == 1.0f) {
            btn.setChecked(true);
        }

        return btn;
    }

    /**
     * Toggle pause state (for keyboard shortcuts).
     */
    public void togglePause() {
        isPaused = !isPaused;
        pauseButton.setText(isPaused ? "▶" : "||");

        if (listener != null) {
            listener.onPauseToggled(isPaused);
        }
    }

    /**
     * Set speed and update button selection (for keyboard shortcuts).
     */
    public void setSpeed(float timeScale) {
        // Unpause if paused
        if (isPaused) {
            togglePause();
        }

        // Update button group to match the selected speed
        for (TextButton btn : speedGroup.getButtons()) {
            String text = btn.getText().toString();
            float buttonSpeed = Float.parseFloat(text.replace("x", ""));
            if (Math.abs(buttonSpeed - timeScale) < 0.01f) {
                btn.setChecked(true);
                break;
            }
        }

        if (listener != null) {
            listener.onSpeedChanged(timeScale);
        }
    }
}
