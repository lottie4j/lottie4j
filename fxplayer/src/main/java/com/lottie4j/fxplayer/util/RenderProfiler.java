package com.lottie4j.fxplayer.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Opt-in timing of the stages of a render, to see where the time of a frame goes.
 * <p>
 * Disabled by default: {@link #start()} then returns 0 and {@link #stop(String, long)} does nothing,
 * so the instrumented code costs a volatile read. Stages are <em>inclusive</em> and may nest, for
 * example the time of an off-screen buffer is also part of the layer that needed it, so the stages
 * do not add up to the frame time.
 * <p>
 * Intended to be switched on from a benchmark or a debug session on the JavaFX application thread.
 */
public final class RenderProfiler {

    private static volatile boolean enabled = false;
    private static final Map<String, long[]> STAGES = new TreeMap<>();

    private RenderProfiler() {
    }

    /**
     * Switches stage timing on or off. Switching on clears what was collected before.
     *
     * @param value {@code true} to collect timings
     */
    public static void setEnabled(boolean value) {
        if (value) {
            reset();
        }
        enabled = value;
    }

    /**
     * Returns whether stage timing is on.
     *
     * @return {@code true} when timings are collected
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Starts timing a stage.
     *
     * @return a token for {@link #stop(String, long)}, 0 when profiling is off
     */
    public static long start() {
        return enabled ? System.nanoTime() : 0L;
    }

    /**
     * Ends timing a stage and adds the elapsed time to it.
     *
     * @param stage name of the stage
     * @param token the value {@link #start()} returned
     */
    public static void stop(String stage, long token) {
        if (token == 0L) {
            return;
        }
        long elapsed = System.nanoTime() - token;
        synchronized (STAGES) {
            long[] totals = STAGES.computeIfAbsent(stage, key -> new long[2]);
            totals[0] += elapsed;
            totals[1]++;
        }
    }

    /**
     * Clears the collected timings.
     */
    public static void reset() {
        synchronized (STAGES) {
            STAGES.clear();
        }
    }

    /**
     * Returns the collected timings.
     *
     * @return per stage: total milliseconds and the number of times it ran, in stage name order
     */
    public static Map<String, StageTiming> snapshot() {
        Map<String, StageTiming> result = new LinkedHashMap<>();
        synchronized (STAGES) {
            STAGES.forEach((stage, totals) -> result.put(stage, new StageTiming(totals[0] / 1_000_000.0, totals[1])));
        }
        return result;
    }

    /**
     * The time spent in one stage.
     *
     * @param totalMillis total time of all runs of the stage, in milliseconds
     * @param calls       number of times the stage ran
     */
    public record StageTiming(double totalMillis, long calls) {
        /**
         * Returns the average time of one run of the stage.
         *
         * @return milliseconds per run
         */
        public double averageMillis() {
            return calls == 0 ? 0.0 : totalMillis / calls;
        }
    }
}
