package com.lottie4j.core.exception;

/**
 * Exception thrown when a Lottie file cannot be read.
 */
public class LottieFileException extends Exception {
    /**
     * Creates an exception for a Lottie file problem
     *
     * @param description description of the problem
     */
    public LottieFileException(String description) {
        super("Problem with the given Lottie file: " + description);
    }
}
