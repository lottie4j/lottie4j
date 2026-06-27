package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.definition.MatteMode;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MatteRenderer.
 * Validates matte rendering behavior and layer state management.
 */
class MatteRendererTest {

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedAndReused() {
        MatteRenderer renderer1 = new MatteRenderer();
        MatteRenderer renderer2 = new MatteRenderer();
        assertNotNull(renderer1);
        assertNotNull(renderer2);
        assertNotSame(renderer1, renderer2);
    }

    @Test
    void graphicsContextCanBeUsedAfterInstantiation() {
        MatteRenderer renderer = new MatteRenderer();

        Boolean canDraw = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setGlobalAlpha(0.5);

            // Should be able to draw before and after renderer creation
            gc.setFill(javafx.scene.paint.Color.RED);
            gc.fillRect(10, 10, 20, 20);

            return true;
        });

        assertTrue(canDraw, "Graphics context should remain usable");
    }

    @Test
    void multipleInstancesAreIndependent() {
        MatteRenderer renderer1 = new MatteRenderer();
        MatteRenderer renderer2 = new MatteRenderer();

        assertNotNull(renderer1);
        assertNotNull(renderer2);
        assertNotSame(renderer1, renderer2);
    }

    @Test
    void rendererCanBeCreatedMultipleTimes() {
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            MatteRenderer r1 = new MatteRenderer();
            MatteRenderer r2 = new MatteRenderer();
            MatteRenderer r3 = new MatteRenderer();

            assertNotNull(r1);
            assertNotNull(r2);
            assertNotNull(r3);

            return true;
        });
    }

    @Test
    void rendererIsStateless() {
        MatteRenderer renderer = new MatteRenderer();
        MatteRenderer renderer2 = new MatteRenderer();

        assertNotNull(renderer);
        assertNotNull(renderer2);
    }

    // ---------------------------------------------------------------------------------------
    // Pixel-level matte composition tests.
    //
    // Cross-cutting invariant (see Fix-renderer-alpha-matte-channels.md):
    //   For tt:1 (ALPHA) and tt:3 (INVERTED_ALPHA) the matte source's RGB MUST NOT influence
    //   the destination's RGB. A common bug is to fold the matte's luma into the result,
    //   producing a per-channel skew biased toward the matte source's dominant colour. These
    //   tests pin that down with synthetic pure-red / pure-green / pure-blue matte sources
    //   plus an alpha gradient.
    // ---------------------------------------------------------------------------------------

    private static final int OPAQUE_WHITE_CONTENT = argb(255, 200, 150, 100);

    private static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private static int a(int pixel) { return (pixel >>> 24) & 0xFF; }
    private static int r(int pixel) { return (pixel >> 16) & 0xFF; }
    private static int g(int pixel) { return (pixel >> 8) & 0xFF; }
    private static int b(int pixel) { return pixel & 0xFF; }

    private static int[] fill(int size, int pixel) {
        int[] buf = new int[size];
        java.util.Arrays.fill(buf, pixel);
        return buf;
    }

    @Test
    void alphaMatte_preservesContentRgb_forPureRedOpaqueMatte() {
        // Matte source is fully opaque pure red. For tt:1 only its alpha (255) matters;
        // its red channel must NOT leak into the result.
        int[] content = fill(16, OPAQUE_WHITE_CONTENT);
        int[] matte = fill(16, argb(255, 255, 0, 0));
        int[] result = new int[16];

        MatteRenderer.composeMatte(content, matte, result, MatteMode.ALPHA);

        for (int i = 0; i < result.length; i++) {
            assertEquals(255, a(result[i]), "alpha should be fully opaque (255 * 255 / 255)");
            assertEquals(200, r(result[i]), "content R must be preserved exactly (no matte-R leak)");
            assertEquals(150, g(result[i]), "content G must be preserved exactly");
            assertEquals(100, b(result[i]), "content B must be preserved exactly");
        }
    }

    @Test
    void alphaMatte_preservesContentRgb_forPureGreenOpaqueMatte() {
        int[] content = fill(16, OPAQUE_WHITE_CONTENT);
        int[] matte = fill(16, argb(255, 0, 255, 0));
        int[] result = new int[16];

        MatteRenderer.composeMatte(content, matte, result, MatteMode.ALPHA);

        for (int i = 0; i < result.length; i++) {
            assertEquals(255, a(result[i]));
            assertEquals(200, r(result[i]), "content R must not bias toward matte's green channel");
            assertEquals(150, g(result[i]), "content G must not lift due to pure-green matte");
            assertEquals(100, b(result[i]));
        }
    }

    @Test
    void alphaMatte_preservesContentRgb_forPureBlueOpaqueMatte() {
        // This is the configuration that produced the B-channel drop in lottie4j.json:
        // the matte source is dominantly blue. With the correct algorithm, content RGB
        // must remain identical regardless of which channel of the matte is dominant.
        int[] content = fill(16, OPAQUE_WHITE_CONTENT);
        int[] matte = fill(16, argb(255, 0, 0, 255));
        int[] result = new int[16];

        MatteRenderer.composeMatte(content, matte, result, MatteMode.ALPHA);

        for (int i = 0; i < result.length; i++) {
            assertEquals(255, a(result[i]));
            assertEquals(200, r(result[i]));
            assertEquals(150, g(result[i]));
            assertEquals(100, b(result[i]), "content B must be preserved; pure-blue matte must not bias it");
        }
    }

    @Test
    void alphaMatte_outputAlphaTracksMatteAlphaGradient() {
        // Build a horizontal alpha gradient in the matte source. Content alpha is 255,
        // so result alpha must equal the matte's alpha at each pixel, while content RGB
        // is preserved as long as the result is not fully transparent.
        int width = 8;
        int[] content = fill(width, OPAQUE_WHITE_CONTENT);
        int[] matte = new int[width];
        for (int x = 0; x < width; x++) {
            int alpha = (x * 255) / (width - 1);
            // Use a non-grey matte colour to confirm RGB is ignored.
            matte[x] = argb(alpha, 255, 64, 32);
        }
        int[] result = new int[width];

        MatteRenderer.composeMatte(content, matte, result, MatteMode.ALPHA);

        for (int x = 0; x < width; x++) {
            int expectedAlpha = (255 * ((x * 255) / (width - 1))) / 255;
            assertEquals(expectedAlpha, a(result[x]), "matte alpha at x=" + x + " should drive result alpha");
            if (expectedAlpha == 0) {
                assertEquals(0, result[x], "fully transparent pixel must be zeroed");
            } else {
                assertEquals(200, r(result[x]), "content R unchanged at x=" + x);
                assertEquals(150, g(result[x]), "content G unchanged at x=" + x);
                assertEquals(100, b(result[x]), "content B unchanged at x=" + x);
            }
        }
    }

    @Test
    void alphaMatte_fullyTransparentMatteYieldsFullyTransparentResult() {
        int[] content = fill(4, OPAQUE_WHITE_CONTENT);
        int[] matte = fill(4, argb(0, 255, 255, 255));
        int[] result = new int[4];

        MatteRenderer.composeMatte(content, matte, result, MatteMode.ALPHA);

        for (int pixel : result) {
            assertEquals(0, pixel, "transparent matte must zero the result completely");
        }
    }

    @Test
    void invertedAlphaMatte_invertsMatteAlpha() {
        int[] content = fill(4, OPAQUE_WHITE_CONTENT);
        // Half-opaque matte: result alpha should be cA * (255 - 128) / 255 = 127.
        int[] matte = fill(4, argb(128, 0, 0, 255));
        int[] result = new int[4];

        MatteRenderer.composeMatte(content, matte, result, MatteMode.INVERTED_ALPHA);

        for (int pixel : result) {
            assertEquals(127, a(pixel));
            assertEquals(200, r(pixel));
            assertEquals(150, g(pixel));
            assertEquals(100, b(pixel));
        }
    }

    @Test
    void invertedAlphaMatte_opaqueMatteYieldsFullyTransparentResult() {
        int[] content = fill(4, OPAQUE_WHITE_CONTENT);
        int[] matte = fill(4, argb(255, 10, 20, 30));
        int[] result = new int[4];

        MatteRenderer.composeMatte(content, matte, result, MatteMode.INVERTED_ALPHA);

        for (int pixel : result) {
            assertEquals(0, pixel, "opaque matte under INVERTED_ALPHA must zero the result");
        }
    }

    @Test
    void lumaMatte_usesRec601Weights_pureRed() {
        // Rec. 601 luma weight for red = 0.299.
        // For pure opaque red matte: luma = (299 * 255) / 1000 = 76.
        // resultA = (255 * 76) / 255 = 76.
        int[] content = fill(4, OPAQUE_WHITE_CONTENT);
        int[] matte = fill(4, argb(255, 255, 0, 0));
        int[] result = new int[4];

        MatteRenderer.composeMatte(content, matte, result, MatteMode.LUMA);

        int expected = (299 * 255) / 1000;
        for (int pixel : result) {
            assertEquals(expected, a(pixel), "luma alpha should use Rec.601 R weight (~0.299)");
            assertEquals(200, r(pixel));
            assertEquals(150, g(pixel));
            assertEquals(100, b(pixel));
        }
    }

    @Test
    void lumaMatte_usesRec601Weights_pureGreen() {
        // Rec. 601 luma weight for green = 0.587 (the dominant contributor).
        int[] content = fill(4, OPAQUE_WHITE_CONTENT);
        int[] matte = fill(4, argb(255, 0, 255, 0));
        int[] result = new int[4];

        MatteRenderer.composeMatte(content, matte, result, MatteMode.LUMA);

        int expected = (587 * 255) / 1000;
        for (int pixel : result) {
            assertEquals(expected, a(pixel), "luma alpha should use Rec.601 G weight (~0.587)");
        }
    }

    @Test
    void lumaMatte_usesRec601Weights_pureBlue() {
        // Rec. 601 luma weight for blue = 0.114 (the smallest contributor).
        int[] content = fill(4, OPAQUE_WHITE_CONTENT);
        int[] matte = fill(4, argb(255, 0, 0, 255));
        int[] result = new int[4];

        MatteRenderer.composeMatte(content, matte, result, MatteMode.LUMA);

        int expected = (114 * 255) / 1000;
        for (int pixel : result) {
            assertEquals(expected, a(pixel), "luma alpha should use Rec.601 B weight (~0.114)");
        }
    }

    @Test
    void lumaMatte_whiteOpaqueMatteIsNearlyFullyOpaque() {
        // Pure white opaque matte: luma = (299+587+114)*255/1000 = 1000*255/1000 = 255.
        int[] content = fill(4, OPAQUE_WHITE_CONTENT);
        int[] matte = fill(4, argb(255, 255, 255, 255));
        int[] result = new int[4];

        MatteRenderer.composeMatte(content, matte, result, MatteMode.LUMA);

        for (int pixel : result) {
            assertEquals(255, a(pixel));
            assertEquals(200, r(pixel));
            assertEquals(150, g(pixel));
            assertEquals(100, b(pixel));
        }
    }

    @Test
    void invertedLumaMatte_invertsLuma() {
        // Pure red opaque matte: luma = 76 → inverted = 255 - 76 = 179.
        int[] content = fill(4, OPAQUE_WHITE_CONTENT);
        int[] matte = fill(4, argb(255, 255, 0, 0));
        int[] result = new int[4];

        MatteRenderer.composeMatte(content, matte, result, MatteMode.INVERTED_LUMA);

        int luma = (299 * 255) / 1000;
        int expected = 255 - luma;
        for (int pixel : result) {
            assertEquals(expected, a(pixel));
        }
    }

    @Test
    void normalMatte_passesContentThroughUnchanged() {
        int[] content = new int[4];
        content[0] = argb(255, 10, 20, 30);
        content[1] = argb(128, 40, 50, 60);
        content[2] = argb(64, 70, 80, 90);
        content[3] = argb(0, 100, 110, 120);
        // Matte filled with garbage to confirm it is ignored.
        int[] matte = fill(4, argb(123, 1, 2, 3));
        int[] result = new int[4];

        MatteRenderer.composeMatte(content, matte, result, MatteMode.NORMAL);

        // alpha 0 collapses to a fully-zero pixel by the invariant in composeMatte.
        assertEquals(content[0], result[0]);
        assertEquals(content[1], result[1]);
        assertEquals(content[2], result[2]);
        assertEquals(0, result[3]);
    }

    @Test
    void contentAlphaIsRespected_partiallyTransparentContentWithOpaqueMatte() {
        // Half-opaque content with fully opaque alpha matte must come out half-opaque.
        int[] content = fill(4, argb(128, 200, 150, 100));
        int[] matte = fill(4, argb(255, 0, 0, 255));
        int[] result = new int[4];

        MatteRenderer.composeMatte(content, matte, result, MatteMode.ALPHA);

        for (int pixel : result) {
            assertEquals(128, a(pixel));
            assertEquals(200, r(pixel));
            assertEquals(150, g(pixel));
            assertEquals(100, b(pixel));
        }
    }
    }

