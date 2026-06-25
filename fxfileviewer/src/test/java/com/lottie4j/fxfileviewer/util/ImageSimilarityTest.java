package com.lottie4j.fxfileviewer.util;

import javafx.application.Platform;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageSimilarityTest {

    private static final int W = 32;
    private static final int H = 32;

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        AtomicBoolean started = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                started.set(true);
                latch.countDown();
            });
        } catch (IllegalStateException alreadyStarted) {
            // Platform already started by another test class — fine.
            return;
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform failed to start");
        assertTrue(started.get());
    }

    @Test
    void identicalImagesScore100() {
        WritableImage a = solid(0xFF8040C0);
        WritableImage b = solid(0xFF8040C0);
        ImageSimilarity.SimilarityResult r = ImageSimilarity.compare(a, b);
        assertEquals(100.0, r.overall(), 0.0001);
        assertEquals(100.0, r.red(), 0.0001);
        assertEquals(100.0, r.green(), 0.0001);
        assertEquals(100.0, r.blue(), 0.0001);
    }

    @Test
    void allRedVsAllBlueIsClearlyDifferent() {
        WritableImage red = solid(0xFFFF0000);
        WritableImage blue = solid(0xFF0000FF);
        ImageSimilarity.SimilarityResult r = ImageSimilarity.compare(red, blue);
        // A grayscale-only SSIM would score these nearly identical because both have low luminance
        // structure. Per-channel SSIM must clearly distinguish them.
        assertTrue(r.overall() < 80.0,
                "expected clear difference between solid red and solid blue, got " + r.overall());
        // Green channel matches (both 0) so it scores high; R and B do not.
        assertTrue(r.green() > 95.0, "green channel should match: " + r.green());
    }

    @Test
    void transparentPixelsCompositedOverWhiteAreEqual() {
        // a: solid white, fully opaque
        WritableImage a = solid(0xFFFFFFFF);
        // b: fully transparent (alpha=0). After compositing over white background, this also reads
        // as white, so similarity must be 100.
        WritableImage b = solid(0x00000000);
        ImageSimilarity.SimilarityResult r = ImageSimilarity.compare(a, b);
        assertEquals(100.0, r.overall(), 0.0001,
                "alpha=0 pixels must be composited over white before comparison");
    }

    @Test
    void compositeOverWhitePreservesOpaqueColor() {
        int opaqueRed = 0xFFFF0000;
        assertEquals(opaqueRed, ImageSimilarity.compositeOverWhite(opaqueRed));
    }

    @Test
    void compositeOverWhiteTurnsTransparentIntoWhite() {
        int fullyTransparent = 0x00123456;
        assertEquals(0xFFFFFFFF, ImageSimilarity.compositeOverWhite(fullyTransparent));
    }

    @Test
    void compositeOverWhiteHalfAlpha() {
        // 50% alpha red over white -> roughly (255, 127, 127)
        int halfRed = 0x80FF0000;
        int composited = ImageSimilarity.compositeOverWhite(halfRed);
        int r = (composited >> 16) & 0xFF;
        int g = (composited >> 8) & 0xFF;
        int b = composited & 0xFF;
        int a = (composited >>> 24) & 0xFF;
        assertEquals(0xFF, a, "alpha must be opaque after compositing");
        assertEquals(0xFF, r, "red channel preserved when blending red onto white: " + r);
        assertTrue(g >= 125 && g <= 130, "expected ~127 green, got " + g);
        assertTrue(b >= 125 && b <= 130, "expected ~127 blue, got " + b);
    }

    private static WritableImage solid(int argb) {
        WritableImage img = new WritableImage(W, H);
        PixelWriter pw = img.getPixelWriter();
        int[] buf = new int[W * H];
        for (int i = 0; i < buf.length; i++) buf[i] = argb;
        pw.setPixels(0, 0, W, H, PixelFormat.getIntArgbInstance(), buf, 0, W);
        return img;
    }
}
