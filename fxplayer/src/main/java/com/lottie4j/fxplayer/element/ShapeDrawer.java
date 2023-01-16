package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.Transform;
import com.lottie4j.core.model.shape.Ellipse;
import com.lottie4j.core.model.shape.Fill;
import com.lottie4j.core.model.shape.Group;
import com.lottie4j.core.model.shape.Stroke;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ShapeDrawer {

    private ShapeDrawer() {
        // Hide constructor
    }

    public static void draw(GraphicsContext graphicContext, Object shape) {
        if (shape instanceof Group group) {
            group.shapes().forEach(s -> draw(graphicContext, s));
        } else if (shape instanceof Ellipse ellipse) {
            drawEllipse(graphicContext, ellipse);
        } else if (shape instanceof Stroke stroke) {
            drawStroke(graphicContext, stroke);
        }
    }

    private static void drawEllipse(GraphicsContext graphicContext, Ellipse ellipse) {
        graphicContext.setFill(Color.DARKKHAKI);
        /*graphicContext.fillOval(ellipse.position().keyframes().get(0).intValue(),
                ellipse.position().keyframes().get(1).intValue(),
                ellipse.size().keyframes().get(0).intValue(),
                ellipse.size().keyframes().get(1).intValue());*/
    }

    private static void drawTransform(GraphicsContext graphicContext, Transform transform) {
    }

    private static void drawFill(GraphicsContext graphicContext, Fill fill) {
        //graphicContext.fillArc(11, 111, 31, 31, 46, 241, ArcType.OPEN);
    }

    private static void drawStroke(GraphicsContext graphicContext, Stroke stroke) {
        graphicContext.setStroke(Color.DARKVIOLET);
        graphicContext.setLineWidth(6);
        /*graphicContext.strokeOval(stroke.position().keyframes().get(0).intValue(),
                stroke.position().keyframes().get(1).intValue(),
                stroke.size().keyframes().get(0).intValue(),
                stroke.size().keyframes().get(1).intValue());*/
    }
}
