package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;

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

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     */
    @JsonCreator
    public static Composite fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(Composite.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(Composite.class, value));
    }

    public int value() {
        return value;
    }

    public String label() {
        return label;
    }
}
