package com.project.tycoon.simulation;

/**
 * Represents the core simulation that receives discrete updates.
 * This allows the GameLoop to drive the simulation without knowing its internal details.
 */
public interface Simulation {
    /**
     * Advance the simulation by one discrete step.
     * This should include all game logic updates (economy, movement, AI).
     *
     * @param tickNumber The current tick count since the start of the simulation.
     */
    void tick(long tickNumber);
}

