package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#linejoin
 */
public enum LineJoin implements DefinitionWithLabel {
    MITER(1, "Miter"),
    ROUND(2, "Round"),
    BEVEL(3, "Bevel");

    @JsonValue
    private final int value;
    private final String label;

    /**
     * Constructs a LineJoin with the specified value and label.
     *
     * @param value the numeric value representing this line join type
     * @param label the human-readable label for this line join type
     */
    LineJoin(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the line join value
     * @return the LineJoin corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any LineJoin
     */
    @JsonCreator
    public static LineJoin fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(LineJoin.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(LineJoin.class, value));
    }

    /**
     * Returns the numeric value of this line join type.
     *
     * @return the numeric value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the human-readable label for this line join type.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
