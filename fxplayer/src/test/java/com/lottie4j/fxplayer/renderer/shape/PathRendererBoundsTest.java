package com.lottie4j.fxplayer.renderer.shape;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PathRenderer#calculateGeometryBounds} and the vertex-bounds fallback.
 *
 * <p>These tests exercise the bezier extrema math directly without needing JavaFX so they can
 * run on any platform.</p>
 */
class PathRendererBoundsTest {

    /**
     * Standard cubic-bezier control-point fraction that approximates a circular arc.
     * Four cubic segments using this handle length trace a near-perfect circle.
     */
    private static final double KAPPA = 0.5522847498307933;

    private static List<Double> point(double x, double y) {
        return List.of(x, y);
    }

    private static List<List<Double>> zeroTangents(int count) {
        List<List<Double>> tangents = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            tangents.add(point(0.0, 0.0));
        }
        return tangents;
    }

    @Test
    void emptyVerticesReturnNull() {
        assertNull(PathRenderer.calculateGeometryBounds(List.of(), List.of(), List.of(), Boolean.FALSE));
        assertNull(PathRenderer.calculateVertexBounds(List.of()));
    }

    @Test
    void singleStraightSegmentEqualsVertexBounds() {
        // Open path, two endpoints, zero tangents — bezier degenerates to a straight line so
        // the analytical bounds must match the vertex bounds exactly.
        List<List<Double>> vertices = List.of(point(10.0, 20.0), point(40.0, 60.0));
        List<List<Double>> tangents = zeroTangents(2);

        double[] bounds = PathRenderer.calculateGeometryBounds(vertices, tangents, tangents, Boolean.FALSE);
        assertNotNull(bounds);
        assertEquals(10.0, bounds[0], 1e-9);
        assertEquals(20.0, bounds[1], 1e-9);
        assertEquals(30.0, bounds[2], 1e-9);
        assertEquals(40.0, bounds[3], 1e-9);
    }

    @Test
    void symmetricCircleMatchesInscribedBoundingSquare() {
        // Approximate a unit circle centered at (10, 10) with radius 5 using four cubic
        // segments. Vertices sit on the axis-aligned cardinal points so the vertex bounds are
        // already the correct bounding square — the test asserts that the full-extent bounds
        // match it to high precision (i.e. the bezier handles do not push the bounds outside
        // the inscribed square).
        double cx = 10.0;
        double cy = 10.0;
        double r = 5.0;
        double h = KAPPA * r;

        List<List<Double>> vertices = List.of(
                point(cx + r, cy),     // right
                point(cx, cy + r),     // bottom
                point(cx - r, cy),     // left
                point(cx, cy - r)      // top
        );

        // tangentOut at each vertex points along the next-quarter tangent direction.
        List<List<Double>> tangentsOut = List.of(
                point(0.0, h),         // right vertex: tangent goes down
                point(-h, 0.0),        // bottom vertex: tangent goes left
                point(0.0, -h),        // left vertex: tangent goes up
                point(h, 0.0)          // top vertex: tangent goes right
        );
        // tangentIn at each vertex points opposite the incoming-tangent direction.
        List<List<Double>> tangentsIn = List.of(
                point(0.0, -h),        // right: incoming from top
                point(h, 0.0),         // bottom: incoming from right
                point(0.0, h),         // left: incoming from bottom
                point(-h, 0.0)         // top: incoming from left
        );

        double[] bounds = PathRenderer.calculateGeometryBounds(vertices, tangentsIn, tangentsOut, Boolean.TRUE);
        assertNotNull(bounds);
        assertEquals(cx - r, bounds[0], 1e-6);
        assertEquals(cy - r, bounds[1], 1e-6);
        assertEquals(2 * r, bounds[2], 1e-6);
        assertEquals(2 * r, bounds[3], 1e-6);
    }

    @Test
    void tangentExtendingAboveVertexHullExpandsBoundsUpward() {
        // Two-vertex open path. The outgoing tangent at vertex 0 pushes the first control
        // point well above the vertex Y range. The analytical extremum on the Y axis must lie
        // strictly above the smaller vertex Y value.
        // V0 = (0, 100), V1 = (100, 100), out0 = (0, -200), in1 = (0, 0)
        // Cubic Y(t) for the Y axis:
        //   v0=100, c1=100+(-200)=-100, c2=100+0=100, v3=100
        //   B'(t) zeros at t* per the quadratic derived in addCubicExtrema.
        // We don't hand-compute t* numerically here; we assert the extremum is below 100
        // (the vertex floor) and matches the analytical formula by comparison with the
        // direct Bezier evaluation at that t.
        List<List<Double>> vertices = List.of(point(0.0, 100.0), point(100.0, 100.0));
        List<List<Double>> tangentsOut = List.of(point(0.0, -200.0), point(0.0, 0.0));
        List<List<Double>> tangentsIn = List.of(point(0.0, 0.0), point(0.0, 0.0));

        double[] bounds = PathRenderer.calculateGeometryBounds(vertices, tangentsIn, tangentsOut, Boolean.FALSE);
        assertNotNull(bounds);
        // Expect Y min strictly less than 100 (vertex min) — the bezier bulges upward
        // (in screen Y) because the first control point has Y = -100.
        assertTrue(bounds[1] < 100.0,
                "Y min should be below the vertex Y due to the tangent reach, got " + bounds[1]);

        // Compute the analytical extremum for Y by solving the quadratic from
        // addCubicExtrema. v0=100, c1=-100, c2=100, v3=100.
        //   a = -v0 + 3 c1 - 3 c2 + v3 = -100 - 300 - 300 + 100 = -600
        //   b = 2 (v0 - 2 c1 + c2)     = 2 (100 + 200 + 100) = 800
        //   c = c1 - v0                = -200
        // Roots of -600 t^2 + 800 t - 200 = 0 → 3 t^2 - 4 t + 1 = 0 → t = 1 or t = 1/3.
        // t = 1 is the endpoint; t = 1/3 is the extremum we want.
        double t = 1.0 / 3.0;
        double mt = 1.0 - t;
        double yAtT = mt * mt * mt * 100.0 + 3 * mt * mt * t * (-100.0) + 3 * mt * t * t * 100.0 + t * t * t * 100.0;
        assertEquals(yAtT, bounds[1], 1e-9);
    }

    @Test
    void fallsBackToVertexBoundsWhenTangentListIsShort() {
        // Tangent lists shorter than the vertex list must trip the fallback so we never
        // dereference past the end of the list.
        List<List<Double>> vertices = List.of(point(0.0, 0.0), point(10.0, 20.0));
        List<List<Double>> shortTangents = List.of(point(0.0, 0.0));

        double[] bounds = PathRenderer.calculateGeometryBounds(vertices, shortTangents, shortTangents, Boolean.FALSE);
        assertNotNull(bounds);
        assertEquals(0.0, bounds[0], 1e-9);
        assertEquals(0.0, bounds[1], 1e-9);
        assertEquals(10.0, bounds[2], 1e-9);
        assertEquals(20.0, bounds[3], 1e-9);
    }

    @Test
    void fallsBackToVertexBoundsWhenTangentsAreNull() {
        List<List<Double>> vertices = List.of(point(0.0, 0.0), point(10.0, 20.0));
        double[] bounds = PathRenderer.calculateGeometryBounds(vertices, null, null, Boolean.FALSE);
        assertNotNull(bounds);
        assertEquals(0.0, bounds[0], 1e-9);
        assertEquals(0.0, bounds[1], 1e-9);
        assertEquals(10.0, bounds[2], 1e-9);
        assertEquals(20.0, bounds[3], 1e-9);
    }

    @Test
    void closedFlagControlsClosingSegmentContribution() {
        // Two vertices on the Y=0 line, no segment tangents.
        // The implicit closing segment is also a flat line (tangents zero), so open vs closed
        // gives the same bounds. Now add a strong tangent on the closing segment via the
        // last-vertex tangentOut and first-vertex tangentIn — this should ONLY affect bounds
        // when closed=true.
        List<List<Double>> vertices = List.of(point(0.0, 0.0), point(100.0, 0.0));
        // Open segment 0→1 stays flat.
        List<List<Double>> tangentsOut = List.of(point(0.0, 0.0), point(0.0, -100.0));
        List<List<Double>> tangentsIn = List.of(point(0.0, -100.0), point(0.0, 0.0));

        double[] open = PathRenderer.calculateGeometryBounds(vertices, tangentsIn, tangentsOut, Boolean.FALSE);
        double[] closed = PathRenderer.calculateGeometryBounds(vertices, tangentsIn, tangentsOut, Boolean.TRUE);

        assertNotNull(open);
        assertNotNull(closed);
        // Open: only segment 0→1 (flat) — bounds are exactly [0,0]–[100,0].
        assertEquals(0.0, open[1], 1e-9);
        assertEquals(0.0, open[3], 1e-9);
        // Closed: closing segment 1→0 has tangents pushing Y negative — bounds extend upward.
        assertTrue(closed[1] < -10.0,
                "Closed path's bounds should include the bulge from the closing segment, got Y min " + closed[1]);
    }
}
