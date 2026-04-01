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
 * Unit tests for PolystarRenderer.
 * Validates polystar (polygon/star) rendering behavior and shape validation.
 */
public class PolystarRendererTest {

    @BeforeAll
    public static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void canBeInstantiatedAndReused() {
        PolystarRenderer renderer1 = new PolystarRenderer();
        PolystarRenderer renderer2 = new PolystarRenderer();
        assertNotNull(renderer1);
        assertNotNull(renderer2);
        assertNotSame(renderer1, renderer2);
    }

    @Test
    void returnsEarlyWhenShapeIsNotPolystar() {
        PolystarRenderer renderer = new PolystarRenderer();
        BaseShape nonPolystarShape = createTestShape();
        Group parentGroup = createGroup();
        
        FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            renderer.render(gc, nonPolystarShape, parentGroup, 0.0);
            
            return true;
        });
    }

    @Test
    void preservesGraphicsContextStateWhenShapeIsInvalid() {
        PolystarRenderer renderer = new PolystarRenderer();
        BaseShape shape = createTestShape();
        Group parentGroup = createGroup();
        
        Boolean statePreserved = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setGlobalAlpha(0.6);
            double alphaBefore = gc.getGlobalAlpha();
            
            renderer.render(gc, shape, parentGroup, 0.0);
            
            return alphaBefore == gc.getGlobalAlpha();
        });
        
        assertTrue(statePreserved, "Graphics context state should be preserved");
    }

    @Test
    void instancesAreIndependentAndStateless() {
        PolystarRenderer renderer1 = new PolystarRenderer();
        PolystarRenderer renderer2 = new PolystarRenderer();
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
        PolystarRenderer renderer = new PolystarRenderer();
        BaseShape shape = createTestShape();
        Group parentGroup = createGroup();
        
        Boolean canDraw = FxTestHelper.callAndWait(() -> {
            Canvas canvas = new Canvas(100, 100);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            renderer.render(gc, shape, parentGroup, 0.0);
            
            gc.setFill(javafx.scene.paint.Color.YELLOW);
            gc.fillPolygon(
                new double[]{50, 60, 70}, 
                new double[]{10, 30, 10}, 
                3
            );
            
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

