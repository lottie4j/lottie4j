package com.lottie4j.fxplayer.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LottieValueHelperTest {

    @Test
    void clampReturnsZeroForNegativeValues() {
        assertEquals(0.0, LottieValueHelper.clamp(-0.2));
    }

    @Test
    void clampKeepsNormalizedValuesUntouched() {
        assertEquals(0.5, LottieValueHelper.clamp(0.5));
        assertEquals(1.0, LottieValueHelper.clamp(1.0));
    }

    @Test
    void clampNormalizesByteLikeValues() {
        assertEquals(128.0 / 255.0, LottieValueHelper.clamp(128.0));
        assertEquals(1.0, LottieValueHelper.clamp(255.0));
    }

    @Test
    void clampCapsLargeValuesToOne() {
        assertEquals(1.0, LottieValueHelper.clamp(256.0));
        assertEquals(1.0, LottieValueHelper.clamp(1024.0));
    }
}

