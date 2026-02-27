package com.lottie4j.fxplayer.util;

public class LottieValueHelper {

    private LottieValueHelper() {
        // Hide constructor
    }

    public static double clamp(double value) {
        // If value is > 1.0, assume it's in 0-255 range or incorrectly parsed
        if (value > 1.0) {
            // Check if it looks like a 0-255 value
            if (value <= 255.0) {
                return value / 255.0;
            }
            // Otherwise clamp to 1.0
            return 1.0;
        }
        return Math.max(0.0, value);
    }
}
