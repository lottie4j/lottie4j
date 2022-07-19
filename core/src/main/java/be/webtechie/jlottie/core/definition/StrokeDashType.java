package be.webtechie.jlottie.core.definition;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#strokedashtype
 */
public enum StrokeDashType {
    DASH("d", "Dash"),
    GAP("g", "Gap"),
    OFFSET("o", "Offset");

    @JsonValue
    private final String value;
    private final String label;

    StrokeDashType(String value, String label) {
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
