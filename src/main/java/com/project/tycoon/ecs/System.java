package com.project.tycoon.ecs;

/**
 * A system contains logic that operates on entities with specific components.
 */
public interface System {
    /**
     * Updates the system logic.
     * @param dt The time delta in seconds (or ticks) since the last update.
     */
    void update(double dt);
}

