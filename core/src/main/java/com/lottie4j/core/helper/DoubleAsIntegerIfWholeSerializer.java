package com.lottie4j.core.helper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Jackson serializer that outputs Double values as integers when they represent whole numbers,
 * but preserves decimal representation for fractional values.
 * <p>
 * This is used to match the Lottie JSON specification which typically uses integer frame numbers
 * (e.g., "t": 0) rather than floating point (e.g., "t": 0.0), while still supporting fractional
 * frames when needed.
 */
public class DoubleAsIntegerIfWholeSerializer extends JsonSerializer<Double> {

    @Override
    public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
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
