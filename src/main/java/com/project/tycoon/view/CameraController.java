package com.project.tycoon.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;

/**
 * Handles camera input (Panning and Zooming).
 */
public class CameraController extends InputAdapter {

    private final OrthographicCamera camera;
    private final Vector3 lastDragPos = new Vector3();
    private boolean isDragging = false;

    // Zoom settings
    private static final float MIN_ZOOM = 0.1f;
    private static final float MAX_ZOOM = 4.0f;
    private static final float ZOOM_SPEED = 0.1f;

    public CameraController(OrthographicCamera camera) {
        this.camera = camera;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Use Right Mouse Button for panning (Standard for RTS/Tycoon)
        if (button == Input.Buttons.RIGHT) {
            isDragging = true;
            // Project screen coordinates to world coordinates for consistent dragging
            lastDragPos.set(camera.unproject(new Vector3(screenX, screenY, 0)));
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.RIGHT) {
            isDragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (isDragging) {
            // Get current world position under mouse
            Vector3 curr = camera.unproject(new Vector3(screenX, screenY, 0));
            
            // Calculate delta: lastPos - currPos
            // We move the camera by this delta to keep the point under mouse "fixed"
            float deltaX = lastDragPos.x - curr.x;
            float deltaY = lastDragPos.y - curr.y;

            camera.translate(deltaX, deltaY);
            camera.update();

            // Update last position to current (which is now under the mouse after translation)
            // We need to re-unproject because the camera moved
            lastDragPos.set(camera.unproject(new Vector3(screenX, screenY, 0)));
            return true;
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        // Zoom In (amountY < 0) -> decrease zoom value (smaller view area = closer look?)
        // LibGDX OrthographicCamera zoom: 1.0 = 100%, 2.0 = 200% view area (zoomed OUT), 0.5 = zoomed IN.
        
        float zoomChange = amountY * ZOOM_SPEED * camera.zoom;
        camera.zoom += zoomChange;

        // Clamp zoom
        if (camera.zoom < MIN_ZOOM) camera.zoom = MIN_ZOOM;
        if (camera.zoom > MAX_ZOOM) camera.zoom = MAX_ZOOM;

        camera.update();
        return true;
    }
    
    /**
     * Optional: Keyboard movement (WASD)
     */
    public void update() {
        float speed = 500f * Gdx.graphics.getDeltaTime() * camera.zoom;
        
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            camera.translate(0, speed);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            camera.translate(0, -speed);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.translate(-speed, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            camera.translate(speed, 0);
        }
        
        if (Gdx.input.isKeyPressed(Input.Keys.W) || 
            Gdx.input.isKeyPressed(Input.Keys.S) ||
            Gdx.input.isKeyPressed(Input.Keys.A) ||
            Gdx.input.isKeyPressed(Input.Keys.D)) {
            camera.update();
        }
    }
}

