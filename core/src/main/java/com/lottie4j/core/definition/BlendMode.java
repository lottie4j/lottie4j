package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#blendmode
 */
public enum BlendMode {
    NORMAL(0, "Normal"),
    MULTIPLY(1, "Multiply"),
    SCREEN(2, "Screen"),
    OVERLAY(3, "Overlay"),
    DARKEN(4, "Darken"),
    LIGHTEN(5, "Lighten"),
    COLOR_DODGE(6, "Color Dodge"),
    COLOR_BURN(7, "Color Burn"),
    HARD_LIGHT(8, "Hard Light"),
    SOFT_LIGHT(9, "Soft Light"),
    DIFFERENCE(10, "Difference"),
    EXCLUSION(11, "Exclusion"),
    HUE(12, "Hue"),
    SATURATION(13, "Saturation"),
    COLOR(14, "Color"),
    LUMINOSITY(15, "Luminosity");

    @JsonValue
    private final int value;
    private final String label;

    BlendMode(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     */
    @JsonCreator
    public static BlendMode fromValue(String value) {
        return Arrays.stream(BlendMode.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .get();
    }

    public int value() {
        return value;
    }

    public String label() {
        return label;
    }
}
