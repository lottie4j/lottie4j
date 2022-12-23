package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#strokedashtype
 */
public enum StrokeDashType {
    DASH("d", "Dash"),
    GAP("g", "Gap"),
    OFFSET("o", "Offset");

    @JsonValue
    private final String value;
    private final String label;

    StrokeDashType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonCreator
    public static StrokeDashType fromValue(String value) {
        return Arrays.stream(StrokeDashType.values()).sequential()
                .filter(v -> String.valueOf(v.value).equals(value))
                .findFirst()
                .get();
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }
}
