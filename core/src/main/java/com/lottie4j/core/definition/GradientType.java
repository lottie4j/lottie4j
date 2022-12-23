package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#linecap
 */
public enum GradientType {
    LINEAR(1, "Linear"),
    RADIAL(2, "Radial");

    @JsonValue
    private final int value;
    private final String label;

    GradientType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonCreator
    public static GradientType fromValue(String value) {
        return Arrays.stream(GradientType.values()).sequential()
                .filter(v -> String.valueOf(v.value).equals(value))
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
