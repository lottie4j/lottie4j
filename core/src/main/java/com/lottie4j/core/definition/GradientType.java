package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * Defines the type of gradient fill used in Lottie animations.
 * This enum represents the two standard gradient types supported in vector graphics:
 * linear and radial gradients.
 * <p>
 * A linear gradient creates a color transition along a straight line, with colors
 * blending from the start point to the end point in a linear fashion.
 * <p>
 * A radial gradient creates a color transition that radiates outward from a central
 * point in a circular or elliptical pattern, with colors blending from the center
 * to the outer edge.
 * <p>
 * This enum is used in Lottie animation files to specify how gradient fills should
 * be rendered on shapes and paths. The enum supports JSON serialization and
 * deserialization for parsing Lottie animation data.
 */
public enum GradientType implements DefinitionWithLabel {
    /** Creates a linear gradient along a straight line. */
    LINEAR(1, "Linear"),
    /** Creates a radial gradient radiating from a center point. */
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
