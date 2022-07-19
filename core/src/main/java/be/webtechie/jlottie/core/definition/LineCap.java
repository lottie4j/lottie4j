package be.webtechie.jlottie.core.definition;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#linecap
 */
public enum LineCap {
    // TODO
    // This undefined is not part of the spec, but seems to be needed to correctly parse the test JSON files.
    // Strange, looks like the @JsonValue doesn't work in this case, why?
    UNDEFINED(0, "Undefined"),
    BUTT(1, "Butt"),
    ROUND(2, "Round"),
    SQUARE(3, "Square");

    @JsonValue
    private final int value;
    private final String label;

    LineCap(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int value() {
        return value;
    }

    public String label() {
        return label;
    }
}
