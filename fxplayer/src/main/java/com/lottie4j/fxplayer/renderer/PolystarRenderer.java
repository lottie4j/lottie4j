package com.lottie4j.fxplayer.renderer;

import com.lottie4j.core.definition.StarType;
import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.Group;
import com.lottie4j.core.model.shape.Polystar;
import com.lottie4j.fxplayer.LottieRenderEngine;
import javafx.scene.canvas.GraphicsContext;

import java.util.logging.Logger;

public class PolystarRenderer implements ShapeRenderer<Polystar> {

    private static final Logger logger = Logger.getLogger(PolystarRenderer.class.getName());

    @Override
    public void render(LottieRenderEngine engine, GraphicsContext gc, Polystar polystar, Group parentGroup, double frame) {
        if (polystar.points() == null) {
            logger.warning("Polystar has no points defined");
            return;
        }

        // Get animated values at current frame
        double centerX = polystar.position() != null ?
                polystar.position().getValue(AnimatedValueType.X, (long) frame) : 0;
        double centerY = polystar.position() != null ?
                polystar.position().getValue(AnimatedValueType.Y, (long) frame) : 0;

        double outerRadius = polystar.outerRadius() != null ?
                polystar.outerRadius().getValue(0, (long) frame) : 50;

        double rotation = polystar.rotation() != null ?
                Math.toRadians(polystar.rotation().getValue(0, (long) frame)) : 0;

        int numPoints = polystar.points() != null ?
                (int) Math.round(polystar.points().getValue(0, (long) frame)) : 5;

        if (numPoints < 3) {
            logger.warning("Polystar must have at least 3 points, got: " + numPoints);
            numPoints = 3;
        }

        StarType starType = polystar.starType() != null ? polystar.starType() : StarType.STAR;

        if (starType == StarType.STAR) {
            renderStar(gc, centerX, centerY, outerRadius, rotation, numPoints, polystar, frame);
        } else {
            renderPolygon(gc, centerX, centerY, outerRadius, rotation, numPoints);
        }
    }

    @Override
    public Class<Polystar> getShapeType() {
        return Polystar.class;
    }

    private void renderStar(GraphicsContext gc, double centerX, double centerY,
                            double outerRadius, double rotation, int numPoints,
                            Polystar polystar, double frame) {

        double innerRadius = polystar.innerRadius() != null ?
                polystar.innerRadius().getValue(0, (long) frame) : outerRadius * 0.5;

        // Calculate points for star (alternating outer and inner points)
        double[] xPoints = new double[numPoints * 2];
        double[] yPoints = new double[numPoints * 2];

        double angleStep = 2 * Math.PI / numPoints;

        for (int i = 0; i < numPoints; i++) {
            // Outer point
            double outerAngle = rotation + i * angleStep - Math.PI / 2; // Start from top
            xPoints[i * 2] = centerX + outerRadius * Math.cos(outerAngle);
            yPoints[i * 2] = centerY + outerRadius * Math.sin(outerAngle);

            // Inner point
            double innerAngle = rotation + (i + 0.5) * angleStep - Math.PI / 2;
            xPoints[i * 2 + 1] = centerX + innerRadius * Math.cos(innerAngle);
            yPoints[i * 2 + 1] = centerY + innerRadius * Math.sin(innerAngle);
        }

        drawPolygonPath(gc, xPoints, yPoints);
    }

    private void renderPolygon(GraphicsContext gc, double centerX, double centerY,
                               double radius, double rotation, int numPoints) {

        // Calculate points for regular polygon
        double[] xPoints = new double[numPoints];
        double[] yPoints = new double[numPoints];

        double angleStep = 2 * Math.PI / numPoints;

        for (int i = 0; i < numPoints; i++) {
            double angle = rotation + i * angleStep - Math.PI / 2; // Start from top
            xPoints[i] = centerX + radius * Math.cos(angle);
            yPoints[i] = centerY + radius * Math.sin(angle);
        }

        drawPolygonPath(gc, xPoints, yPoints);
    }

    private void drawPolygonPath(GraphicsContext gc, double[] xPoints, double[] yPoints) {
        if (xPoints.length == 0) return;

        gc.beginPath();
        gc.moveTo(xPoints[0], yPoints[0]);

        for (int i = 1; i < xPoints.length; i++) {
            gc.lineTo(xPoints[i], yPoints[i]);
        }

        gc.closePath();

        // Fill and stroke the path
        gc.fill();
        gc.stroke();
    }
}