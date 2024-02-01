package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lottie4j.core.exception.LottieModelDefinitionException;
import com.lottie4j.core.info.DefinitionWithLabel;

import java.util.Arrays;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#linejoin
 */
public enum LineJoin implements DefinitionWithLabel {
    MITER(1, "Miter"),
    ROUND(2, "Round"),
    BEVEL(3, "Bevel");

    @JsonValue
    private final int value;
    private final String label;

    LineJoin(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Some files seem to contain decimal values. So some extra convertion is needed.
     */
    @JsonCreator
    public static LineJoin fromValue(String value) throws LottieModelDefinitionException {
        return Arrays.stream(LineJoin.values()).sequential()
                .filter(v -> Math.round(Double.valueOf(value)) == v.value)
                .findFirst()
                .orElseThrow(() -> new LottieModelDefinitionException(LineJoin.class, value));
    }

    public int value() {
        return value;
    }

    @Override
    public String label() {
        return label;
    }
}
