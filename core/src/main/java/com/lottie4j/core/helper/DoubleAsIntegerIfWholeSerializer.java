package com.lottie4j.core.helper;

import tools.jackson.core.JacksonException;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;


/**
 * Jackson serializer that outputs Double values as integers when they represent whole numbers,
 * but preserves decimal representation for fractional values.
 * <p>
 * This is used to match the Lottie JSON specification which typically uses integer frame numbers
 * (e.g., "t": 0) rather than floating point (e.g., "t": 0.0), while still supporting fractional
 * frames when needed.
 */
public class DoubleAsIntegerIfWholeSerializer extends ValueSerializer<Double> {

    /**
     * Creates a serializer that writes whole-valued doubles as JSON integers.
     */
    public DoubleAsIntegerIfWholeSerializer() {
        // Default constructor for serializer registration.
    }

    /**
     * Serializes a Double value to JSON, writing whole numbers as integers and fractional values as decimals.
     * This method outputs Double values without decimal points when they represent whole numbers (e.g., 5.0 becomes 5),
     * while preserving decimal representation for fractional values (e.g., 5.5 remains 5.5).
     * Null values are written as JSON null, and infinite values are written as decimals.
     *
     * @param value the Double value to serialize, may be null
     * @param gen the JSON generator used to write the output
     * @param serializers the serialization context
     * @throws JacksonException if an error occurs during serialization
     */
    @Override
    public void serialize(Double value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        // Check if the double is a whole number
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            // Write as integer
            gen.writeNumber(value.longValue());
        } else {
            // Write as decimal
            gen.writeNumber(value);
        }
    }
}
