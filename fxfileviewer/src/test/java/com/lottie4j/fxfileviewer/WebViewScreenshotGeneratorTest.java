package com.lottie4j.fxfileviewer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebViewScreenshotGeneratorTest {

    @Test
    void stepOneCoversEveryFrameInclusiveOfLast() {
        List<Integer> frames = WebViewScreenshotGenerator.buildSampledFrames(0, 5, 1);
        assertEquals(List.of(0, 1, 2, 3, 4, 5), frames);
    }

    @Test
    void stepFiveStillAlwaysIncludesLastFrame() {
        // 0,5,10,15,20 — but lastFrame=22 is not on the cadence; helper must append it.
        List<Integer> frames = WebViewScreenshotGenerator.buildSampledFrames(0, 22, 5);
        assertEquals(List.of(0, 5, 10, 15, 20, 22), frames);
    }

    @Test
    void stepFiveLandsExactlyOnLastFrame() {
        List<Integer> frames = WebViewScreenshotGenerator.buildSampledFrames(0, 20, 5);
        assertEquals(List.of(0, 5, 10, 15, 20), frames);
    }

    @Test
    void singleFrameAnimationProducesSingleEntry() {
        List<Integer> frames = WebViewScreenshotGenerator.buildSampledFrames(7, 7, 1);
        assertEquals(List.of(7), frames);
    }

    @Test
    void zeroStepCoercesToOne() {
        List<Integer> frames = WebViewScreenshotGenerator.buildSampledFrames(0, 3, 0);
        assertEquals(List.of(0, 1, 2, 3), frames);
    }

    @Test
    void inPointEqualsOutPoint() {
        List<Integer> frames = WebViewScreenshotGenerator.buildSampledFrames(42, 42, 5);
        assertEquals(List.of(42), frames);
    }
}
