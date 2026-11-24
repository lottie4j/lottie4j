package com.lottie4j.fxplayer.util;

import java.util.Arrays;

/**
 * Keeps the render time of the most recent frames, to tell how fast a player can render
 * instead of how fast the display asked for frames.
 * <p>
 * The frame rate a running {@code AnimationTimer} delivers is capped by the display refresh rate,
 * so it reads 60 for every animation that renders within 16.6 ms and says nothing about the
 * headroom. The render time per frame does, and {@link #getAchievableFps()} is derived from it.
 * <p>
 * Not thread-safe: record and read on the JavaFX application thread.
 */
public final class RenderStats {

    /**
     * The number of frames kept.
     */
    public static final int DEFAULT_CAPACITY = 600;

    private final double[] renderMillis;
    private int count = 0;
    private int next = 0;

    /**
     * Creates statistics over the last {@link #DEFAULT_CAPACITY} frames.
     */
    public RenderStats() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates statistics over the last {@code capacity} frames.
     *
     * @param capacity number of frames to keep, at least 1
     */
    public RenderStats(int capacity) {
        this.renderMillis = new double[Math.max(1, capacity)];
    }

    /**
     * Records the render time of one frame.
     *
     * @param millis time spent rendering the frame, in milliseconds
     */
    public void record(double millis) {
        renderMillis[next] = millis;
        next = (next + 1) % renderMillis.length;
        if (count < renderMillis.length) {
            count++;
        }
    }

    /**
     * Forgets all recorded frames.
     */
    public void reset() {
        count = 0;
        next = 0;
    }

    /**
     * Returns the number of frames currently kept.
     *
     * @return number of frames, at most the capacity
     */
    public int getFrameCount() {
        return count;
    }

    /**
     * Returns the average render time.
     *
     * @return average in milliseconds, 0 when nothing is recorded
     */
    public double getAverageMillis() {
        if (count == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (int i = 0; i < count; i++) {
            sum += renderMillis[i];
        }
        return sum / count;
    }

    /**
     * Returns the slowest frame.
     *
     * @return maximum in milliseconds, 0 when nothing is recorded
     */
    public double getMaxMillis() {
        double max = 0.0;
        for (int i = 0; i < count; i++) {
            max = Math.max(max, renderMillis[i]);
        }
        return max;
    }

    /**
     * Returns a percentile of the render time (nearest rank).
     *
     * @param percentile between 0 and 100
     * @return the render time in milliseconds that the given share of the frames stayed within,
     * 0 when nothing is recorded
     */
    public double getPercentileMillis(double percentile) {
        if (count == 0) {
            return 0.0;
        }
        double[] sorted = Arrays.copyOf(renderMillis, count);
        Arrays.sort(sorted);
        int rank = (int) Math.ceil(Math.max(0.0, Math.min(100.0, percentile)) / 100.0 * count);
        return sorted[Math.max(0, rank - 1)];
    }

    /**
     * Returns the frame rate the player could reach if the display did not limit it.
     *
     * @return {@code 1000 / average render milliseconds}, 0 when nothing is recorded
     */
    public double getAchievableFps() {
        double average = getAverageMillis();
        return average > 0.0 ? 1000.0 / average : 0.0;
    }
}
