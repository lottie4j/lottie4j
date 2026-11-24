package com.lottie4j.fxplayer.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderStatsTest {

    @Test
    void emptyStatsAreZero() {
        var stats = new RenderStats();
        assertEquals(0, stats.getFrameCount());
        assertEquals(0.0, stats.getAverageMillis());
        assertEquals(0.0, stats.getPercentileMillis(95));
        assertEquals(0.0, stats.getAchievableFps());
    }

    @Test
    void computesAverageMaxPercentilesAndAchievableFps() {
        var stats = new RenderStats();
        for (int i = 1; i <= 100; i++) {
            stats.record(i);
        }
        assertEquals(50.5, stats.getAverageMillis(), 1e-9);
        assertEquals(100.0, stats.getMaxMillis());
        assertEquals(50.0, stats.getPercentileMillis(50));
        assertEquals(95.0, stats.getPercentileMillis(95));
        assertEquals(1000.0 / 50.5, stats.getAchievableFps(), 1e-9);
    }

    @Test
    void keepsOnlyTheMostRecentFrames() {
        var stats = new RenderStats(3);
        for (int i = 1; i <= 10; i++) {
            stats.record(i);
        }
        assertEquals(3, stats.getFrameCount());
        assertEquals(9.0, stats.getAverageMillis(), 1e-9);
        stats.reset();
        assertEquals(0, stats.getFrameCount());
    }

    @Test
    void profilerCollectsOnlyWhenEnabled() {
        RenderProfiler.setEnabled(false);
        RenderProfiler.stop("x", RenderProfiler.start());
        RenderProfiler.setEnabled(true);
        RenderProfiler.stop("x", RenderProfiler.start());
        RenderProfiler.stop("x", RenderProfiler.start());
        var timing = RenderProfiler.snapshot().get("x");
        RenderProfiler.setEnabled(false);
        assertEquals(2, timing.calls());
    }
}
