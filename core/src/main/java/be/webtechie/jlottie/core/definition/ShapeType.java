package be.webtechie.jlottie.core.definition;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * https://lottiefiles.github.io/lottie-docs/shapes/#shape-element
 */
public enum ShapeType {
    RECTANGLE("rc", "Rectangle"),
    ELLIPSE("el", "Ellipse"),
    POLYSTAR("sr", "PolyStar"),
    PATH("sh", "Path"),
    FILL("fl", "Fill"),
    STROKE("st", "Stroke"),
    GRADIENT_FILL("gf", "Gradient Fill"),
    GRADIENT_STROKE("gs", "Gradient Stroke"),
    NO_STYLE("no", "No Style"),
    GROUP("gr", "Group"),
    TRANSFORM("tr", "Transform"),
    REPEATER("rp", "Repeater"),
    TRIM("tm", "Trim"),
    ROUNDED_CORNERS("rd", "Rounded Corners"),
    PUCKER("pb", "Pucker / Bloat"),
    MERGE("mm", "Merge"),
    TWIST("tw", "Twist"),
    OFFSET_PATH("op", "Offset Path"),
    ZIG_ZAG("zz", "Zig Zag");

    @JsonValue
    private final String value;
    private final String label;

    ShapeType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }
}
