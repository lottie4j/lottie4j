package com.lottie4j.fxplayer.renderer.shape;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Renders Lottie path geometry and fill, and delegates stroke/trim rendering
 * to {@link PathStrokeRenderer}.
 */
public class PathRenderer implements ShapeRenderer {

    private static final Logger logger = LoggerFactory.getLogger(PathRenderer.class);
    private final PathStrokeRenderer pathStrokeRenderer = new PathStrokeRenderer();
    private final PathBezierInterpolator bezierInterpolator = new PathBezierInterpolator();

    /**
     * Creates a new PathRenderer.
     */
    public PathRenderer() {
        // Constructor for PathRenderer
    }

    /**
     * Builds the JavaFX path for the current frame, applies fill styles,
     * then delegates stroke and trim-path rendering.
     *
     * @param gc          graphics context
     * @param shape       path shape to render
     * @param parentGroup parent group containing styles
     * @param frame       animation frame
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
        logger.debug("  First vertex: {}", vertices.get(0));

        buildPathGeometry(gc, vertices, tangentsIn, tangentsOut);
        closePathIfNeeded(gc, bezierDef.closed(), vertices, tangentsIn, tangentsOut);

        // Calculate bounding box for gradient coordinate transformation. The tight cubic-bezier
        // bounds (vertices plus the reach of their control handles) are required so the gradient
        // axis covers the actual painted silhouette — using vertex-only bounds under-sizes the
        // gradient for shapes whose bezier handles bulge beyond the vertex hull (typical for
        // smooth emoji bodies), clipping the end stops and leaving warm-colour edges.
        double[] bounds = calculateGeometryBounds(vertices, tangentsIn, tangentsOut, bezierDef.closed());
        applyFill(gc, parentGroup, frame, bounds);

        // Delegate all stroke and trim-path rendering to dedicated collaborator.
        pathStrokeRenderer.renderStroke(gc, parentGroup, frame, path.name(), vertices, tangentsIn, tangentsOut, bezierDef.closed());

        gc.restore();
    }

    /**
     * Builds the path geometry by adding vertices and Bezier curves to the graphics context.
     *
     * @param gc          graphics context
     * @param vertices    path vertices
     * @param tangentsIn  incoming tangent vectors
     * @param tangentsOut outgoing tangent vectors
     */
    private void buildPathGeometry(GraphicsContext gc,
                                   List<List<Double>> vertices,
                                   List<List<Double>> tangentsIn,
                                   List<List<Double>> tangentsOut) {
        boolean first = true;

        for (int i = 0; i < vertices.size(); i++) {
            List<Double> vertex = vertices.get(i);
            if (vertex.size() < 2) {
                continue;
            }

            double x = vertex.get(0);
            double y = vertex.get(1);

            if (first) {
                gc.moveTo(x, y);
                first = false;
                continue;
            }

            if (tangentsIn != null && tangentsOut != null && i - 1 < tangentsOut.size() && i < tangentsIn.size()) {
                List<Double> prevTangentOut = tangentsOut.get(i - 1);
                List<Double> currentTangentIn = tangentsIn.get(i);

                if (prevTangentOut.size() >= 2 && currentTangentIn.size() >= 2) {
                    List<Double> prevVertex = vertices.get(i - 1);
                    double cp1x = prevVertex.get(0) + prevTangentOut.get(0);
                    double cp1y = prevVertex.get(1) + prevTangentOut.get(1);
                    double cp2x = x + currentTangentIn.get(0);
                    double cp2y = y + currentTangentIn.get(1);
                    gc.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y);
                    continue;
                }
            }

            gc.lineTo(x, y);
        }
    }

    /**
     * Closes the path with a Bezier curve if marked as closed.
     *
     * @param gc          graphics context
     * @param closed      whether the path should be closed
     * @param vertices    path vertices
     * @param tangentsIn  incoming tangent vectors
     * @param tangentsOut outgoing tangent vectors
     */
    private void closePathIfNeeded(GraphicsContext gc,
                                   Boolean closed,
                                   List<List<Double>> vertices,
                                   List<List<Double>> tangentsIn,
                                   List<List<Double>> tangentsOut) {
        if (!Boolean.TRUE.equals(closed) || vertices.size() <= 1) {
            return;
        }

        int lastIdx = vertices.size() - 1;
        if (tangentsIn != null && tangentsOut != null && lastIdx < tangentsOut.size() && !tangentsIn.isEmpty()) {
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

        gc.closePath();
    }

    /**
     * Applies fill color or gradient to the current path.
     *
     * @param gc          graphics context
     * @param parentGroup parent group containing fill styles
     * @param frame       animation frame
     * @param bounds      bounding box for gradient coordinate transformation
     */
    private void applyFill(GraphicsContext gc, Group parentGroup, double frame, double[] bounds) {
        var gradientFillStyle = getGradientFillStyle(parentGroup);
        if (gradientFillStyle.isPresent()) {
            Paint gradientPaint;
            if (bounds != null && bounds.length == 4) {
                // bounds = [minX, minY, width, height]
                gradientPaint = gradientFillStyle.get().getPaint(frame, bounds[0], bounds[1], bounds[2], bounds[3]);
            } else {
                gradientPaint = gradientFillStyle.get().getPaint(frame);
            }
            gc.setFill(gradientPaint);
            double opacity = gradientFillStyle.get().getOpacity(frame);
            logger.debug("  Applying gradient fill, opacity: {}", opacity);
            double currentAlpha = gc.getGlobalAlpha();
            if (opacity < 1.0) {
                gc.setGlobalAlpha(currentAlpha * opacity);
            }
            gc.fill();
            // Restore previous alpha
            gc.setGlobalAlpha(currentAlpha);
            return;
        }

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


    /**
     * Returns solid fill style from the parent group if present.
     *
     * @param group parent group to search
     * @return optional fill style
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
     *
     * @param group parent group to search
     * @return optional gradient fill style
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
     *
     * @param shape path shape containing bezier data
     * @param frame animation frame
     * @return bezier definition for rendering
     */
    private BezierDefinition getBezierDefinition(Path shape, double frame) {
        if (shape.bezier() instanceof FixedBezier fixedBezier) {
            return fixedBezier.bezier();
        } else if (shape.bezier() instanceof AnimatedBezier animatedBezier) {
            return bezierInterpolator.getInterpolatedBezier(animatedBezier, frame);
        }
        return null;
    }

    /**
     * Calculates a bounding box that uses only the path vertices.
     *
     * <p>This is the cheap fallback used when tangent lists are absent or shorter than the
     * vertex list. For most shapes the gradient axis should use
     * {@link #calculateGeometryBounds(List, List, List, Boolean)} instead so the cubic-bezier
     * reach is accounted for.
     *
     * @param vertices list of vertex coordinates
     * @return array containing {@code [minX, minY, width, height]}, or {@code null} if vertices
     *         are empty
     */
    static double[] calculateVertexBounds(List<List<Double>> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            return null;
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (List<Double> vertex : vertices) {
            if (vertex.size() >= 2) {
                double x = vertex.get(0);
                double y = vertex.get(1);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        if (minX == Double.MAX_VALUE) {
            return null;
        }

        return new double[]{minX, minY, maxX - minX, maxY - minY};
    }

    /**
     * Calculates a tight bounding box around the actual cubic-bezier path that JavaFX paints.
     *
     * <p>Lottie defines each segment between vertices {@code V[i]} and {@code V[i+1]} as a cubic
     * curve with control points {@code P1 = V[i] + tangentsOut[i]} and
     * {@code P2 = V[i+1] + tangentsIn[i+1]}. The painted silhouette can bulge well outside the
     * polygonal hull of the vertices, especially for smooth shapes such as emoji bodies — so a
     * gradient axis that uses the vertex-hull bounds clips its end stops on those bulges and
     * leaves a thin band of flat end-stop colour at the top/bottom edges.
     *
     * <p>For each segment this method evaluates {@code B(t)} at the segment endpoints plus the
     * roots of {@code B'(t) = 0} (a quadratic in {@code t}), per axis. Closed paths additionally
     * include the implicit closing segment from the last vertex back to the first.
     *
     * <p>Falls back to {@link #calculateVertexBounds(List)} when tangent data is missing or any
     * segment lacks the expected pair of components.
     *
     * @param vertices    path vertices in Lottie composition space
     * @param tangentsIn  per-vertex tangent vectors pointing into each vertex
     * @param tangentsOut per-vertex tangent vectors pointing out of each vertex
     * @param closed      {@code Boolean.TRUE} if the implicit closing segment should be included
     * @return array containing {@code [minX, minY, width, height]}, or {@code null} if vertices
     *         are empty
     */
    static double[] calculateGeometryBounds(List<List<Double>> vertices,
                                            List<List<Double>> tangentsIn,
                                            List<List<Double>> tangentsOut,
                                            Boolean closed) {
        if (vertices == null || vertices.isEmpty()) {
            return null;
        }
        if (tangentsIn == null || tangentsOut == null
                || tangentsIn.size() < vertices.size()
                || tangentsOut.size() < vertices.size()) {
            return calculateVertexBounds(vertices);
        }

        double[] acc = new double[]{Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        boolean any = false;

        for (int i = 0; i < vertices.size() - 1; i++) {
            if (!accumulateCubicSegment(acc, vertices.get(i), tangentsOut.get(i),
                    vertices.get(i + 1), tangentsIn.get(i + 1))) {
                return calculateVertexBounds(vertices);
            }
            any = true;
        }

        if (Boolean.TRUE.equals(closed) && vertices.size() > 1) {
            int last = vertices.size() - 1;
            if (!accumulateCubicSegment(acc, vertices.get(last), tangentsOut.get(last),
                    vertices.get(0), tangentsIn.get(0))) {
                return calculateVertexBounds(vertices);
            }
            any = true;
        } else if (vertices.size() == 1) {
            List<Double> only = vertices.get(0);
            if (only.size() < 2) {
                return null;
            }
            double x = only.get(0);
            double y = only.get(1);
            acc[0] = x;
            acc[1] = y;
            acc[2] = x;
            acc[3] = y;
            any = true;
        }

        if (!any || acc[0] == Double.MAX_VALUE) {
            return calculateVertexBounds(vertices);
        }

        return new double[]{acc[0], acc[1], acc[2] - acc[0], acc[3] - acc[1]};
    }

    /**
     * Adds the analytical bounding box of one cubic-bezier segment into the running accumulator.
     *
     * @param acc        running {@code [minX, minY, maxX, maxY]} accumulator
     * @param v0         segment start vertex
     * @param outFromV0  outgoing tangent at {@code v0} (added to {@code v0} to get the first
     *                   control point)
     * @param v1         segment end vertex
     * @param inToV1     incoming tangent at {@code v1} (added to {@code v1} to get the second
     *                   control point)
     * @return {@code true} when both endpoints had at least two coordinates and the segment was
     *         processed, {@code false} when the caller should fall back to vertex-only bounds
     */
    private static boolean accumulateCubicSegment(double[] acc,
                                                  List<Double> v0, List<Double> outFromV0,
                                                  List<Double> v1, List<Double> inToV1) {
        if (v0 == null || v1 == null || v0.size() < 2 || v1.size() < 2) {
            return false;
        }
        double v0x = v0.get(0);
        double v0y = v0.get(1);
        double v1x = v1.get(0);
        double v1y = v1.get(1);

        double outX = (outFromV0 != null && outFromV0.size() >= 2) ? outFromV0.get(0) : 0.0;
        double outY = (outFromV0 != null && outFromV0.size() >= 2) ? outFromV0.get(1) : 0.0;
        double inX = (inToV1 != null && inToV1.size() >= 2) ? inToV1.get(0) : 0.0;
        double inY = (inToV1 != null && inToV1.size() >= 2) ? inToV1.get(1) : 0.0;

        double p1x = v0x + outX;
        double p1y = v0y + outY;
        double p2x = v1x + inX;
        double p2y = v1y + inY;

        addCubicExtrema(v0x, p1x, p2x, v1x, acc, true);
        addCubicExtrema(v0y, p1y, p2y, v1y, acc, false);
        return true;
    }

    /**
     * Records the endpoints and any in-range derivative roots of a single axis of a cubic bezier
     * into the bounds accumulator.
     *
     * <p>For {@code B(t) = (1-t)^3 v0 + 3 (1-t)^2 t c1 + 3 (1-t) t^2 c2 + t^3 v3}, the derivative
     * {@code B'(t)} is a quadratic in {@code t}; its real roots in {@code (0, 1)} correspond to
     * the segment's extrema. Endpoints {@code t=0} and {@code t=1} are always included.
     *
     * @param v0     segment start coordinate on this axis
     * @param c1     first control coordinate on this axis
     * @param c2     second control coordinate on this axis
     * @param v3     segment end coordinate on this axis
     * @param acc    running {@code [minX, minY, maxX, maxY]} accumulator
     * @param xAxis  {@code true} for the X axis (indices 0/2), {@code false} for Y (indices 1/3)
     */
    private static void addCubicExtrema(double v0, double c1, double c2, double v3,
                                        double[] acc, boolean xAxis) {
        int minIdx = xAxis ? 0 : 1;
        int maxIdx = xAxis ? 2 : 3;

        if (v0 < acc[minIdx]) acc[minIdx] = v0;
        if (v0 > acc[maxIdx]) acc[maxIdx] = v0;
        if (v3 < acc[minIdx]) acc[minIdx] = v3;
        if (v3 > acc[maxIdx]) acc[maxIdx] = v3;

        // B'(t) = 3 * [ (1-t)^2 (c1-v0) + 2 (1-t) t (c2-c1) + t^2 (v3-c2) ]
        // Expanding the bracket as A t^2 + B t + C with:
        //   A = (v3 - v0) - 3 (c2 - c1)
        //   B = 2 ((c2 - c1) - (c1 - v0)) * ? — derive directly below.
        double a = -v0 + 3.0 * c1 - 3.0 * c2 + v3;
        double b = 2.0 * (v0 - 2.0 * c1 + c2);
        double c = c1 - v0;

        double eps = 1e-12;
        if (Math.abs(a) < eps) {
            if (Math.abs(b) < eps) {
                return;
            }
            double t = -c / b;
            evaluateAt(t, v0, c1, c2, v3, acc, minIdx, maxIdx);
            return;
        }

        double disc = b * b - 4.0 * a * c;
        if (disc < 0.0) {
            return;
        }
        double sqrtDisc = Math.sqrt(disc);
        double t1 = (-b + sqrtDisc) / (2.0 * a);
        double t2 = (-b - sqrtDisc) / (2.0 * a);
        evaluateAt(t1, v0, c1, c2, v3, acc, minIdx, maxIdx);
        evaluateAt(t2, v0, c1, c2, v3, acc, minIdx, maxIdx);
    }

    /**
     * Evaluates the cubic bezier at parameter {@code t} on a single axis and merges the result
     * into the bounds accumulator when {@code t} lies in {@code (0, 1)}.
     */
    private static void evaluateAt(double t, double v0, double c1, double c2, double v3,
                                   double[] acc, int minIdx, int maxIdx) {
        if (!(t > 0.0 && t < 1.0)) {
            return;
        }
        double mt = 1.0 - t;
        double value = mt * mt * mt * v0
                + 3.0 * mt * mt * t * c1
                + 3.0 * mt * t * t * c2
                + t * t * t * v3;
        if (value < acc[minIdx]) acc[minIdx] = value;
        if (value > acc[maxIdx]) acc[maxIdx] = value;
    }
    }
