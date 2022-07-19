package be.webtechie.jlottie.core.definition;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#linejoin
 */
public enum LineJoin {
    NO("n", "No"),
    ADD("a", "Add"),
    SUBSTRACT("s", "Subtract"),
    INTERSECT("i", "Intersect"),
    LIGHTEN("l", "Lighten"),
    DARKEN("d", "Darken"),
    DIFFERENCE("f", "Difference");

    @JsonValue
    private final String value;
    private final String label;

    LineJoin(String value, String label) {
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
