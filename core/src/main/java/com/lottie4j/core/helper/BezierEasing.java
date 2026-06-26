package com.lottie4j.core.helper;

import com.lottie4j.core.model.animation.EasingHandle;

/**
 * Cubic-Bezier easing solver matching lottie-web's
 * <a href="https://github.com/airbnb/lottie-web/blob/master/player/js/utils/BezierEaser.js">BezierEaser.js</a>
 * byte-for-byte.
 *
 * <p>Given a unit cubic Bezier with end points (0, 0) and (1, 1) and two
 * control points (x1, y1) and (x2, y2), the solver maps an input progress
 * {@code x} to the corresponding {@code y} along the curve. This is the same
 * algorithm used by CSS {@code cubic-bezier(...)} and by lottie-web for keyframe
 * easing, so matching its constants exactly is required for pixel-faithful
 * playback.
 *
 * <p>Algorithm summary:
 * <ul>
 *   <li>Precompute an 11-entry lookup table sampling the x-component at
 *       {@code t = i / 10} for {@code i = 0..10}.</li>
 *   <li>For an input {@code aX}, locate the interval in the LUT and form an
 *       initial guess for the parameter {@code t} via linear interpolation.</li>
 *   <li>If the slope at the guess is at least {@link #NEWTON_MIN_SLOPE}, refine
 *       with {@link #NEWTON_ITERATIONS} steps of Newton-Raphson.</li>
 *   <li>If the slope is zero, the guess is returned directly.</li>
 *   <li>Otherwise, fall back to binary subdivision bounded by
 *       {@link #SUBDIVISION_PRECISION} or {@link #SUBDIVISION_MAX_ITERATIONS}.</li>
 *   <li>Finally evaluate the y-component cubic Bezier at the recovered
 *       parameter.</li>
 * </ul>
 */
public final class BezierEasing {

    /**
     * Number of Newton-Raphson refinement steps. Matches lottie-web's
     * {@code NEWTON_ITERATIONS}.
     */
    public static final int NEWTON_ITERATIONS = 4;

    /**
     * Minimum slope required to use Newton-Raphson. Below this threshold the
     * solver falls back to binary subdivision to avoid divergence near
     * flat-point beziers.
     */
    public static final double NEWTON_MIN_SLOPE = 0.001;

    /** Convergence tolerance for the binary subdivision fallback. */
    public static final double SUBDIVISION_PRECISION = 0.0000001;

    /** Maximum number of binary subdivision iterations. */
    public static final int SUBDIVISION_MAX_ITERATIONS = 10;

    /** Number of samples in the precomputed x-component lookup table. */
    public static final int KSPLINE_TABLE_SIZE = 11;

    /** Spacing between adjacent LUT samples. */
    public static final double SAMPLE_STEP_SIZE = 1.0 / (KSPLINE_TABLE_SIZE - 1);

    private final double mX1;
    private final double mY1;
    private final double mX2;
    private final double mY2;
    private final double[] sampleValues = new double[KSPLINE_TABLE_SIZE];
    private final boolean linear;

    /**
     * Builds a solver for the cubic Bezier with control points
     * {@code (x1, y1)} and {@code (x2, y2)}. The x coordinates are clamped to
     * {@code [0, 1]} to mirror lottie-web's input contract and to keep the
     * solver well-defined for keyframe data that may carry tiny floating-point
     * overshoots.
     *
     * @param x1 x-coordinate of the first control point
     * @param y1 y-coordinate of the first control point
     * @param x2 x-coordinate of the second control point
     * @param y2 y-coordinate of the second control point
     */
    public BezierEasing(double x1, double y1, double x2, double y2) {
        this.mX1 = clamp01(x1);
        this.mY1 = y1;
        this.mX2 = clamp01(x2);
        this.mY2 = y2;
        this.linear = (this.mX1 == this.mY1 && this.mX2 == this.mY2);
        if (!linear) {
            for (int i = 0; i < KSPLINE_TABLE_SIZE; i++) {
                sampleValues[i] = calcBezier(i * SAMPLE_STEP_SIZE, this.mX1, this.mX2);
            }
        }
    }

