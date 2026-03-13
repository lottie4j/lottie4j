package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * Enumeration representing composite operation types used in Lottie animations.
 * Defines how layers or shapes are composited together in the rendering pipeline.
 * <p>
 * This enum implements DefinitionWithLabel to provide human-readable labels for each composite type
 * and supports JSON serialization/deserialization through Jackson annotations.
 * <p>
 * The composite values correspond to standard compositing operations where elements can be
 * positioned relative to each other in the layer stack.
 */
public enum Composite implements DefinitionWithLabel {
    ABOVE(1, "Above"),
    BELOW(2, "Below");

    @JsonValue
    private final int value;
    private final String label;

    /**
     * Constructs a Composite with the specified value and label.
     *
     * @param value the numeric value representing this composite type
     * @param label the human-readable label for this composite type
     */
    Composite(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the composite value
     * @return the Composite corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any Composite
     */
    @JsonCreator
    public static Composite fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(Composite.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(Composite.class, value));
    }

    /**
     * Returns the numeric value of this composite type.
     *
     * @return the numeric value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the human-readable label for this composite type.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
