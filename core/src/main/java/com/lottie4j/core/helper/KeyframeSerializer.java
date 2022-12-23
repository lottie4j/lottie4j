package com.lottie4j.core.helper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class KeyframeSerializer extends JsonSerializer {

    /**
     * A custom serializer is needed as
     * <ul>
     *     <li>A single value must be written as value</li>
     *     <li>Multiple values must be written as array</li>
     *     <li>Zero value must be written as 0 and not 0.0</li>
     * </ul>
     */
    @Override
    public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (o instanceof List<?> list) {
            if (list.size() == 1 && list.get(0) instanceof Double value) {
                jsonGenerator.writeRawValue(value == 0 ? "0" : value.toString());
            } else {
                jsonGenerator.writeRawValue("[" + list.stream()
                        .map(v -> getNumberValue((double) v))
                        .collect(Collectors.joining(",")) + "]");
            }
        }
    }

    private String getNumberValue(Double value) {
        return Math.round(value) == value ? String.valueOf(Math.round(value)) : String.valueOf(value);
    }
}
