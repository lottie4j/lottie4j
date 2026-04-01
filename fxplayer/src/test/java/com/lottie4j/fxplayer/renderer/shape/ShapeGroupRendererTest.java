package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.fxplayer.renderer.layer.TransformApplier;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ShapeGroupRenderer.
 */
class ShapeGroupRendererTest {

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedAndReused() {
        TransformApplier ta = new TransformApplier();
        ShapeRendererFactory srf = new ShapeRendererFactory();
        ShapeGroupRenderer r1 = new ShapeGroupRenderer(ta, srf);
        ShapeGroupRenderer r2 = new ShapeGroupRenderer(ta, srf);
        assertNotNull(r1);
        assertNotNull(r2);
    }

    @Test
    void instancesAreIndependent() {
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            TransformApplier ta = new TransformApplier();
            ShapeRendererFactory srf = new ShapeRendererFactory();
            new ShapeGroupRenderer(ta, srf).renderShapeTypeGroup(gc, createGroup(), 0.0, null);
            new ShapeGroupRenderer(ta, srf).renderShapeTypeGroup(gc, createGroup(), 1.0, null);
            return true;
        });
    }

    @Test
    void graphicsContextCanBeUsedAfterRendering() {
        Boolean result = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            TransformApplier ta = new TransformApplier();
            ShapeRendererFactory srf = new ShapeRendererFactory();
            new ShapeGroupRenderer(ta, srf).renderShapeTypeGroup(gc, createGroup(), 0.0, null);
            gc.setFill(javafx.scene.paint.Color.BLUE);
            gc.fillRect(0, 0, 10, 10);
            return true;
        });
        assertTrue(result);
    }

    @Test
    void rendererIsReusableAcrossFrames() {
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            TransformApplier ta = new TransformApplier();
            ShapeRendererFactory srf = new ShapeRendererFactory();
            ShapeGroupRenderer r = new ShapeGroupRenderer(ta, srf);
            r.renderShapeTypeGroup(gc, createGroup(), 0.0, null);
            r.renderShapeTypeGroup(gc, createGroup(), 1.0, null);
            r.renderShapeTypeGroup(gc, createGroup(), 2.0, null);
            return true;
        });
    }

    @Test
    void canRenderEmptyGroup() {
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            TransformApplier ta = new TransformApplier();
            ShapeRendererFactory srf = new ShapeRendererFactory();
            Group emptyGroup = createGroup();
            new ShapeGroupRenderer(ta, srf).renderShapeTypeGroup(gc, emptyGroup, 0.0, null);
            return true;
        });
    }

    private static Group createGroup() {
        return new Group(null, null, null, null, null, null, null, null, null, null, new ArrayList<>());
    }
}


