package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * Enumeration of effect types supported in Lottie animations.
 * <p>
 * Each effect type represents a visual transformation or filter that can be applied to layers
 * in a Lottie animation. Effect types are identified by numeric values used in Lottie JSON files
 * and include descriptive labels for human readability.
 * <p>
 * This enum implements DefinitionWithLabel to provide both programmatic access via numeric values
 * and user-friendly display names. JSON deserialization is supported through the fromValue method,
 * which handles both integer and decimal string representations of effect type values.
 */
public enum EffectType implements DefinitionWithLabel {
    /** Legacy effect type for older Lottie files. */
    NORMAL(5, "Old-style Effect"),
    /** Paints effects over transparent areas. */
    PAINT_OVER_TRANSPARENT(7, "Paint Over Transparent"),
    /** Applies a color tint to the layer. */
    TINT(20, "Tint"),
    /** Fills the layer with a solid color. */
    FILL(21, "Fill"),
    /** Adds a stroke effect to the layer. */
    STROKE(22, "Stroke"),
    /** Applies a three-color gradient mapping. */
    TRITONE(23, "Tritone"),
    /** Advanced color level adjustments. */
    PRO_LEVELS(24, "Pro Levels"),
    /** Creates a shadow effect beneath the layer. */
    DROP_SHADOW(25, "Drop Shadow"),
    /** Wipes the layer in a circular pattern. */
    RADIAL_WIPE(26, "Radial Wipe"),
    /** Displaces pixels based on a displacement map. */
    DISPLACEMENT_MAP(27, "Displacement Map"),
    /** Applies a matte effect (version 3). */
    MATTE3(28, "Matte3"),
    /** Blurs the layer using a Gaussian blur algorithm. */
    GAUSSIAN_BLUR(29, "Gaussian Blur"),
    /** Warps the layer using a mesh deformation. */
    MESH_WARP(31, "Mesh Warp"),
    /** Creates a wavy distortion effect. */
    WAVY(32, "Wavy"),
    /** Applies a spherical distortion to the layer. */
    SPHERIZE(33, "Spherize"),
    /** Puppet pin deformation effect. */
    PUPPET(34, "Puppet");

    @JsonValue
    private final int value;
    private final String label;

    /**
     * Constructs an EffectType with the specified value and label.
     *
     * @param value the numeric value representing this effect type
     * @param label the human-readable label for this effect type
     */
    EffectType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the effect type value
     * @return the EffectType corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any EffectType
     */
    @JsonCreator
    public static EffectType fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(EffectType.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(EffectType.class, value));
    }

    /**
     * Returns the numeric value of this effect type.
     *
     * @return the numeric value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the human-readable label for this effect type.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
