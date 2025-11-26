package com.project.tycoon.view;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;
import com.project.tycoon.view.util.IsoUtils;

public class MousePicker {
    private final WorldMap map;
    private final Camera camera;

    public MousePicker(WorldMap map, Camera camera) {
        this.map = map;
        this.camera = camera;
    }

    public Vector2 pickTile(float screenX, float screenY) {
        Ray ray = camera.getPickRay(screenX, screenY);
        
        // Intersect ray with the bounding box of the entire map to find start/end T would be better,
        // but purely estimating from y=average_height plane is faster for MVP.
        
        // Plane Y=10 intersection (approx mid-height):
        // Ray P = O + tD
        // P.y = 10
        // t = (10 - O.y) / D.y
        float t0 = (10f - ray.origin.y) / ray.direction.y;
        if (t0 < 0) t0 = 0; // Clamp
        
        Vector3 intersection = new Vector3(ray.direction).scl(t0).add(ray.origin);
        
        int startX = (int)intersection.x;
        int startZ = (int)intersection.z;
        
        // Search radius to catch mountains or valleys
        int radius = 30; 
        
        Vector3 p1 = new Vector3();
        Vector3 p2 = new Vector3();
        Vector3 p3 = new Vector3();
        Vector3 p4 = new Vector3();
        Vector3 intersectionPoint = new Vector3();

        float minDst = Float.MAX_VALUE;
        Vector2 bestMatch = null;
        
        int minX = Math.max(0, startX - radius);
        int maxX = Math.min(map.getWidth() - 2, startX + radius);
        int minZ = Math.max(0, startZ - radius);
        int maxZ = Math.min(map.getDepth() - 2, startZ + radius);

        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                Tile tile = map.getTile(x, z);
                if (tile == null) continue;
                
                float h1 = tile.getHeight() * IsoUtils.HEIGHT_SCALE;
                float h2 = map.getTile(x+1, z).getHeight() * IsoUtils.HEIGHT_SCALE;
                float h3 = map.getTile(x+1, z+1).getHeight() * IsoUtils.HEIGHT_SCALE;
                float h4 = map.getTile(x, z+1).getHeight() * IsoUtils.HEIGHT_SCALE;
                
                // T1: p1, p2, p3
                p1.set(x, h1, z);
                p2.set(x+1, h2, z);
                p3.set(x+1, h3, z+1);
                
                if (Intersector.intersectRayTriangle(ray, p1, p2, p3, intersectionPoint)) {
                     float dst = intersectionPoint.dst2(ray.origin);
                     if (dst < minDst) {
                         minDst = dst;
                         bestMatch = new Vector2(x, z);
                     }
                }
                
                // T2: p3, p4, p1
                p4.set(x, h4, z+1);
                if (Intersector.intersectRayTriangle(ray, p3, p4, p1, intersectionPoint)) {
                     float dst = intersectionPoint.dst2(ray.origin);
                     if (dst < minDst) {
                         minDst = dst;
                         bestMatch = new Vector2(x, z);
                     }
                }
            }
        }
        
        return bestMatch;
    }
}