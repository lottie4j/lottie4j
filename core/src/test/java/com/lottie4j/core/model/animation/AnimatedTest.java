package com.lottie4j.core.model.animation;

import com.lottie4j.core.definition.AnimatedValueType;
import com.lottie4j.core.model.keyframe.Keyframe;
import com.lottie4j.core.model.keyframe.NumberKeyframe;
import com.lottie4j.core.model.keyframe.TimedKeyframe;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boundary tests for {@link Animated#getValue}.
 *
 * <p>The renderer evaluates animated properties one frame at a time. The most failure-prone
 * inputs are the exact keyframe times — segment starts, segment ends, and the very first /
 * last frame — because both linear interpolation and bezier easing converge to a single
 * point there. A single off-by-one in the segment search produces a visible one-frame jolt
 * during playback, so these cases must remain correct.
 */
class AnimatedTest {

    private static final double EPS = 1.0e-6;

    // ── Setup helpers ────────────────────────────────────────────────────────

    private static List<BigDecimal> bd(double... values) {
        return Arrays.stream(values).boxed().map(BigDecimal::valueOf).toList();
    }

    private static TimedKeyframe kf(double t, double... values) {
        return new TimedKeyframe(t, bd(values), null, null, null, null, null, null);
    }

    private static TimedKeyframe kfEased(double t, double[] values, EasingHandle in, EasingHandle out) {
        return new TimedKeyframe(t, bd(values), null, in, out, null, null, null);
    }

    private static TimedKeyframe kfHold(double t, double... values) {
        return new TimedKeyframe(t, bd(values), null, null, null, null, null, 1);
    }

    private static TimedKeyframe kfSpatial(double t, double[] values, double[] tangentOut, double[] tangentIn) {
        return new TimedKeyframe(t, bd(values), null, null, null, bd(tangentOut), bd(tangentIn), null);
    }

    private static Animated animated(Keyframe... keyframes) {
        return new Animated(1, List.of(keyframes), null, null, null, null, null);
    }

    // ── Single-segment boundary tests ────────────────────────────────────────

    @Test
    void evaluatingAtFirstKeyframeStartReturnsFirstValue() {
        Animated a = animated(kf(0, 0), kf(100, 100));
        assertEquals(0.0, a.getValue(0, 0.0), EPS);
    }

    @Test
    void evaluatingAtMidpointReturnsLinearlyInterpolatedValue() {
        Animated a = animated(kf(0, 0), kf(100, 100));
        assertEquals(50.0, a.getValue(0, 50.0), EPS);
    }

    @Test
    void evaluatingAtLastKeyframeReturnsLastValue() {
        // Boundary: t == k[last].t. The evaluator must return the segment's end value
        // (which equals k[last].s), not zero from a failed segment search.
        Animated a = animated(kf(0, 0), kf(100, 100));
        assertEquals(100.0, a.getValue(0, 100.0), EPS);
    }

    @Test
    void evaluatingPastLastKeyframeClampsToLastValue() {
        Animated a = animated(kf(0, 0), kf(100, 100));
        assertEquals(100.0, a.getValue(0, 150.0), EPS);
    }

    @Test
    void evaluatingBeforeFirstKeyframeClampsToFirstValue() {
        Animated a = animated(kf(10, 5), kf(100, 100));
        assertEquals(5.0, a.getValue(0, 0.0), EPS);
    }

    // ── Interior boundary continuity ─────────────────────────────────────────

    @Test
    void interiorBoundaryReturnsKeyframeValueExactly() {
        // 3-keyframe sequence; at t == k[1].t the evaluator must return k[1].s exactly.
        // A single-frame jolt at this point would come from the search returning either
        // segment 0 at fraction 1.0 with a non-1 eased progress, or segment 1 at fraction 0
        // with an unexpected easing — both should yield the same value here.
        Animated a = animated(kf(0, 0), kf(50, 50), kf(100, 100));
        assertEquals(50.0, a.getValue(0, 50.0), EPS);
    }

    @Test
    void interiorBoundaryIsContinuousWithEitherSide() {
        Animated a = animated(kf(0, 0), kf(50, 50), kf(100, 100));
        double justBefore = a.getValue(0, 49.999);
        double atBoundary = a.getValue(0, 50.0);
        double justAfter = a.getValue(0, 50.001);

        assertEquals(50.0, atBoundary, EPS);
        // Sub-frame samples on either side stay within the same neighbourhood — no jolt.
        assertTrue(Math.abs(justBefore - atBoundary) < 0.01,
                "just-before " + justBefore + " differs from boundary " + atBoundary);
        assertTrue(Math.abs(justAfter - atBoundary) < 0.01,
                "just-after " + justAfter + " differs from boundary " + atBoundary);
    }

    // ── Hold (step) keyframes ────────────────────────────────────────────────

    @Test
    void holdFrameKeepsStartValueAcrossSegmentAndJumpsAtNextKeyframe() {
        // A hold segment must return k[i].s for every t in [k[i].t, k[i+1].t), then the
        // new value at exactly k[i+1].t. This matches lottie-web's step behaviour.
        Animated a = animated(kfHold(0, 10), kf(100, 100));
        assertEquals(10.0, a.getValue(0, 0.0), EPS);
        assertEquals(10.0, a.getValue(0, 50.0), EPS);
        assertEquals(10.0, a.getValue(0, 99.0), EPS);
        assertEquals(100.0, a.getValue(0, 100.0), EPS);
    }

    // ── Bezier easing convergence ────────────────────────────────────────────

    @Test
    void bezierEasingPreservesBoundaryValues() {
        // Bezier easing must produce easedProgress(0) == 0 and easedProgress(1) == 1
        // regardless of control point shape — otherwise the boundary frames jolt.
        EasingHandle in = new EasingHandle(List.of(0.83), List.of(0.83));
        EasingHandle out = new EasingHandle(List.of(0.17), List.of(0.17));
        Animated a = animated(kfEased(0, new double[]{0}, in, out), kf(100, 100));
        assertEquals(0.0, a.getValue(0, 0.0), EPS);
        assertEquals(100.0, a.getValue(0, 100.0), EPS);
    }

    @Test
    void bezierEasingWithSharpControlPointsStillConvergesAtBoundaries() {
        // Asymmetric cubic-bezier control points — the kind that produced the
        // 78%-min single-frame artifact on java_duke_fadein. The y(0) and y(1) values
        // of cubic Bezier (0, y1, y2, 1) are always 0 and 1, but only if the solver
        // doesn't drift.
        EasingHandle out = new EasingHandle(List.of(0.95), List.of(0.0));
        EasingHandle in = new EasingHandle(List.of(0.05), List.of(1.0));
        Animated a = animated(kfEased(0, new double[]{0}, in, out), kf(100, 100));
        assertEquals(0.0, a.getValue(0, 0.0), EPS);
        assertEquals(100.0, a.getValue(0, 100.0), EPS);
    }

    // ── Spatial bezier (position) boundaries ─────────────────────────────────

    @Test
    void spatialBezierBoundaryReturnsKeyframeEndpoints() {
        // Position animation with tangents: cubic Bezier of (P0, P0+to, P3+ti, P3) must
        // return P0 at progress=0 and P3 at progress=1. Values lifted from
        // java_duke_slidein.json so the test mirrors the actual failing animation.
        Animated a = animated(
                kfSpatial(0, new double[]{0, 407, 0}, new double[]{0, -76.58, 0}, new double[]{0, 2.38, 0}),
                kfSpatial(37, new double[]{0, 305.55, 0}, new double[]{0, 0, 0}, new double[]{0, 0, 0})
        );
        assertEquals(407.0, a.getValue(AnimatedValueType.Y, 0.0), 1.0e-3);
        assertEquals(305.55, a.getValue(AnimatedValueType.Y, 37.0), 1.0e-3);
    }

    // ── Static (non-animated) values ─────────────────────────────────────────

    @Test
    void staticAnimatedReturnsConstantValueForAllFrames() {
        Animated a = new Animated(0, List.of(new NumberKeyframe(42)),
                null, null, null, null, null);
        assertEquals(42.0, a.getValue(0, 0.0), EPS);
        assertEquals(42.0, a.getValue(0, 100.0), EPS);
    }

    // ── Defensive: never return NaN at any sampled integer frame ─────────────

    @Test
    void doesNotReturnNaNAtAnyIntegerFrameBetweenKeyframes() {
        // Sweep every integer frame from before the first keyframe to past the last.
        // Even with sharp bezier easing the result must stay finite.
        EasingHandle out = new EasingHandle(List.of(0.95), List.of(0.0));
        EasingHandle in = new EasingHandle(List.of(0.05), List.of(1.0));
        Animated a = animated(
                kfEased(0, new double[]{0}, in, out),
                kfEased(60, new double[]{50}, in, out),
                kf(120, 100)
        );
        for (int f = -5; f <= 125; f++) {
            double v = a.getValue(0, f);
            assertFalse(Double.isNaN(v), "value at frame " + f + " was NaN");
        }
    }

    @Test
    void duplicateTimeKeyframesDoNotProduceNaN() {
        // Two keyframes at the same time create a zero-width segment used for instant
        // jumps. Sampling exactly at that time must not divide by zero.
        Animated a = animated(kf(0, 0), kf(50, 25), kf(50, 75), kf(100, 100));
        double atBoundary = a.getValue(0, 50.0);
        assertFalse(Double.isNaN(atBoundary), "boundary at zero-duration segment was NaN");
    }
}
