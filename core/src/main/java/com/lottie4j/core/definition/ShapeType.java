package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

/**
 * Enumeration of shape types used in Lottie animations.
 * <p>
 * This enum defines all supported shape element types that can appear in a Lottie animation,
 * categorized by their functional role. Each shape type includes a short string value used
 * for JSON serialization/deserialization, a human-readable label, and a classification into
 * a shape group category.
 * <p>
 * Shape types are organized into several categories:
 * <p>
 * Basic shapes (SHAPE group) define visible geometric elements like ellipses, rectangles,
 * paths, and polystar shapes that form the visual content of the animation.
 * <p>
 * Style elements (STYLE group) control the visual appearance of shapes through fills,
 * strokes, gradients, and other styling properties.
 * <p>
 * Modifiers (MODIFIER group) transform or alter existing shapes through operations like
 * merge, trim, repeater, rounded corners, pucker/bloat, twist, zig zag, and offset path.
 * <p>
 * Grouping elements (GROUP group) provide organizational structure through groups and
 * transforms that allow hierarchical composition and transformation of shape elements.
 * <p>
 * The UNKNOWN type serves as a fallback for unrecognized or unsupported shape types
 * encountered during parsing.
 * <p>
 * This enum implements DefinitionWithLabel to provide human-readable descriptions and
 * uses Jackson annotations for JSON serialization, with the value field serving as the
 * JSON representation.
 */
public enum ShapeType implements DefinitionWithLabel {
    /** Ellipse or circle shape. */
    ELLIPSE("el", "Ellipse", ShapeGroup.SHAPE),
    /** Solid color fill. */
    FILL("fl", "Fill", ShapeGroup.STYLE),
    /** Gradient fill with color transitions. */
    GRADIENT_FILL("gf", "Gradient Fill", ShapeGroup.STYLE),
    /** Gradient stroke with color transitions. */
    GRADIENT_STROKE("gs", "Gradient Stroke", ShapeGroup.STYLE),
    /** Container for organizing multiple shapes. */
    GROUP("gr", "Group", ShapeGroup.GROUP),
    /** Merges multiple paths using boolean operations. */
    MERGE("mm", "Merge", ShapeGroup.MODIFIER),
    /** Placeholder for no style applied. */
    NO_STYLE("no", "No Style", ShapeGroup.STYLE),
    /** Offsets a path by a specified distance. */
    OFFSET_PATH("op", "Offset Path", ShapeGroup.MODIFIER),
    /** Custom bezier path shape. */
    PATH("sh", "Path", ShapeGroup.SHAPE),
    /** Star or polygon shape. */
    POLYSTAR("sr", "PolyStar", ShapeGroup.SHAPE),
    /** Pucker or bloat distortion effect. */
    PUCKER("pb", "Pucker / Bloat", ShapeGroup.MODIFIER),
    /** Rectangle or rounded rectangle shape. */
    RECTANGLE("rc", "Rectangle", ShapeGroup.SHAPE),
    /** Repeats shapes multiple times. */
    REPEATER("rp", "Repeater", ShapeGroup.MODIFIER),
    /** Rounds the corners of shapes. */
    ROUNDED_CORNERS("rd", "Rounded Corners", ShapeGroup.MODIFIER),
    /** Stroke outline. */
    STROKE("st", "Stroke", ShapeGroup.STYLE),
    /** Transformation properties for shapes. */
    TRANSFORM("tr", "Transform", ShapeGroup.GROUP),
    /** Trims paths to partial segments. */
    TRIM("tm", "Trim", ShapeGroup.SHAPE),
    /** Twists shapes around a point. */
    TWIST("tw", "Twist", ShapeGroup.MODIFIER),
    /** Creates a zigzag pattern along a path. */
    ZIG_ZAG("zz", "Zig Zag", ShapeGroup.MODIFIER),
    /** Unknown or unrecognized shape type. */
    UNKNOWN("", "", ShapeGroup.UNKNOWN);

    @JsonValue
    private final String value;
    private final String label;
    private final ShapeGroup shapeGroup;

    /**
     * Constructs a ShapeType with the specified value, label, and shape group.
     *
     * @param value      the string value representing this shape type
     * @param label      the human-readable label for this shape type
     * @param shapeGroup the shape group category this type belongs to
     */
    ShapeType(String value, String label, ShapeGroup shapeGroup) {
        this.value = value;
        this.label = label;
        this.shapeGroup = shapeGroup;
    }

    /**
     * Creates a ShapeType from its string value.
     *
     * @param value the string representation of the shape type
     * @return the ShapeType corresponding to the given value
     * @throws LottieModelDefinitionException if the value doesn't match any ShapeType
     */
    @JsonCreator
    public static ShapeType fromValue(String value) throws LottieModelDefinitionException {
        for (ShapeType shapeType : ShapeType.values()) {
            if (shapeType.value.equalsIgnoreCase(value)) {
                return shapeType;
            }
        }
        throw new LottieModelDefinitionException(ShapeType.class, value);
    }

    /**
     * Returns the string value of this shape type.
     *
     * @return the string value
     */
    public String value() {
        return value;
    }

    /**
     * Returns the human-readable label for this shape type.
     *
     * @return the label
     */
    @Override
    public String label() {
        return label;
    }

    /**
     * Returns the shape group category this type belongs to.
     *
     * @return the shape group
     */
    public ShapeGroup shapeGroup() {
        return shapeGroup;
    }
}
