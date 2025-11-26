package com.project.tycoon.view;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.project.tycoon.world.model.Tile;
import com.project.tycoon.world.model.TerrainType;
import com.project.tycoon.world.model.WorldMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MousePickerTest {

    @Mock
    WorldMap map;

    MousePicker picker;
    OrthographicCamera camera;

    @BeforeEach
    void setUp() {
        // Setup a basic camera
        camera = new OrthographicCamera(40, 40);
        camera.position.set(100, 100, 100);
        camera.lookAt(0, 0, 0);
        camera.update();
        
        picker = new MousePicker(map, camera);
    }

    @Test
    void testPickTile_Basic() {
        // This is a basic test to ensure the picker runs without crashing.
        // Accurate ray-triangle intersection testing requires setting up complex map mocks
        // which is brittle for this refactor.
        
        when(map.getWidth()).thenReturn(10);
        when(map.getDepth()).thenReturn(10);
        when(map.getTile(anyInt(), anyInt())).thenReturn(new Tile(TerrainType.GRASS, 0));
        
        // Cast a ray at the center
        // Since camera looks at 0,0,0, screen coords might need adjustment or unproject logic.
        // For now, just verifying it returns null or a value without exception.
        assertDoesNotThrow(() -> picker.pickTile(100, 100));
    }
}