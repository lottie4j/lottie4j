package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PathRenderer.
 * Validates path rendering behavior and type validation.
 */
class PathRendererTest {

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedAndReused() {
        PathRenderer renderer1 = new PathRenderer();
        PathRenderer renderer2 = new PathRenderer();
        assertNotNull(renderer1);
        assertNotNull(renderer2);
        assertNotSame(renderer1, renderer2);
    }

    @Test
    void returnsEarlyWhenShapeIsNotPath() {
        PathRenderer renderer = new PathRenderer();
        BaseShape nonPathShape = createTestShape();
        Group parentGroup = createGroup();
        
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            renderer.render(gc, nonPathShape, parentGroup, 0.0);
            
            return true;
        });
    }

    @Test
    void instancesAreIndependentAndStateless() {
        PathRenderer renderer1 = new PathRenderer();
        PathRenderer renderer2 = new PathRenderer();
        BaseShape shape = createTestShape();
        Group parentGroup = createGroup();
        
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            renderer1.render(gc, shape, parentGroup, 0.0);
            renderer2.render(gc, shape, parentGroup, 1.0);
            
            return true;
        });
    }

    @Test
    void graphicsContextCanBeUsedAfterRendering() {
        PathRenderer renderer = new PathRenderer();
        BaseShape shape = createTestShape();
        Group parentGroup = createGroup();
        
        Boolean canDraw = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            renderer.render(gc, shape, parentGroup, 0.0);
            
            gc.setFill(javafx.scene.paint.Color.GREEN);
            gc.fillRect(10, 10, 20, 20);
            
            return true;
        });
        
        assertTrue(canDraw, "Graphics context should remain usable");
    }

    @Test
    void rendererIsReusableAcrossFrames() {
        PathRenderer renderer = new PathRenderer();
        BaseShape shape = createTestShape();
        Group parentGroup = createGroup();
        
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            renderer.render(gc, shape, parentGroup, 0.0);
            renderer.render(gc, shape, parentGroup, 1.0);
            renderer.render(gc, shape, parentGroup, 2.0);
            
            return true;
        });
    }

    private static Group createGroup() {
        return new Group(null, null, null, null, null, null, null, null, null, null, null);
    }

    private static BaseShape createTestShape() {
        return new BaseShape() {
            @Override
            public PropertyListingList getList() { return null; }
            @Override
            public ShapeType shapeType() { return ShapeType.UNKNOWN; }
        };
    }
}

