package com.project.tycoon.view.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/**
 * Bottom category bar with buttons for Construction, Finances, Manage,
 * Overview, System.
 */
public class CategoryBar {

    public interface CategoryListener {
        void onCategorySelected(String categoryName);
    }

    private final Skin skin;
    private final Stage stage;
    private final CategoryListener listener;

    private Table bottomBar;
    private final ButtonGroup<TextButton> categoryGroup = new ButtonGroup<>();

    public CategoryBar(Skin skin, Stage stage, CategoryListener listener) {
        this.skin = skin;
        this.stage = stage;
        this.listener = listener;

        buildUI();
    }

    private void buildUI() {
        categoryGroup.setMinCheckCount(0); // Allow deselecting all
        categoryGroup.setMaxCheckCount(1);

        bottomBar = new Table();
        bottomBar.setFillParent(true);
        bottomBar.bottom().padBottom(10);

        addCategoryButton("Construction");
        addCategoryButton("Finances");
        addCategoryButton("Manage");
        addCategoryButton("Overview");
        addCategoryButton("System");

        stage.addActor(bottomBar);
    }

    private void addCategoryButton(String name) {
        TextButton btn = new TextButton(name, skin, "toggle");
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // If checked, notify selection
                if (btn.isChecked()) {
                    if (listener != null) {
                        listener.onCategorySelected(name);
                    }
                } else {
                    // If unchecked, notify with null (deselection)
                    if (listener != null) {
                        listener.onCategorySelected(null);
                    }
                }
            }
        });
        categoryGroup.add(btn);
        bottomBar.add(btn).height(50).width(150).pad(5);
    }

    public void uncheckAll() {
        categoryGroup.uncheckAll();
    }
}
