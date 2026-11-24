package com.lottie4j.fxfileviewer;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxplayer.LottiePlayer;
import com.lottie4j.fxplayer.util.RenderProfiler;
import com.lottie4j.fxplayer.util.RenderStats;
import javafx.application.Platform;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Measures how fast {@link LottiePlayer} renders an animation, without the display refresh limit of
 * a running {@code AnimationTimer}: frames are rendered back to back.
 *
 * <p>Opt-in, skipped unless {@code -Dlottie.bench=true}:</p>
 * <pre>
 *   mvn -pl fxfileviewer test -Pheadless-tests -Dtest=PlaybackBenchmarkTest -Dlottie.bench=true \
 *       -Dlottie.file=dot/winter-town.lottie
 * </pre>
 * <p>Optional properties: {@code lottie.bench.sizes} (default {@code 600x400,1200x800}; the
 * animation size is used for {@code native}), {@code lottie.bench.warmup} (default 200 frames),
 * {@code lottie.bench.frames} (number of frames measured, spread evenly over the animation, default 120), {@code lottie.bench.csv} (path to append result rows to).</p>
 *
 * <p>Two numbers are reported per run. <em>record</em> is the time of {@code render(frame)}: it
 * draws into the canvas command buffer, which JavaFX only rasterizes in a scene at a pulse, so it
 * is a lower bound. <em>raster</em> is {@code snapshotFrame(frame)}: it also rasterizes and reads
 * the pixels back, a slight overestimate. The truth for a window is the viewer run
 * ({@code --bench}), see {@code BENCHMARK-LOG.md}.</p>
 */
class PlaybackBenchmarkTest {

    private static final String DEFAULT_FILE = "dot/winter-town.lottie";

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        var latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            return;
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS), "JavaFX Platform failed to start");
    }

    @Test
    void benchmarkPlayback() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("lottie.bench"), "set -Dlottie.bench=true to run the benchmark");

        String fileName = System.getProperty("lottie.file", "");
        if (fileName.isBlank()) {
            fileName = DEFAULT_FILE;
        }
        var resource = PlaybackBenchmarkTest.class.getResource("/" + fileName);
        assertNotNull(resource, "Test animation not found: " + fileName);
        Animation animation = LottieFileLoader.load(new File(resource.toURI()));

        int warmup = Integer.getInteger("lottie.bench.warmup", 200);
        int first = (int) Math.round(animation.inPoint());
        int totalFrames = (int) Math.round(animation.outPoint()) - first;
        // evenly spaced over the whole animation: the first frames are rarely representative
        int frames = Math.min(totalFrames, Integer.getInteger("lottie.bench.frames", 120));

        var report = new ArrayList<String>();
        for (String size : System.getProperty("lottie.bench.sizes", "600x400,1200x800").split(",")) {
            int width = animation.width() != null ? animation.width() : 500;
            int height = animation.height() != null ? animation.height() : 500;
            if (!size.trim().equals("native")) {
                String[] parts = size.trim().split("x");
                width = Integer.parseInt(parts[0]);
                height = Integer.parseInt(parts[1]);
            }
            for (boolean adaptive : new boolean[]{false, true}) {
                report.addAll(runOne(fileName, animation, width, height, adaptive, first, totalFrames, frames, warmup));
            }
        }

        report.forEach(System.out::println);
        String csv = System.getProperty("lottie.bench.csv", "");
        if (!csv.isBlank()) {
            appendCsv(Path.of(csv), report);
        }
    }

    private List<String> runOne(String fileName, Animation animation, int width, int height, boolean adaptive,
                                int first, int totalFrames, int frames, int warmup) throws Exception {
        var result = new AtomicReference<List<String>>();
        var failure = new AtomicReference<Throwable>();
        var done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                var player = new LottiePlayer(animation, width, height);
                player.setAdaptiveOffscreenScalingEnabled(adaptive);

                for (int i = 0; i < warmup; i++) {
                    player.render(first + (int) ((long) (i % frames) * totalFrames / frames));
                }

                player.getRenderStats().reset();
                RenderProfiler.setEnabled(true);
                var raster = new RenderStats(frames);
                for (int i = 0; i < frames; i++) {
                    player.render(first + (int) ((long) i * totalFrames / frames));
                }
                RenderStats record = player.getRenderStats();
                var recordLine = line(fileName, width, height, adaptive, "record", record, frames);
                var stages = RenderProfiler.snapshot();
                RenderProfiler.setEnabled(false);

                for (int i = 0; i < frames; i++) {
                    long start = System.nanoTime();
                    player.snapshotFrame(first + (int) ((long) i * totalFrames / frames));
                    raster.record((System.nanoTime() - start) / 1_000_000.0);
                }
                var lines = new ArrayList<String>();
                lines.add(recordLine);
                lines.add(line(fileName, width, height, adaptive, "raster", raster, frames));
                stages.forEach((stage, t) -> lines.add(String.format(
                        "BENCH-STAGE,%s,%dx%d,adaptive=%s,%s,share_ms_per_frame=%.3f,calls_per_frame=%.2f,avg_ms=%.4f",
                        fileName, width, height, adaptive, stage,
                        t.totalMillis() / frames, (double) t.calls() / frames, t.averageMillis())));
                result.set(lines);
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(15, TimeUnit.MINUTES), "benchmark timed out");
        if (failure.get() != null) {
            throw new AssertionError("benchmark failed", failure.get());
        }
        return result.get();
    }

    private static String line(String file, int width, int height, boolean adaptive, String mode, RenderStats s, int frames) {
        return String.format("BENCH,%s,%dx%d,adaptive=%s,%s,frames=%d,avg_ms=%.2f,p50_ms=%.2f,p95_ms=%.2f,p99_ms=%.2f,max_ms=%.2f,achievable_fps=%.1f",
                file, width, height, adaptive, mode, frames, s.getAverageMillis(), s.getPercentileMillis(50),
                s.getPercentileMillis(95), s.getPercentileMillis(99), s.getMaxMillis(), s.getAchievableFps());
    }

    private static void appendCsv(Path path, List<String> lines) throws IOException {
        if (path.toAbsolutePath().getParent() != null) {
            Files.createDirectories(path.toAbsolutePath().getParent());
        }
        Files.write(path, lines, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }
}
