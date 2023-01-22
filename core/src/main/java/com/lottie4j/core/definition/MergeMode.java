package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#blendmode
 */
public enum MergeMode {
    NORMAL(1, "Normal"),
    ADD(2, "Add"),
    SUBTRACT(3, "Subtract"),
    INTERSECT(4, "Intersect"),
    EXCLUDE(5, "Exclude Intersections");

    @JsonValue
    private final int value;
    private final String label;

    MergeMode(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     */
    @JsonCreator
    public static MergeMode fromValue(String value) {
        return Arrays.stream(MergeMode.values()).sequential()
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
