package com.lottie4j.fxplayer.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class LottieCoordinateHelperTest {

    @Test
    void convertsCenterToTopLeftCoordinates() {
        double[] topLeft = LottieCoordinateHelper.centerToTopLeft(50, 40, 20, 10);

        assertArrayEquals(new double[]{40, 35}, topLeft);
    }

    @Test
    void convertsTopLeftToCenterCoordinates() {
        double[] center = LottieCoordinateHelper.topLeftToCenter(40, 35, 20, 10);

        assertArrayEquals(new double[]{50, 40}, center);
    }
}

