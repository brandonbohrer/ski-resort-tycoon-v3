package com.project.tycoon.core;

/**
 * Defines the contract for the game's main execution loop.
 * It is responsible for driving the Simulation at a consistent rate.
 */
public interface GameLoop {
    /**
     * Starts the game loop. This may block the calling thread or run asynchronously
     * depending on implementation.
     */
    void start();

    /**
     * Signals the game loop to stop execution.
     */
    void stop();

    /**
     * Checks if the loop is currently running.
     * @return true if running, false otherwise.
     */
    boolean isRunning();
}

