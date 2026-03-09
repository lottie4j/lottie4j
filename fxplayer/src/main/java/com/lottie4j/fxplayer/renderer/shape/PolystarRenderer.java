package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.definition.StarType;
import com.lottie4j.core.model.AnimatedValueType;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.shape.Polystar;
import com.lottie4j.core.model.shape.style.Fill;
import com.lottie4j.core.model.shape.style.GradientFill;
import com.lottie4j.core.model.shape.style.Stroke;
import com.lottie4j.fxplayer.element.FillStyle;
import com.lottie4j.fxplayer.element.GradientFillStyle;
import com.lottie4j.fxplayer.element.StrokeStyle;
import com.lottie4j.fxplayer.util.StrokeHelper;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class PolystarRenderer implements ShapeRenderer {

    private static final Logger logger = LoggerFactory.getLogger(PolystarRenderer.class);

    /**
     * {@inheritDoc}
     * <p>
     * Renders a Lottie polystar (polygon or star) shape with fill and stroke support.
     */
    @Override
    public void render(GraphicsContext gc, BaseShape shape, Group parentGroup, double frame) {
        if (!(shape instanceof Polystar polystar)) {
            logger.warn("PolystarRenderer called with non-Polystar shape: {}", shape.getClass().getSimpleName());
            return;
        }

        if (polystar.points() == null) {
            logger.warn("Polystar has no points defined");
            return;
        }

        // Get animated values at current frame
        double centerX = polystar.position() != null ?
                polystar.position().getValue(AnimatedValueType.X, frame) : 0;
        double centerY = polystar.position() != null ?
                polystar.position().getValue(AnimatedValueType.Y, frame) : 0;

        double outerRadius = polystar.outerRadius() != null ?
                polystar.outerRadius().getValue(0, frame) : 50;

        double rotation = polystar.rotation() != null ?
                Math.toRadians(polystar.rotation().getValue(0, frame)) : 0;

        int numPoints = polystar.points() != null ?
                (int) Math.round(polystar.points().getValue(0, frame)) : 5;

        if (numPoints < 3) {
            logger.warn("Polystar must have at least 3 points, got: {}", numPoints);
            numPoints = 3;
        }

        StarType starType = polystar.starType() != null ? polystar.starType() : StarType.STAR;

        if (starType == StarType.STAR) {
            renderStar(gc, centerX, centerY, outerRadius, rotation, numPoints, polystar, frame, parentGroup);
        } else {
            renderPolygon(gc, centerX, centerY, outerRadius, rotation, numPoints, parentGroup, frame);
        }
    }

    /**
     * Renders a star shape with alternating outer and inner points.
     *
     * @param gc          graphics context
     * @param centerX     star center x
     * @param centerY     star center y
     * @param outerRadius outer point radius
     * @param rotation    rotation in radians
     * @param numPoints   number of star points
     * @param polystar    polystar shape definition
     * @param frame       animation frame
     * @param parentGroup parent group containing styles
     */
    private void renderStar(GraphicsContext gc, double centerX, double centerY,
                            double outerRadius, double rotation, int numPoints,
                            Polystar polystar, double frame, Group parentGroup) {

        double innerRadius = polystar.innerRadius() != null ?
                polystar.innerRadius().getValue(0, frame) : outerRadius * 0.5;

        double outerRoundness = polystar.outerRoundness() != null ?
                polystar.outerRoundness().getValue(0, frame) : 0;

        double innerRoundness = polystar.innerRoundness() != null ?
                polystar.innerRoundness().getValue(0, frame) : 0;

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

        drawPolygonPath(gc, xPoints, yPoints, parentGroup, frame);

        // If roundness is applied, use quadratic curves instead of straight lines
        if (outerRoundness > 0 || innerRoundness > 0) {
            // Implement quadratic curve drawing for rounded corners
            // Use gc.quadraticCurveTo() for rounded corners
            // TODO
        }
    }

    /**
     * Renders a regular polygon.
     *
     * @param gc          graphics context
     * @param centerX     polygon center x
     * @param centerY     polygon center y
     * @param radius      vertex radius
     * @param rotation    rotation in radians
     * @param numPoints   number of vertices
     * @param parentGroup parent group containing styles
     * @param frame       animation frame
     */
    private void renderPolygon(GraphicsContext gc, double centerX, double centerY,
                               double radius, double rotation, int numPoints,
                               Group parentGroup, double frame) {

        // Calculate points for regular polygon
        double[] xPoints = new double[numPoints];
        double[] yPoints = new double[numPoints];

        double angleStep = 2 * Math.PI / numPoints;

        for (int i = 0; i < numPoints; i++) {
            double angle = rotation + i * angleStep - Math.PI / 2; // Start from top
            xPoints[i] = centerX + radius * Math.cos(angle);
            yPoints[i] = centerY + radius * Math.sin(angle);
        }

        drawPolygonPath(gc, xPoints, yPoints, parentGroup, frame);
    }

    /**
     * Draws a polygon path and applies fill and stroke from parent group.
     *
     * @param gc          graphics context
     * @param xPoints     x-coordinates of vertices
     * @param yPoints     y-coordinates of vertices
     * @param parentGroup parent group containing styles
     * @param frame       animation frame
     */
    private void drawPolygonPath(GraphicsContext gc, double[] xPoints, double[] yPoints,
                                 Group parentGroup, double frame) {
        if (xPoints.length == 0) return;

        gc.beginPath();
        gc.moveTo(xPoints[0], yPoints[0]);

        for (int i = 1; i < xPoints.length; i++) {
            gc.lineTo(xPoints[i], yPoints[i]);
        }

        gc.closePath();

        // Apply fill and stroke from parent group
        // Check for gradient fill first, then regular fill
        var gradientFillStyle = getGradientFillStyle(parentGroup);
        if (gradientFillStyle.isPresent()) {
            Paint gradientPaint = gradientFillStyle.get().getPaint(frame);
            gc.setFill(gradientPaint);
            double opacity = gradientFillStyle.get().getOpacity(frame);
            if (opacity < 1.0) {
                double currentAlpha = gc.getGlobalAlpha();
                gc.setGlobalAlpha(currentAlpha * opacity);
            }
            gc.fill();
            gc.setGlobalAlpha(1.0); // Reset
        } else {
            var fillStyle = getFillStyle(parentGroup);
            if (fillStyle.isPresent()) {
                gc.setFill(fillStyle.get().getColor(frame));
                gc.fill();
            }
        }

        var strokeStyle = getStrokeStyle(parentGroup);
        if (strokeStyle.isPresent()) {
            var strokeWidth = strokeStyle.get().getStrokeWidth(frame);

            if (StrokeHelper.shouldRenderStroke(strokeWidth)) {
                double compensatedWidth = StrokeHelper.getCompensatedStrokeWidth(gc, strokeWidth);

                gc.setStroke(strokeStyle.get().getColor(frame));
                gc.setLineWidth(compensatedWidth);
                gc.stroke();
            }
        }
    }

    /**
     * Extracts fill style from parent group.
     *
     * @param group parent group containing styles
     * @return fill style if present
     */
    private Optional<FillStyle> getFillStyle(Group group) {
        if (group == null) {
            return Optional.empty();
        }
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof Fill fill) {
                return Optional.of(new FillStyle(fill));
            }
        }
        return Optional.empty();
    }

    /**
     * Extracts gradient fill style from parent group.
     *
     * @param group parent group containing styles
     * @return gradient fill style if present
     */
    private Optional<GradientFillStyle> getGradientFillStyle(Group group) {
        if (group == null) {
            return Optional.empty();
        }
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof GradientFill gradientFill) {
                return Optional.of(new GradientFillStyle(gradientFill));
            }
        }
        return Optional.empty();
    }

    /**
     * Extracts stroke style from parent group.
     *
     * @param group parent group containing styles
     * @return stroke style if present
     */
    private Optional<StrokeStyle> getStrokeStyle(Group group) {
        if (group == null) {
            return Optional.empty();
        }
        for (BaseShape baseShape : group.shapes()) {
            if (baseShape instanceof Stroke stroke) {
                return Optional.of(new StrokeStyle(stroke));
            }
        }
        return Optional.empty();
    }
}