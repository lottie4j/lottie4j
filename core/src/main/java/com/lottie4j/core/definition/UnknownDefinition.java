package com.lottie4j.core.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What the definitions do with a value they do not know: report it and read it as absent.
 * <p>
 * A Lottie file from a newer exporter, or a damaged one, may carry a blend mode, a layer type or
 * a line cap this library has no constant for. Failing on it would reject the whole file for the
 * sake of one member; reading it as {@code null} keeps the rest of the animation usable, and the
 * code that uses the value already has to deal with the member being absent.
 */
final class UnknownDefinition {

    private static final Logger logger = LoggerFactory.getLogger(UnknownDefinition.class);

    private UnknownDefinition() {
    }

    /**
     * Reports a value that is not one of the constants of a definition.
     *
     * @param type  the definition
     * @param value the value found in the file
     * @param <T>   the definition
     * @return always {@code null}
     */
    static <T> T report(Class<T> type, String value) {
        logger.warn("Unknown {} value \"{}\" is ignored", type.getSimpleName(), value);
        return null;
    }

    /**
     * Reads a numeric value, which some files write with decimals ({@code 4.0} for {@code 4}).
     *
     * @param value the text of the value
     * @return the value rounded to a whole number, or {@link Long#MIN_VALUE}, which no definition
     *         uses, if the text is not a number
     */
    static long round(String value) {
        try {
            double number = Double.parseDouble(value);
            return Double.isFinite(number) ? Math.round(number) : Long.MIN_VALUE;
        } catch (NumberFormatException | NullPointerException e) {
            return Long.MIN_VALUE;
        }
    }
}
