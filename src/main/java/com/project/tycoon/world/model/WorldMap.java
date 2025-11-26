package com.project.tycoon.world.model;

/**
 * Represents the game world grid.
 * Manages tiles, elevation, and spatial queries.
 */
public class WorldMap {
    private final int width;
    private final int depth; // Using depth instead of height to avoid confusion with elevation
    private final Tile[] tiles; // 1D array for cache locality
    private boolean dirty = true; // Default to dirty to force initial build

    public WorldMap(int width, int depth) {
        if (width <= 0 || depth <= 0) {
            throw new IllegalArgumentException("Map dimensions must be positive.");
        }
        this.width = width;
        this.depth = depth;
        this.tiles = new Tile[width * depth];

        // Initialize with default terrain
        for (int i = 0; i < tiles.length; i++) {
            tiles[i] = new Tile(TerrainType.GRASS, 0);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isValid(int x, int z) {
        return x >= 0 && x < width && z >= 0 && z < depth;
    }

    public Tile getTile(int x, int z) {
        if (!isValid(x, z)) {
            return null; // Or throw exception, depending on preference. Null is safer for edge querying.
        }
        return tiles[z * width + x];
    }
    
    public void setTileHeight(int x, int z, int height) {
        Tile t = getTile(x, z);
        if (t != null) {
            t.setHeight(height);
            this.dirty = true;
        }
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void clean() {
        this.dirty = false;
    }
    
    /**
     * Sets the tile at the given coordinates.
     * Note: This replaces the Tile object. For modifying, use getTile() and setters.
     */
    public void setTile(int x, int z, Tile tile) {
        if (!isValid(x, z)) return;
        tiles[z * width + x] = tile;
        this.dirty = true;
    }
}