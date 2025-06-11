package com.lottie4j.fxplayer.renderer;

import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.shape.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.logging.Logger;

public class GroupRenderer implements ShapeRenderer<Group> {

    private static final Logger logger = Logger.getLogger(GroupRenderer.class.getName());

    @Override
    public void render(GraphicsContext gc, Group group, double frame) {
        logger.info("GroupRenderer.render called for group with " + group.shapes().size() + " items");

        gc.save();

        // Find transform, fill and stroke styles in the group
        Transform transform = null;
        Fill fill = null;
        Stroke stroke = null;

        for (BaseShape item : group.shapes()) {
            if (item instanceof Transform) {
                transform = (Transform) item;
            } else if (item instanceof Fill) {
                fill = (Fill) item;
            } else if (item instanceof Stroke) {
                stroke = (Stroke) item;
            }
        }

        // Apply group transform if it exists
        if (transform != null) {
            applyTransform(gc, transform, frame);
        }

        // Render shapes with the found styles
        for (BaseShape item : group.shapes()) {
            if (item instanceof Rectangle rectangle) {
                renderRectangleWithStyle(gc, rectangle, fill, stroke, frame);
            }
            // Add other shape types here as needed
        }

        gc.restore();
    }

    @Override
    public Class<Group> getShapeType() {
        return Group.class;
    }

    private void renderRectangleWithStyle(GraphicsContext gc, Rectangle rectangle, Fill fill, Stroke stroke, double frame) {
        logger.info("Rendering rectangle with styles");

        if (rectangle.size() == null || rectangle.position() == null) {
            logger.warning("Rectangle missing size or position data");
            return;
        }

        // Get animated values at current frame
        double x = rectangle.position().getValue(Animated.ValueType.X, (long) frame);
        double y = rectangle.position().getValue(Animated.ValueType.Y, (long) frame);
        double width = rectangle.size().getValue(Animated.ValueType.WIDTH, (long) frame);
        double height = rectangle.size().getValue(Animated.ValueType.HEIGHT, (long) frame);

        logger.info("Rectangle dimensions: x=" + x + ", y=" + y + ", w=" + width + ", h=" + height);

        // Rectangle position in Lottie is center-based
        double rectX = x - width / 2;
        double rectY = y - height / 2;

        logger.info("Drawing rectangle at: " + rectX + ", " + rectY);

        // Apply fill if available
        if (fill != null && fill.color() != null) {
            double[] colorValues = new double[]{0.1, 0.5, 0.9};
            // TODO fill.color().getValue(Animated.ValueType.COLOR, (long) frame);
            Color fillColor = Color.color(colorValues[0], colorValues[1], colorValues[2]);
            double opacity = fill.opacity() != null ?
                    fill.opacity().getValue(Animated.ValueType.OPACITY, (long) frame) / 100.0 : 1.0;

            logger.info("Using fill color: " + fillColor + " with opacity: " + opacity);
            gc.setFill(fillColor.deriveColor(0, 1, 1, opacity));
            gc.fillRect(rectX, rectY, width, height);
        } else {
            // Use default fill for debugging
            gc.setFill(Color.HOTPINK);
            gc.fillRect(rectX, rectY, width, height);
        }

        // Apply stroke if available
        if (stroke != null && stroke.color() != null) {
            double[] colorValues = new double[]{0.1, 0.5, 0.9};
            // TODO stroke.color().getValue(Animated.ValueType.COLOR, (long) frame);
            Color strokeColor = Color.color(colorValues[0], colorValues[1], colorValues[2]);
            double opacity = stroke.opacity() != null ?
                    stroke.opacity().getValue(Animated.ValueType.OPACITY, (long) frame) / 100.0 : 1.0;
            double strokeWidth = stroke.strokeWidth() != null ?
                    stroke.strokeWidth().getValue(0, (long) frame) : 1.0;

            logger.info("Using stroke color: " + strokeColor + " with width: " + strokeWidth);
            gc.setStroke(strokeColor.deriveColor(0, 1, 1, opacity));
            gc.setLineWidth(strokeWidth);
            gc.strokeRect(rectX, rectY, width, height);
        } else {
            // Use default stroke for debugging
            gc.setStroke(Color.DARKBLUE);
            gc.setLineWidth(2);
            gc.strokeRect(rectX, rectY, width, height);
        }
    }

    private void applyTransform(GraphicsContext gc, Transform transform, double frame) {
        // Apply position
        if (transform.position() != null) {
            double x = transform.position().getValue(Animated.ValueType.X, (long) frame);
            double y = transform.position().getValue(Animated.ValueType.Y, (long) frame);
            logger.info("Group transform position: " + x + ", " + y);
            gc.translate(x, y);
        }

        // Apply anchor point offset (note: it's anchor(), not anchorPoint())
        if (transform.anchor() != null) {
            double ax = transform.anchor().getValue(Animated.ValueType.X, (long) frame);
            double ay = transform.anchor().getValue(Animated.ValueType.Y, (long) frame);
            logger.info("Group transform anchor: " + ax + ", " + ay);
            gc.translate(-ax, -ay);
        }

        // Apply scale
        if (transform.scale() != null) {
            double scaleX = transform.scale().getValue(Animated.ValueType.X, (long) frame) / 100.0;
            double scaleY = transform.scale().getValue(Animated.ValueType.Y, (long) frame) / 100.0;
            logger.info("Group transform scale: " + scaleX + ", " + scaleY);
            gc.scale(scaleX, scaleY);
        }

        // Apply rotation
        if (transform.rotation() != null) {
            double rotation = Math.toRadians(transform.rotation().getValue(0, (long) frame));
            logger.info("Group transform rotation: " + Math.toDegrees(rotation) + " degrees");
            gc.rotate(rotation);
        }

        // Apply opacity
        if (transform.opacity() != null) {
            double opacity = transform.opacity().getValue(Animated.ValueType.OPACITY, (long) frame);
            logger.info("Group transform opacity: " + opacity);
            gc.setGlobalAlpha(gc.getGlobalAlpha() * (opacity / 100.0));
        }
    }
}