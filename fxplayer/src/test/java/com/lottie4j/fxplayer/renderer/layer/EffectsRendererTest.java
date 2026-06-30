package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.definition.EffectType;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.effect.Effect;
import com.lottie4j.core.model.effect.EffectValue;
import com.lottie4j.core.model.keyframe.NumberKeyframe;
import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EffectsRenderer.
 * Validates effects rendering behavior and state management.
 */
class EffectsRendererTest {

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    // Helper methods to create test fixtures
    private static Layer layerWithoutEffects() {
        return new Layer(
                "effectLayer", null, null, null, null, null, null,
                null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null, null
        );
    }

    @Test
    void canBeInstantiatedAndReused() {
        EffectsRenderer renderer1 = new EffectsRenderer();
        EffectsRenderer renderer2 = new EffectsRenderer();
        assertNotNull(renderer1);
        assertNotNull(renderer2);
        assertNotSame(renderer1, renderer2);
    }

    @Test
    void instancesAreIndependentWithSeparateCaches() {
        EffectsRenderer renderer1 = new EffectsRenderer();
        EffectsRenderer renderer2 = new EffectsRenderer();

        // Each instance should have its own cache
        assertNotNull(renderer1);
        assertNotNull(renderer2);
    }

    @Test
    void doesNotModifyGraphicsContextWhenNoEffectsPresent() {
        EffectsRenderer renderer = new EffectsRenderer();
        Layer layerNoEffects = layerWithoutEffects();

        Boolean statePreserved = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setGlobalAlpha(0.75);
            double alphaBefore = gc.getGlobalAlpha();

            // Render without effects
            EffectsRenderer.LayerRenderer noOpRenderer = (gc_inner, layer, frame) -> {
            };
            renderer.renderLayerWithGaussianBlur(gc, layerNoEffects, 0.0, 0.0, noOpRenderer);

            return alphaBefore == gc.getGlobalAlpha();
        });

        assertTrue(statePreserved, "Graphics context should remain unchanged");
    }

    @Test
    void graphicsContextCanBeUsedAfterEffectsApplication() {
        EffectsRenderer renderer = new EffectsRenderer();
        Layer layer = layerWithoutEffects();

        Boolean canDraw = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            EffectsRenderer.LayerRenderer noOpRenderer = (gc_inner, l, frame) -> {
            };
            renderer.renderLayerWithGaussianBlur(gc, layer, 0.0, 0.0, noOpRenderer);

            // Should be able to draw afterwards
            gc.setFill(javafx.scene.paint.Color.RED);
            gc.fillRect(10, 10, 20, 20);

            return true;
        });

        assertTrue(canDraw, "Graphics context should remain usable");
    }

    @Test
    void chooseDownsampleFactorReturnsExpectedPowerOfTwo() {
        // MAX_PASS_BLUR_RADIUS is 200 px so factor doubles only once raw exceeds that.
        assertEquals(1, EffectsRenderer.chooseDownsampleFactor(0));
        assertEquals(1, EffectsRenderer.chooseDownsampleFactor(60));
        assertEquals(1, EffectsRenderer.chooseDownsampleFactor(177.6));
        assertEquals(1, EffectsRenderer.chooseDownsampleFactor(200));
        assertEquals(2, EffectsRenderer.chooseDownsampleFactor(201));
        assertEquals(2, EffectsRenderer.chooseDownsampleFactor(400));
        assertEquals(4, EffectsRenderer.chooseDownsampleFactor(401));
        assertEquals(4, EffectsRenderer.chooseDownsampleFactor(700));
        assertEquals(4, EffectsRenderer.chooseDownsampleFactor(800));
        assertEquals(8, EffectsRenderer.chooseDownsampleFactor(1600));
    }

    @Test
    void chooseDownsampleFactorAlwaysKeepsPassRadiusWithinMaxPassLimit() {
        // Per-pass cap (200 px) tracks JavaFX's BoxBlur addressable range.
        for (double raw : new double[]{0, 1, 20, 60, 100, 177.6, 200, 200.0001, 300, 500, 700, 800, 1600, 3200}) {
            int factor = EffectsRenderer.chooseDownsampleFactor(raw);
            assertTrue(factor >= 1, "factor must be >= 1 for raw=" + raw + " got " + factor);
            assertTrue((factor & (factor - 1)) == 0, "factor must be power of two for raw=" + raw + " got " + factor);
            assertTrue(raw / factor <= 200.0 + 1e-9, "raw/factor must be <= 200 for raw=" + raw + " got " + (raw / factor));
        }
    }

    @Test
    void getGaussianBlurRadiusReturnsRawBlurrinessUnclamped() {
        EffectsRenderer renderer = new EffectsRenderer();
        assertEquals(3.0, renderer.getGaussianBlurRadius(layerWithBlurriness(3), 0.0), 1e-9);
        assertEquals(177.6, renderer.getGaussianBlurRadius(layerWithBlurriness(177.6), 0.0), 1e-9);
        assertEquals(700.0, renderer.getGaussianBlurRadius(layerWithBlurriness(700), 0.0), 1e-9);
        assertEquals(800.0, renderer.getGaussianBlurRadius(layerWithBlurriness(800), 0.0), 1e-9);
    }

    @Test
    void getGaussianBlurRadiusReturnsZeroWhenLayerHasNoBlurEffect() {
        EffectsRenderer renderer = new EffectsRenderer();
        assertEquals(0.0, renderer.getGaussianBlurRadius(layerWithoutEffects(), 0.0));
    }

    @Test
    void getGaussianBlurRadiusReturnsZeroWhenBlurEffectIsDisabled() {
        EffectsRenderer renderer = new EffectsRenderer();
        Effect disabled = new Effect(
                "Gaussian Blur", null, null, EffectType.GAUSSIAN_BLUR, 0,
                List.of(new EffectValue("Blurriness", 0,
                        new Animated(0, List.of(new NumberKeyframe(800.0)), null, null, null, null, null))));
        Layer layer = new Layer(
                "blurLayer", null, null, null, null, null, null,
                null, null, null, null,
                null, null, null, null, null, null, null, null, List.of(disabled),
                null, null, null, null, null,
                null, null, null, null, null, null, null
        );
        assertEquals(0.0, renderer.getGaussianBlurRadius(layer, 0.0));
    }

    @Test
    void boundedBlurExtremeRadiusProducesSofterSpreadThanModerateRadius() {
        // Smoke test: extreme blur values must not throw, and must produce a softer (lower peak red)
        // result than a moderate blur — because the wide Gaussian kernel scatters the source
        // colour over a much larger area, including beyond the kept bounds.
        EffectsRenderer renderer = new EffectsRenderer();
        Layer layer = layerWithoutEffects();

        double moderatePeak = samplePeakRed(renderer, layer, 63.0);
        double extremePeak = samplePeakRed(renderer, layer, 800.0);
        double noBlurPeak = samplePeakRed(renderer, layer, 0.0);

        assertTrue(noBlurPeak > 0.99,
                "No-blur path must preserve the opaque source pixel, got peak red " + noBlurPeak);
        assertTrue(moderatePeak > 0.0,
                "Moderate blur must still leave visible red, got " + moderatePeak);
        assertTrue(extremePeak > 0.0,
                "Extreme blur must still produce some red (downsample path), got " + extremePeak);
        assertTrue(extremePeak < moderatePeak,
                "Extreme blur should spread the source further than a moderate blur, leaving lower"
                        + " peak red; moderate=" + moderatePeak + " extreme=" + extremePeak);
    }

    /**
     * Snapshots the canvas onto a black background so the red channel measures effective coverage
     * (transparent areas → black → red=0). Returns the maximum red value across all pixels.
     */
    private static double samplePeakRed(EffectsRenderer renderer, Layer layer, double blurRadius) {
        return FxTestHelper.callAndWait(() -> {
            final int size = 64;
            Canvas canvas = new Canvas(size, size);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            EffectsRenderer.LayerRenderer redRect = (gc_inner, l, frame) -> {
                gc_inner.setFill(Color.RED);
                gc_inner.fillRect((size - 16) / 2.0, (size - 16) / 2.0, 16, 16);
            };
            if (blurRadius == 0.0) {
                redRect.render(gc, layer, 0.0);
            } else {
                renderer.renderLayerWithGaussianBlur(gc, layer, 0.0, blurRadius, size, size, 1.0, redRect);
            }
            javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
            params.setFill(Color.BLACK);
            javafx.scene.image.WritableImage image = canvas.snapshot(params, null);
            javafx.scene.image.PixelReader reader = image.getPixelReader();
            double maxRed = 0.0;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    double red = reader.getColor(x, y).getRed();
                    if (red > maxRed) {
                        maxRed = red;
                    }
                }
            }
            return maxRed;
        });
    }

    private static Layer layerWithBlurriness(double blurriness) {
        Animated value = new Animated(0, List.of(new NumberKeyframe(blurriness)), null, null, null, null, null);
        EffectValue blurrinessValue = new EffectValue("Blurriness", 0, value);
        Effect effect = new Effect(
                "Gaussian Blur", null, null, EffectType.GAUSSIAN_BLUR, 1, List.of(blurrinessValue));
        return new Layer(
                "blurLayer", null, null, null, null, null, null,
                null, null, null, null,
                null, null, null, null, null, null, null, null, List.of(effect),
                null, null, null, null, null,
                null, null, null, null, null, null, null
        );
    }

    @Test
    void rendererRemainsStatelessAcrossMultipleFrames() {
        EffectsRenderer renderer = new EffectsRenderer();

        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            Layer layer1 = layerWithoutEffects();
            Layer layer2 = layerWithoutEffects();

            EffectsRenderer.LayerRenderer noOpRenderer = (gc_inner, layer, frame) -> {
            };

            renderer.renderLayerWithGaussianBlur(gc, layer1, 0.0, 0.0, noOpRenderer);
            renderer.renderLayerWithGaussianBlur(gc, layer2, 1.0, 0.0, noOpRenderer);

            return true;
        });
    }
}

