package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/layers/
 */
public enum LayerType implements DefinitionWithLabel {
    PRECOMPOSITION(0D, "Precomposition", "Renders a Precomposition"),
    SOLD_COLOR(1D, "Solid Color", "Static rectangle filling the canvas with a single color"),
    IMAGE(2D, "Image", "Renders an Image"),
    NULL(3D, "Null (Empty)", "No contents, only used for parenting"),
    SHAPE(4D, "Shape", "Has an array of shapes"),
    TEXT(5D, "Text", "Renders text"),
    AUDIO(6D, "Audio", "Plays some audio"),
    VIDEO_PLACEHOLDER(7D, "Video Placeholder", ""),
    IMAGE_SEQUENCE(8D, "Image Sequence", ""),
    VIDEO(9D, "Video", ""),
    IMAGE_PLACEHOLDER(10D, "Image Placeholder", ""),
    GUIDE(11D, "Guide", ""),
    ADJUSTMENT(12D, "Adjustment", ""),
    CAMERA(13D, "Camera 	3D Camera", ""),
    LIGHT(14D, "Light", ""),
    DATA(15D, "Data", "");

    @JsonValue
    private final Double value;
    private final String label;
    private final String description;

    /**
     * Constructs a LayerType with the specified value, label, and description.
     *
     * @param value the numeric value representing this layer type
     * @param label the human-readable label for this layer type
     * @param description the description explaining this layer type's purpose
     */
    LayerType(Double value, String label, String description) {
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
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(LayerType.class, value));
    }

    /**
     * Returns the numeric value of this layer type.
     *
     * @return the numeric value
     */
    public Double value() {
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
