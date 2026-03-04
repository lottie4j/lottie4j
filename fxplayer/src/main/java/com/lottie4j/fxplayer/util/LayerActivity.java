package com.lottie4j.fxplayer.util;

import com.lottie4j.core.model.Layer;

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
        return frame >= layer.inPoint() && frame <= layer.outPoint();
    }
}
