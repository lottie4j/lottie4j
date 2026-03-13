package com.lottie4j.fxplayer.util;

import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.layer.Layer;

/**
 * Utility for extracting frame timing and composition dimensions from Lottie animations.
 * Provides safe defaults when animation properties are absent.
 */
public final class FrameTiming {

    /**
     * Prevents instantiation of this utility class.
     */
    private FrameTiming() {
    }

    /**
     * Returns animation in-point with a safe default.
     *
     * @param animation animation model
     * @return first frame index used for playback
     */
    public static int getInPoint(Animation animation) {
        return animation.inPoint() != null ? animation.inPoint() : 0;
    }

    /**
     * Returns animation out-point (exclusive) with a safe default.
     *
     * @param animation animation model
     * @return exclusive end frame index used for playback bounds
     */
    public static int getOutPoint(Animation animation) {
        int inPoint = getInPoint(animation);
        int defaultOutPoint = inPoint + 60;
        int outPoint = animation.outPoint() != null ? animation.outPoint() : defaultOutPoint;
        // Ensure exclusive out-point stays ahead of in-point by at least one frame.
        return Math.max(inPoint + 1, outPoint);
    }

    /**
     * Alias for explicit readability at call sites that rely on exclusive semantics.
     *
     * @param animation animation model
     * @return exclusive end frame index used for playback bounds
     */
    public static int getOutPointExclusive(Animation animation) {
        return getOutPoint(animation);
    }

    /**
     * Returns the last frame that should be sampled/rendered (inclusive).
     *
     * @param animation animation model
     * @return last frame that should be sampled or rendered
     */
    public static int getLastRenderableFrame(Animation animation) {
        int inPoint = getInPoint(animation);
        int outExclusive = getOutPointExclusive(animation);
        return Math.max(inPoint, outExclusive - 1);
    }

    /**
     * Returns frames-per-second with a safe default.
     *
     * @param animation animation model
     * @return playback frame rate
     */
    public static int getFramesPerSecond(Animation animation) {
        return animation.framesPerSecond() != null ? animation.framesPerSecond() : 30;
    }

    /**
     * Returns animation width with a safe default.
     *
     * @param animation animation model
     * @return composition width in pixels
     */
    public static int getAnimationWidth(Animation animation) {
        return animation.width() != null ? animation.width() : 500;
    }

    /**
     * Returns animation height with a safe default.
     *
     * @param animation animation model
     * @return composition height in pixels
     */
    public static int getAnimationHeight(Animation animation) {
        return animation.height() != null ? animation.height() : 500;
    }

    /**
     * Converts a parent timeline frame into a precomposition-local frame.
     *
     * @param layer       precomposition layer carrying start time and stretch values
     * @param parentFrame frame in parent timeline
     * @return mapped frame in precomposition timeline
     */
    public static double toLocalFrame(Layer layer, double parentFrame) {
        double start = layer.startTime() != null ? layer.startTime() : 0.0;
        double stretch = (layer.timeStretch() != null && layer.timeStretch() != 0) ? layer.timeStretch() : 1.0;
        return (parentFrame - start) / stretch;
    }
}
