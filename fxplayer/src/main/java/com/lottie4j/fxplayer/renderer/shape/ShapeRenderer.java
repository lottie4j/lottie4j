package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.fxplayer.LottieRenderEngine;
import javafx.scene.canvas.GraphicsContext;

/**
 * Base interface for rendering Lottie shapes to JavaFX Canvas
 */
public interface ShapeRenderer<T extends BaseShape> {

    /**
     * Renders the given shape at the specified frame
     *
     * @param gc    Graphics context to render to
     * @param shape The shape to render
     * @param frame Current animation frame
     */
    void render(LottieRenderEngine engine, GraphicsContext gc, T shape, Group parentGroup, double frame);

    /**
     * Returns the shape type this renderer handles
     */
    Class<T> getShapeType();
}