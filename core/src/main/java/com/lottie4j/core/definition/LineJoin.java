package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * Defines the line join style used to render the connection points where two line segments meet.
 * This enum represents the three standard join styles used in vector graphics to determine
 * how corners and vertices should be rendered when stroke paths change direction.
 * <p>
 * The miter join style extends the outer edges of the strokes until they meet at a point,
 * creating a sharp corner. This creates a pointed join that extends beyond the actual
 * intersection point of the path segments.
 * <p>
 * The round join style connects the outer edges of the strokes with a circular arc,
 * creating a smooth rounded corner. The radius of the arc equals half the stroke width,
 * resulting in a curved transition between path segments.
 * <p>
 * The bevel join style connects the outer edges of the strokes with a straight line,
 * creating a flat, chamfered corner. This clips the corner at the point where the outer
 * edges of each stroke would intersect, producing a truncated join.
 * <p>
 * This enum is used in Lottie animation files to specify how stroked paths should be
 * rendered at their join points, affecting the visual appearance of corners in lines
 * and shapes.
 */
public enum LineJoin implements DefinitionWithLabel {
    /** Sharp pointed corner extending beyond the join point. */
    MITER(1, "Miter"),
    /** Rounded corner with a circular arc. */
    ROUND(2, "Round"),
    /** Flat beveled corner connecting the stroke edges. */
    BEVEL(3, "Bevel");

    @JsonValue
    private final int value;
    private final String label;

    /**
     * Constructs a LineJoin with the specified value and label.
     *
     * @param value the numeric value representing this line join type
     * @param label the human-readable label for this line join type
     */
    LineJoin(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     *
     * @param value the string representation of the line join value
     * @return the LineJoin corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any LineJoin
     */
    @JsonCreator
    public static LineJoin fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(LineJoin.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(LineJoin.class, value));
    }

    /**
     * Returns the numeric value of this line join type.
     *
     * @return the numeric value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the human-readable label for this line join type.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }
}
