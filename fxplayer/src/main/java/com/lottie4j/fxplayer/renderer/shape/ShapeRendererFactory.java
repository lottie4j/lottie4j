package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.model.shape.BaseShape;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for obtaining shape renderers based on shape type.
 * Centralizes the renderer selection logic and maintains renderer instances.
 */
public class ShapeRendererFactory {

    private final Map<Class<? extends BaseShape>, ShapeRenderer> renderers = new HashMap<>();

    /**
     * Creates a factory with default shape renderers.
     */
    public ShapeRendererFactory() {
        renderers.put(com.lottie4j.core.model.shape.shape.Path.class, new PathRenderer());
        renderers.put(com.lottie4j.core.model.shape.shape.Ellipse.class, new EllipseRenderer());
        renderers.put(com.lottie4j.core.model.shape.shape.Rectangle.class, new RectangleRenderer());
        renderers.put(com.lottie4j.core.model.shape.shape.Polystar.class, new PolystarRenderer());
    }

    /**
     * Gets the appropriate renderer for a shape.
     *
     * @param shape shape to render
     * @return shape renderer, or null if no renderer is registered for this shape type
     */
    public ShapeRenderer getRenderer(BaseShape shape) {
        return renderers.get(shape.getClass());
    }

    /**
     * Registers a custom renderer for a specific shape type.
     *
     * @param shapeClass shape class
     * @param renderer   renderer instance
     * @param <T>        shape type
     */
    public <T extends BaseShape> void registerRenderer(Class<T> shapeClass, ShapeRenderer renderer) {
        renderers.put(shapeClass, renderer);
    }
}

