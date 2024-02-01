package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

/**
 * https://lottiefiles.github.io/lottie-docs/shapes/#shape-element
 */
public enum ShapeType implements DefinitionWithLabel {
    ELLIPSE("el", "Ellipse", ShapeGroup.SHAPE),
    FILL("fl", "Fill", ShapeGroup.STYLE),
    GRADIENT_FILL("gf", "Gradient Fill", ShapeGroup.STYLE),
    GRADIENT_STROKE("gs", "Gradient Stroke", ShapeGroup.STYLE),
    GROUP("gr", "Group", ShapeGroup.GROUP),
    MERGE("mm", "Merge", ShapeGroup.MODIFIER),
    NO_STYLE("no", "No Style", ShapeGroup.STYLE),
    OFFSET_PATH("op", "Offset Path", ShapeGroup.MODIFIER),
    PATH("sh", "Path", ShapeGroup.SHAPE),
    POLYSTAR("sr", "PolyStar", ShapeGroup.SHAPE),
    PUCKER("pb", "Pucker / Bloat", ShapeGroup.MODIFIER),
    RECTANGLE("rc", "Rectangle", ShapeGroup.SHAPE),
    REPEATER("rp", "Repeater", ShapeGroup.MODIFIER),
    ROUNDED_CORNERS("rd", "Rounded Corners", ShapeGroup.MODIFIER),
    STROKE("st", "Stroke", ShapeGroup.STYLE),
    TRANSFORM("tr", "Transform", ShapeGroup.GROUP),
    TRIM("tm", "Trim", ShapeGroup.SHAPE),
    TWIST("tw", "Twist", ShapeGroup.MODIFIER),
    ZIG_ZAG("zz", "Zig Zag", ShapeGroup.MODIFIER),
    UNKNOWN("", "", ShapeGroup.UNKNOWN);

    @JsonValue
    private final String value;
    private final String label;
    private final ShapeGroup shapeGroup;

    ShapeType(String value, String label, ShapeGroup shapeGroup) {
        this.value = value;
        this.label = label;
        this.shapeGroup = shapeGroup;
    }

    @JsonCreator
    public static ShapeType fromValue(String value) throws LottieModelDefinitionException {
        for (ShapeType shapeType : ShapeType.values()) {
            if (shapeType.value.equals(value)) {
                return shapeType;
            }
        }
        throw new LottieModelDefinitionException(ShapeType.class, value);
    }

    public String value() {
        return value;
    }

    @Override
    public String label() {
        return label;
    }

    public ShapeGroup shapeGroup() {
        return shapeGroup;
    }
}
