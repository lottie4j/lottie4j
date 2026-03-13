package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * Defines the merge mode used to combine multiple paths or shapes in vector graphics.
 * This enum represents the different boolean operations that can be applied when merging
 * multiple shape layers or paths together in a Lottie animation.
 * <p>
 * The normal mode renders shapes without any merge operation, stacking them as separate layers.
 * <p>
 * The add mode performs a union operation, combining all overlapping and non-overlapping
 * areas of the shapes into a single merged shape.
 * <p>
 * The subtract mode removes the area of subsequent shapes from the first shape, creating
 * a difference operation where overlapping regions are cut out.
 * <p>
 * The intersect mode keeps only the overlapping areas where all shapes intersect, discarding
 * all non-overlapping regions.
 * <p>
 * The exclude intersections mode keeps only the non-overlapping areas of shapes, removing
 * any regions where shapes overlap, creating an exclusive-or operation.
 * <p>
 * This enum is used in Lottie animation files to control how multiple shape paths are
 * combined, enabling complex shape compositions through boolean path operations.
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
