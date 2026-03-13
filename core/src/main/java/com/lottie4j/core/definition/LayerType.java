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
    /** Renders a precomposition (nested composition). */
    PRECOMPOSITION(0, "Precomposition", "Renders a Precomposition"),
    /** Static rectangle filling the canvas with a single color. */
    SOLD_COLOR(1, "Solid Color", "Static rectangle filling the canvas with a single color"),
    /** Renders a static image. */
    IMAGE(2, "Image", "Renders an Image"),
    /** Empty layer used only for parenting other layers. */
    NULL(3, "Null (Empty)", "No contents, only used for parenting"),
    /** Contains an array of shapes to render vector graphics. */
    SHAPE(4, "Shape", "Has an array of shapes"),
    /** Renders text content. */
    TEXT(5, "Text", "Renders text"),
    /** Plays audio content. */
    AUDIO(6, "Audio", "Plays some audio"),
    /** Placeholder for video content. */
    VIDEO_PLACEHOLDER(7, "Video Placeholder", ""),
    /** Sequence of images for frame-by-frame animation. */
    IMAGE_SEQUENCE(8, "Image Sequence", ""),
    /** Renders video content. */
    VIDEO(9, "Video", ""),
    /** Placeholder for image content. */
    IMAGE_PLACEHOLDER(10, "Image Placeholder", ""),
    /** Guide layer for design reference (not rendered). */
    GUIDE(11, "Guide", ""),
    /** Adjustment layer for applying effects to layers below. */
    ADJUSTMENT(12, "Adjustment", ""),
    /** 3D camera for perspective and view control. */
    CAMERA(13, "3D Camera", ""),
    /** Light source for 3D lighting effects. */
    LIGHT(14, "Light", ""),
    /** Data layer for storing animation data. */
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
