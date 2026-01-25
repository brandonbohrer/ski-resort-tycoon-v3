package com.project.tycoon.world.model;

import com.badlogic.gdx.graphics.Color;

/**
 * Trail difficulty ratings based on slope steepness.
 * Follows standard ski resort classification system.
 */
public enum TrailDifficulty {
    GREEN(0, 15, "Green Circle", new Color(0.0f, 0.8f, 0.0f, 1.0f)), // Beginner
    BLUE(15, 25, "Blue Square", new Color(0.0f, 0.4f, 1.0f, 1.0f)), // Intermediate
    BLACK(25, 35, "Black Diamond", new Color(0.1f, 0.1f, 0.1f, 1.0f)), // Advanced
    DOUBLE_BLACK(35, 90, "Double Black Diamond", new Color(0.8f, 0.0f, 0.0f, 1.0f)); // Expert

    private final float minSlope; // Minimum slope angle in degrees
    private final float maxSlope; // Maximum slope angle in degrees
    private final String displayName;
    private final Color markerColor;

    TrailDifficulty(float minSlope, float maxSlope, String displayName, Color markerColor) {
        this.minSlope = minSlope;
        this.maxSlope = maxSlope;
        this.displayName = displayName;
        this.markerColor = markerColor;
    }

    /**
     * Determine difficulty from average slope angle.
     * 
     * @param avgSlope Average slope in degrees
     * @return Appropriate difficulty rating
     */
    public static TrailDifficulty fromSlope(float avgSlope) {
        if (avgSlope < 15)
            return GREEN;
        if (avgSlope < 25)
            return BLUE;
        if (avgSlope < 35)
            return BLACK;
        return DOUBLE_BLACK;
    }

    public float getMinSlope() {
        return minSlope;
    }

    public float getMaxSlope() {
        return maxSlope;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getMarkerColor() {
        return markerColor.cpy(); // Return copy to prevent modification
    }

    @Override
    public String toString() {
        return displayName;
    }
}
