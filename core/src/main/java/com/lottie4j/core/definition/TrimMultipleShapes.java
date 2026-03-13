package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * Defines how trim operations are applied when multiple shapes are being trimmed together.
 * This enum represents the behavior modes for trim path effects when applied to multiple
 * shapes within a Lottie animation, controlling whether shapes are trimmed as separate
 * entities or as a unified group.
 * <p>
 * The individually mode applies the trim operation to each shape separately, treating
 * each shape as an independent entity. Each shape's trim calculations are performed
 * independently, resulting in each shape being trimmed according to its own path length
 * and position.
 * <p>
 * The simultaneously mode applies the trim operation to all shapes as a single unified
 * path. The shapes are treated as if they were merged together, and the trim calculation
 * is performed on the combined path length, resulting in a synchronized trim effect
 * across all shapes.
 * <p>
 * This enum is used in Lottie animation files to specify the behavior of trim path
 * modifiers when working with groups of shapes, affecting how animated trims are
 * distributed across multiple shape elements.
 */
public enum TrimMultipleShapes implements DefinitionWithLabel {
    /**
     * Apply trim operation to each shape separately.
     */
    INDIVIDUALLY(1, "Individually"),

    /**
     * Apply trim operation to all shapes as unified path.
     */
    SIMULTANEOUSLY(2, "Simultaneously");

    @JsonValue
    private final int value;
    private final String label;

    /**
     * Constructs a TrimMultipleShapes with the specified value and label.
     *
     * @param value the numeric value representing this trim mode
     * @param label the human-readable label for this trim mode
     */
    TrimMultipleShapes(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the trim mode value
     * @return the TrimMultipleShapes corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any TrimMultipleShapes
     */
    @JsonCreator
    public static TrimMultipleShapes fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(TrimMultipleShapes.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(TrimMultipleShapes.class, value));
    }

    /**
     * Returns the numeric value of this trim mode.
     *
     * @return the numeric value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the human-readable label for this trim mode.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
