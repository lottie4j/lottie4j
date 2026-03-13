package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * Defines the line cap style used to render the endpoints of open paths and strokes.
 * This enum represents the three standard cap styles used in vector graphics to determine
 * how the end of a stroke should be rendered.
 * <p>
 * The butt cap style terminates the stroke exactly at the endpoint with a flat edge
 * perpendicular to the stroke direction, with no extension beyond the endpoint.
 * <p>
 * The round cap style extends the stroke beyond the endpoint with a semicircular cap
 * whose diameter equals the stroke width, creating a smooth rounded end.
 * <p>
 * The square cap style extends the stroke beyond the endpoint with a rectangular cap
 * whose width equals the stroke width and extends by half the stroke width, creating
 * a squared end that projects beyond the endpoint.
 * <p>
 * This enum is used in Lottie animation files to specify how stroked paths should be
 * rendered at their endpoints, affecting the visual appearance of lines and open shapes.
 */
public enum LineCap implements DefinitionWithLabel {
    /** Flat cap with no extension beyond the endpoint. */
    BUTT(1, "Butt"),
    /** Rounded cap extending beyond the endpoint. */
    ROUND(2, "Round"),
    /** Square cap extending beyond the endpoint. */
    SQUARE(3, "Square");

    @JsonValue
    private final int value;
    private final String label;

    /**
     * Constructs a LineCap with the specified value and label.
     *
     * @param value the numeric value representing this line cap type
     * @param label the human-readable label for this line cap type
     */
    LineCap(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the line cap value
     * @return the LineCap corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any LineCap
     */
    @JsonCreator
    public static LineCap fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(LineCap.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(LineCap.class, value));
    }

    /**
     * Returns the numeric value of this line cap type.
     *
     * @return the numeric value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the human-readable label for this line cap type.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
