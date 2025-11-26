package com.project.tycoon.core;

import com.project.tycoon.simulation.Simulation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameLoopTest {

    @Mock
    Simulation simulation;

    @Test
    void testGameLoopTicks() throws InterruptedException {
        // Setup: 60 ticks per second
        double tickRate = 60.0;
        GameLoop loop = new FixedStepGameLoop(simulation, tickRate);

        // Action: Run for approx 100ms (should result in ~6 ticks)
        loop.start();
        assertTrue(loop.isRunning(), "Loop should be running after start()");
        
        Thread.sleep(100); 
        
        loop.stop();
        assertFalse(loop.isRunning(), "Loop should not be running after stop()");

        // Assert: verify at least a few ticks happened
        // We don't check exact count due to thread scheduling jitter, but 100ms at 60Hz is 6 ticks.
        // We expect at least 3 to be safe.
        verify(simulation, atLeast(3)).tick(anyLong());
    }

    @Test
    void testStartStopIdempotency() {
        double tickRate = 60.0;
        GameLoop loop = new FixedStepGameLoop(simulation, tickRate);

        loop.start();
        loop.start(); // Should be safe
        assertTrue(loop.isRunning());

        loop.stop();
        loop.stop(); // Should be safe
        assertFalse(loop.isRunning());
    }
}

