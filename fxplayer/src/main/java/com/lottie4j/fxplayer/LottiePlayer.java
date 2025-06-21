package com.lottie4j.fxplayer;

import com.lottie4j.core.definition.ShapeGroup;
import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.fxplayer.renderer.shape.*;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.logging.Logger;

/**
 * JavaFX Canvas component for playing Lottie animations
 */
public class LottiePlayer extends Canvas {

    private static final Logger logger = Logger.getLogger(LottiePlayer.class.getName());

    private final Animation animation;
    private final GraphicsContext gc;

    private AnimationTimer animationTimer;
    private long startTime;
    private boolean isPlaying = false;
    private boolean debug = false;
    private double currentFrame = 0;

    public LottiePlayer(Animation animation) {
        this(animation, false);
    }

    public LottiePlayer(Animation animation, boolean debug) {
        this.animation = animation;
        this.debug = debug;

        // Set canvas size to animation size
        setWidth(animation.width());
        setHeight(animation.height());

        this.gc = getGraphicsContext2D();

        // Initial render
        renderFrame(animation.inPoint());
    }

    public void play() {
        if (isPlaying) return;

        isPlaying = true;
        startTime = System.nanoTime();
        currentFrame = animation.inPoint();

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double elapsedSeconds = (now - startTime) / 1_000_000_000.0;
                double totalFrames = animation.outPoint() - animation.inPoint();
                double animationDuration = totalFrames / animation.framesPerSecond();

                if (elapsedSeconds >= animationDuration) {
                    // Loop animation
                    startTime = now;
                    elapsedSeconds = 0;
                }

                currentFrame = animation.inPoint() + (elapsedSeconds * animation.framesPerSecond());
                renderFrame(currentFrame);
            }
        };

        animationTimer.start();
    }

    public void stop() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        isPlaying = false;
    }

    public void seekToFrame(double frame) {
        currentFrame = Math.max(animation.inPoint(),
                Math.min(animation.outPoint(), frame));
        renderFrame(currentFrame);
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

        if (debug) {
            // Draw debug info
            gc.setFill(Color.BLACK);
            gc.fillText("Frame: " + String.format("%.1f", frame), 10, gc.getCanvas().getHeight() - 30);
            gc.fillText("Scale: " + String.format("%.2f", scale), 10, gc.getCanvas().getHeight() - 10);
        }
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

            for (BaseShape shape : layer.shapes().stream()
                    .filter(s -> s.type().shapeGroup() == ShapeGroup.SHAPE)
                    .toList()) {
                renderShape(gc, shape, null, frame);
            }
        } else {
            logger.info("Layer has no shapes");
        }

        gc.restore();
    }

    @SuppressWarnings("unchecked")
    public void renderShape(GraphicsContext gc, BaseShape shape, Group parentGroup, double frame) {
        if (gc == null || shape == null) {
            logger.warning("Skipping render: gc or shape is null");
            return;
        }

        logger.info("Rendering shape: " + shape.getClass().getSimpleName() +
                " (name: " + (shape.name() != null ? shape.name() : "unnamed") + ")");

        var renderer = getShapeRenderer(shape.getClass());
        if (renderer != null) {
            logger.fine("Using renderer: " + renderer.getClass().getSimpleName());

            // Draw a small debug marker at origin
            gc.save();
            gc.setFill(Color.MAGENTA);
            gc.fillOval(-2, -2, 4, 4);
            gc.restore();

            try {
                // The unchecked cast is still here but now with proper error handling
                //renderer.render(gc, shape, parentGroup, frame);
            } catch (ClassCastException e) {
                logger.severe("Type mismatch when rendering shape: " + e.getMessage());
                // Fall back to placeholder rendering
                renderPlaceholder(gc);
            }
        } else {
            logger.severe("No renderer found for shape type: " + shape.getClass().getSimpleName());

            // Draw a placeholder for unhandled shapes
            gc.save();
            gc.setFill(Color.ORANGE);
            gc.fillRect(0, 0, 50, 20);
            gc.setFill(Color.BLACK);
            gc.fillText("?", 20, 15);
            gc.restore();
        }
    }

    private void renderPlaceholder(GraphicsContext gc) {
        gc.save();
        gc.setFill(Color.ORANGE);
        gc.fillRect(0, 0, 50, 20);
        gc.setFill(Color.BLACK);
        gc.fillText("?", 20, 15);
        gc.restore();
    }

    private ShapeRenderer getShapeRenderer(Class<? extends BaseShape> shapeType) {
        switch (shapeType.getSimpleName()) {
            case "Rectangle":
                return new RectangleRenderer();
            case "Ellipse":
                return new EllipseRenderer();
            case "Path":
                return new PathRenderer();
            case "Polystar":
                return new PolystarRenderer();
            default:
                return null;
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
            double opacity = layer.transform().opacity().getValue(AnimatedValueType.OPACITY, (long) frame);
            logger.fine("Setting opacity: " + opacity);
            gc.setGlobalAlpha(opacity / 100.0);
        }

        // Apply position
        if (layer.transform().position() != null) {
            double x = layer.transform().position().getValue(AnimatedValueType.X, (long) frame);
            double y = layer.transform().position().getValue(AnimatedValueType.Y, (long) frame);
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
            double scaleX = layer.transform().scale().getValue(AnimatedValueType.X, (long) frame) / 100.0;
            double scaleY = layer.transform().scale().getValue(AnimatedValueType.Y, (long) frame) / 100.0;
            logger.fine("Scaling by: " + scaleX + ", " + scaleY);
            gc.scale(scaleX, scaleY);
        }
    }

    private void renderFrame(double frame) {
        render(gc, frame);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public double getCurrentFrame() {
        return currentFrame;
    }

    public Animation getAnimation() {
        return animation;
    }
}