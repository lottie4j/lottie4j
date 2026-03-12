package com.lottie4j.fxplayer.util;

import com.lottie4j.core.model.Layer;

/**
 * Utility for determining layer visibility and activity at specific animation frames.
 * Checks whether a layer should be rendered based on its in-point and out-point.
 */
public final class LayerActivity {

    /**
     * Prevents instantiation of this utility class.
     */
    private LayerActivity() {
    }

    /**
     * Checks whether a layer is active at the specified frame.
     *
     * @param layer layer to evaluate
     * @param frame frame index to test
     * @return {@code true} when frame is inside the layer in/out range
     */
    public static boolean isActiveAtFrame(Layer layer, double frame) {
        double inPoint = layer.inPoint() != null ? layer.inPoint() : 0.0;
        double outPointExclusive = layer.outPoint() != null ? layer.outPoint() : Double.POSITIVE_INFINITY;
        return frame >= inPoint && frame < outPointExclusive;
    }
}
