package com.lottie4j.core.helper;

import com.lottie4j.core.model.animation.EasingHandle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fidelity tests for {@link BezierEasing} against lottie-web's
 * {@code BezierEaser.js}.
 *
 * <p>These tests guard against three classes of regression that historically
 * produced distributed sub-pixel rendering drift versus lottie-web:
 * <ul>
 *   <li>Boundary preservation: {@code solve(0) = 0} and {@code solve(1) = 1}
 *       exactly.</li>
 *   <li>Algorithmic fidelity: the Newton-Raphson + binary-subdivision pipeline
 *       must agree with the mathematical inverse to the same precision lottie-web
 *       requires ({@code SUBDIVISION_PRECISION = 1e-7}). Verified by composing
 *       {@code solve} with the analytic cubic Bezier x-component over a 1/30
 *       sweep — the deepest test we can perform without a JavaScript reference
 *       at hand.</li>
 *   <li>Constant-table fidelity: the LUT must have exactly
 *       {@link BezierEasing#KSPLINE_TABLE_SIZE} entries; otherwise the initial
 *       guess strategy diverges and small errors accumulate frame-by-frame.</li>
 * </ul>
 */
class BezierEasingTest {

    /** Cubic Bezier on the unit interval, end points (0,0) and (1,1). */
    private static double cubicBezier(double t, double p1, double p2) {
        double mt = 1.0 - t;
        return 3.0 * mt * mt * t * p1 + 3.0 * mt * t * t * p2 + t * t * t;
    }

    // ── Constant table fidelity ──────────────────────────────────────────────

    @Test
    void constantsMatchLottieWebReference() {
        // These constants are the contract with lottie-web. Changing any of them
        // breaks pixel-faithful playback even when the algorithm is otherwise
        // correct. Asserted here so a future "optimisation" cannot silently
        // alter them.
        assertEquals(4, BezierEasing.NEWTON_ITERATIONS);
        assertEquals(0.001, BezierEasing.NEWTON_MIN_SLOPE);
        assertEquals(0.0000001, BezierEasing.SUBDIVISION_PRECISION);
        assertEquals(10, BezierEasing.SUBDIVISION_MAX_ITERATIONS);
        assertEquals(11, BezierEasing.KSPLINE_TABLE_SIZE);
        assertEquals(1.0 / 10.0, BezierEasing.SAMPLE_STEP_SIZE);
    }

    // ── Boundary preservation ────────────────────────────────────────────────

    @Test
    void solveAtZeroAndOneIsExact() {
        // The renderer must reach exactly the start and end values at segment
        // boundaries. Any drift here produces a one-frame jolt visible in the
        // SSIM comparison.
        BezierEasing easing = new BezierEasing(0.42, 0.0, 0.58, 1.0);
        assertEquals(0.0, easing.solve(0.0));
        assertEquals(1.0, easing.solve(1.0));
    }

    @Test
    void solveAtZeroAndOneIsExactForSharpBezier() {
        // Asymmetric ease — the kind that triggered the historical lottie_lego
        // and foojay_reporter score gaps. Boundaries must still be exact.
        BezierEasing easing = new BezierEasing(0.95, 0.0, 0.05, 1.0);
        assertEquals(0.0, easing.solve(0.0));
        assertEquals(1.0, easing.solve(1.0));
    }

    // ── Linear identity ──────────────────────────────────────────────────────

    @Test
    void linearBezierActsAsIdentity() {
        // When (x1, y1) == (y1's mirror) the curve degenerates to y = x and the
        // solver short-circuits. Verifying both that the short-circuit triggers
        // and that the result is exact (no LUT round-trip error).
        BezierEasing easing = new BezierEasing(0.25, 0.25, 0.75, 0.75);
        for (int i = 0; i <= 100; i++) {
            double t = i / 100.0;
            assertEquals(t, easing.solve(t), 0.0,
                    "linear bezier must be identity at t=" + t);
        }
    }

    // ── Algorithmic fidelity: inverse round-trip ─────────────────────────────

    @Test
    void solveInvertsCubicBezierToSubdivisionPrecision() {
        // For each control point set, pick parameters t' along the curve,
        // compute the corresponding x = B_x(t'), then verify that
        // B_y(solve(x)) == B_y(t') to lottie-web's precision. This is the
        // strongest mathematical test we can run without a JavaScript reference.
        double[][] controlPointSets = {
                {0.42, 0.0, 0.58, 1.0},   // CSS ease-in-out
                {0.25, 0.1, 0.25, 1.0},   // CSS ease
                {0.42, 0.0, 1.0, 1.0},    // CSS ease-in
                {0.0, 0.0, 0.58, 1.0},    // CSS ease-out
                {0.95, 0.0, 0.05, 1.0},   // sharp asymmetric
                {0.17, 0.17, 0.83, 0.83}, // shallow symmetric
                {0.6, 0.0, 0.4, 1.0},     // material standard
        };

        for (double[] cp : controlPointSets) {
            BezierEasing easing = new BezierEasing(cp[0], cp[1], cp[2], cp[3]);
            for (int i = 0; i <= 30; i++) {
                double tPrime = i / 30.0;
                double x = cubicBezier(tPrime, cp[0], cp[2]);
                double expectedY = cubicBezier(tPrime, cp[1], cp[3]);
                double actualY = easing.solve(x);
                // 1e-5 leaves margin above SUBDIVISION_PRECISION (1e-7) and is
                // tight enough to detect a single missing Newton iteration.
                assertEquals(expectedY, actualY, 1.0e-5,
                        "bezier(" + cp[0] + "," + cp[1] + "," + cp[2] + "," + cp[3] + ")"
                                + " inverse failed at x=" + x);
            }
        }
    }

    // ── Algorithmic fidelity: 1/30-step sweep ────────────────────────────────

    @Test
    void solveIsMonotonicAndStaysInUnitInterval() {
        // A monotone strictly-increasing easing must produce monotone output and
        // never escape [0, 1]. Both properties are required for stable
        // keyframe interpolation; a violation manifests as a flicker or jump.
        BezierEasing easing = new BezierEasing(0.42, 0.0, 0.58, 1.0);
        double previous = -1.0;
        for (int i = 0; i <= 30; i++) {
            double t = i / 30.0;
            double y = easing.solve(t);
            assertTrue(y >= -1.0e-12 && y <= 1.0 + 1.0e-12, "y out of range at t=" + t + ": " + y);
            assertTrue(y >= previous - 1.0e-12, "non-monotone at t=" + t + ": " + previous + " -> " + y);
            previous = y;
        }
    }

    // ── EasingHandle convenience ─────────────────────────────────────────────

    @Test
    void staticSolveWithEasingHandlesAppliesLottieWebDefaults() {
        // When a handle component is missing, lottie-web treats the outgoing
        // handle as (0, 0) and the incoming handle as (1, 1). Verify those
        // defaults so partially-specified easing data does not silently change
        // shape relative to lottie-web.
        EasingHandle empty = new EasingHandle(null, null);
        // (0,0)+(1,1) is the identity bezier — must produce y = x exactly.
        for (int i = 0; i <= 10; i++) {
            double t = i / 10.0;
            assertEquals(t, BezierEasing.solve(t, empty, empty), 1.0e-12);
        }
    }

    @Test
    void staticSolveUsesFirstHandleComponentWhenMultiplePresent() {
        // Lottie sometimes stores easing handle x/y as multi-element arrays
        // (one per dimension). lottie-web takes the first element; we must too.
        EasingHandle out = new EasingHandle(List.of(0.42, 0.5), List.of(0.0, 0.5));
        EasingHandle in = new EasingHandle(List.of(0.58, 0.5), List.of(1.0, 0.5));

        BezierEasing reference = new BezierEasing(0.42, 0.0, 0.58, 1.0);

        for (int i = 0; i <= 10; i++) {
            double t = i / 10.0;
            assertEquals(reference.solve(t), BezierEasing.solve(t, out, in), 1.0e-12);
        }
    }

    @Test
    void staticSolveHandlesNullHandlesGracefully() {
        // Defensive: null handles must not crash and must behave as the
        // lottie-web defaults (identity bezier).
        for (int i = 0; i <= 10; i++) {
            double t = i / 10.0;
            double y = BezierEasing.solve(t, null, null);
            assertFalse(Double.isNaN(y));
            assertEquals(t, y, 1.0e-12);
        }
    }

    // ── Constructor robustness ───────────────────────────────────────────────

    @Test
    void constructorClampsOutOfRangeXValuesInsteadOfThrowing() {
        // Lottie keyframe data occasionally carries x values slightly outside
        // [0, 1] due to authoring tools. The solver should clamp rather than
        // throw so the renderer keeps producing frames; the difference at the
        // clamped edge is negligible and matches what lottie-web ends up with
        // once its own internal clamping kicks in.
        BezierEasing easing = new BezierEasing(-0.01, 0.0, 1.01, 1.0);
        assertEquals(0.0, easing.solve(0.0));
        assertEquals(1.0, easing.solve(1.0));
        double mid = easing.solve(0.5);
        assertFalse(Double.isNaN(mid));
        assertTrue(mid >= 0.0 && mid <= 1.0);
    }
}
