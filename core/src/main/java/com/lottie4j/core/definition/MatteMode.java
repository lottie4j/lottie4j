package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#blendmode
 */
public enum MatteMode implements DefinitionWithLabel {
    NORMAL(0, "Normal"),
    ALPHA(1, "Alpha"),
    INVERTED_ALPHA(2, "Inverted Alpha"),
    LUMA(3, "Luma"),
    INVERTED_LUMA(4, "Inverted Luma");

    @JsonValue
    private final int value;
    private final String label;

    /**
     * Constructs a MatteMode with the specified value and label.
     *
     * @param value the numeric value representing this matte mode
     * @param label the human-readable label for this matte mode
     */
    MatteMode(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the matte mode value
     * @return the MatteMode corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any MatteMode
     */
    @JsonCreator
    public static MatteMode fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(MatteMode.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(MatteMode.class, value));
    }

    /**
     * Returns the numeric value of this matte mode.
     *
     * @return the numeric value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the human-readable label for this matte mode.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
