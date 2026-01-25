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

        // 1. Generate Textures with Modern Color Palette
        skin.add("white", new Texture(createColorPixmap(Color.WHITE)));
        
        // Dark UI theme
        skin.add("dark_bg", new Texture(createColorPixmap(new Color(0.12f, 0.12f, 0.15f, 0.95f)))); // Almost black, slightly blue
        skin.add("medium_bg", new Texture(createColorPixmap(new Color(0.18f, 0.18f, 0.22f, 0.95f)))); // Medium dark
        skin.add("light_bg", new Texture(createColorPixmap(new Color(0.25f, 0.25f, 0.30f, 0.95f)))); // Lighter dark
        
        // Legacy names for backward compatibility
        skin.add("dark_gray", new Texture(createColorPixmap(new Color(0.18f, 0.18f, 0.22f, 0.95f)))); // Same as medium_bg
        skin.add("light_gray", new Texture(createColorPixmap(new Color(0.25f, 0.25f, 0.30f, 0.95f)))); // Same as light_bg
        
        // Accent colors
        skin.add("accent", new Texture(createColorPixmap(new Color(0.25f, 0.6f, 0.95f, 1f)))); // Bright blue (renamed from accent)
        skin.add("accent_blue", new Texture(createColorPixmap(new Color(0.25f, 0.6f, 0.95f, 1f)))); // Bright blue
        skin.add("accent_hover", new Texture(createColorPixmap(new Color(0.35f, 0.7f, 1.0f, 1f)))); // Lighter blue for hover
        
        // 2. Fonts - Use normal size (no scaling to avoid blurriness)
        BitmapFont defaultFont = new BitmapFont();
        skin.add("default", defaultFont);
        
        BitmapFont titleFont = new BitmapFont();
        skin.add("title-font", titleFont);
        
        BitmapFont smallFont = new BitmapFont();
        skin.add("small-font", smallFont);

        // 3. Label Styles
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = defaultFont;
        labelStyle.fontColor = new Color(0.95f, 0.95f, 0.95f, 1f); // Slightly off-white
        skin.add("default", labelStyle);

        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = titleFont;
        titleStyle.fontColor = Color.WHITE;
        skin.add("title", titleStyle);
        
        Label.LabelStyle smallStyle = new Label.LabelStyle();
        smallStyle.font = smallFont;
        smallStyle.fontColor = new Color(0.85f, 0.85f, 0.85f, 1f);
        skin.add("small", smallStyle);

        // 4. Modern Button Style
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = defaultFont;
        textButtonStyle.up = getDrawable(skin, "medium_bg");
        textButtonStyle.down = getDrawable(skin, "accent_blue");
        textButtonStyle.over = getDrawable(skin, "light_bg");
        textButtonStyle.fontColor = new Color(0.95f, 0.95f, 0.95f, 1f);
        textButtonStyle.downFontColor = Color.WHITE;
        textButtonStyle.overFontColor = Color.WHITE;
        skin.add("default", textButtonStyle);

        // 5. Toggle Button Style
        TextButton.TextButtonStyle toggleStyle = new TextButton.TextButtonStyle();
        toggleStyle.font = defaultFont;
        toggleStyle.up = getDrawable(skin, "medium_bg");
        toggleStyle.checked = getDrawable(skin, "accent_blue");
        toggleStyle.over = getDrawable(skin, "light_bg");
        toggleStyle.fontColor = new Color(0.95f, 0.95f, 0.95f, 1f);
        toggleStyle.checkedFontColor = Color.WHITE;
        toggleStyle.overFontColor = Color.WHITE;
        skin.add("toggle", toggleStyle);

        // 6. Window/Panel Style
        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.background = getDrawable(skin, "dark_bg");
        windowStyle.titleFont = titleFont;
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
