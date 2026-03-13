package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * Defines the matte mode used for track matte compositing operations in Lottie animations.
 * A track matte uses the pixel information from one layer to control the transparency or
 * visibility of another layer, enabling complex masking and compositing effects.
 * <p>
 * Normal mode disables track matte compositing, rendering the layer without any matte effects.
 * <p>
 * Alpha mode uses the alpha channel values of the matte layer to determine the transparency
 * of the target layer, where fully opaque pixels in the matte result in fully visible pixels
 * in the target layer.
 * <p>
 * Inverted Alpha mode inverts the alpha channel of the matte layer before applying it,
 * making transparent areas of the matte reveal the target layer and opaque areas hide it.
 * <p>
 * Luma mode uses the luminance (brightness) values of the matte layer to determine the
 * transparency of the target layer, where brighter pixels in the matte result in more
 * visible pixels in the target layer.
 * <p>
 * Inverted Luma mode inverts the luminance values of the matte layer before applying it,
 * making darker areas of the matte reveal the target layer and brighter areas hide it.
 * <p>
 * This enum is used in Lottie animation files to specify how layers interact through
 * track matte compositing, enabling sophisticated masking and transparency effects.
 */
public enum MatteMode implements DefinitionWithLabel {
    NORMAL(0, "Normal"),
    ALPHA(1, "Alpha"),
    INVERTED_ALPHA(2, "Inverted Alpha"),
    LUMA(3, "Luma"),
    INVERTED_LUMA(4, "Inverted Luma");

    @JsonValue
    private final int value;
    private final String label;

    /**
     * Constructs a MatteMode with the specified value and label.
     *
     * @param value the numeric value representing this matte mode
     * @param label the human-readable label for this matte mode
     */
    MatteMode(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the matte mode value
     * @return the MatteMode corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any MatteMode
     */
    @JsonCreator
    public static MatteMode fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(MatteMode.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(MatteMode.class, value));
    }

    /**
     * Returns the numeric value of this matte mode.
     *
     * @return the numeric value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the human-readable label for this matte mode.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
