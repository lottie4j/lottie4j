package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * Defines the fill rule algorithm used to determine which areas of a path should be filled.
 * This enum represents the two standard fill rule algorithms used in vector graphics:
 * non-zero winding and even-odd.
 * <p>
 * The non-zero winding rule determines whether a point is inside a path by drawing
 * a ray from that point to infinity and counting the number of times the path crosses
 * the ray, taking direction into account. If the count is non-zero, the point is inside.
 * <p>
 * The even-odd rule determines whether a point is inside a path by drawing a ray from
 * that point to infinity and counting the number of times the path crosses the ray.
 * If the count is odd, the point is inside; if even, it is outside.
 * <p>
 * This enum is used in Lottie animation files to specify how filled shapes should be rendered,
 * particularly when dealing with complex paths that may overlap or have holes.
 */
public enum FillRule implements DefinitionWithLabel {
    NON_ZERO(1, "Non Zero"),
    EVEN_ODD(2, "Even Odd");

    @JsonValue
    private final int value;
    private final String label;

    /**
     * Constructs a FillRule with the specified value and label.
     *
     * @param value the numeric value representing this fill rule
     * @param label the human-readable label for this fill rule
     */
    FillRule(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the fill rule value
     * @return the FillRule corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any FillRule
     */
    @JsonCreator
    public static FillRule fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(FillRule.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(FillRule.class, value));
    }

    /**
     * Returns the numeric value of this fill rule.
     *
     * @return the numeric value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the human-readable label for this fill rule.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
