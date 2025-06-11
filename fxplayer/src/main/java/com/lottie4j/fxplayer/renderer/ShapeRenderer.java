package com.lottie4j.fxplayer.renderer;

import com.lottie4j.core.model.shape.BaseShape;
import javafx.scene.canvas.GraphicsContext;

/**
 * Base interface for rendering Lottie shapes to JavaFX Canvas
 */
public interface ShapeRenderer<T extends BaseShape> {

    /**
     * Renders the given shape at the specified frame
     * @param gc Graphics context to render to
     * @param shape The shape to render
     * @param frame Current animation frame
     */
    void render(GraphicsContext gc, T shape, double frame);

    /**
     * Returns the shape type this renderer handles
     */
    Class<T> getShapeType();
}