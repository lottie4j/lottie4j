package com.lottie4j.fxfileviewer;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The renderers cache off-screen buffers between frames. A player that played the previous frames
 * must render a frame exactly like a new player that seeks straight to it.
 *
 * <p>Regression test for <a href="https://github.com/lottie4j/lottie4j/issues/12">issue #12</a>:
 * {@code json/collision.json} renders mattes of different sizes within the same frame, and a reused
 * snapshot image of the wrong size cropped the matte and stretched it into horizontal streaks.</p>
 */
class SequentialPlaybackTest {

    private static final int SIZE = 72;

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            // Platform already started by another test class — fine.
            return;
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform failed to start");
    }

    @Test
    void sequentialPlaybackMatchesFreshSeek() throws Exception {
        var resource = SequentialPlaybackTest.class.getResource("/json/collision.json");
        assertNotNull(resource, "Test animation not found");
        Animation animation = LottieFileLoader.load(new File(resource.getFile()));

        runOnFxThread(() -> {
            LottiePlayer sequential = newPlayer(animation);
            List<String> differingFrames = new ArrayList<>();
            for (int frame = animation.inPoint(); frame < animation.outPoint(); frame++) {
                sequential.seekToFrame(frame);
                LottiePlayer fresh = newPlayer(animation);
                fresh.seekToFrame(frame);

                int differing = countDifferingPixels(pixels(sequential), pixels(fresh));
                if (differing > 0) {
                    differingFrames.add("frame " + frame + ": " + differing + " pixels");
                }
            }
            assertTrue(differingFrames.isEmpty(),
                    "Sequential playback differs from a fresh seek on " + differingFrames);
        });
    }

    /**
     * Adaptive scaling depends on how long the previous frames took to render, so it would differ
     * between the sequential and the fresh player independent of the cached buffers.
     */
    private static LottiePlayer newPlayer(Animation animation) {
        LottiePlayer player = new LottiePlayer(animation, SIZE, SIZE);
        player.setAdaptiveOffscreenScalingEnabled(false);
        return player;
    }

    private static int[] pixels(LottiePlayer player) {
        WritableImage image = player.snapshot(new SnapshotParameters(), null);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int[] pixels = new int[width * height];
        image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        return pixels;
    }

    private static int countDifferingPixels(int[] a, int[] b) {
        int differing = Math.abs(a.length - b.length);
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            if (a[i] != b[i]) {
                differing++;
            }
        }
        return differing;
    }

    private static void runOnFxThread(Runnable test) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                test.run();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(60, TimeUnit.SECONDS), "Test timed out on the JavaFX thread");
        if (failure.get() instanceof AssertionError assertionError) {
            throw assertionError;
        } else if (failure.get() != null) {
            throw new Exception(failure.get());
        }
    }
}
