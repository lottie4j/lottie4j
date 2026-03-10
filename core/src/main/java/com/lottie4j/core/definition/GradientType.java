package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#linecap
 */
public enum GradientType implements DefinitionWithLabel {
    LINEAR(1, "Linear"),
    RADIAL(2, "Radial");

    @JsonValue
    private final int value;
    private final String label;

    /**
     * Constructs a GradientType with the specified value and label.
     *
     * @param value the numeric value representing this gradient type
     * @param label the human-readable label for this gradient type
     */
    GradientType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the gradient type value
     * @return the GradientType corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any GradientType
     */
    @JsonCreator
    public static GradientType fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(GradientType.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(GradientType.class, value));
    }

    /**
     * Returns the numeric value of this gradient type.
     *
     * @return the numeric value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the human-readable label for this gradient type.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
