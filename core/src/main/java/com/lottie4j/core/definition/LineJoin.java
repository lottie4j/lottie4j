package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#linejoin
 */
public enum LineJoin {
    MITER(1, "Miter"),
    ROUND(2, "Round"),
    BEVEL(3, "Bevel");

    @JsonValue
    private final int value;
    private final String label;

    LineJoin(int value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonCreator
    public static LineJoin fromValue(String value) {
        return Arrays.stream(LineJoin.values()).sequential()
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
