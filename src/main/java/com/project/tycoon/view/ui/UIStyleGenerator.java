package com.project.tycoon.view.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class UIStyleGenerator {

    public static Skin generateSkin() {
        Skin skin = new Skin();

        // 1. Generate Textures
        skin.add("white", new Texture(createColorPixmap(Color.WHITE)));
        skin.add("dark_gray", new Texture(createColorPixmap(new Color(0.2f, 0.2f, 0.2f, 0.9f))));
        skin.add("light_gray", new Texture(createColorPixmap(new Color(0.4f, 0.4f, 0.4f, 1f))));
        skin.add("accent", new Texture(createColorPixmap(new Color(0.2f, 0.5f, 0.8f, 1f)))); // Blue accent

        // 2. Fonts (Default)
        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        // 3. Label Style
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // Title Label Style (for headers and important displays)
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = font;
        titleStyle.fontColor = Color.WHITE;
        skin.add("title", titleStyle);

        // 4. Button Style (Flat, modern)
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = font;
        textButtonStyle.up = getDrawable(skin, "dark_gray");
        textButtonStyle.down = getDrawable(skin, "accent");
        textButtonStyle.over = getDrawable(skin, "light_gray");
        textButtonStyle.fontColor = Color.WHITE;
        skin.add("default", textButtonStyle);

        // 5. Toggle Button Style
        TextButton.TextButtonStyle toggleStyle = new TextButton.TextButtonStyle();
        toggleStyle.font = font;
        toggleStyle.up = getDrawable(skin, "dark_gray");
        toggleStyle.checked = getDrawable(skin, "accent"); // Checked state looks active
        toggleStyle.over = getDrawable(skin, "light_gray");
        toggleStyle.fontColor = Color.WHITE;
        skin.add("toggle", toggleStyle);

        // 6. Window/Panel Style
        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.background = getDrawable(skin, "dark_gray");
        windowStyle.titleFont = font;
        windowStyle.titleFontColor = Color.WHITE;
        skin.add("default", windowStyle);

        return skin;
    }

    private static Drawable getDrawable(Skin skin, String name) {
        return new TextureRegionDrawable(skin.get(name, Texture.class));
    }

    private static Pixmap createColorPixmap(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        return pixmap;
    }
}
