package com.project.tycoon.world;

import com.badlogic.gdx.math.Vector2;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.TrailDifficulty;
import com.project.tycoon.world.model.WorldMap;

import java.util.List;

/**
 * Calculates trail difficulty based on slope analysis.
 * Examines average steepness across all trail segments.
 */
public class TrailDifficultyCalculator {

    /**
     * Calculate difficulty rating for a trail.
     * 
     * @param map        WorldMap for height lookups
     * @param trailTiles List of (x,z) coordinates representing the trail
     * @return Difficulty rating based on average slope
     */
    public static TrailDifficulty calculate(WorldMap map, List<Vector2> trailTiles) {
        if (trailTiles == null || trailTiles.size() < 2) {
            return TrailDifficulty.GREEN; // Default for invalid trails
        }

        float totalSlope = 0;
        int slopeCount = 0;

        // Calculate slope between each consecutive pair of tiles
        for (int i = 0; i < trailTiles.size() - 1; i++) {
            Vector2 current = trailTiles.get(i);
            Vector2 next = trailTiles.get(i + 1);

            // Get tile heights
            Tile t1 = map.getTile((int) current.x, (int) current.y);
            Tile t2 = map.getTile((int) next.x, (int) next.y);

            if (t1 == null || t2 == null)
                continue;

            float h1 = t1.getHeight();
            float h2 = t2.getHeight();
            float deltaH = Math.abs(h2 - h1);

            // Horizontal distance (2D)
            float distance = current.dst(next);

            if (distance < 0.01f)
                continue; // Avoid division by zero

            // Calculate slope angle in degrees
            float slopeAngle = (float) Math.toDegrees(Math.atan(deltaH / distance));

            totalSlope += slopeAngle;
            slopeCount++;
        }

        if (slopeCount == 0) {
            return TrailDifficulty.GREEN;
        }

        // Average slope across all segments
        float avgSlope = totalSlope / slopeCount;

        return TrailDifficulty.fromSlope(avgSlope);
    }

    /**
     * Get human-readable slope description.
     * 
     * @param avgSlope Average slope in degrees
     * @return Description like "Gentle (8°)" or "Steep (32°)"
     */
    public static String getSlopeDescription(float avgSlope) {
        if (avgSlope < 10)
            return String.format("Gentle (%.1f°)", avgSlope);
        if (avgSlope < 20)
            return String.format("Moderate (%.1f°)", avgSlope);
        if (avgSlope < 30)
            return String.format("Steep (%.1f°)", avgSlope);
        return String.format("Very Steep (%.1f°)", avgSlope);
    }
}
