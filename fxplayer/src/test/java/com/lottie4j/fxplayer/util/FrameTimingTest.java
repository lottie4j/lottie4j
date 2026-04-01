package com.lottie4j.fxplayer.util;

import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.layer.Layer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrameTimingTest {

    @Test
    void usesSafeDefaultsWhenAnimationTimingIsMissing() {
        Animation animation = new Animation(null, null, null, null, null, null, null, null, null,
                null, null, null, null);

        assertEquals(0, FrameTiming.getInPoint(animation));
        assertEquals(60, FrameTiming.getOutPointExclusive(animation));
        assertEquals(59, FrameTiming.getLastRenderableFrame(animation));
        assertEquals(30, FrameTiming.getFramesPerSecond(animation));
        assertEquals(500, FrameTiming.getAnimationWidth(animation));
        assertEquals(500, FrameTiming.getAnimationHeight(animation));
    }

    @Test
    void enforcesOutPointToBeAfterInPoint() {
        Animation animation = new Animation(null, null, null, null, 24, 20, 20, null, null,
                null, null, null, null);

        assertEquals(21, FrameTiming.getOutPointExclusive(animation));
        assertEquals(20, FrameTiming.getLastRenderableFrame(animation));
    }

    @Test
    void convertsParentFrameToLocalFrameUsingStartAndStretch() {
        Layer layer = layer(2, null, null, 10);

        assertEquals(8.0, FrameTiming.toLocalFrame(layer, 26.0));
    }

    @Test
    void fallsBackToNoOffsetAndUnitStretchForInvalidValues() {
        Layer layer = layer(0, null, null, null);

        assertEquals(15.0, FrameTiming.toLocalFrame(layer, 15.0));
    }

    @Test
    void returnsConfiguredTimingAndDimensionsWhenPresent() {
        Animation animation = new Animation(null, null, null, null, 12, 3, 44, 640, 360,
                null, null, null, null);

        assertEquals(3, FrameTiming.getInPoint(animation));
        assertEquals(44, FrameTiming.getOutPointExclusive(animation));
        assertEquals(43, FrameTiming.getLastRenderableFrame(animation));
        assertEquals(12, FrameTiming.getFramesPerSecond(animation));
        assertEquals(640, FrameTiming.getAnimationWidth(animation));
        assertEquals(360, FrameTiming.getAnimationHeight(animation));
    }

    private static Layer layer(Integer timeStretch, Integer inPoint, Integer outPoint, Integer startTime) {
        return new Layer(
                null, null, null, null, null, null, null,
                timeStretch, inPoint, outPoint, startTime,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null, null
        );
    }
}
