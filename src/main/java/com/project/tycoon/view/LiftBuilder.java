package com.project.tycoon.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to calculate lift paths and pylon positions.
 */
public class LiftBuilder {

    private static final float PYLON_INTERVAL = 8.0f; // Tiles between pylons

    public static class LiftPreview {
        public List<Vector2> pylonPositions = new ArrayList<>();
        public boolean isValid = true;
        public Color statusColor = Color.GREEN;
    }

    /**
     * Calculates where pylons should go between start and end.
     */
    public static LiftPreview calculatePreview(int startX, int startZ, int endX, int endZ) {
        LiftPreview preview = new LiftPreview();
        
        Vector2 start = new Vector2(startX, startZ);
        Vector2 end = new Vector2(endX, endZ);
        
        float dist = start.dst(end);
        
        // Always add start
        preview.pylonPositions.add(new Vector2(startX, startZ));
        
        // Add intermediate pylons
        if (dist > PYLON_INTERVAL) {
            Vector2 dir = new Vector2(end).sub(start).nor();
            float currentDist = PYLON_INTERVAL;
            
            while (currentDist < dist - (PYLON_INTERVAL/2)) { // Don't put one too close to end
                Vector2 pylonPos = new Vector2(start).mulAdd(dir, currentDist);
                // Snap to nearest integer tile for pylon placement? 
                // Or keep floating point? Pylons usually sit ON a tile.
                // Let's round to nearest tile.
                pylonPos.x = Math.round(pylonPos.x);
                pylonPos.y = Math.round(pylonPos.y);
                
                preview.pylonPositions.add(pylonPos);
                
                currentDist += PYLON_INTERVAL;
            }
        }
        
        // Always add end
        if (dist > 0) {
             preview.pylonPositions.add(new Vector2(endX, endZ));
        }

        // TODO: Check for obstacles (rocks) here and set preview.isValid = false / Color.RED
        
        return preview;
    }
}

