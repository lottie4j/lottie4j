package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SolidColorRendererTest {

    private static final double EPS = 0.01;

    @BeforeAll
    public static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void rendersSolidColorUsingLayerDimensionsWhenProvided() {
        SolidColorRenderer renderer = new SolidColorRenderer();
        Layer layer = layer("#FF0000", 4, 6);

        Color color = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(10, 10);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            renderer.render(gc, layer, 100, 100);
            return samplePixel(canvas, 2, 2);
        });

        assertColor(color, Color.color(1.0, 0.0, 0.0, 1.0));
    }

    @Test
    void fallsBackToAnimationDimensionsWhenLayerDimensionsMissing() {
        SolidColorRenderer renderer = new SolidColorRenderer();
        Layer layer = layer("0000FF", null, null);

        Color color = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(12, 12);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            renderer.render(gc, layer, 4, 5);
            return samplePixel(canvas, 6, 6);
        });

        assertColor(color, Color.color(0.0, 0.0, 1.0, 1.0));
    }

    @Test
    void keepsCanvasTransparentWhenColorIsMissing() {
        SolidColorRenderer renderer = new SolidColorRenderer();
        Layer layer = layer(null, 4, 4);

        Color[] colors = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(8, 8);
            Color before = samplePixel(canvas, 4, 4);
            renderAndSample(renderer, layer, canvas);
            Color after = samplePixel(canvas, 4, 4);
            return new Color[]{before, after};
        });

        assertColor(colors[1], colors[0]);
    }

    @Test
    void keepsCanvasTransparentWhenColorCannotBeParsed() {
        SolidColorRenderer renderer = new SolidColorRenderer();
        Layer layer = layer("NOT_HEX", 4, 4);

        Color[] colors = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(8, 8);
            Color before = samplePixel(canvas, 4, 4);
            renderAndSample(renderer, layer, canvas);
            Color after = samplePixel(canvas, 4, 4);
            return new Color[]{before, after};
        });

        assertColor(colors[1], colors[0]);
    }

    private static Color renderAndSample(SolidColorRenderer renderer, Layer layer, Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        renderer.render(gc, layer, 4, 4);
        return samplePixel(canvas, 4, 4);
    }

    private static Color samplePixel(Canvas canvas, int x, int y) {
        WritableImage image = canvas.snapshot(new SnapshotParameters(), null);
        return image.getPixelReader().getColor(x, y);
    }

    private static void assertColor(Color actual, Color expected) {
        assertEquals(expected.getRed(), actual.getRed(), EPS);
        assertEquals(expected.getGreen(), actual.getGreen(), EPS);
        assertEquals(expected.getBlue(), actual.getBlue(), EPS);
        assertEquals(expected.getOpacity(), actual.getOpacity(), EPS);
    }

    private static Layer layer(String solidColor, Integer width, Integer height) {
        return new Layer(
                "solid", null, null, null, null, null, null,
                null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null,
                null, null, width, height, null, solidColor, null
        );
    }
}
