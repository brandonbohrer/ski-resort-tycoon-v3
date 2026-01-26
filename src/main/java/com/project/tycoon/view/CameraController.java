package com.project.tycoon.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.project.tycoon.ecs.Entity;
import com.project.tycoon.ecs.Engine;
import com.project.tycoon.ecs.components.TransformComponent;

/**
 * Handles camera input (Panning and Zooming) for 3D Isometric view.
 */
public class CameraController extends InputAdapter {

    private final OrthographicCamera camera;
    private final Engine engine;
    private int lastScreenX, lastScreenY;
    private boolean isDragging = false;
    
    // Camera follow state
    private Entity followTarget = null;
    private boolean isFollowing = false;

    private static final float MIN_ZOOM = 0.02f; // Allow very close zoom
    private static final float MAX_ZOOM = 4.0f;
    private static final float ZOOM_SPEED = 0.1f;
    
    // Isometric Basis Vectors for Movement (Normalized)
    // Camera is at (+, +, +) looking at (0,0,0)
    // Forward on ground (Up on screen) -> (-1, 0, -1)
    // Right on ground (Right on screen) -> (1, 0, -1)
    private final Vector3 forward = new Vector3(-1, 0, -1).nor();
    private final Vector3 right = new Vector3(1, 0, -1).nor();
    private final Vector3 temp = new Vector3();

    public CameraController(OrthographicCamera camera, Engine engine) {
        this.camera = camera;
        this.engine = engine;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.RIGHT) {
            isDragging = true;
            lastScreenX = screenX;
            lastScreenY = screenY;
            // Stop following when user drags
            stopFollowing();
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
            float deltaX = screenX - lastScreenX;
            float deltaY = screenY - lastScreenY; // Down is positive
            
            // Speed Factor (adjust based on zoom so panning feels 1:1)
            float speed = 1.5f * camera.zoom; 
            
            // Move Right vector by -deltaX
            temp.set(right).scl(-deltaX * speed);
            camera.position.add(temp);
            
            // Move Forward vector by +deltaY (Drag Down -> Move Backwards/South-East)
            temp.set(forward).scl(deltaY * speed);
            camera.position.add(temp);
            
            camera.update();
            
            lastScreenX = screenX;
            lastScreenY = screenY;
            return true;
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float zoomChange = amountY * ZOOM_SPEED * camera.zoom;
        camera.zoom += zoomChange;

        if (camera.zoom < MIN_ZOOM) camera.zoom = MIN_ZOOM;
        if (camera.zoom > MAX_ZOOM) camera.zoom = MAX_ZOOM;

        camera.update();
        return true;
    }
    
    public void update() {
        // If following an entity, update camera position
        if (isFollowing && followTarget != null) {
            TransformComponent transform = engine.getComponent(followTarget, TransformComponent.class);
            if (transform != null) {
                // Smoothly move camera to target
                float targetX = transform.x + 100f; // Offset for isometric view
                float targetZ = transform.z + 100f;
                
                // Lerp for smooth following
                float lerpFactor = 5.0f * Gdx.graphics.getDeltaTime();
                camera.position.x += (targetX - camera.position.x) * lerpFactor;
                camera.position.z += (targetZ - camera.position.z) * lerpFactor;
                camera.update();
            } else {
                // Entity no longer exists, stop following
                stopFollowing();
            }
        }
        
        // Keyboard movement (stops following)
        float dt = Gdx.graphics.getDeltaTime();
        float speed = 600f * dt * camera.zoom; // Fast default speed
        
        boolean moved = false;
        
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            // Move Forward
            temp.set(forward).scl(speed);
            camera.position.add(temp);
            moved = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            // Move Backward
            temp.set(forward).scl(-speed);
            camera.position.add(temp);
            moved = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            // Move Left
            temp.set(right).scl(-speed);
            camera.position.add(temp);
            moved = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            // Move Right
            temp.set(right).scl(speed);
            camera.position.add(temp);
            moved = true;
        }
        
        if (moved) {
            stopFollowing(); // Stop following if user moves manually
            camera.update();
        }
    }
    
    /**
     * Start following an entity (e.g., a skier).
     */
    public void followEntity(Entity entity) {
        this.followTarget = entity;
        this.isFollowing = true;
        System.out.println("Now following entity: " + entity.getId());
    }
    
    /**
     * Stop following the current entity.
     */
    public void stopFollowing() {
        if (isFollowing) {
            System.out.println("Stopped following entity");
        }
        this.isFollowing = false;
        this.followTarget = null;
    }
    
    /**
     * Check if camera is currently following an entity.
     */
    public boolean isFollowing() {
        return isFollowing;
    }
}