package com.project.tycoon.world.model;

/**
 * Represents the optimal location for base camp placement.
 * Returned by terrain generator after finding flattest area.
 */
public class BaseCampLocation {
    public final int x;
    public final int z;
    public final float height;

    public BaseCampLocation(int x, int z, float height) {
        this.x = x;
        this.z = z;
        this.height = height;
    }
}
