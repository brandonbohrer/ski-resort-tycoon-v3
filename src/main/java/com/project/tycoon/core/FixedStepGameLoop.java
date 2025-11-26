package com.project.tycoon.core;

import com.project.tycoon.simulation.Simulation;

/**
 * A deterministic, fixed-timestep game loop.
 * It ensures the Simulation.tick() method is called at a constant rate,
 * regardless of rendering performance or system load.
 */
public class FixedStepGameLoop implements GameLoop {

    private final Simulation simulation;
    private final double targetTicksPerSecond;
    private volatile boolean running = false;
    private long tickCount = 0;

    // For controlling the loop thread
    private Thread loopThread;

    public FixedStepGameLoop(Simulation simulation, double targetTicksPerSecond) {
        if (targetTicksPerSecond <= 0) {
            throw new IllegalArgumentException("Target ticks per second must be positive.");
        }
        this.simulation = simulation;
        this.targetTicksPerSecond = targetTicksPerSecond;
    }

    @Override
    public synchronized void start() {
        if (running) return;
        running = true;
        loopThread = new Thread(this::runLoop, "GameLoop-Thread");
        loopThread.start();
    }

    @Override
    public synchronized void stop() {
        running = false;
        try {
            if (loopThread != null) {
                loopThread.join(1000); // Wait for thread to finish cleanly
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void runLoop() {
        final double nsPerTick = 1_000_000_000.0 / targetTicksPerSecond;
        long lastTime = System.nanoTime();
        double accumulator = 0;

        while (running) {
            long now = System.nanoTime();
            // Handle potential large time jumps (e.g. system sleep)
            long frameTime = now - lastTime;
            if (frameTime > 250_000_000) { // Cap at 0.25 seconds to prevent spiral of death
                frameTime = 250_000_000;
            }
            lastTime = now;

            accumulator += frameTime;

            while (accumulator >= nsPerTick && running) {
                simulation.tick(tickCount++);
                accumulator -= nsPerTick;
            }

            // Sleep to yield CPU, preventing 100% usage
            long sleepTime = (long) ((nsPerTick - accumulator) / 1_000_000.0);
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    // Re-interrupt and exit loop if interrupted
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        }
    }
}

