package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * https://lottiefiles.github.io/lottie-docs/shapes/#shape-element
 */
public enum ShapeType {
    ELLIPSE("el", "Ellipse"),
    FILL("fl", "Fill"),
    GRADIENT_FILL("gf", "Gradient Fill"),
    GRADIENT_STROKE("gs", "Gradient Stroke"),
    GROUP("gr", "Group"),
    MERGE("mm", "Merge"),
    NO_STYLE("no", "No Style"),
    OFFSET_PATH("op", "Offset Path"),
    PATH("sh", "Path"),
    POLYSTAR("sr", "PolyStar"),
    PUCKER("pb", "Pucker / Bloat"),
    RECTANGLE("rc", "Rectangle"),
    REPEATER("rp", "Repeater"),
    ROUNDED_CORNERS("rd", "Rounded Corners"),
    STROKE("st", "Stroke"),
    TRANSFORM("tr", "Transform"),
    TRIM("tm", "Trim"),
    TWIST("tw", "Twist"),
    ZIG_ZAG("zz", "Zig Zag");

    @JsonValue
    private final String value;
    private final String label;

    ShapeType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static ShapeType fromValue(String ty) {
        for (ShapeType shapeType : ShapeType.values()) {
            if (shapeType.value.equals(ty)) {
                return shapeType;
            }
        }
        throw new IllegalArgumentException("ShapeType " + ty + " is not defined");
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }
}
