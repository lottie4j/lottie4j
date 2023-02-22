package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.Animated.ValueType;
import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.Transform;
import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.bezier.BezierDefinition;
import com.lottie4j.core.model.bezier.FixedBezier;
import com.lottie4j.core.model.shape.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Optional;
import java.util.logging.Logger;

public class GroupDrawer {

    private final Logger logger = Logger.getLogger(GroupDrawer.class.getName());

    private final GraphicsContext gc;
    private final Group group;
    private final Layer layer;

    public GroupDrawer(GraphicsContext gc, Group group) {
        this(gc, group, null);
    }

    public GroupDrawer(GraphicsContext gc, Group group, Layer layer) {
        this.gc = gc;
        this.group = group;
        this.layer = layer;
        draw();
    }

    private void drawPath(GraphicsContext gc, Path path) {
        var fillStyle = getFillStyle();
        if (fillStyle.isPresent()) {
            gc.setFill(fillStyle.get().getColor(0));
        }
        var strokeStyle = getStrokeStyle();
        if (strokeStyle.isPresent()) {
            gc.setStroke(strokeStyle.get().getColor(0));
            gc.setLineWidth(strokeStyle.get().getStrokeWidth(0));
        }
        if (path.bezier() == null) {
            return;
        }
        if (path.bezier() instanceof FixedBezier fixedBezier) {
            if (fixedBezier.bezier() != null) {
                drawBezier(gc, fixedBezier.bezier());
            }
        } else if (path.bezier() instanceof AnimatedBezier animatedBezier) {
            if (animatedBezier.beziers() != null
                    && !animatedBezier.beziers().isEmpty()) {
                drawBezier(gc, animatedBezier.beziers().get(0).beziers().get(0));
            }
        }
    }

    private void drawBezier(GraphicsContext gc, BezierDefinition bezierDefinition) {
        var vertices = bezierDefinition.vertices();
        if (vertices.size() >= 2) {
            gc.moveTo(vertices.get(0).get(0) + getOffsetX(), vertices.get(0).get(1) + getOffsetY());
            for (int i = 1; i < vertices.size(); i++) {
                gc.lineTo(vertices.get(i).get(0) + getOffsetX(), vertices.get(i).get(1) + getOffsetY());
                gc.stroke();
            }
        }
    }

    private Double getOffsetX() {
        if (layer == null) {
            return 0D;
        }
        return layer.transform().position().getValue(ValueType.X, 0);
    }

    private Double getOffsetY() {
        if (layer == null) {
            return 0D;
        }
        return layer.transform().position().getValue(ValueType.X, 0);
    }

    private void setStroke(GraphicsContext gc, Group group, Long time) {
        for (BaseShape shape : group.shapes()) {
            if (shape instanceof Stroke stroke) {
                var strokeWidth = 1;
                /*
                var strokeWidthKeyframe = stroke.strokeWidth().getValue(ValueType.WIDTH, time);
                if (strokeWidthKeyframe instanceof NumberKeyframe numberKeyframe) {
                    strokeWidth = numberKeyframe.intValue();
                } else if (strokeWidthKeyframe instanceof TimedKeyframe timedKeyframe) {
                    strokeWidth = timedKeyframe.values() != null && !timedKeyframe.values().isEmpty() ? timedKeyframe.values().get(0).intValue() : 1;
                }
                */
                gc.setLineWidth(strokeWidth);
                gc.setStroke(Color.DARKVIOLET);
            }
        }
    }

    private void drawEllipse(GraphicsContext gc, Ellipse ellipse) {
        gc.setFill(Color.DARKKHAKI);
        /*graphicContext.fillOval(ellipse.position().keyframes().get(0).intValue(),
                ellipse.position().keyframes().get(1).intValue(),
                ellipse.size().keyframes().get(0).intValue(),
                ellipse.size().keyframes().get(1).intValue());*/
    }

    private void drawRectangle(GraphicsContext gc, Rectangle rectangle) {
        logger.info("   > Drawing Rectangle " + rectangle.name());
        var fillStyle = getFillStyle();
        if (fillStyle.isPresent()) {
            gc.setFill(fillStyle.get().getColor(0));
        }
        var strokeStyle = getStrokeStyle();
        if (strokeStyle.isPresent()) {
            gc.setStroke(strokeStyle.get().getColor(0));
            gc.setLineWidth(strokeStyle.get().getStrokeWidth(0));
        }
        gc.strokeRect(rectangle.position().getValue(ValueType.X, 0L),
                rectangle.position().getValue(ValueType.X, 0L),
                rectangle.size().getValue(ValueType.WIDTH, 0L),
                rectangle.size().getValue(ValueType.HEIGHT, 0L));
    }

    private Optional<FillStyle> getFillStyle() {
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof Fill fill) {
                return Optional.of(new FillStyle(fill));
            }
        }
        return Optional.empty();
    }

    private Optional<StrokeStyle> getStrokeStyle() {
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof Stroke stroke) {
                return Optional.of(new StrokeStyle(stroke));
            }
        }
        return Optional.empty();
    }

    private void drawTransform(GraphicsContext gc, Transform transform) {
    }

    private void drawFill(GraphicsContext gc, Fill fill) {
        //graphicContext.fillArc(11, 111, 31, 31, 46, 241, ArcType.OPEN);
    }

    private void drawStroke(GraphicsContext gc, Stroke stroke) {
        gc.setStroke(Color.DARKVIOLET);
        gc.setLineWidth(6);
        /*graphicContext.strokeOval(stroke.position().keyframes().get(0).intValue(),
                stroke.position().keyframes().get(1).intValue(),
                stroke.size().keyframes().get(0).intValue(),
                stroke.size().keyframes().get(1).intValue());*/
    }

    /**
     * A Lottie Group consists of a ShapeGroup.SHAPE object
     * with one or more ShapeGroup.STYLE and/or ShapeGroup.TRANSFORM.
     */
    private void draw() {
        logger.info("\t> Group " + group.name());
        for (BaseShape baseShape : group.shapes()) {
            logger.info("\t\t> Shape " + baseShape.getClass().getName());
            if (baseShape instanceof Group subGroup) {
                //draw(gc, subGroup);
            } else if (baseShape instanceof Ellipse ellipse) {
                drawEllipse(gc, ellipse);
            } else if (baseShape instanceof Rectangle rectangle) {
                drawRectangle(gc, rectangle);
            } else if (baseShape instanceof Path path) {
                drawPath(gc, path);
            }
        }
    }
}
