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
