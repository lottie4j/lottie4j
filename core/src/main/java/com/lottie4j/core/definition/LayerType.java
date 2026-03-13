package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * Enumeration of layer types supported in Lottie animations.
 * Each layer type represents a different kind of visual or functional element that can be rendered
 * in a Lottie composition. Layer types define how the layer content is interpreted and displayed,
 * ranging from visual elements like shapes and images to functional elements like cameras and guides.
 * <p>
 * This enum supports JSON deserialization with tolerance for decimal numeric values that may appear
 * in some Lottie files, automatically rounding them to the nearest integer for matching.
 */
public enum LayerType implements DefinitionWithLabel {
    PRECOMPOSITION(0, "Precomposition", "Renders a Precomposition"),
    SOLD_COLOR(1, "Solid Color", "Static rectangle filling the canvas with a single color"),
    IMAGE(2, "Image", "Renders an Image"),
    NULL(3, "Null (Empty)", "No contents, only used for parenting"),
    SHAPE(4, "Shape", "Has an array of shapes"),
    TEXT(5, "Text", "Renders text"),
    AUDIO(6, "Audio", "Plays some audio"),
    VIDEO_PLACEHOLDER(7, "Video Placeholder", ""),
    IMAGE_SEQUENCE(8, "Image Sequence", ""),
    VIDEO(9, "Video", ""),
    IMAGE_PLACEHOLDER(10, "Image Placeholder", ""),
    GUIDE(11, "Guide", ""),
    ADJUSTMENT(12, "Adjustment", ""),
    CAMERA(13, "3D Camera", ""),
    LIGHT(14, "Light", ""),
    DATA(15, "Data", "");

    @JsonValue
    private final Integer value;
    private final String label;
    private final String description;

    /**
     * Constructs a LayerType with the specified value, label, and description.
     *
     * @param value       the numeric value representing this layer type
     * @param label       the human-readable label for this layer type
     * @param description the description explaining this layer type's purpose
     */
    LayerType(Integer value, String label, String description) {
        this.value = value;
        this.label = label;
        this.description = description;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the layer type value
     * @return the LayerType corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any LayerType
     */
    @JsonCreator
    public static LayerType fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(LayerType.values()).sequential()
                .filter(v -> Math.round(Double.parseDouble(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(LayerType.class, value));
    }

    /**
     * Returns the numeric value of this layer type.
     *
     * @return the numeric value
     */
    public Integer value() {
        return value;
    }

    /**
     * Returns the human-readable label for this layer type.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }

    /**
     * Returns the description explaining this layer type's purpose.
     *
     * @return the description
     */
    public String description() {
        return description;
    }
}
