package com.lottie4j.core.definition;

import com.lottie4j.core.info.DefinitionWithLabel;

public enum ShapeGroup implements DefinitionWithLabel {
    GROUP,
    MODIFIER,
    SHAPE,
    STYLE,
    UNKNOWN;

    @Override
    public String label() {
        return this.name();
    }
}
