package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#strokedashtype
 */
public enum StrokeDashType implements DefinitionWithLabel {
    DASH("d", "Dash"),
    GAP("g", "Gap"),
    OFFSET("o", "Offset");

    @JsonValue
    private final String value;
    private final String label;

    /**
     * Constructs a StrokeDashType with the specified value and label.
     *
     * @param value the string value representing this stroke dash type
     * @param label the human-readable label for this stroke dash type
     */
    StrokeDashType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Creates a StrokeDashType from its string value.
     *
     * @param value the string representation of the stroke dash type
     * @return the StrokeDashType corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any StrokeDashType
     */
    @JsonCreator
    public static StrokeDashType fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(StrokeDashType.values()).sequential()
                .filter(v -> String.valueOf(v.value).equals(value))
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(StrokeDashType.class, value));
    }

    /**
     * Returns the string value of this stroke dash type.
     *
     * @return the string value
     */
    public String value() {
        return value;
    }

    /**
     * Returns the human-readable label for this stroke dash type.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
