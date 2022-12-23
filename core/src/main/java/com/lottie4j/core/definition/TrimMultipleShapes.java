package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#trim-multiple-shapes
 */
public enum TrimMultipleShapes {
    INDIVIDUALLY(1, "Individually"),
    SIMULTANEOUSLY(2, "Simultaneously");

    @JsonValue
    private final int value;
    private final String label;

    TrimMultipleShapes(int value, String label) {
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
