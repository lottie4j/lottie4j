package com.lottie4j.core.definition;

import com.lottie4j.core.info.DefinitionWithLabel;

/**
 * Defines the different categories of shape elements in a Lottie animation.
 * Each shape element belongs to one of these groups for organizational purposes.
 */
public enum ShapeGroup implements DefinitionWithLabel {
    GROUP,
    MODIFIER,
    SHAPE,
    STYLE,
    UNKNOWN;

    /**
     * Returns the label for this shape group, which is the enum constant name.
     *
     * @return the label
     */
    @Override
    public String label() {
        return this.name();
    }
}
