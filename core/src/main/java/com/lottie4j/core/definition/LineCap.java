package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#linecap
 */
public enum LineCap implements DefinitionWithLabel {
    BUTT(1, "Butt"),
    ROUND(2, "Round"),
    SQUARE(3, "Square");

    @JsonValue
    private final int value;
    private final String label;

    /**
     * Constructs a LineCap with the specified value and label.
     *
     * @param value the numeric value representing this line cap type
     * @param label the human-readable label for this line cap type
     */
    LineCap(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the line cap value
     * @return the LineCap corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any LineCap
     */
    @JsonCreator
    public static LineCap fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(LineCap.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(LineCap.class, value));
    }

    /**
     * Returns the numeric value of this line cap type.
     *
     * @return the numeric value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the human-readable label for this line cap type.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
