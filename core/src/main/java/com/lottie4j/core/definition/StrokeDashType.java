package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * Defines the dash pattern components used to create dashed or dotted strokes in vector graphics.
 * This enum represents the three essential elements that compose a stroke dash pattern in Lottie animations.
 * <p>
 * A dash pattern is created by combining these components to define how a continuous stroke should be
 * broken into segments. The dash component represents the visible portion of the stroke pattern,
 * defining the length of each drawn segment. The gap component represents the invisible portion,
 * defining the spacing between consecutive dash segments. The offset component shifts the starting
 * position of the entire dash pattern along the stroke path.
 * <p>
 * These components work together to create various stroke effects, from simple dashed lines to
 * complex animated patterns. The dash and gap values are typically used in alternating sequences
 * to define repeating patterns, while the offset value allows for pattern animation by shifting
 * the pattern position.
 * <p>
 * This enum is used in Lottie animation files to specify stroke dash patterns, enabling the
 * creation of non-continuous strokes and animated stroke effects that are commonly used in
 * motion graphics and vector animations.
 */
public enum StrokeDashType implements DefinitionWithLabel {
    /**
     * Visible portion of the stroke pattern.
     */
    DASH("d", "Dash"),

    /**
     * Invisible spacing between consecutive dash segments.
     */
    GAP("g", "Gap"),

    /**
     * Starting position shift of the dash pattern.
     */
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
