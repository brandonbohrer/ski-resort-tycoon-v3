package com.project.tycoon.view;

import com.badlogic.gdx.math.Vector2;
import com.project.tycoon.view.util.IsoUtils;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MousePickerTest {

    @Mock
    WorldMap map;

    @Test
    void testPickFlatTile() {
        when(map.getWidth()).thenReturn(100);
        when(map.getDepth()).thenReturn(100);
        // Setup Tile at 10, 10 with Height 0
        when(map.getTile(10, 10)).thenReturn(createTile(0));
        
        MousePicker picker = new MousePicker(map);
        
        // Calculate expected center of 10,10
        Vector2 worldPos = IsoUtils.gridToWorld(10, 10);
        float centerX = worldPos.x + IsoUtils.TILE_WIDTH/2f;
        float centerY = worldPos.y + IsoUtils.TILE_HEIGHT/2f; // H=0
        
        Vector2 result = picker.pickTile(centerX, centerY);
        
        assertNotNull(result);
        assertEquals(10, (int)result.x);
        assertEquals(10, (int)result.y); // Vector2.y maps to Grid Z
    }

    @Test
    void testPickElevatedTile() {
        when(map.getWidth()).thenReturn(100);
        when(map.getDepth()).thenReturn(100);
        
        // Target Tile: 10,10 at Height 5
        // This shifts visual Y up by 5 * 8 = 40px
        when(map.getTile(10, 10)).thenReturn(createTile(5));
        
        MousePicker picker = new MousePicker(map);
        
        Vector2 worldPos = IsoUtils.gridToWorld(10, 10);
        float centerX = worldPos.x + IsoUtils.TILE_WIDTH/2f;
        float centerY = worldPos.y + IsoUtils.TILE_HEIGHT/2f + (5 * IsoUtils.HEIGHT_SCALE);
        
        Vector2 result = picker.pickTile(centerX, centerY);
        
        assertNotNull(result, "Should hit elevated tile");
        assertEquals(10, (int)result.x);
        assertEquals(10, (int)result.y);
    }

    @Test
    void testOcclusion() {
        when(map.getWidth()).thenReturn(100);
        when(map.getDepth()).thenReturn(100);
        
        // Scenario:
        // Tile A (10, 10) Height 0 (Behind/Below)
        // Tile B (11, 11) Height 10 (Front/High - Occluding A)
        // In Isometric, (11,11) is "lower" on screen (higher Y index) so drawn LATER.
        
        when(map.getTile(10, 10)).thenReturn(createTile(0));
        when(map.getTile(11, 11)).thenReturn(createTile(10));
        
        MousePicker picker = new MousePicker(map);
        
        // Click where B visually is
        Vector2 posB = IsoUtils.gridToWorld(11, 11);
        float cx = posB.x + IsoUtils.TILE_WIDTH/2f;
        float cy = posB.y + IsoUtils.TILE_HEIGHT/2f + (10 * IsoUtils.HEIGHT_SCALE);
        
        Vector2 result = picker.pickTile(cx, cy);
        
        assertEquals(11, (int)result.x);
        assertEquals(11, (int)result.y);
    }
    
    // Helper to create mock tile
    private Tile createTile(int height) {
        Tile t = new Tile(null, height); 
        return t;
    }
}

