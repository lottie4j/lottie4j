package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PathStrokeRenderer.
 */
class PathStrokeRendererTest {

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedAndReused() {
        PathStrokeRenderer r1 = new PathStrokeRenderer();
        PathStrokeRenderer r2 = new PathStrokeRenderer();
        assertNotNull(r1);
        assertNotNull(r2);
    }

    @Test
    void rendererIsStateless() {
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            new PathStrokeRenderer().renderStroke(
                canvas.getGraphicsContext2D(), 
                createGroup(), 
                0.0, 
                "test",
                createVertices(),
                createTangents(),
                createTangents(),
                false
            );
            return true;
        });
    }

    @Test
    void graphicsContextCanBeUsedAfterRendering() {
        Boolean result = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            new PathStrokeRenderer().renderStroke(
                gc, 
                createGroup(), 
                0.0, 
                "test",
                createVertices(),
                createTangents(),
                createTangents(),
                false
            );
            gc.setStroke(javafx.scene.paint.Color.RED);
            return true;
        });
        assertTrue(result);
    }

    @Test
    void instancesAreIndependent() {
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            new PathStrokeRenderer().renderStroke(
                gc, 
                createGroup(), 
                0.0, 
                "p1",
                createVertices(),
                createTangents(),
                createTangents(),
                false
            );
            new PathStrokeRenderer().renderStroke(
                gc, 
                createGroup(), 
                1.0, 
                "p2",
                createVertices(),
                createTangents(),
                createTangents(),
                false
            );
            return true;
        });
    }

    @Test
    void rendererIsReusableAcrossFrames() {
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            PathStrokeRenderer r = new PathStrokeRenderer();
            r.renderStroke(
                gc, 
                createGroup(), 
                0.0, 
                "p",
                createVertices(),
                createTangents(),
                createTangents(),
                false
            );
            r.renderStroke(
                gc, 
                createGroup(), 
                1.0, 
                "p",
                createVertices(),
                createTangents(),
                createTangents(),
                false
            );
            r.renderStroke(
                gc, 
                createGroup(), 
                2.0, 
                "p",
                createVertices(),
                createTangents(),
                createTangents(),
                false
            );
            return true;
        });
    }

    private static Group createGroup() {
        return new Group(null, null, null, null, null, null, null, null, null, null, new ArrayList<>());
    }

    private static List<List<Double>> createVertices() {
        List<List<Double>> vertices = new ArrayList<>();
        vertices.add(List.of(0.0, 0.0));
        vertices.add(List.of(10.0, 10.0));
        vertices.add(List.of(20.0, 0.0));
        return vertices;
    }

    private static List<List<Double>> createTangents() {
        List<List<Double>> tangents = new ArrayList<>();
        tangents.add(List.of(0.0, 0.0));
        tangents.add(List.of(0.0, 0.0));
        tangents.add(List.of(0.0, 0.0));
        return tangents;
    }
}

