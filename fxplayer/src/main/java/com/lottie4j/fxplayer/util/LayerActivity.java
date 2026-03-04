package com.lottie4j.fxplayer.util;

import com.lottie4j.core.model.Layer;

public final class LayerActivity {

    private LayerActivity() {
    }

    public static boolean isActiveAtFrame(Layer layer, double frame) {
        return frame >= layer.inPoint() && frame <= layer.outPoint();
    }
}

