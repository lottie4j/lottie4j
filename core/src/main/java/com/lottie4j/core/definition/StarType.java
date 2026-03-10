package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#star-type
 */
public enum StarType implements DefinitionWithLabel {
    STAR(1, "Star"),
    POLYGON(2, "Polygon");

    @JsonValue
    private final int value;
    private final String label;

    /**
     * Constructs a StarType with the specified value and label.
     *
     * @param value the numeric value representing this star type
     * @param label the human-readable label for this star type
     */
    StarType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the star type value
     * @return the StarType corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any StarType
     */
    @JsonCreator
    public static StarType fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(StarType.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(StarType.class, value));
    }

    /**
     * Returns the numeric value of this star type.
     *
     * @return the numeric value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the human-readable label for this star type.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
