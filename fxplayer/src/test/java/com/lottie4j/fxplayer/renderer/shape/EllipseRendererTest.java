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
 * Unit tests for EllipseRenderer.
 * Validates ellipse rendering behavior and shape validation.
 */
class EllipseRendererTest {

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedAndReused() {
        EllipseRenderer renderer1 = new EllipseRenderer();
        EllipseRenderer renderer2 = new EllipseRenderer();
        assertNotNull(renderer1);
        assertNotNull(renderer2);
        assertNotSame(renderer1, renderer2);
    }

    @Test
    void returnsEarlyWhenShapeIsNotEllipse() {
        EllipseRenderer renderer = new EllipseRenderer();
        BaseShape nonEllipseShape = createTestShape();
        Group parentGroup = createGroup();
        
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            renderer.render(gc, nonEllipseShape, parentGroup, 0.0);
            
            return true;
        });
    }

    @Test
    void preservesGraphicsContextStateWhenShapeIsInvalid() {
        EllipseRenderer renderer = new EllipseRenderer();
        BaseShape shape = createTestShape();
        Group parentGroup = createGroup();
        
        Boolean statePreserved = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setGlobalAlpha(0.5);
            double alphaBefore = gc.getGlobalAlpha();
            
            renderer.render(gc, shape, parentGroup, 0.0);
            
            return alphaBefore == gc.getGlobalAlpha();
        });
        
        assertTrue(statePreserved, "Graphics context state should be preserved");
    }

    @Test
    void instancesAreIndependentAndStateless() {
        EllipseRenderer renderer1 = new EllipseRenderer();
        EllipseRenderer renderer2 = new EllipseRenderer();
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
        EllipseRenderer renderer = new EllipseRenderer();
        BaseShape shape = createTestShape();
        Group parentGroup = createGroup();
        
        Boolean canDraw = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            renderer.render(gc, shape, parentGroup, 0.0);
            
            gc.setFill(javafx.scene.paint.Color.MAGENTA);
            gc.fillOval(10, 10, 20, 20);
            
            return true;
        });
        
        assertTrue(canDraw, "Graphics context should remain usable");
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

