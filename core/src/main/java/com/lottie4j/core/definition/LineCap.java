package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#linecap
 */
public enum LineCap {
    BUTT(1, "Butt"),
    ROUND(2, "Round"),
    SQUARE(3, "Square");

    @JsonValue
    private final int value;
    private final String label;

    LineCap(int value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonCreator
    public static LineCap fromValue(String value) {
        return Arrays.stream(LineCap.values()).sequential()
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
