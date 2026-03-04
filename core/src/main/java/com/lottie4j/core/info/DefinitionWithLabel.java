package com.lottie4j.core.info;

/**
 * Interface for definition enums that provide human-readable labels.
 * Used by enums like BlendMode, LayerType, etc. to expose descriptive names.
 */
public interface DefinitionWithLabel {

    /**
     * Returns a human-readable label for this definition.
     *
     * @return display label
     */
    String label();
}
