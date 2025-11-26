package com.project.tycoon.world;

import com.project.tycoon.world.model.TerrainType;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.WorldMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldMapTest {

    @Test
    void testMapInitialization() {
        WorldMap map = new WorldMap(10, 20);
        assertEquals(10, map.getWidth());
        assertEquals(20, map.getDepth());
        
        // Check default initialization
        Tile t = map.getTile(0, 0);
        assertNotNull(t);
        assertEquals(TerrainType.GRASS, t.getType());
        assertEquals(0, t.getHeight());
    }

    @Test
    void testBoundsChecking() {
        WorldMap map = new WorldMap(5, 5);
        
        assertTrue(map.isValid(0, 0));
        assertTrue(map.isValid(4, 4));
        assertFalse(map.isValid(-1, 0));
        assertFalse(map.isValid(0, -1));
        assertFalse(map.isValid(5, 0));
        assertFalse(map.isValid(0, 5));
        
        assertNull(map.getTile(-1, 0));
        assertNull(map.getTile(5, 5));
    }

    @Test
    void testTileModification() {
        WorldMap map = new WorldMap(10, 10);
        
        Tile t = map.getTile(5, 5);
        t.setHeight(3);
        t.setType(TerrainType.SNOW);
        
        Tile t2 = map.getTile(5, 5);
        assertEquals(3, t2.getHeight());
        assertEquals(TerrainType.SNOW, t2.getType());
    }
}

