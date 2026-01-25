package com.project.tycoon.world.model;

import java.util.Random;

/**
 * Perlin noise generator for natural-looking procedural terrain.
 * Based on Ken Perlin's improved noise algorithm (2002).
 */
public class PerlinNoise {

    private final int[] permutation;

    public PerlinNoise(long seed) {
        permutation = new int[512];
        Random random = new Random(seed);

        // Create permutation array based on seed
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }

        // Fisher-Yates shuffle
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = p[i];
            p[i] = p[j];
            p[j] = temp;
        }

        // Duplicate for wrapping
        for (int i = 0; i < 512; i++) {
            permutation[i] = p[i % 256];
        }
    }

    /**
     * Get 2D Perlin noise value at (x, y).
     * 
     * @return Value in range approximately [-1, 1]
     */
    public double noise(double x, double y) {
        // Find unit square containing point
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;

        // Find relative x,y in square
        x -= Math.floor(x);
        y -= Math.floor(y);

        // Compute fade curves
        double u = fade(x);
        double v = fade(y);

        // Hash coordinates of 4 square corners
        int a = permutation[X] + Y;
        int aa = permutation[a];
        int ab = permutation[a + 1];
        int b = permutation[X + 1] + Y;
        int ba = permutation[b];
        int bb = permutation[b + 1];

        // Blend results from 4 corners
        double x1 = lerp(u, grad(permutation[aa], x, y), grad(permutation[ba], x - 1, y));
        double x2 = lerp(u, grad(permutation[ab], x, y - 1), grad(permutation[bb], x - 1, y - 1));

        return lerp(v, x1, x2);
    }

    /**
     * Multi-octave Perlin noise for more natural-looking terrain.
     * 
     * @param x           X coordinate
     * @param y           Y coordinate
     * @param octaves     Number of noise layers (more = more detail)
     * @param persistence How much each octave contributes (0.0 - 1.0, typically
     *                    0.5)
     * @return Normalized noise value in range [-1, 1]
     */
    public double octaveNoise(double x, double y, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }

        return total / maxValue;
    }

    private double fade(double t) {
        // Fade function: 6t^5 - 15t^4 + 10t^3
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private double grad(int hash, double x, double y) {
        // Convert low 4 bits of hash into gradient direction
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : 0;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
