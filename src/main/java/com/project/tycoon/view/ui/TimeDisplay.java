package com.project.tycoon.view.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.project.tycoon.simulation.DayTimeSystem;

/**
 * Displays current day number and time of day.
 */
public class TimeDisplay {

    private final DayTimeSystem dayTimeSystem;
    private final Label timeLabel;
    private final Label dayLabel;

    public TimeDisplay(Skin skin, Stage stage, DayTimeSystem dayTimeSystem) {
        this.dayTimeSystem = dayTimeSystem;

        Table table = new Table();
        table.setFillParent(true);
        table.top().right();
        table.pad(15);

        // Day label
        dayLabel = new Label("Day 1", skin, "title");
        table.add(dayLabel).padRight(20);

        // Time label
        timeLabel = new Label("9:00 AM", skin, "title");
        table.add(timeLabel);

        table.row();

        stage.addActor(table);
    }

    public void update(float dt) {
        dayLabel.setText("Day " + dayTimeSystem.getCurrentDay());
        timeLabel.setText(dayTimeSystem.getTimeString());
    }
}
