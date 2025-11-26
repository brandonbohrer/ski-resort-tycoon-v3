package com.project.tycoon;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.project.tycoon.simulation.TycoonSimulation;
import com.project.tycoon.view.TycoonGame;

/**
 * Desktop launcher class.
 */
public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Ski Resort Tycoon");
        config.setWindowedMode(1280, 720);
        config.setForegroundFPS(60);

        // Initialize the simulation core first
        TycoonSimulation simulation = new TycoonSimulation();
        
        // Launch the visual layer, injecting the simulation
        new Lwjgl3Application(new TycoonGame(simulation), config);
    }
}

