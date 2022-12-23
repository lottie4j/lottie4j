package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

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

    @JsonCreator
    public static TrimMultipleShapes fromValue(String value) {
        return Arrays.stream(TrimMultipleShapes.values()).sequential()
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
