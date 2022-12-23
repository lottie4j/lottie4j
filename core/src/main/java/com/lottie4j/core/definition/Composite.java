package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#composite
 */
public enum Composite {
    ABOVE(1, "Above"),
    BELOW(2, "Below");

    @JsonValue
    private final int value;
    private final String label;

    Composite(int value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonCreator
    public static Composite fromValue(String value) {
        return Arrays.stream(Composite.values()).sequential()
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
