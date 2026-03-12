package com.lottie4j.core.helper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom serializer for lists of Double values.
 * Serializes the list as a JSON array.
 */
public class ListDoubleSerializer extends JsonSerializer {

    /**
     * Serializes a list of Double values to a JSON array.
     *
     * @param o                  the object to serialize (expected to be a List of Doubles)
     * @param jsonGenerator      the JSON generator
     * @param serializerProvider the serializer provider
     * @throws IOException if serialization fails
     */
    @Override
    public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        String rt = "[" +
                ((List<Double>) o).stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")) +
                "]";
        jsonGenerator.writeRawValue(rt);
    }
}
