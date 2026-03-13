package com.lottie4j.core.definition;

import com.lottie4j.core.info.DefinitionWithLabel;

/**
 * Categorizes shapes and shape-related elements in Lottie animations into logical groups.
 * This enum is used to classify different types of shape elements based on their role
 * in the animation structure.
 * <p>
 * GROUP represents shape elements that act as containers for other shapes, allowing
 * hierarchical organization and transformation of multiple shape elements together.
 * <p>
 * MODIFIER represents shape elements that alter or transform other shapes, such as
 * trim paths, merge paths, or repeaters, which modify the appearance or behavior
 * of the shapes they affect.
 * <p>
 * SHAPE represents basic geometric shape elements such as rectangles, ellipses,
 * paths, and polygons that define the actual visible geometry in the animation.
 * <p>
 * STYLE represents shape elements that define visual appearance properties like
 * fills, strokes, gradients, and other styling attributes applied to shapes.
 * <p>
 * UNKNOWN represents shape elements that do not fit into any of the defined
 * categories or are not yet classified, providing a fallback category for
 * unrecognized or future shape types.
 * <p>
 * This enum is used throughout the Lottie parsing and rendering process to identify
 * and handle different types of shape elements appropriately.
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
