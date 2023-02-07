package com.lottie4j.core.exception;

public class LottieModelDefinitionException extends Exception {
    public LottieModelDefinitionException(Class clazz, String value) {
        super("Definition not found in " + clazz.getName() + " for: " + value);
    }
}
