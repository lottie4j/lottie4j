package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/layers/
 */
public enum LayerType {
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
    CAMERA(13, "Camera 	3D Camera", ""),
    LIGHT(14, "Light", ""),
    DATA(15, "Data", "");

    @JsonValue
    private final int value;
    private final String label;
    private final String description;

    LayerType(int value, String label, String description) {
        this.value = value;
        this.label = label;
        this.description = description;
    }

    @JsonCreator
    public static LayerType fromValue(String value) {
        return Arrays.stream(LayerType.values()).sequential()
                .filter(v -> String.valueOf(v.value).equals(value))
                .findFirst()
                .get();
    }

    public int value() {
        return value;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }
}
