package com.lottie4j.fxplayer.renderer;

import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.shape.BaseShape;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class LottieRenderEngine {

    private static final Logger logger = Logger.getLogger(LottieRenderEngine.class.getName());

    private final Animation animation;
    private final Map<Class<?>, ShapeRenderer> renderers;

    public LottieRenderEngine(Animation animation) {
        this.animation = animation;
        this.renderers = new HashMap<>();

        // Register shape renderers
        registerRenderer(new RectangleRenderer());
        registerRenderer(new EllipseRenderer());
        registerRenderer(new PathRenderer());
        registerRenderer(new GroupRenderer());
        registerRenderer(new PolystarRenderer());

        logger.info("LottieRenderEngine initialized with " + renderers.size() + " renderers");
    }

    private void registerRenderer(ShapeRenderer<?> renderer) {
        renderers.put(renderer.getShapeType(), renderer);
        logger.info("Registered renderer for: " + renderer.getShapeType().getSimpleName());
    }

    public void render(GraphicsContext gc, double frame) {
        logger.fine("Rendering frame: " + frame);

        // Clear canvas with a visible background for debugging
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        // Draw a border to verify canvas is working
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);
        gc.strokeRect(1, 1, gc.getCanvas().getWidth() - 2, gc.getCanvas().getHeight() - 2);

        if (animation == null) {
            logger.warning("No animation to render");
            return;
        }

        if (animation.layers() == null || animation.layers().isEmpty()) {
            logger.warning("No layers in animation");
            gc.setFill(Color.BLACK);
            gc.fillText("No layers found in animation", 10, 30);
            return;
        }

        logger.info("Animation has " + animation.layers().size() + " layers");

        // Calculate scaling to fit canvas while maintaining aspect ratio
        double scaleX = gc.getCanvas().getWidth() / animation.width();
        double scaleY = gc.getCanvas().getHeight() / animation.height();
        double scale = Math.min(scaleX, scaleY) * 0.8; // Leave some margin

        double offsetX = (gc.getCanvas().getWidth() - animation.width() * scale) / 2;
        double offsetY = (gc.getCanvas().getHeight() - animation.height() * scale) / 2;

        logger.info("Animation size: " + animation.width() + "x" + animation.height());
        logger.info("Canvas size: " + gc.getCanvas().getWidth() + "x" + gc.getCanvas().getHeight());
        logger.info("Scale: " + scale + ", Offset: " + offsetX + ", " + offsetY);

        gc.save();
        gc.translate(offsetX, offsetY);
        gc.scale(scale, scale);

        // Set default colors for debugging
        gc.setFill(Color.BLUE);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);

        // Render layers in order
        for (Layer layer : animation.layers()) {
            logger.info("Processing layer: " + layer.name());

            if (isLayerActiveAtFrame(layer, frame)) {
                logger.info("Rendering active layer: " + layer.name());
                renderLayer(gc, layer, frame);
            }
        }

        gc.restore();

        // Draw debug info
        gc.setFill(Color.BLACK);
        gc.fillText("Frame: " + String.format("%.1f", frame), 10, gc.getCanvas().getHeight() - 30);
        gc.fillText("Scale: " + String.format("%.2f", scale), 10, gc.getCanvas().getHeight() - 10);

    }

    private boolean isLayerActiveAtFrame(Layer layer, double frame) {
        boolean active = frame >= layer.inPoint() && frame <= layer.outPoint();
        logger.fine("Layer " + layer.name() + " active at frame " + frame + ": " + active +
                " (in: " + layer.inPoint() + ", out: " + layer.outPoint() + ")");
        return active;
    }

    private void renderLayer(GraphicsContext gc, Layer layer, double frame) {
        gc.save();

        // Apply layer transform
        applyLayerTransform(gc, layer, frame);

        // Render layer shapes
        if (layer.shapes() != null && !layer.shapes().isEmpty()) {
            logger.info("Layer has " + layer.shapes().size() + " shapes");

            for (BaseShape shape : layer.shapes()) {
                renderShape(gc, shape, frame);
            }
        } else {
            logger.info("Layer has no shapes");
        }

        gc.restore();
    }

    @SuppressWarnings("unchecked")
    private void renderShape(GraphicsContext gc, BaseShape shape, double frame) {
        logger.info("Rendering shape: " + shape.getClass().getSimpleName() +
                " (name: " + (shape.getName() != null ? shape.getName() : "unnamed") + ")");

        ShapeRenderer renderer = renderers.get(shape.getClass());
        if (renderer != null) {
            logger.fine("Using renderer: " + renderer.getClass().getSimpleName());

            // Draw a small debug marker at origin
            gc.save();
            gc.setFill(Color.MAGENTA);
            gc.fillOval(-2, -2, 4, 4);
            gc.restore();

            renderer.render(gc, shape, frame);
        } else {
            logger.warning("No renderer found for shape type: " + shape.getClass().getSimpleName());

            // Draw a placeholder for unhandled shapes
            gc.save();
            gc.setFill(Color.ORANGE);
            gc.fillRect(0, 0, 50, 20);
            gc.setFill(Color.BLACK);
            gc.fillText("?", 20, 15);
            gc.restore();
        }
    }

    private void applyLayerTransform(GraphicsContext gc, Layer layer, double frame) {
        if (layer.transform() == null) {
            logger.fine("No transform for layer: " + layer.name());
            return;
        }

        logger.fine("Applying transform for layer: " + layer.name());

        // Apply opacity
        if (layer.transform().opacity() != null) {
            double opacity = layer.transform().opacity().getValue(Animated.ValueType.OPACITY, (long) frame);
            logger.fine("Setting opacity: " + opacity);
            gc.setGlobalAlpha(opacity / 100.0);
        }

        // Apply position
        if (layer.transform().position() != null) {
            double x = layer.transform().position().getValue(Animated.ValueType.X, (long) frame);
            double y = layer.transform().position().getValue(Animated.ValueType.Y, (long) frame);
            logger.fine("Translating by: " + x + ", " + y);
            gc.translate(x, y);
        }

        // Apply rotation
        if (layer.transform().rotation() != null) {
            double rotation = Math.toRadians(layer.transform().rotation().getValue(0, (long) frame));
            logger.fine("Rotating by: " + Math.toDegrees(rotation) + " degrees");
            gc.rotate(rotation);
        }

        // Apply scale
        if (layer.transform().scale() != null) {
            double scaleX = layer.transform().scale().getValue(Animated.ValueType.X, (long) frame) / 100.0;
            double scaleY = layer.transform().scale().getValue(Animated.ValueType.Y, (long) frame) / 100.0;
            logger.fine("Scaling by: " + scaleX + ", " + scaleY);
            gc.scale(scaleX, scaleY);
        }
    }
}