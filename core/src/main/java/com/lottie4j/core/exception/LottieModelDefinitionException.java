package com.lottie4j.core.exception;

/**
 * Exception thrown when a Lottie definition value cannot be mapped to a known enum constant.
 * Typically occurs when parsing JSON with unrecognized or unsupported values.
 */
public class LottieModelDefinitionException extends Exception {
    /**
     * Creates an exception for an unmappable definition value.
     *
     * @param clazz the enum class that failed to map the value
     * @param value the unrecognized value that could not be mapped
     */
    public LottieModelDefinitionException(Class clazz, String value) {
        super("Definition not found in " + clazz.getName() + " for: " + value);
    }
}
