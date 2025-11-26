package com.project.tycoon.world.model;

/**
 * Represents a single cell in the world grid.
 * Contains terrain data and elevation.
 */
public class Tile {
    private TerrainType type;
    private int height; // Elevation level

    public Tile(TerrainType type, int height) {
        this.type = type;
        this.height = height;
    }

    public TerrainType getType() {
        return type;
    }

    public void setType(TerrainType type) {
        this.type = type;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}

