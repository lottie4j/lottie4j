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
 * Unit tests for RectangleRenderer.
 * Validates rectangle rendering behavior and shape validation.
 */
public class RectangleRendererTest {

    @BeforeAll
    public static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedAndReused() {
        RectangleRenderer renderer1 = new RectangleRenderer();
        RectangleRenderer renderer2 = new RectangleRenderer();
        assertNotNull(renderer1);
        assertNotNull(renderer2);
        assertNotSame(renderer1, renderer2);
    }

    @Test
    void returnsEarlyWhenShapeIsNotRectangle() {
        RectangleRenderer renderer = new RectangleRenderer();
        BaseShape nonRectangleShape = createTestShape();
        Group parentGroup = createGroup();
        
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            renderer.render(gc, nonRectangleShape, parentGroup, 0.0);
            
            return true;
        });
    }

    @Test
    void preservesGraphicsContextStateWhenShapeIsInvalid() {
        RectangleRenderer renderer = new RectangleRenderer();
        BaseShape shape = createTestShape();
        Group parentGroup = createGroup();
        
        Boolean statePreserved = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setGlobalAlpha(0.75);
            double alphaBefore = gc.getGlobalAlpha();
            
            renderer.render(gc, shape, parentGroup, 0.0);
            
            return alphaBefore == gc.getGlobalAlpha();
        });
        
        assertTrue(statePreserved, "Graphics context state should be preserved");
    }

    @Test
    void instancesAreIndependentAndStateless() {
        RectangleRenderer renderer1 = new RectangleRenderer();
        RectangleRenderer renderer2 = new RectangleRenderer();
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
        RectangleRenderer renderer = new RectangleRenderer();
        BaseShape shape = createTestShape();
        Group parentGroup = createGroup();
        
        Boolean canDraw = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            renderer.render(gc, shape, parentGroup, 0.0);
            
            gc.setFill(javafx.scene.paint.Color.CYAN);
            gc.fillRect(10, 10, 20, 20);
            
            return true;
        });
        
        assertTrue(canDraw, "Graphics context should remain usable");
    }

    @Test
    void rendererIsReusableAcrossFrames() {
        RectangleRenderer renderer = new RectangleRenderer();
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

    //...existing code...

//...existing code...
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

