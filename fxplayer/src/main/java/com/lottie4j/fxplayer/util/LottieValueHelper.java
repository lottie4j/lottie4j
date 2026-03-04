package com.lottie4j.fxplayer.util;

public class LottieValueHelper {

    /**
     * Prevents instantiation of this utility class.
     */
    private LottieValueHelper() {
        // Hide constructor
    }

    /**
     * Normalizes color channel-like values into the {@code [0, 1]} range.
     * Values above 1 are treated as potential 0-255 values and scaled accordingly.
     *
     * @param value input value from parsed animation data
     * @return clamped and normalized value in the {@code [0, 1]} range
     */
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
