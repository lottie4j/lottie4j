package com.lottie4j.fxplayer.renderer.shape;

import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.bezier.BezierDefinition;
import com.lottie4j.core.model.bezier.FixedBezier;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.shape.Path;
import com.lottie4j.core.model.shape.style.Fill;
import com.lottie4j.core.model.shape.style.GradientFill;
import com.lottie4j.fxplayer.element.FillStyle;
import com.lottie4j.fxplayer.element.GradientFillStyle;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Renders Lottie path geometry and fill, and delegates stroke/trim rendering
 * to {@link PathStrokeRenderer}.
 */
public class PathRenderer implements ShapeRenderer {

    private static final Logger logger = LoggerFactory.getLogger(PathRenderer.class);
    private final PathStrokeRenderer pathStrokeRenderer = new PathStrokeRenderer();
    private final PathBezierInterpolator bezierInterpolator = new PathBezierInterpolator();

    /**
     * Builds the JavaFX path for the current frame, applies fill styles,
     * then delegates stroke and trim-path rendering.
     */
    @Override
    public void render(GraphicsContext gc, BaseShape shape, Group parentGroup, double frame) {
        if (!(shape instanceof Path path)) {
            logger.warn("PathRenderer called with non-Path shape: {}", shape.getClass().getSimpleName());
            return;
        }

        if (path.bezier() == null) return;

        BezierDefinition bezierDef = getBezierDefinition(path, frame);
        if (bezierDef == null || bezierDef.vertices() == null || bezierDef.vertices().isEmpty()) return;

        gc.save();
        gc.beginPath();

        List<List<Double>> vertices = bezierDef.vertices();
        List<List<Double>> tangentsIn = bezierDef.tangentsIn();
        List<List<Double>> tangentsOut = bezierDef.tangentsOut();

        logger.debug("Path '{}' - vertices: {}, closed: {}", path.name(), vertices.size(), bezierDef.closed());
        if (!vertices.isEmpty()) {
            logger.debug("  First vertex: {}", vertices.get(0));
        }

        boolean first = true;

        for (int i = 0; i < vertices.size(); i++) {
            List<Double> vertex = vertices.get(i);
            if (vertex.size() < 2) continue;

            double x = vertex.get(0);
            double y = vertex.get(1);

            if (first) {
                gc.moveTo(x, y);
                first = false;
            } else {
                // Handle bezier curves if tangents are available
                if (tangentsIn != null && tangentsOut != null &&
                        i - 1 < tangentsOut.size() && i < tangentsIn.size()) {

                    List<Double> prevTangentOut = tangentsOut.get(i - 1);
                    List<Double> currentTangentIn = tangentsIn.get(i);

                    if (prevTangentOut.size() >= 2 && currentTangentIn.size() >= 2) {
                        List<Double> prevVertex = vertices.get(i - 1);

                        double cp1x = prevVertex.get(0) + prevTangentOut.get(0);
                        double cp1y = prevVertex.get(1) + prevTangentOut.get(1);
                        double cp2x = x + currentTangentIn.get(0);
                        double cp2y = y + currentTangentIn.get(1);

                        gc.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y);
                    } else {
                        gc.lineTo(x, y);
                    }
                } else {
                    gc.lineTo(x, y);
                }
            }
        }

        // Handle closing bezier curve from last vertex back to first vertex
        if (bezierDef.closed() != null && bezierDef.closed() && vertices.size() > 1) {
            int lastIdx = vertices.size() - 1;

            if (tangentsIn != null && tangentsOut != null &&
                    lastIdx < tangentsOut.size() && 0 < tangentsIn.size()) {

                List<Double> lastTangentOut = tangentsOut.get(lastIdx);
                List<Double> firstTangentIn = tangentsIn.get(0);

                if (lastTangentOut.size() >= 2 && firstTangentIn.size() >= 2) {
                    List<Double> lastVertex = vertices.get(lastIdx);
                    List<Double> firstVertex = vertices.get(0);

                    double cp1x = lastVertex.get(0) + lastTangentOut.get(0);
                    double cp1y = lastVertex.get(1) + lastTangentOut.get(1);
                    double cp2x = firstVertex.get(0) + firstTangentIn.get(0);
                    double cp2y = firstVertex.get(1) + firstTangentIn.get(1);

                    gc.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, firstVertex.get(0), firstVertex.get(1));
                }
            }

            // Always close the path - JavaFX needs this to properly render the path
            gc.closePath();
        }

        // Apply fill and stroke from parent group
        // Check for gradient fill first, then regular fill
        var gradientFillStyle = getGradientFillStyle(parentGroup);
        if (gradientFillStyle.isPresent()) {
            Paint gradientPaint = gradientFillStyle.get().getPaint(frame);
            gc.setFill(gradientPaint);
            double opacity = gradientFillStyle.get().getOpacity(frame);
            logger.debug("  Applying gradient fill, opacity: {}", opacity);
            if (opacity < 1.0) {
                double currentAlpha = gc.getGlobalAlpha();
                gc.setGlobalAlpha(currentAlpha * opacity);
            }
            gc.fill();
            gc.setGlobalAlpha(1.0); // Reset
        } else {
            var fillStyle = getFillStyle(parentGroup);
            if (fillStyle.isPresent()) {
                var fillColor = fillStyle.get().getColor(frame);
                logger.debug("  Applying fill: {}", fillColor);
                gc.setFill(fillColor);
                gc.fill();
            } else {
                logger.debug("  No fill style found");
            }
        }

        // Delegate all stroke and trim-path rendering to dedicated collaborator.
        pathStrokeRenderer.renderStroke(gc, parentGroup, frame, path.name(), vertices, tangentsIn, tangentsOut, bezierDef.closed());

        gc.restore();
    }

    /**
     * Returns solid fill style from the parent group if present.
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
     * Returns gradient fill style from the parent group if present.
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
     * Resolves either fixed or interpolated Bezier definition for the frame.
     */
    private BezierDefinition getBezierDefinition(Path shape, double frame) {
        if (shape.bezier() instanceof FixedBezier fixedBezier) {
            return fixedBezier.bezier();
        } else if (shape.bezier() instanceof AnimatedBezier animatedBezier) {
            return bezierInterpolator.getInterpolatedBezier(animatedBezier, frame);
        }
        return null;
    }
}