    /**
     * Convenience constructor for the common case where the easing is taken
     * directly from a pair of Lottie {@link EasingHandle} objects, applying the
     * same defaults used by lottie-web when individual components are missing
     * (start defaults to {@code (0, 0)}, end defaults to {@code (1, 1)}).
     *
     * @param easingOut outgoing easing handle from the previous keyframe; may be {@code null}
     * @param easingIn  incoming easing handle for the next keyframe; may be {@code null}
     */
    public BezierEasing(EasingHandle easingOut, EasingHandle easingIn) {
        this(
                firstOr(easingOut == null ? null : easingOut.x(), 0.0),
                firstOr(easingOut == null ? null : easingOut.y(), 0.0),
                firstOr(easingIn == null ? null : easingIn.x(), 1.0),
                firstOr(easingIn == null ? null : easingIn.y(), 1.0)
        );
    }

    /**
     * Evaluates the eased progress for the given input.
     *
     * @param x input progress in {@code [0, 1]}
     * @return eased progress in {@code [0, 1]} (modulo y-control-point extrema)
     */
    public double solve(double x) {
        if (linear) {
            return x;
        }
        if (x == 0.0) {
            return 0.0;
        }
        if (x == 1.0) {
            return 1.0;
        }
        return calcBezier(getTForX(x), mY1, mY2);
    }

    /**
     * Static convenience for callers that do not need to retain the solver
     * across invocations. Equivalent to
     * {@code new BezierEasing(easingOut, easingIn).solve(t)}.
     *
     * @param t         input progress in {@code [0, 1]}
     * @param easingOut outgoing easing handle from the previous keyframe
     * @param easingIn  incoming easing handle for the next keyframe
     * @return eased progress
     */
    public static double solve(double t, EasingHandle easingOut, EasingHandle easingIn) {
        return new BezierEasing(easingOut, easingIn).solve(t);
    }

    private double getTForX(double aX) {
        double intervalStart = 0.0;
        int currentSample = 1;
        int lastSample = KSPLINE_TABLE_SIZE - 1;

        for (; currentSample != lastSample && sampleValues[currentSample] <= aX; currentSample++) {
            intervalStart += SAMPLE_STEP_SIZE;
        }
        currentSample--;

        double dist = (aX - sampleValues[currentSample])
                / (sampleValues[currentSample + 1] - sampleValues[currentSample]);
        double guessForT = intervalStart + dist * SAMPLE_STEP_SIZE;

        double initialSlope = getSlope(guessForT, mX1, mX2);
        if (initialSlope >= NEWTON_MIN_SLOPE) {
            return newtonRaphsonIterate(aX, guessForT, mX1, mX2);
        } else if (initialSlope == 0.0) {
            return guessForT;
        } else {
            return binarySubdivide(aX, intervalStart, intervalStart + SAMPLE_STEP_SIZE, mX1, mX2);
        }
    }

    private static double a(double a1, double a2) {
        return 1.0 - 3.0 * a2 + 3.0 * a1;
    }

    private static double b(double a1, double a2) {
        return 3.0 * a2 - 6.0 * a1;
    }

    private static double c(double a1) {
        return 3.0 * a1;
    }

    private static double calcBezier(double t, double a1, double a2) {
        return ((a(a1, a2) * t + b(a1, a2)) * t + c(a1)) * t;
    }

    private static double getSlope(double t, double a1, double a2) {
        return 3.0 * a(a1, a2) * t * t + 2.0 * b(a1, a2) * t + c(a1);
    }

    private static double newtonRaphsonIterate(double aX, double aGuessT, double mX1, double mX2) {
        for (int i = 0; i < NEWTON_ITERATIONS; i++) {
            double currentSlope = getSlope(aGuessT, mX1, mX2);
            if (currentSlope == 0.0) {
                return aGuessT;
            }
            double currentX = calcBezier(aGuessT, mX1, mX2) - aX;
            aGuessT -= currentX / currentSlope;
        }
        return aGuessT;
    }

    private static double binarySubdivide(double aX, double aA, double aB, double mX1, double mX2) {
        double currentX;
        double currentT;
        int i = 0;
        do {
            currentT = aA + (aB - aA) / 2.0;
            currentX = calcBezier(currentT, mX1, mX2) - aX;
            if (currentX > 0.0) {
                aB = currentT;
            } else {
                aA = currentT;
            }
        } while (Math.abs(currentX) > SUBDIVISION_PRECISION && ++i < SUBDIVISION_MAX_ITERATIONS);
        return currentT;
    }

    private static double clamp01(double v) {
        if (v < 0.0) {
            return 0.0;
        }
        if (v > 1.0) {
            return 1.0;
        }
        return v;
    }

    private static double firstOr(java.util.List<Double> values, double fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        Double v = values.get(0);
        return v == null ? fallback : v;
    }
}
