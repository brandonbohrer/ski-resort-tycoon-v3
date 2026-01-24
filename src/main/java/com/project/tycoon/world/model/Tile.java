package com.project.tycoon.world.model;

/**
 * Represents a single cell in the world grid.
 * Contains terrain data, elevation, and objects.
 */
public class Tile {

    public enum TrailDifficulty {
        GREEN, // Beginner (base)
        BLUE, // Intermediate (mid-mountain)
        BLACK, // Advanced (high)
        DOUBLE_BLACK // Expert (peak)
    }

    private TerrainType type;
    private Decoration decoration = Decoration.NONE;
    private int height; // Elevation level
    private boolean isTrail = false;
    private TrailDifficulty difficulty = TrailDifficulty.GREEN; // Auto-assigned for trails

    public Tile(TerrainType type, int height) {
        this.type = type;
        this.height = height;
    }

    public boolean isTrail() {
        return isTrail;
    }

    public void setTrail(boolean trail) {
        isTrail = trail;

        // Auto-calculate difficulty when tile becomes a trail
        if (trail) {
            this.difficulty = calculateDifficulty();
        }
    }

    /**
     * Auto-calculate trail difficulty based on elevation.
     */
    private TrailDifficulty calculateDifficulty() {
        if (height < 25) {
            return TrailDifficulty.GREEN; // Bottom 25% = Green
        } else if (height < 50) {
            return TrailDifficulty.BLUE; // 25-50% = Blue
        } else if (height < 75) {
            return TrailDifficulty.BLACK; // 50-75% = Black
        } else {
            return TrailDifficulty.DOUBLE_BLACK; // Top 25% = Double Black
        }
    }

    public TrailDifficulty getDifficulty() {
        return difficulty;
    }

    public TerrainType getType() {
        return type;
    }

    public void setType(TerrainType type) {
        this.type = type;
    }

    public Decoration getDecoration() {
        return decoration;
    }

    public void setDecoration(Decoration decoration) {
        this.decoration = decoration;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}