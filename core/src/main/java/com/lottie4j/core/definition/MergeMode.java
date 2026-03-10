package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#blendmode
 */
public enum MergeMode implements DefinitionWithLabel {
    NORMAL(1, "Normal"),
    ADD(2, "Add"),
    SUBTRACT(3, "Subtract"),
    INTERSECT(4, "Intersect"),
    EXCLUDE(5, "Exclude Intersections");

    @JsonValue
    private final int value;
    private final String label;

    /**
     * Constructs a MergeMode with the specified value and label.
     *
     * @param value the numeric value representing this merge mode
     * @param label the human-readable label for this merge mode
     */
    MergeMode(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the merge mode value
     * @return the MergeMode corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any MergeMode
     */
    @JsonCreator
    public static MergeMode fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(MergeMode.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(MergeMode.class, value));
    }

    /**
     * Returns the numeric value of this merge mode.
     *
     * @return the numeric value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the human-readable label for this merge mode.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
