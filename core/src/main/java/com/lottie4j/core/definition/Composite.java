package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonValue;

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

    public int value() {
        return value;
    }

    public String label() {
        return label;
    }
}
