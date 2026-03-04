package com.lottie4j.fxplayer.util;

import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Layer;

public final class FrameTiming {

    private FrameTiming() {
    }

    public static int getInPoint(Animation animation) {
        return animation.inPoint() != null ? animation.inPoint() : 0;
    }

    public static int getOutPoint(Animation animation) {
        return animation.outPoint() != null ? animation.outPoint() : 60;
    }

    public static int getFramesPerSecond(Animation animation) {
        return animation.framesPerSecond() != null ? animation.framesPerSecond() : 30;
    }

    public static int getAnimationWidth(Animation animation) {
        return animation.width() != null ? animation.width() : 500;
    }

    public static int getAnimationHeight(Animation animation) {
        return animation.height() != null ? animation.height() : 500;
    }

    public static double toLocalFrame(Layer layer, double parentFrame) {
        double start = layer.startTime() != null ? layer.startTime() : 0.0;
        double stretch = (layer.timeStretch() != null && layer.timeStretch() != 0) ? layer.timeStretch() : 1.0;
        return (parentFrame - start) / stretch;
    }
}

