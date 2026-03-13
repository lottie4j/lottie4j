package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * Enumeration of blend modes used in Lottie animations for compositing layers and effects.
 * <p>
 * Blend modes determine how a layer's colors are combined with the colors of layers beneath it.
 * Each blend mode uses a different mathematical formula to calculate the final pixel color.
 * The modes range from simple operations like Normal (no blending) to complex color manipulations
 * like Hue, Saturation, and Luminosity.
 * <p>
 * This enum implements DefinitionWithLabel to provide human-readable labels for each blend mode
 * and supports JSON serialization/deserialization with numeric values. The fromValue method
 * handles both integer and decimal representations found in Lottie JSON files.
 */
public enum BlendMode implements DefinitionWithLabel {
    /** No blending; the layer appears as-is without color mixing. */
    NORMAL(0, "Normal"),
    /** Multiplies the colors, resulting in a darker image. */
    MULTIPLY(1, "Multiply"),
    /** Inverts, multiplies, and inverts again, resulting in a brighter image. */
    SCREEN(2, "Screen"),
    /** Combines Multiply and Screen modes depending on the base color. */
    OVERLAY(3, "Overlay"),
    /** Selects the darker of the two colors. */
    DARKEN(4, "Darken"),
    /** Selects the lighter of the two colors. */
    LIGHTEN(5, "Lighten"),
    /** Brightens the base color by decreasing contrast. */
    COLOR_DODGE(6, "Color Dodge"),
    /** Darkens the base color by increasing contrast. */
    COLOR_BURN(7, "Color Burn"),
    /** Combines Multiply and Screen modes with increased contrast. */
    HARD_LIGHT(8, "Hard Light"),
    /** Similar to Overlay but with a softer effect. */
    SOFT_LIGHT(9, "Soft Light"),
    /** Subtracts the darker color from the lighter color. */
    DIFFERENCE(10, "Difference"),
    /** Similar to Difference but with lower contrast. */
    EXCLUSION(11, "Exclusion"),
    /** Applies the hue of the blend color while preserving saturation and luminosity. */
    HUE(12, "Hue"),
    /** Applies the saturation of the blend color while preserving hue and luminosity. */
    SATURATION(13, "Saturation"),
    /** Applies the hue and saturation of the blend color while preserving luminosity. */
    COLOR(14, "Color"),
    /** Applies the luminosity of the blend color while preserving hue and saturation. */
    LUMINOSITY(15, "Luminosity");

    @JsonValue
    private final int value;
    private final String label;

    /**
     * Constructs a BlendMode with the specified value and label.
     *
     * @param value the numeric value representing this blend mode
     * @param label the human-readable label for this blend mode
     */
    BlendMode(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the blend mode value
     * @return the BlendMode corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any BlendMode
     */
    @JsonCreator
    public static BlendMode fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(BlendMode.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(BlendMode.class, value));
    }

    /**
     * Returns the numeric value of this blend mode.
     *
     * @return the numeric value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the human-readable label for this blend mode.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
