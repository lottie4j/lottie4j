package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#star-type
 */
public enum StarType {
    STAR(1, "Star"),
    POLYGON(2, "Polygon");

    @JsonValue
    private final int value;
    private final String label;

    StarType(int value, String label) {
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
