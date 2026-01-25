package com.project.tycoon.world.model;

/**
 * Represents a single cell in the world grid.
 * Contains terrain data, elevation, and objects.
 */
public class Tile {

    private TerrainType type;
    private Decoration decoration = Decoration.NONE;
    private int height; // Elevation level
    private boolean isTrail = false;
    private TrailDifficulty trailDifficulty = TrailDifficulty.GREEN; // Set by trail builder

    public Tile(TerrainType type, int height) {
        this.type = type;
        this.height = height;
    }

    public boolean isTrail() {
        return isTrail;
    }

    public void setTrail(boolean trail) {
        isTrail = trail;
    }

    public TrailDifficulty getTrailDifficulty() {
        return trailDifficulty;
    }

    public void setTrailDifficulty(TrailDifficulty difficulty) {
        this.trailDifficulty = difficulty;
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