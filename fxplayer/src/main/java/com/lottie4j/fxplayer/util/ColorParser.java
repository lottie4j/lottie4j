package com.lottie4j.fxplayer.util;

import javafx.scene.paint.Color;
import org.slf4j.Logger;

public final class ColorParser {

    /**
     * Prevents instantiation of this utility class.
     */
    private ColorParser() {
    }

    /**
     * Parses hex color strings in {@code RRGGBB} or {@code RRGGBBAA} format.
     *
     * @param colorStr input color string with or without a leading '#'
     * @param logger   logger used for parse error diagnostics
     * @return parsed JavaFX color, or {@code null} when parsing fails
     */
    public static Color parse(String colorStr, Logger logger) {
        try {
            String hex = colorStr.startsWith("#") ? colorStr.substring(1) : colorStr;

            if (hex.length() == 6) {
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                return Color.color(r / 255.0, g / 255.0, b / 255.0);
            }
            if (hex.length() == 8) {
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                int a = Integer.parseInt(hex.substring(6, 8), 16);
                return Color.color(r / 255.0, g / 255.0, b / 255.0, a / 255.0);
            }
        } catch (Exception e) {
            logger.warn("Error parsing color string '{}': {}", colorStr, e.getMessage());
        }
        return null;
    }
}
